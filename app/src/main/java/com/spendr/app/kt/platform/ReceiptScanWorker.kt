package com.spendr.app.kt.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.spendr.app.kt.BuildConfig
import com.spendr.app.kt.MainActivity
import com.spendr.app.kt.R
import com.spendr.app.kt.data.vision.ReceiptScan
import com.spendr.app.kt.data.vision.ReceiptScanStore
import com.spendr.app.kt.data.vision.VisionApiClient
import com.spendr.app.kt.data.vision.VisionApiException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

class ReceiptScanWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getString("scan_id") ?: return Result.failure()
        val store = ReceiptScanStore(applicationContext)
        var scan = store.get(id) ?: return Result.failure()
        if (!scan.isPending) return Result.success()
        try {
            check(System.currentTimeMillis() - scan.createdAt < 31 * 60_000L) {
                "Receipt scan timed out. Try again."
            }
            val client = VisionApiClient(scan.configuration)
            if (scan.jobId == null) {
                scan = scan.copy(status = "uploading")
                store.updatePending(scan)
                val image = store.imageFile(id)
                val jobId = client.submit(image.readBytes(), scan.mime)
                scan = scan.copy(jobId = jobId, status = "queued")
                store.updatePending(scan)
                image.delete()
            }
            // Short polling batches survive app exits without a long-running service.
            repeat(20) {
                if (store.get(id)?.status == "cancelled") return Result.success()
                val job = client.poll(scan.jobId!!)
                scan = scan.copy(status = job.status, result = job.result, error = job.error)
                store.updatePending(scan)
                if (!scan.isPending) {
                    store.imageFile(id).delete()
                    if (store.get(id)?.status != "cancelled") notifyFinished(scan)
                    return Result.success()
                }
                delay(3_000)
            }
            return Result.retry()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            val terminal = e is VisionApiException && e.status in listOf(400, 401, 403, 404, 413, 415)
            if (!terminal && System.currentTimeMillis() - scan.createdAt < 31 * 60_000L) {
                store.updatePending(scan.copy(status = "reconnecting"))
                return Result.retry()
            }
            scan = scan.copy(status = "failed", error = if (terminal) e.message else "Could not reconnect to the scanner. Try again.")
        } catch (e: Exception) {
            scan = scan.copy(status = "failed", error = e.message ?: "Could not scan the receipt.")
        }
        if (store.get(id)?.status == "cancelled") return Result.success()
        store.updatePending(scan)
        store.imageFile(id).delete()
        notifyFinished(scan)
        return Result.failure()
    }

    private fun notifyFinished(scan: ReceiptScan) {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Receipt scans", NotificationManager.IMPORTANCE_DEFAULT))
        val notifications = NotificationManagerCompat.from(applicationContext)
        if (!notifications.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= 33 && applicationContext.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) return
        val uri = Uri.parse("${BuildConfig.DEEP_LINK_SCHEME}://receipt?scanId=${scan.id}")
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = uri
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(applicationContext, scan.id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL)
            .setSmallIcon(R.drawable.ic_qs_add_spending)
            .setContentTitle(if (scan.status == "succeeded") "Receipt scan ready" else "Receipt scan failed")
            .setContentText(if (scan.status == "succeeded") "Tap to review the merchant, total, and items." else "Tap to review the scan and try again.")
            .setContentIntent(pending).setAutoCancel(true).build()
        try { notifications.notify(scan.id.hashCode(), notification) } catch (_: SecurityException) { }
    }

    private companion object { const val CHANNEL = "receipt_scans" }
}
