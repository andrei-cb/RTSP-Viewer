package com.byteul.rtspviewer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.byteul.*
import com.byteul.Utils.decodeString
import com.byteul.Utils.encodeString
import com.byteul.Utils.parseUrl
import com.byteul.Utils.replacePassword
import com.byteul.rtspviewer.databinding.ActivityEditBinding

class EditActivity : AppCompatActivity() {
    private val binding by lazy { ActivityEditBinding.inflate(layoutInflater) }
    private var streamId: Int = -1
    private val streams by lazy { StreamData.getAll() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initActivity()
    }

    private fun initActivity() {
        streamId = intent.getIntExtra("streamId", -1)
        val stream = StreamData.getById(streamId)
        binding.toolbar.tvToolbarLink.text = getString(R.string.save)
        binding.toolbar.tvToolbarLink.setTextColor(getColor(R.color.files_link))
        if (stream == null) {
            streamId = -1
            binding.toolbar.tvToolbarLabel.text = getString(R.string.cam_add)
            binding.tvDeleteLink.visibility = View.GONE
            binding.tvCopyLink.visibility = View.GONE
            if (StreamData.copyStreamId >= 0) {
                binding.tvPasteLink.visibility = View.VISIBLE
                binding.tvPasteLink.setOnClickListener {
                    paste()
                }
            }
            binding.llChannelBox.layoutParams.height = 0
            binding.rbEditTcp.isChecked = true
        } else {
            binding.toolbar.tvToolbarLabel.text = stream.name

            binding.etEditName.setText(stream.name)
            binding.etEditUrl.setText(safeUrl(stream.url))
            binding.etEditChannel.setText(safeUrl(stream.url2))
            binding.etEditSftpUrl.setText(safeUrl(stream.sftp))
            binding.rbEditTcp.isChecked = stream.tcp
            binding.rbEditUdp.isChecked = !stream.tcp

            binding.tvDeleteLink.setOnClickListener {
                delete()
            }
            binding.tvPasteLink.visibility = View.GONE
            binding.tvCopyLink.visibility = View.VISIBLE
            binding.tvCopyLink.setOnClickListener {
                copy()
            }
            if (stream.url2 == null)
                binding.llChannelBox.layoutParams.height = 0
            else
                binding.tvAddChannel.visibility = View.GONE
        }
        binding.tvAddChannel.setOnClickListener {
            binding.tvAddChannel.visibility = View.GONE
            binding.tvDelChannel.visibility = View.VISIBLE
            binding.etEditChannel.setText(binding.etEditUrl.text.toString().trim())
            Effects.toggle(binding.llChannelBox)
        }
        binding.tvDelChannel.setOnClickListener {
            binding.tvDelChannel.visibility = View.GONE
            binding.tvAddChannel.visibility = View.VISIBLE
            binding.etEditChannel.setText("")
            Effects.toggle(binding.llChannelBox)
        }
        binding.btnSave.setOnClickListener {
            save()
        }
        binding.toolbar.tvToolbarLink.setOnClickListener {
            save()
        }
        binding.toolbar.btnBack.setOnClickListener {
            back()
        }
        this.onBackPressedDispatcher.addCallback(callback)
    }

    private fun safeUrl(url: String?): String {
        return if (url != null) replacePassword(url, "***") else ""
    }

    private fun getEncodedUrl(newUrl: String, oldUrl: String?): String {
        val new = parseUrl(newUrl)
        val old = parseUrl(oldUrl)
        if (new != null && old != null && new.password == "***")
            return replacePassword(newUrl, encodeString(decodeString(old.password)))
        if (new != null && new.password != "")
            return replacePassword(newUrl, encodeString(decodeString(new.password)))
        return newUrl
    }

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (streams.isNotEmpty())
                back()
            else
                finishAffinity()
        }
    }

    private fun save() {
        val oldStream = if (streamId >= 0)
            StreamData.getById(streamId)
        else
            StreamData.getById(StreamData.copyStreamId)

        var streamUrl = binding.etEditUrl.text.toString().trim()
        streamUrl = getEncodedUrl(streamUrl, oldStream?.url)

        var channelUrl = binding.etEditChannel.text.toString().trim()
        val oldChannelUrl = if (oldStream?.url2 != null) oldStream.url2
            else if (oldStream?.url != null) oldStream.url
            else streamUrl
        channelUrl = getEncodedUrl(channelUrl, oldChannelUrl)

        var sftpUrl = binding.etEditSftpUrl.text.toString().trim()
        sftpUrl = getEncodedUrl(sftpUrl, oldStream?.sftp)

        if (!validate(streamUrl, channelUrl))
            return

        val newStream = StreamDataModel(
            binding.etEditName.text.toString().trim(),
            streamUrl,
            if (channelUrl != "") channelUrl else null,
            binding.rbEditTcp.isChecked,
            if (sftpUrl != "") sftpUrl else null
        )
        if (streamId < 0)
            StreamData.add(newStream)
        else
            StreamData.update(streamId, newStream)

        back()
    }

    private fun validate(url: String, channelUrl: String): Boolean {
        val name = binding.etEditName.text.toString().trim()
        var ok = true

        if (name.isEmpty() || name.length > 255) {
            binding.etEditName.error = getString(R.string.err_invalid)
            ok = false
        }
        if (url.isEmpty() || url.length > 255) {
            binding.etEditUrl.error = getString(R.string.err_invalid)
            ok = false
        }
        for (i in streams.indices) {
            if (i == streamId)
                break

            if (streams[i].name == name) {
                binding.etEditName.error = getString(R.string.err_cam_exists)
                ok = false
            }
            if (streams[i].url == url) {
                binding.etEditUrl.error = getString(R.string.err_cam_exists)
                ok = false
            }
        }
        if (channelUrl != "" && channelUrl == url) {
            binding.etEditChannel.error = getString(R.string.err_channels_equal)
            ok = false
        }
        return ok
    }

    private fun delete() {
        AlertDialog.Builder(this)
            .setMessage(R.string.cam_delete)
            .setPositiveButton(R.string.delete) { _, _ ->
                StreamData.delete(streamId)
                back()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .create().show()
    }

    private fun copy() {
        StreamData.copyStreamId = streamId
        Effects.fadeOut(arrayOf(binding.tvCopyLink))
    }

    private fun paste() {
        Effects.fadeOut(arrayOf(binding.tvPasteLink))
        val stream = StreamData.getById(StreamData.copyStreamId) ?: return

        binding.etEditName.setText(stream.name, TextView.BufferType.EDITABLE)
        binding.etEditUrl.setText(safeUrl(stream.url), TextView.BufferType.EDITABLE)
        binding.etEditSftpUrl.setText(safeUrl(stream.sftp), TextView.BufferType.EDITABLE)
        binding.rbEditTcp.isChecked = stream.tcp
        binding.rbEditUdp.isChecked = !stream.tcp
    }

    private fun back() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}