package com.example.qceqapp.uis.QCInspection

import android.net.Uri
import android.view.SurfaceHolder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.MediaController
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.VideoView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.qceqapp.R
import kotlinx.coroutines.*
import java.io.File

class VideoMediaPagerAdapter(
    private val onDelete: (Int) -> Unit,
    private val boxIdToInspect: String,
    private val orderNum: String,
    private val scope: CoroutineScope
) : ListAdapter<String, VideoMediaPagerAdapter.VideoViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video_pager, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val guid = getItem(position)
        holder.bind(guid, position, itemCount, boxIdToInspect, orderNum, onDelete, scope)
    }

    class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val videoView: VideoView = view.findViewById(R.id.videoView)
        private val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteVideo)
        private val progressBar: ProgressBar = view.findViewById(R.id.progressBarVideo)
        private val tvVideoPosition: TextView = view.findViewById(R.id.tvVideoPosition)

        private var currentTempFile: File? = null
        private var loadingJob: Job? = null
        private var pendingVideoUri: Uri? = null

        fun bind(
            guid: String,
            position: Int,
            totalItems: Int,
            boxIdToInspect: String,
            orderNum: String,
            onDelete: (Int) -> Unit,
            scope: CoroutineScope
        ) {
            loadingJob?.cancel()
            pendingVideoUri = null

            videoView.stopPlayback()
            videoView.setVideoURI(null)
            currentTempFile?.delete()
            currentTempFile = null

            tvVideoPosition.text = "${position + 1}/$totalItems"

            progressBar.visibility = View.VISIBLE
            videoView.visibility = View.VISIBLE
            videoView.setBackgroundColor(android.graphics.Color.BLACK)


            loadingJob = scope.launch {
                try {
                    val service = com.example.qceqapp.data.network.Service()

                    val startTime = System.currentTimeMillis()

                    val result = withTimeout(30000L) {
                        withContext(Dispatchers.IO) {
                            service.getFilesByBoxAndOrder(guid, orderNum, boxIdToInspect)
                        }
                    }

                    val downloadTime = System.currentTimeMillis() - startTime

                    if (result.isSuccess) {
                        val videoBytes = result.getOrNull()
                        if (videoBytes != null && videoBytes.isNotEmpty()) {
                            val sizeInMB = videoBytes.size / (1024.0 * 1024.0)
                            android.util.Log.d("VideoView", "Video downloaded: ${videoBytes.size} bytes (${"%.2f".format(sizeInMB)} MB)")

                            val header = videoBytes.take(12).toByteArray()
                            val headerHex = header.joinToString(" ") { "%02x".format(it) }
                            android.util.Log.d("VideoView", "File header: $headerHex")

                            val tempFile = withContext(Dispatchers.IO) {
                                try {
                                    File(videoView.context.cacheDir, "video_$guid").apply {
                                        if (exists()) delete()
                                        writeBytes(videoBytes)
                                        android.util.Log.d("VideoView", "Temp file: $absolutePath (${length()} bytes)")
                                    }
                                } catch (e: Exception) {
                                    null
                                }
                            }

                            if (tempFile != null && tempFile.exists() && tempFile.length() > 0) {
                                currentTempFile = tempFile

                                withContext(Dispatchers.Main) {
                                    try {
                                        val uri = Uri.fromFile(tempFile)
                                        setupVideoWhenSurfaceReady(uri, scope)
                                    } catch (e: Exception) {
                                        showError("Setup error")
                                    }
                                }
                            } else {
                                showError("Invalid file")
                            }
                        } else {
                            showError("Empty data")
                        }
                    } else {
                        val error = result.exceptionOrNull()?.message ?: "Unknown error"
                        showError("Download failed")
                    }

                } catch (e: TimeoutCancellationException) {
                    showError("Download timeout")
                } catch (e: CancellationException) {
                    android.util.Log.d("VideoView", "Loading cancelled")
                } catch (e: Exception) {
                    showError("Error: ${e.message}")
                }
            }

            btnDelete.setOnClickListener {
                loadingJob?.cancel()
                videoView.stopPlayback()
                videoView.setVideoURI(null)
                currentTempFile?.delete()
                currentTempFile = null
                onDelete(position)
            }
        }

        private fun setupVideoWhenSurfaceReady(uri: Uri, scope: CoroutineScope) {
            val holder = videoView.holder

            if (holder.surface != null && holder.surface.isValid) {
                loadVideoToView(uri, scope)
            } else {
                pendingVideoUri = uri

                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        holder.removeCallback(this)

                        if (pendingVideoUri != null) {
                            loadVideoToView(pendingVideoUri!!, scope)
                            pendingVideoUri = null
                        }
                    }

                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                    }

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                    }
                })
            }
        }

        private fun loadVideoToView(uri: Uri, scope: CoroutineScope) {

            var isPrepared = false
            val prepareTimeout = scope.launch {
                delay(10000L)
                if (!isPrepared) {
                    showError("Timeout")
                }
            }

            videoView.setVideoURI(uri)

            val controller = MediaController(videoView.context)
            controller.setAnchorView(videoView)
            videoView.setMediaController(controller)

            videoView.setOnPreparedListener { mediaPlayer ->
                isPrepared = true
                prepareTimeout.cancel()
                progressBar.visibility = View.GONE
                videoView.setBackgroundColor(android.graphics.Color.TRANSPARENT)

                try {
                    videoView.start()
                } catch (e: Exception) {
                    android.util.Log.e("VideoView", "Error starting video", e)
                }
                videoView.requestLayout()
                videoView.invalidate()
            }

            videoView.setOnErrorListener { _, what, extra ->
                isPrepared = true
                prepareTimeout.cancel()

                val errorMsg = when (what) {
                    android.media.MediaPlayer.MEDIA_ERROR_UNKNOWN -> "Unknown"
                    android.media.MediaPlayer.MEDIA_ERROR_SERVER_DIED -> "Server died"
                    else -> "Code $what"
                }

                val extraMsg = when (extra) {
                    android.media.MediaPlayer.MEDIA_ERROR_IO -> "IO error"
                    android.media.MediaPlayer.MEDIA_ERROR_MALFORMED -> "Malformed"
                    android.media.MediaPlayer.MEDIA_ERROR_UNSUPPORTED -> "Unsupported"
                    android.media.MediaPlayer.MEDIA_ERROR_TIMED_OUT -> "Timed out"
                    else -> "Extra $extra"
                }

                showError("Cannot play")
                true
            }

            videoView.setOnInfoListener { _, what, extra ->
                when (what) {
                    android.media.MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START -> {
                    }
                    android.media.MediaPlayer.MEDIA_INFO_BUFFERING_START -> {
                    }
                    android.media.MediaPlayer.MEDIA_INFO_BUFFERING_END -> {
                    }
                }
                false
            }
        }

        private fun showError(message: String) {
            progressBar.visibility = View.GONE
            videoView.visibility = View.VISIBLE
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String) = oldItem == newItem
        override fun areContentsTheSame(oldItem: String, newItem: String) = oldItem == newItem
    }
}