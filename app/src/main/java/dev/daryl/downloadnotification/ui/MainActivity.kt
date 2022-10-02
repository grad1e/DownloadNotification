package dev.daryl.downloadnotification.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.daryl.downloadnotification.R
import dev.daryl.downloadnotification.databinding.ActivityMainBinding
import dev.daryl.downloadnotification.services.DownloadService

@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private var binding: ActivityMainBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding?.btnStartDownload?.setOnClickListener {
            startDownloadService()
        }
        binding?.btnStopDownload?.setOnClickListener {
            stopDownloadService()
        }
    }

    private fun startDownloadService() {
        val url = "https://download.samplelib.com/mp4/sample-30s.mp4"
        val intent = Intent(this, DownloadService::class.java).also {
            it.putExtra("url", url)
            it.putExtra("shouldRun", true)
        }
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopDownloadService() {
        val intent = Intent(this, DownloadService::class.java).also {
            it.putExtra("shouldRun", false)
        }
        ContextCompat.startForegroundService(this, intent)
    }

}