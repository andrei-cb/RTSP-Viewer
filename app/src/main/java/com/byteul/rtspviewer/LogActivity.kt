package com.byteul.rtspviewer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.byteul.Effects
import com.byteul.LogAdapter
import com.byteul.StreamData
import com.byteul.rtspviewer.databinding.ActivityLogBinding
import com.byteul.rtspviewer.databinding.LogSettingsBinding
import java.io.BufferedReader
import java.io.InputStreamReader


class LogActivity: AppCompatActivity() {
    private val binding by lazy { ActivityLogBinding.inflate(layoutInflater) }
    private val settingsBinding by lazy { LogSettingsBinding.inflate(layoutInflater) }
    private val lineCountVariants = listOf(10, 50, 100, 500, 1000)

    companion object {
        private var lineCount = 10
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initActivity()
    }

    private fun initActivity() {
        binding.toolbar.tvToolbarLabel.text = getString(R.string.logs)
        binding.toolbar.btnBack.setOnClickListener {
            back()
        }
        this.onBackPressedDispatcher.addCallback(callback)

        binding.toolbar.tvToolbarLink.text = getString(R.string.copy)
        binding.toolbar.tvToolbarLink.setTextColor(getColor(R.color.accent))
        binding.toolbar.tvToolbarLink.setOnClickListener {
            copyToClipboard()
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = LogAdapter(arrayListOf())
        settings()
    }

    private fun initLog() {
        binding.recyclerView.adapter = LogAdapter(getLog())
    }

    private fun settings() {
        val spinner = settingsBinding.spBox
        val spinnerArrayAdapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_dropdown_item,
            lineCountVariants)
        spinner.adapter = spinnerArrayAdapter
        spinner.setSelection(lineCountVariants.indexOf(lineCount))

        settingsBinding.cbLogCons.isChecked = StreamData.logConnections

        AlertDialog.Builder(this)
            .setTitle(R.string.settings)
            .setView(settingsBinding.root)
            .setPositiveButton(R.string.btn_continue) { dialog, _ ->

                lineCount = spinner.selectedItem.toString().toInt()
                StreamData.logConnections = settingsBinding.cbLogCons.isChecked

                dialog.dismiss()
                initLog()
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                back()
            }
            .create().show()
    }

    private fun getLog(): ArrayList<String> {
        val log = arrayListOf<String>()
        try {
            val cmd = "logcat -t $lineCount"
            val process = Runtime.getRuntime().exec(cmd)
            val bufferedReader = BufferedReader(
                InputStreamReader(process.inputStream)
            )
            var line: String
            while (bufferedReader.readLine().also { line = it } != null) {
                log.add(line)
            }
        } catch (e: Exception) {
            Log.e("Log", "Can't read logcat (${e.localizedMessage})")
        }
        return log
    }

    private fun back() {
        val intent = Intent(this, AboutActivity::class.java)
        startActivity(intent)
    }

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            back()
        }
    }

    private fun copyToClipboard() {
        val textToCopy = getLog().joinToString(separator = "\n\n")
        val label = getString(R.string.logs)
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText(label, textToCopy)
        clipboardManager.setPrimaryClip(clipData)
        Effects.fadeOut(arrayOf(binding.toolbar.tvToolbarLink))
    }
}