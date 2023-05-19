package com.byteul.rtspviewer

import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.byteul.Utils
import com.byteul.rtspviewer.databinding.ActivityAboutBinding
import java.text.SimpleDateFormat
import java.util.*

class AboutActivity: AppCompatActivity() {
    private val binding by lazy { ActivityAboutBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initActivity()
    }

    private fun initActivity() {
        binding.toolbar.tvToolbarLabel.text = getString(R.string.about)
        binding.toolbar.btnBack.setOnClickListener {
            back()
        }
        this.onBackPressedDispatcher.addCallback(callback)

        var versionName = ""
        try {
            val info = Utils.getPackageInfo()
            versionName = info.versionName
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val sdf = SimpleDateFormat("yyyy", Locale.getDefault())
        val year = sdf.format(Date())

        binding.tvAbout.text = HtmlCompat.fromHtml(
            getString(R.string.about_text, versionName, year),
            HtmlCompat.FROM_HTML_MODE_COMPACT)
        binding.tvAbout.movementMethod = LinkMovementMethod.getInstance()

        binding.tvLog.setOnClickListener {
            logScreen()
        }
    }

    private fun back() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            back()
        }
    }

    private fun logScreen() {
        val intent = Intent(this, LogActivity::class.java)
        startActivity(intent)
    }
}