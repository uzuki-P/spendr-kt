package com.spendr.app.kt.data.vision

import android.app.Application
import java.net.ServerSocket
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class VisionApiClientTest {
    @Test
    fun scanUsesJobEndpointAndPollsUntilResult() = runBlocking {
        val polls = AtomicInteger()
        val server = ServerSocket(0)
        val worker = Thread {
            repeat(3) {
                server.accept().use { socket ->
                    val input = socket.getInputStream()
                    val request = readLine(input)
                    var contentLength = 0
                    while (true) {
                        val line = readLine(input)
                        if (line.isEmpty()) break
                        if (line.startsWith("Content-Length:", true)) contentLength = line.substringAfter(':').trim().toInt()
                        if (line.startsWith("Authorization:", true)) assertEquals("Bearer test-token", line.substringAfter(':').trim())
                    }
                    val body = ByteArray(contentLength)
                    var offset = 0
                    while (offset < body.size) offset += input.read(body, offset, body.size - offset)
                    val method = request.substringBefore(' ')
                    assertTrue(request.contains("/v1/jobs"))
                    val response = when (method) {
                "POST" -> {
                    val form = body.toString(StandardCharsets.UTF_8)
                    assertTrue(form.contains("name=\"provider\"\r\n\r\ncodex"))
                    assertTrue(form.contains("name=\"model\"\r\n\r\ntest-model"))
                    assertTrue(form.contains("name=\"reasoning_effort\"\r\n\r\nhigh"))
                    """{"id":"123e4567-e89b-12d3-a456-426614174000","status":"queued"}"""
                }
                "GET" -> if (polls.incrementAndGet() == 1) {
                    """{"status":"running"}"""
                } else {
                    """{"status":"succeeded","response":{"result":{"merchant":"Market","total":130000,"items":[{"name":"Rice","paidAmount":125000,"quantity":1}]}}}"""
                }
                else -> error("Unexpected method")
                    }
                    val bytes = response.toByteArray(StandardCharsets.UTF_8)
                    val status = if (method == "POST") "202 Accepted" else "200 OK"
                    socket.getOutputStream().apply {
                        write("HTTP/1.1 $status\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray())
                        write(bytes)
                        flush()
                    }
                }
            }
        }
        worker.start()
        try {
            val config = VisionConfiguration("http://127.0.0.1:${server.localPort}", "test-token", "codex", "test-model", "high")
            val result = VisionApiClient(config.url, config.token, 10, config)
                .analyze(byteArrayOf(1, 2, 3), "image/png")
            assertEquals("Market", result.merchant)
            assertEquals(130000L, result.total)
            assertEquals("Rice", result.items.single().name)
            assertTrue(polls.get() >= 2)
        } finally {
            server.close()
            worker.join(1000)
        }
    }

    private fun readLine(input: java.io.InputStream): String {
        val bytes = mutableListOf<Byte>()
        while (true) {
            val next = input.read()
            if (next == -1 || next == '\n'.code) break
            if (next != '\r'.code) bytes.add(next.toByte())
        }
        return bytes.toByteArray().toString(StandardCharsets.UTF_8)
    }
}
