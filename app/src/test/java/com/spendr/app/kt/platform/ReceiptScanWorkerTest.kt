package com.spendr.app.kt.platform

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.spendr.app.kt.data.vision.ReceiptScan
import com.spendr.app.kt.data.vision.ReceiptScanStore
import com.spendr.app.kt.data.vision.VisionConfiguration
import java.net.ServerSocket
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ReceiptScanWorkerTest {
    @Test
    fun recreatedWorkerResumesExistingJobAndPersistsResult() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val id = UUID.randomUUID().toString()
        val jobId = UUID.randomUUID().toString()
        val store = ReceiptScanStore(context)
        withResponse(200, """{"status":"succeeded","response":{"result":{"merchant":"Market","total":12500,"items":[{"name":"Rice","paidAmount":12500,"quantity":"0.5"}]}}}""") { port ->
            store.put(ReceiptScan(id, null, VisionConfiguration("http://127.0.0.1:$port", "test-token"),
                "image/png", status = "running", jobId = jobId))
            val worker = TestListenableWorkerBuilder<ReceiptScanWorker>(context,
                inputData = workDataOf("scan_id" to id)).build()
            assertEquals(ListenableWorker.Result.success(), worker.doWork())
            val restored = ReceiptScanStore(context).get(id)!!
            assertEquals(jobId, restored.jobId)
            assertEquals("succeeded", restored.status)
            assertEquals("Market", restored.result!!.merchant)
            assertEquals("0.5", restored.result.items.single().quantity)
            assertTrue(!store.imageFile(id).exists())
        }
    }

    @Test
    fun temporaryApiFailureKeepsJobIdForRetry() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val store = ReceiptScanStore(context)
        val id = UUID.randomUUID().toString()
        val jobId = UUID.randomUUID().toString()
        withResponse(503, """{"error":{"message":"Unavailable"}}""") { port ->
            store.put(ReceiptScan(id, null, VisionConfiguration("http://127.0.0.1:$port", "test-token"),
                "image/png", status = "running", jobId = jobId))
            val worker = TestListenableWorkerBuilder<ReceiptScanWorker>(context,
                inputData = workDataOf("scan_id" to id)).build()
            assertEquals(ListenableWorker.Result.retry(), worker.doWork())
            val restored = store.get(id)!!
            assertEquals(jobId, restored.jobId)
            assertEquals("reconnecting", restored.status)
        }
    }

    @Test
    fun cancellingCannotBeOverwrittenByLatePollResult() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val store = ReceiptScanStore(context)
        val scan = ReceiptScan(UUID.randomUUID().toString(), null, VisionConfiguration(), "image/png")
        store.put(scan.copy(status = "cancelled"))
        store.updatePending(scan.copy(status = "running"))
        assertEquals("cancelled", store.get(scan.id)!!.status)
    }

    private suspend fun withResponse(status: Int, response: String, test: suspend (Int) -> Unit) {
        val server = ServerSocket(0).apply { soTimeout = 10_000 }
        var requestLine: String? = null
        val thread = Thread {
            server.accept().use { socket ->
                val reader = socket.getInputStream().bufferedReader()
                requestLine = reader.readLine()
                while (reader.readLine().isNotEmpty()) { }
                val bytes = response.toByteArray()
                socket.getOutputStream().apply {
                    write("HTTP/1.1 $status Test\r\nContent-Length: ${bytes.size}\r\nContent-Type: application/json\r\nConnection: close\r\n\r\n".toByteArray())
                    write(bytes)
                    flush()
                }
            }
        }
        thread.start()
        try { test(server.localPort) } finally {
            thread.join(2000)
            server.close()
        }
        assertNotNull(requestLine)
        assertTrue("Recovery must poll instead of uploading again", requestLine!!.startsWith("GET /v1/jobs/"))
    }
}
