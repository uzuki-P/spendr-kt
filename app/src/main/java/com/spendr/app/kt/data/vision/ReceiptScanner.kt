package com.spendr.app.kt.data.vision

import android.content.Context
import android.net.Uri
import com.spendr.app.kt.BuildConfig
import com.spendr.app.kt.data.repo.ReceiptItemInput
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class ScannedReceipt(
    val merchant: String?,
    val total: Long?,
    val items: List<ReceiptItemInput>,
)

data class VisionConfiguration(
    val url: String = "https://vision-api.ts.uzuki-p.my.id",
    val token: String = BuildConfig.VISION_API_TOKEN,
    val provider: String = "",
    val model: String = "",
    val reasoningEffort: String = "",
) {
    fun validated(requireToken: Boolean = true): VisionConfiguration {
        val parsed = Uri.parse(url.trim())
        require(parsed.scheme == "https" && !parsed.host.isNullOrBlank() && parsed.userInfo == null &&
            parsed.query == null && parsed.fragment == null) { "Enter an HTTPS API address." }
        require((!requireToken || token.trim().isNotEmpty()) && token.trim().all { it.code in 33..126 }) {
            "Enter a valid API token."
        }
        require(provider in listOf("", "opencode", "codex")) { "Choose a supported provider." }
        return copy(url = url.trim().trimEnd('/'), token = token.trim(), model = model.trim(),
            reasoningEffort = reasoningEffort.trim())
    }
}

data class VisionDefaults(val provider: String, val model: String?, val reasoningEffort: String?)
data class VisionModel(val id: String, val label: String, val reasoningEfforts: List<String>)
data class VisionJob(val status: String, val result: ScannedReceipt? = null, val error: String? = null)

internal class VisionApiException(val status: Int, message: String) : java.io.IOException(message)

internal class VisionApiClient(
    private val baseUrl: String,
    private val token: String,
    private val pollIntervalMs: Long = 3_000,
    private val configuration: VisionConfiguration = VisionConfiguration(url = baseUrl, token = token),
) {
    constructor(configuration: VisionConfiguration) : this(configuration.url.trimEnd('/'), configuration.token,
        configuration = configuration)

    suspend fun connection(): VisionDefaults = withContext(Dispatchers.IO) {
        configuration.validated()
        val response = get("/v1/providers")
        VisionDefaults(response.getString("default_provider"), response.optionalString("default_model"),
            response.optionalString("default_reasoning_effort"))
    }

    suspend fun models(provider: String): List<VisionModel> = withContext(Dispatchers.IO) {
        require(provider in listOf("opencode", "codex"))
        val models = get("/v1/models?provider=$provider").getJSONArray("models")
        (0 until models.length()).map { index ->
            val model = models.getJSONObject(index)
            val efforts = model.optJSONArray("reasoning_efforts")
            VisionModel(model.getString("id"), model.optString("label", model.getString("id")),
                (0 until (efforts?.length() ?: 0)).map { efforts!!.getString(it) })
        }
    }

    suspend fun poll(jobId: String): VisionJob = withContext(Dispatchers.IO) {
        require(runCatching { UUID.fromString(jobId) }.isSuccess) { "Invalid scan job ID." }
        val status = get("/v1/jobs/$jobId")
        when (val state = status.optString("status")) {
            "queued", "running" -> VisionJob(state)
            "succeeded" -> VisionJob(state, result = parseResult(status.getJSONObject("response")))
            "failed" -> VisionJob(state, error = status.optJSONObject("error")?.optionalString("message")
                ?: "Receipt scan failed.")
            else -> error("Vision API returned an unknown job status.")
        }
    }

    suspend fun analyze(image: ByteArray, mime: String): ScannedReceipt = withContext(Dispatchers.IO) {
        val jobId = submit(image, mime)
        val deadline = System.nanoTime() + 31L * 60 * 1_000_000_000
        while (System.nanoTime() < deadline) {
            delay(pollIntervalMs)
            val status = get("/v1/jobs/$jobId")
            when (status.optString("status")) {
                "queued", "running" -> continue
                "succeeded" -> return@withContext parseResult(status.getJSONObject("response"))
                "failed" -> error(status.optJSONObject("error")?.optString("message")
                    ?.takeIf { it.isNotBlank() } ?: "Receipt scan failed.")
                else -> error("Vision API returned an unknown job status.")
            }
        }
        error("Receipt scan timed out. Try again later.")
    }

    suspend fun submit(image: ByteArray, mime: String): String = withContext(Dispatchers.IO) {
        val boundary = "spendr-${UUID.randomUUID()}"
        val connection = URL("$baseUrl/v1/jobs").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 5_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            connection.outputStream.use { output ->
                fun write(value: String) = output.write(value.toByteArray(Charsets.UTF_8))
                write("--$boundary\r\nContent-Disposition: form-data; name=\"instruction\"\r\n\r\n")
                write("Read this supermarket receipt. Return only a JSON object with merchant (string or null), total (integer rupiah or null), and items (array of objects with name, paidAmount as integer line total in rupiah, and quantity as positive number, including fractional quantities). Do not invent unreadable values. Ignore subtotal, tax, discounts, change, payment, and total lines as items.\r\n")
                mapOf("provider" to configuration.provider, "model" to configuration.model,
                    "reasoning_effort" to configuration.reasoningEffort).forEach { (name, value) ->
                    if (value.isNotBlank()) {
                        write("--$boundary\r\nContent-Disposition: form-data; name=\"$name\"\r\n\r\n$value\r\n")
                    }
                }
                write("--$boundary\r\nContent-Disposition: form-data; name=\"image\"; filename=\"receipt\"\r\nContent-Type: $mime\r\n\r\n")
                output.write(image)
                write("\r\n--$boundary--\r\n")
            }
            val response = readResponse(connection, 202)
            response.getString("id").also { require(runCatching { UUID.fromString(it) }.isSuccess) { "Invalid scan job ID." } }
        } finally {
            connection.disconnect()
        }
    }

    private fun get(path: String): JSONObject {
        val connection = URL("$baseUrl$path").openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 5_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Authorization", "Bearer $token")
            return readResponse(connection, 200)
        } finally {
            connection.disconnect()
        }
    }

    private fun readResponse(connection: HttpURLConnection, expectedStatus: Int): JSONObject {
        val status = connection.responseCode
        val body = (if (status == expectedStatus) connection.inputStream else connection.errorStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (status != expectedStatus) {
            val message = runCatching { JSONObject(body).getJSONObject("error").getString("message") }.getOrNull()
            throw VisionApiException(status, message ?: "Vision API returned HTTP $status.")
        }
        return JSONObject(body)
    }

    private fun parseResult(response: JSONObject): ScannedReceipt {
        val result = response.getJSONObject("result")
        val parsedItems = result.optJSONArray("items")
        return ScannedReceipt(
            merchant = result.optString("merchant").takeIf { it.isNotBlank() && it != "null" },
            total = result.optLong("total").takeIf { it > 0 },
            items = (0 until (parsedItems?.length() ?: 0)).mapNotNull { index ->
                val item = parsedItems!!.optJSONObject(index) ?: return@mapNotNull null
                val name = item.optString("name").trim()
                val amount = item.optLong("paidAmount", -1)
                if (name.isEmpty() || amount < 0) null else ReceiptItemInput(
                    name = name,
                    paidAmount = amount,
                    quantity = item.optString("quantity", "1").takeIf { it.toBigDecimalOrNull()?.signum() == 1 } ?: "1",
                )
            },
        )
    }
}

private fun JSONObject.optionalString(name: String) = optString(name).takeIf { it.isNotBlank() && it != "null" }
