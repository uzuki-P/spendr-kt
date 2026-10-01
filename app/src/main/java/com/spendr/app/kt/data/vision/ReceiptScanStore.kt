package com.spendr.app.kt.data.vision

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.spendr.app.kt.data.repo.ReceiptItemInput
import com.spendr.app.kt.platform.ReceiptScanWorker
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private val Context.receiptScanStore by preferencesDataStore(name = "receipt_scans")

internal data class ReceiptScan(
    val id: String,
    val transactionId: Long?,
    val configuration: VisionConfiguration,
    val mime: String,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "waiting",
    val jobId: String? = null,
    val result: ScannedReceipt? = null,
    val error: String? = null,
    val saved: Boolean = false,
) {
    val isPending get() = status in setOf("waiting", "uploading", "queued", "running", "reconnecting")
}

class ReceiptScanStore(
    private val context: Context,
    private val store: DataStore<Preferences> = context.applicationContext.receiptScanStore,
) {
    internal val scans = store.data.map { preferences ->
        preferences.asMap().filterKeys { it.name.startsWith("scan_") }.values
            .mapNotNull { value -> runCatching { decode(JSONObject(value as String)) }.getOrNull() }
            .sortedByDescending { it.createdAt }
    }

    internal suspend fun get(id: String) = scans.first().firstOrNull { it.id == id }
    internal suspend fun put(scan: ReceiptScan) = store.edit {
        it[stringPreferencesKey("scan_${scan.id}")] = encode(scan).toString()
    }
    internal suspend fun updatePending(scan: ReceiptScan) = store.edit {
        val key = stringPreferencesKey("scan_${scan.id}")
        val current = it[key]?.let { value -> decode(JSONObject(value)) }
        if (current?.isPending == true) it[key] = encode(scan).toString()
    }
    internal suspend fun markSaved(id: String) { get(id)?.let { put(it.copy(saved = true)) } }

    internal fun imageFile(id: String): File {
        require(runCatching { UUID.fromString(id) }.isSuccess)
        return File(context.filesDir, "receipt_scans/$id.image")
    }

    suspend fun start(uri: Uri, configuration: VisionConfiguration, transactionId: Long?): String =
        withContext(Dispatchers.IO) {
            val config = configuration.validated()
            // Check credentials and reachability again before accepting an image.
            VisionApiClient(config).connection()
            val mime = when (val type = context.contentResolver.getType(uri)) {
                "image/jpg", null -> "image/jpeg"
                else -> type
            }
            require(mime in setOf("image/jpeg", "image/png", "image/webp", "image/gif")) {
                "Choose a JPEG, PNG, WebP, or GIF image."
            }
            val id = UUID.randomUUID().toString()
            val image = imageFile(id)
            image.parentFile!!.mkdirs()
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    image.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var total = 0L
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            require(total <= 12_582_912) { "Receipt image must be under 12 MB." }
                            output.write(buffer, 0, count)
                        }
                        require(total > 0) { "The receipt image is empty." }
                    }
                } ?: error("Could not read the receipt image.")
                put(ReceiptScan(id, transactionId, config, mime))
                val request = OneTimeWorkRequestBuilder<ReceiptScanWorker>()
                    .setInputData(workDataOf("scan_id" to id))
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
                    .build()
                WorkManager.getInstance(context).enqueueUniqueWork("receipt-scan-$id", ExistingWorkPolicy.KEEP, request)
                id
            } catch (e: Exception) {
                image.delete()
                store.edit { it.remove(stringPreferencesKey("scan_$id")) }
                throw e
            }
        }

    internal suspend fun cancel(id: String) {
        WorkManager.getInstance(context).cancelUniqueWork("receipt-scan-$id")
        get(id)?.let { put(it.copy(status = "cancelled", error = "Scan checks stopped. Add the receipt manually or choose another image.")) }
        imageFile(id).delete()
    }
}

private fun encode(scan: ReceiptScan) = JSONObject().apply {
    put("id", scan.id)
    put("transactionId", scan.transactionId)
    put("createdAt", scan.createdAt)
    put("status", scan.status)
    put("jobId", scan.jobId)
    put("mime", scan.mime)
    put("error", scan.error)
    put("saved", scan.saved)
    put("configuration", JSONObject().apply {
        put("url", scan.configuration.url)
        put("token", scan.configuration.token)
        put("provider", scan.configuration.provider)
        put("model", scan.configuration.model)
        put("reasoningEffort", scan.configuration.reasoningEffort)
    })
    scan.result?.let { result ->
        put("result", JSONObject().apply {
            put("merchant", result.merchant)
            put("total", result.total)
            put("items", JSONArray().apply {
                result.items.forEach { item -> put(JSONObject().apply {
                    put("name", item.name)
                    put("paidAmount", item.paidAmount)
                    put("quantity", item.quantity)
                }) }
            })
        })
    }
}

private fun decode(json: JSONObject): ReceiptScan {
    val config = json.getJSONObject("configuration")
    val result = json.optJSONObject("result")?.let { data ->
        val items = data.getJSONArray("items")
        ScannedReceipt(data.stringOrNull("merchant"), data.optLong("total").takeIf { it > 0 },
            (0 until items.length()).map { index ->
                val item = items.getJSONObject(index)
                ReceiptItemInput(item.getString("name"), item.getLong("paidAmount"), item.getString("quantity"))
            })
    }
    return ReceiptScan(
        id = json.getString("id"), transactionId = json.optLong("transactionId").takeIf { it > 0 },
        configuration = VisionConfiguration(config.getString("url"), config.getString("token"),
            config.optString("provider"), config.optString("model"), config.optString("reasoningEffort")),
        mime = json.getString("mime"), createdAt = json.getLong("createdAt"), status = json.getString("status"),
        jobId = json.stringOrNull("jobId"), result = result, error = json.stringOrNull("error"),
        saved = json.optBoolean("saved"),
    )
}

private fun JSONObject.stringOrNull(name: String) = optString(name).takeIf { it.isNotBlank() && it != "null" }
