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

class ReceiptScanner(private val context: Context) {
    suspend fun scan(uri: Uri): ScannedReceipt = withContext(Dispatchers.IO) {
        check(BuildConfig.VISION_API_TOKEN.isNotBlank()) { "Vision API token is unavailable in this build." }
        val mime = when (val reported = context.contentResolver.getType(uri)) {
            "image/jpg" -> "image/jpeg"
            null -> "image/jpeg"
            else -> reported
        }
        require(mime in setOf("image/jpeg", "image/png", "image/webp", "image/gif")) {
            "Choose a JPEG, PNG, WebP, or GIF image."
        }
        val image = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Could not read the receipt image.")
        require(image.size <= 12_582_912) { "Receipt image must be under 12 MB." }
        VisionApiClient(BuildConfig.VISION_API_URL, BuildConfig.VISION_API_TOKEN).analyze(image, mime)
    }
}

internal class VisionApiClient(
    private val baseUrl: String,
    private val token: String,
    private val pollIntervalMs: Long = 3_000,
) {
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

    private fun submit(image: ByteArray, mime: String): String {
        val boundary = "spendr-${UUID.randomUUID()}"
        val connection = URL("$baseUrl/v1/jobs").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            connection.outputStream.use { output ->
                fun write(value: String) = output.write(value.toByteArray(Charsets.UTF_8))
                write("--$boundary\r\nContent-Disposition: form-data; name=\"instruction\"\r\n\r\n")
                write("Read this supermarket receipt. Return only a JSON object with merchant (string or null), total (integer rupiah or null), and items (array of objects with name, paidAmount as integer line total in rupiah, and quantity as positive number, including fractional quantities). Do not invent unreadable values. Ignore subtotal, tax, discounts, change, payment, and total lines as items.\r\n")
                write("--$boundary\r\nContent-Disposition: form-data; name=\"image\"; filename=\"receipt\"\r\nContent-Type: $mime\r\n\r\n")
                output.write(image)
                write("\r\n--$boundary--\r\n")
            }
            val response = readResponse(connection, 202)
            return response.getString("id")
        } finally {
            connection.disconnect()
        }
    }

    private fun get(path: String): JSONObject {
        val connection = URL("$baseUrl$path").openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
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
            error(message ?: "Vision API returned HTTP $status.")
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
