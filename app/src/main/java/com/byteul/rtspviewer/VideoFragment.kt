package com.byteul.rtspviewer

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.byteul.NetworkState
import com.byteul.StreamData
import com.byteul.StreamDataModel
import com.byteul.Utils
import com.byteul.rtspviewer.databinding.FragmentVideoBinding
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import java.io.IOException

class VideoFragment : Fragment() {
    private val binding by lazy { FragmentVideoBinding.inflate(layoutInflater) }

    private lateinit var libVlc: LibVLC
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var stream: StreamDataModel
    private var streamId: Int = -1
    private var isBuffered = false

    companion object {
        /**
         * Factory method to create a new instance of this fragment using the provided parameters
         *
         * @param streamId
         * @return A new instance of this fragment
         */
        @JvmStatic
        fun newInstance(streamId: Int) =
            VideoFragment().apply {
                arguments = Bundle().apply {
                    putInt("streamId", streamId)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            streamId = it.getInt("streamId", -1)
        }
        stream = StreamData.getById(streamId) ?: return
        initFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return binding.root
    }

    private fun initFragment() {
        libVlc = LibVLC(requireContext(), ArrayList<String>().apply {
            if (stream.tcp)
                add("--rtsp-tcp")
            if (!StreamData.logConnections)
                add("--verbose=-1")
        })
        mediaPlayer = MediaPlayer(libVlc)

        observeNetworkState()
    }

    private fun start() {
        try {
            val media = Media(libVlc, Uri.parse(getUrl()))

            media.apply {
                mediaPlayer.media = this
            }.release()

            mediaPlayer.play()
            mediaPlayer.volume = 0

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onStart() {
        super.onStart()

        mediaPlayer.attachViews(binding.videoLayout, null, false, false)
        mediaPlayer.volume = 0
        start()

        mediaPlayer.setEventListener {
            if (it.type == MediaPlayer.Event.Buffering && it.buffering == 100f) {
                binding.pbLoading.visibility = View.GONE
                isBuffered = true
            }
        }
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer.stop()
        mediaPlayer.detachViews()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
        libVlc.release()
    }

    fun stop() {
        mediaPlayer.stop()
    }

    fun play() {
        mediaPlayer.play()
        mediaPlayer.volume = 0
    }

    private fun observeNetworkState() {
        val isLocal = Utils.isUrlLocal(stream.url)
        NetworkState(isLocal).observe(this) { isConnected ->
            if (isConnected) {
                binding.tvAlert.visibility = View.GONE
                if (isBuffered) {
                    mediaPlayer.stop()
                    mediaPlayer.play()
                    mediaPlayer.volume = 0
                }
            } else {
                binding.tvAlert.visibility = View.VISIBLE
                binding.tvAlert.text =
                    if (isLocal) getString(R.string.no_wifi) else getString(R.string.no_internet)
            }
        }
    }

    private fun getUrl(): String {
        val url = if (stream.url2 != null)
            stream.url2!!
        else
            stream.url
        return Utils.getFullUrl(url, 554, "rtsp")
    }
}