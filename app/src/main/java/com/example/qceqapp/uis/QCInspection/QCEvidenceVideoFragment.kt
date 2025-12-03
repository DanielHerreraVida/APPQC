package com.example.qceqapp.uis.QCInspection

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.qceqapp.R
import com.example.qceqapp.utils.PermissionHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class QCEvidenceVideoFragment : Fragment() {

    private val viewModel: QCMediaViewModel by activityViewModels()

    private var viewPager: ViewPager2? = null
    private var counter: TextView? = null
    private var btnPrev: ImageButton? = null
    private var btnNext: ImageButton? = null
    private var adapter: VideoMediaPagerAdapter? = null
    private var progressBar: ProgressBar? = null
    private var tempVideoFile: File? = null

    // ✅ Launcher para solicitar permisos
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }

        if (allGranted) {
            // Todos los permisos concedidos, abrir cámara
            captureVideoWithoutSavingToGallery()
        } else {
            // Algunos permisos fueron denegados
            Toast.makeText(
                requireContext(),
                "Camera and storage permissions are required to record videos",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val captureVideo = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            tempVideoFile?.let { file ->
                try {
                    if (file.exists() && file.length() > 0) {
                        val uri = FileProvider.getUriForFile(
                            requireContext(),
                            "${requireContext().packageName}.provider",
                            file
                        )
                        uploadVideoToServer(uri)
                    } else {
                        Toast.makeText(requireContext(), "Video capture failed - file is empty", Toast.LENGTH_SHORT).show()
                        file.delete()
                        tempVideoFile = null
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    file.delete()
                    tempVideoFile = null
                }
            } ?: run {
                Toast.makeText(requireContext(), "Error: No video file reference", Toast.LENGTH_SHORT).show()
            }
        } else {
            tempVideoFile?.delete()
            tempVideoFile = null
        }
    }

    private val selectVideos = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uploadMultipleVideosToServer(uris)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_qc_evidence_video, container, false)

        val btnVideo = view.findViewById<Button>(R.id.btn_Video)
        val btnFiles = view.findViewById<Button>(R.id.btn_VFiles)
        val btnContinue = view.findViewById<Button>(R.id.btn_Continue)
        val btnDeleteAll = view.findViewById<ImageButton>(R.id.btn_DeleteVLst)

        counter = view.findViewById(R.id.tV_TotalVideos)
        viewPager = view.findViewById(R.id.viewPagerVideos)
        btnPrev = view.findViewById(R.id.btnPrevVideo)
        btnNext = view.findViewById(R.id.btnNextVideo)
        progressBar = view.findViewById(R.id.progressBar)

        arguments?.let { bundle ->
            viewModel.boxIdToInspect = bundle.getString("boxIdToInspect", "")
            viewModel.orderNum = bundle.getString("orderNum", "")

            val videosJson = bundle.getString("listVideosCaptured")
            if (!videosJson.isNullOrEmpty()) {
                val gson = Gson()
                val type = object : TypeToken<MutableList<String>>() {}.type
                val videosList: MutableList<String> = gson.fromJson(videosJson, type)
                viewModel.videoPaths.postValue(videosList)
            }
        }

        adapter = VideoMediaPagerAdapter(
            onDelete = { position ->
                showDeleteSingleVideoDialog(position)
            },
            boxIdToInspect = viewModel.boxIdToInspect,
            orderNum = viewModel.orderNum,
            scope = viewLifecycleOwner.lifecycleScope
        )
        viewPager?.adapter = adapter

        viewPager?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateArrowButtons(position)
            }
        })

        // ✅ Verificar permisos antes de grabar video
        btnVideo.setOnClickListener {
            checkPermissionsAndCaptureVideo()
        }

        // ✅ Verificar permisos antes de seleccionar archivos
        btnFiles.setOnClickListener {
            checkPermissionsAndSelectFiles()
        }

        btnContinue.setOnClickListener {
            (activity as? QCMediaActivity)?.onContinue()
        }

        btnDeleteAll.setOnClickListener {
            showDeleteAllVideosDialog()
        }

        btnPrev?.setOnClickListener {
            val total = viewModel.videoPaths.value?.size ?: 0
            if (total > 0) {
                val currentItem = viewPager?.currentItem ?: 0
                if (currentItem > 0) {
                    viewPager?.currentItem = currentItem - 1
                } else {
                    viewPager?.currentItem = total - 1
                }
            }
        }

        btnNext?.setOnClickListener {
            val total = viewModel.videoPaths.value?.size ?: 0
            if (total > 0) {
                val currentItem = viewPager?.currentItem ?: 0
                if (currentItem < total - 1) {
                    viewPager?.currentItem = currentItem + 1
                } else {
                    viewPager?.currentItem = 0
                }
            }
        }

        viewModel.videoPaths.observe(viewLifecycleOwner) {
            updateUI()
        }

        return view
    }

    // ✅ Verificar permisos antes de capturar video
    private fun checkPermissionsAndCaptureVideo() {
        when {
            PermissionHelper.hasAllPermissions(requireContext()) -> {
                // Ya tiene todos los permisos
                captureVideoWithoutSavingToGallery()
            }
            shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA) -> {
                // Mostrar explicación
                AlertDialog.Builder(requireContext())
                    .setTitle("Camera Permission Required")
                    .setMessage("Camera permission is needed to record videos for inspection. Please grant the permission to continue.")
                    .setPositiveButton("OK") { _, _ ->
                        requestPermissions()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            else -> {
                // Solicitar permisos directamente
                requestPermissions()
            }
        }
    }

    // ✅ Verificar permisos antes de seleccionar archivos
    private fun checkPermissionsAndSelectFiles() {
        when {
            PermissionHelper.hasAllPermissions(requireContext()) -> {
                // Ya tiene todos los permisos
                selectVideos.launch("video/*")
            }
            else -> {
                // Solicitar permisos
                Toast.makeText(
                    requireContext(),
                    "Storage permissions are required to select videos",
                    Toast.LENGTH_LONG
                ).show()
                requestPermissions()
            }
        }
    }

    // ✅ Solicitar permisos
    private fun requestPermissions() {
        requestPermissionsLauncher.launch(PermissionHelper.getRequiredPermissions())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tempVideoFile?.delete()
        tempVideoFile = null

        viewPager = null
        counter = null
        btnPrev = null
        btnNext = null
        adapter = null
        progressBar = null
    }

    private fun captureVideoWithoutSavingToGallery() {
        try {
            tempVideoFile?.delete()
            tempVideoFile = File(requireContext().cacheDir, "temp_video_${System.currentTimeMillis()}.mp4")

            val videoUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                tempVideoFile!!
            )

            val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, videoUri)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                captureVideo.launch(intent)
            } else {
                Toast.makeText(requireContext(), "No camera app found", Toast.LENGTH_SHORT).show()
                tempVideoFile?.delete()
                tempVideoFile = null
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error starting camera: ${e.message}", Toast.LENGTH_SHORT).show()
            tempVideoFile?.delete()
            tempVideoFile = null
        }
    }

    // ... resto de métodos sin cambios (showDeleteSingleVideoDialog, deleteSingleVideo, etc.)

    private fun showDeleteSingleVideoDialog(position: Int) {
        if (!isAdded) return

        AlertDialog.Builder(requireContext())
            .setTitle("Delete Video")
            .setMessage("Are you sure you want to delete this video?")
            .setPositiveButton("Delete") { _, _ ->
                deleteSingleVideo(position)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteSingleVideo(position: Int) {
        val list = viewModel.videoPaths.value?.toMutableList() ?: mutableListOf()

        if (position in list.indices) {
            val currentPosition = viewPager?.currentItem ?: 0
            list.removeAt(position)
            viewModel.videoPaths.postValue(list)

            if (list.isNotEmpty()) {
                val newPosition = when {
                    currentPosition >= list.size -> list.size - 1
                    else -> currentPosition
                }
                viewPager?.setCurrentItem(newPosition, false)
            }

            if (isAdded) {
                Toast.makeText(requireContext(), "Video deleted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteAllVideosDialog() {
        if (!isAdded) return

        val list = viewModel.videoPaths.value ?: emptyList()

        if (list.isEmpty()) {
            Toast.makeText(requireContext(), "No videos to delete", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Delete All Videos")
            .setMessage("Are you sure you want to delete all ${list.size} videos? This action cannot be undone.")
            .setPositiveButton("Delete All") { _, _ ->
                deleteAllVideos()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAllVideos() {
        val list = viewModel.videoPaths.value?.toMutableList() ?: mutableListOf()
        val count = list.size

        list.clear()
        viewModel.videoPaths.postValue(list)

        if (isAdded) {
            Toast.makeText(
                requireContext(),
                "All $count videos deleted",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun uploadVideoToServer(uri: Uri) {
        if (!isAdded) return

        lifecycleScope.launch {
            try {
                if (viewModel.orderNum.isEmpty() || viewModel.boxIdToInspect.isEmpty()) {
                    Toast.makeText(requireContext(), "Missing order or box information", Toast.LENGTH_SHORT).show()
                    tempVideoFile?.delete()
                    tempVideoFile = null
                    return@launch
                }

                progressBar?.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "Uploading video, please wait...", Toast.LENGTH_SHORT).show()

                val byteArray = withContext(Dispatchers.IO) {
                    try {
                        val inputStream = requireContext().contentResolver.openInputStream(uri)
                        inputStream?.use { it.readBytes() }
                    } catch (e: Exception) {
                        android.util.Log.e("VideoUpload", "Error reading video file", e)
                        null
                    }
                }

                if (byteArray == null || byteArray.isEmpty()) {
                    Toast.makeText(requireContext(), "Error reading video", Toast.LENGTH_SHORT).show()
                    progressBar?.visibility = View.GONE
                    tempVideoFile?.delete()
                    tempVideoFile = null
                    return@launch
                }

                val service = com.example.qceqapp.data.network.Service()
                val result = withContext(Dispatchers.IO) {
                    service.uploadVideo(viewModel.orderNum, viewModel.boxIdToInspect, byteArray)
                }

                progressBar?.visibility = View.GONE

                if (result.isSuccess) {
                    val guidFile = result.getOrNull()
                    if (!guidFile.isNullOrEmpty()) {
                        val list = viewModel.videoPaths.value?.toMutableList() ?: mutableListOf()
                        list.add(guidFile)
                        viewModel.videoPaths.postValue(list)
                        Toast.makeText(requireContext(), "Video added successfully", Toast.LENGTH_SHORT).show()
                        tempVideoFile?.delete()
                        tempVideoFile = null
                    } else {
                        Toast.makeText(requireContext(), "Error: No GUID returned", Toast.LENGTH_SHORT).show()
                        tempVideoFile?.delete()
                        tempVideoFile = null
                    }
                } else {
                    val exception = result.exceptionOrNull()
                    val errorMsg = exception?.message ?: "Unknown error"
                    Toast.makeText(requireContext(), "Error uploading: $errorMsg", Toast.LENGTH_LONG).show()
                    tempVideoFile?.delete()
                    tempVideoFile = null
                }

            } catch (e: Exception) {
                progressBar?.visibility = View.GONE
                tempVideoFile?.delete()
                tempVideoFile = null
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun uploadMultipleVideosToServer(uris: List<Uri>) {
        if (!isAdded) return

        lifecycleScope.launch {
            try {
                if (viewModel.orderNum.isEmpty() || viewModel.boxIdToInspect.isEmpty()) {
                    Toast.makeText(requireContext(), "Missing order or box information", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                progressBar?.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "Uploading ${uris.size} videos, please wait...", Toast.LENGTH_LONG).show()

                val service = com.example.qceqapp.data.network.Service()
                val newGuids = mutableListOf<String>()
                var successCount = 0
                var failCount = 0

                for ((index, uri) in uris.withIndex()) {
                    val byteArray = withContext(Dispatchers.IO) {
                        try {
                            val inputStream = requireContext().contentResolver.openInputStream(uri)
                            inputStream?.use { it.readBytes() }
                        } catch (e: Exception) {
                            null
                        }
                    }

                    if (byteArray != null && byteArray.isNotEmpty()) {
                        val result = withContext(Dispatchers.IO) {
                            service.uploadVideo(viewModel.orderNum, viewModel.boxIdToInspect, byteArray)
                        }

                        if (result.isSuccess) {
                            val guidFile = result.getOrNull()
                            if (!guidFile.isNullOrEmpty()) {
                                newGuids.add(guidFile)
                                successCount++
                            }
                        } else {
                            failCount++
                        }
                    } else {
                        failCount++
                    }
                }

                progressBar?.visibility = View.GONE

                if (newGuids.isNotEmpty()) {
                    val list = viewModel.videoPaths.value?.toMutableList() ?: mutableListOf()
                    list.addAll(newGuids)
                    viewModel.videoPaths.postValue(list)

                    if (failCount > 0) {
                        Toast.makeText(
                            requireContext(),
                            "Added $successCount videos, $failCount failed",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Added $successCount videos",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Could not upload any videos",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                progressBar?.visibility = View.GONE
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateUI() {
        val list = viewModel.videoPaths.value ?: emptyList()
        counter?.text = "Total: ${list.size}"
        adapter?.submitList(list.toList())
        updateArrowButtons(viewPager?.currentItem ?: 0)
    }

    private fun updateArrowButtons(currentPosition: Int) {
        val total = viewModel.videoPaths.value?.size ?: 0

        if (total <= 1) {
            btnPrev?.visibility = View.INVISIBLE
            btnNext?.visibility = View.INVISIBLE
        } else {
            btnPrev?.visibility = View.VISIBLE
            btnNext?.visibility = View.VISIBLE
        }
    }
}