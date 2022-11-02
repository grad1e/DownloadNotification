package dev.daryl.downloadnotification.services

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.format.Formatter
import android.widget.Toast
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.daryl.downloadnotification.BuildConfig
import dev.daryl.downloadnotification.R
import dev.daryl.downloadnotification.data.repo.AppRepository
import dev.daryl.downloadnotification.utils.network.Resource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.onStart
import okhttp3.ResponseBody
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class DownloadService : LifecycleService() {

    @Inject
    lateinit var repository: AppRepository

    private val notificationChannel by lazy {
        NotificationChannelCompat.Builder(
            NOTIFICATION_CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_DEFAULT
        ).setDescription(NOTIFICATION_CHANNEL_DESC)
            .setVibrationEnabled(false)
            .setName(NOTIFICATION_CHANNEL_NAME)
            .build()
    }

    private val notificationManager by lazy {
        NotificationManagerCompat.from(this)
    }

    private val cancelButtonAction by lazy {
        val intent = Intent(this, DownloadService::class.java).also {
            it.putExtra("shouldRun", false)
        }

        val pendingIntentFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        NotificationCompat.Action.Builder(
            R.drawable.ic_launcher_foreground,
            "Cancel",
            PendingIntent.getService(
                this,
                0,
                intent,
                pendingIntentFlag
            )
        ).setAllowGeneratedReplies(false)
            .setContextual(false)
            .build()
    }

    private val notification by lazy {
        NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Downloading...")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(cancelButtonAction)
    }

    var file: File? = null
    var deleteFile = true
    var downloadJob: Job? = null
    var downloadTick = 0L

    override fun onCreate() {
        super.onCreate()
        notificationManager.createNotificationChannel(notificationChannel)
        startForeground(NOTIFICATION_ID, notification.build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val shouldRun = intent?.getBooleanExtra("shouldRun", false)
        if (shouldRun == true) {
            downloadJob = lifecycleScope.launch {
                val url = intent.getStringExtra("url")
                startDownload(url)
                Toast.makeText(this@DownloadService, "Download complete", Toast.LENGTH_LONG).show()
                stopSelf()
            }
        } else {
            downloadJob?.cancel()
            notificationManager.cancelAll()
            stopForeground(true)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private suspend fun startDownload(url: String?) = withContext(Dispatchers.IO) {
        repository.getVideoSuspended(url)
            .onStart {
                println("loading getVideoSuspended")
            }.collect {
                when (it) {
                    is Resource.Error -> println("${it.errorMessage} ${it.data}")
                    is Resource.Loading -> println("loading getVideoSuspended")
                    is Resource.Success -> saveToAppStorage(url, it.data)
                }
            }
    }

    private suspend fun saveToAppStorage(url: String?, data: ResponseBody?) =
        withContext(Dispatchers.IO) {
            try {
                val linkUri = Uri.parse(url)
                val fileName = linkUri.lastPathSegment

                val fileDownloadDir = File(this@DownloadService.filesDir, "downloads")
                if (!fileDownloadDir.exists()) {
                    fileDownloadDir.mkdir()
                }

                file = File(fileDownloadDir.path, fileName!!)
                data?.byteStream()?.buffered()?.use { inputStream ->
                    file?.outputStream()?.use { outputStream ->
                        val totalBytes = data.contentLength()
                        val data = ByteArray(8_192)
                        var progressBytes = 0L

                        while (isActive) {
                            val bytes = inputStream.read(data)

                            if (bytes == -1) {
                                break
                            }

                            outputStream.write(data, 0, bytes)
                            progressBytes += bytes
                            updateProgress(
                                totalBytes,
                                progressBytes
                            )
                        }
                        if (totalBytes == progressBytes) {
                            deleteFile = false
                        }
                    }
                }
            } catch (e: Exception) {
                file?.delete()
                println(e.message)
            } finally {
                if (deleteFile)
                    file?.delete()
            }
        }

    private fun updateProgress(
        totalBytes: Long,
        currentBytes: Long
    ) {
        val currentTick = System.currentTimeMillis()
        if (currentTick > downloadTick + 1000) {
            println(currentBytes)
            val totalBytesReadable = Formatter.formatShortFileSize(this, totalBytes)
            val currentBytesReadable = Formatter.formatShortFileSize(this, currentBytes)
            notification.setContentText("$currentBytesReadable / $totalBytesReadable")
            notificationManager.notify(NOTIFICATION_ID, notification.build())

            notification.setProgress(
                ((totalBytes * 100) / totalBytes).toInt(),
                ((currentBytes * 100) / totalBytes).toInt(),
                false
            )
            downloadTick = currentTick
        }
    }

    override fun onDestroy() {
        downloadJob?.cancel()
        notificationManager.cancelAll()
        stopForeground(true)
        if (deleteFile)
            file?.delete()
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID =
            "${BuildConfig.APPLICATION_ID}.notification.channel"
        private const val NOTIFICATION_CHANNEL_DESC = "Shows the notification while downloading"
        private const val NOTIFICATION_CHANNEL_NAME = "Downloads"
        private const val NOTIFICATION_ID = 69
    }
}