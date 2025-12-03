package com.example.qceqapp.uis.QCInspection

import android.graphics.BitmapFactory
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.qceqapp.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MediaPagerAdapter(
    private val onDelete: (Int) -> Unit,
    private val boxIdToInspect: String,
    private val orderNum: String,
    private val scope: CoroutineScope
) : ListAdapter<String, MediaPagerAdapter.MediaViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_media_pager, parent, false)
        return MediaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val guid = getItem(position)
        holder.bind(guid, position, currentList.size, boxIdToInspect, orderNum, onDelete, scope)
    }

    class MediaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imageView: ImageView = view.findViewById(R.id.imageView)
        private val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
        private val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
        private val tvPhotoPosition: TextView = view.findViewById(R.id.tvPhotoPosition)

        fun bind(
            guid: String,
            position: Int,
            totalItems: Int,
            boxIdToInspect: String,
            orderNum: String,
            onDelete: (Int) -> Unit,
            scope: CoroutineScope
        ) {
            tvPhotoPosition.text = "${position + 1}/$totalItems"
            progressBar.visibility = View.VISIBLE
            imageView.setImageDrawable(null)
            android.util.Log.d("MediaPagerAdapter", "=== LOADING IMAGE ===")
            android.util.Log.d("MediaPagerAdapter", "guid: $guid")
            android.util.Log.d("MediaPagerAdapter", "orderNum: $orderNum")
            android.util.Log.d("MediaPagerAdapter", "boxIdToInspect: $boxIdToInspect")

            scope.launch {
                try {
                    val service = com.example.qceqapp.data.network.Service()

                    android.util.Log.d("MediaPagerAdapter", "📞 Calling API...")

                    val result = withContext(Dispatchers.IO) {
                        service.getFilesByBoxAndOrder(guid, orderNum, boxIdToInspect)
                    }

                    android.util.Log.d("MediaPagerAdapter", "result.isSuccess: ${result.isSuccess}")

                    if (result.isSuccess) {
                        val imageBytes = result.getOrNull()
                        if (imageBytes != null) {
                            android.util.Log.d("MediaPagerAdapter", "✅ imageBytes size: ${imageBytes.size}")
                            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            if (bitmap != null) {
                                android.util.Log.d("MediaPagerAdapter", "✅ Bitmap created: ${bitmap.width}x${bitmap.height}")
                                imageView.setImageBitmap(bitmap)
                            } else {
                                android.util.Log.e("MediaPagerAdapter", "❌ Bitmap is NULL after decode")
                                showError()
                            }
                        } else {
                            android.util.Log.e("MediaPagerAdapter", "❌ imageBytes is null")
                            showError()
                        }
                    } else {
                        android.util.Log.e("MediaPagerAdapter", "❌ API call failed")
                        val exception = result.exceptionOrNull()
                        if (exception != null) {
                            android.util.Log.e("MediaPagerAdapter", "❌ Exception: ${exception.message}", exception)
                        }
                        showError()
                    }

                } catch (e: Exception) {
                    android.util.Log.e("MediaPagerAdapter", "❌ Exception loading image", e)
                    showError()
                } finally {
                    progressBar.visibility = View.GONE
                }
            }

            btnDelete.setOnClickListener {
                onDelete(position)
            }
        }

        private fun showError() {
            progressBar.visibility = View.GONE
            imageView.setImageResource(R.drawable.image_error)
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String) = oldItem == newItem
        override fun areContentsTheSame(oldItem: String, newItem: String) = oldItem == newItem
    }
}
