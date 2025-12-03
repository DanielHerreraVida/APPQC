package com.example.qceqapp.uis.QCInspection

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
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
import java.io.ByteArrayOutputStream
import java.io.File

class QCEvidencePhotoFragment : Fragment() {

    private val viewModel: QCMediaViewModel by activityViewModels()
    private var viewPager: ViewPager2? = null
    private var counter: TextView? = null
    private var btnPrev: ImageButton? = null
    private var btnNext: ImageButton? = null
    private var adapter: MediaPagerAdapter? = null
    private var progressBar: ProgressBar? = null

    private var imageUri: Uri? = null

    // ✅ Launcher para solicitar permisos
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }

        if (allGranted) {
            // Todos los permisos concedidos, abrir cámara
            openCamera()
        } else {
            // Algunos permisos fueron denegados
            Toast.makeText(
                requireContext(),
                "Camera and storage permissions are required to take photos",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val capturePhoto = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val photoBitmap = result.data?.extras?.get("data") as? Bitmap
            if (photoBitmap != null) {
                uploadImageToServer(photoBitmap)
            } else {
                Toast.makeText(requireContext(), "Could not get the image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val selectPhotos = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uploadMultipleImagesToServer(uris)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_qc_evidence_photo, container, false)

        val btnCamera = view.findViewById<Button>(R.id.btn_Camera)
        val btnFiles = view.findViewById<Button>(R.id.btn_Files)
        val btnContinue = view.findViewById<Button>(R.id.btn_Continue)
        val btnDeleteAll = view.findViewById<ImageButton>(R.id.btn_DeletePhLst)

        counter = view.findViewById(R.id.tV_TotalImages)
        viewPager = view.findViewById(R.id.viewPagerPhotos)
        btnPrev = view.findViewById(R.id.btnPrev)
        btnNext = view.findViewById(R.id.btnNext)
        progressBar = view.findViewById(R.id.progressBar)

        arguments?.let { bundle ->
            viewModel.boxIdToInspect = bundle.getString("boxIdToInspect", "")
            viewModel.orderNum = bundle.getString("orderNum", "")

            val imagesJson = bundle.getString("listImagesCaptured")
            if (!imagesJson.isNullOrEmpty()) {
                val gson = Gson()
                val type = object : TypeToken<MutableList<String>>() {}.type
                val imagesList: MutableList<String> = gson.fromJson(imagesJson, type)
                viewModel.photoPaths.postValue(imagesList)
            }
        }

        adapter = MediaPagerAdapter(
            onDelete = { position ->
                showDeleteSinglePhotoDialog(position)
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

        // ✅ Verificar permisos antes de abrir cámara
        btnCamera.setOnClickListener {
            checkPermissionsAndOpenCamera()
        }

        // ✅ Verificar permisos antes de seleccionar archivos
        btnFiles.setOnClickListener {
            checkPermissionsAndSelectFiles()
        }

        btnContinue.setOnClickListener {
            (activity as? QCMediaActivity)?.onContinue()
        }

        btnDeleteAll.setOnClickListener {
            showDeleteAllPhotosDialog()
        }

        btnPrev?.setOnClickListener {
            val total = viewModel.photoPaths.value?.size ?: 0
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
            val total = viewModel.photoPaths.value?.size ?: 0
            if (total > 0) {
                val currentItem = viewPager?.currentItem ?: 0
                if (currentItem < total - 1) {
                    viewPager?.currentItem = currentItem + 1
                } else {
                    viewPager?.currentItem = 0
                }
            }
        }

        viewModel.photoPaths.observe(viewLifecycleOwner) {
            updateUI()
        }

        return view
    }

    // ✅ Verificar permisos antes de abrir cámara
    private fun checkPermissionsAndOpenCamera() {
        when {
            PermissionHelper.hasAllPermissions(requireContext()) -> {
                // Ya tiene todos los permisos
                openCamera()
            }
            shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA) -> {
                // Mostrar explicación
                AlertDialog.Builder(requireContext())
                    .setTitle("Camera Permission Required")
                    .setMessage("Camera permission is needed to take photos for inspection. Please grant the permission to continue.")
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
                selectPhotos.launch("image/*")
            }
            else -> {
                // Solicitar permisos
                Toast.makeText(
                    requireContext(),
                    "Storage permissions are required to select photos",
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

    // ✅ Abrir cámara
    private fun openCamera() {
        try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                capturePhoto.launch(intent)
            } else {
                Toast.makeText(
                    requireContext(),
                    "No camera app found",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Error opening camera: ${e.localizedMessage}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewPager = null
        counter = null
        btnPrev = null
        btnNext = null
        adapter = null
        progressBar = null
    }

    // ... resto de métodos sin cambios (showDeleteSinglePhotoDialog, deleteSinglePhoto, etc.)

    private fun showDeleteSinglePhotoDialog(position: Int) {
        if (!isAdded) return

        AlertDialog.Builder(requireContext())
            .setTitle("Delete Photo")
            .setMessage("Are you sure you want to delete this photo?")
            .setPositiveButton("Delete") { _, _ ->
                deleteSinglePhoto(position)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteSinglePhoto(position: Int) {
        val list = viewModel.photoPaths.value?.toMutableList() ?: mutableListOf()

        if (position in list.indices) {
            val currentPosition = viewPager?.currentItem ?: 0
            list.removeAt(position)
            viewModel.photoPaths.postValue(list)

            if (list.isNotEmpty()) {
                val newPosition = when {
                    currentPosition >= list.size -> list.size - 1
                    else -> currentPosition
                }
                viewPager?.setCurrentItem(newPosition, false)
            }

            if (isAdded) {
                Toast.makeText(requireContext(), "Photo deleted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteAllPhotosDialog() {
        if (!isAdded) return

        val list = viewModel.photoPaths.value ?: emptyList()

        if (list.isEmpty()) {
            Toast.makeText(requireContext(), "No photos to delete", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Delete All Photos")
            .setMessage("Are you sure you want to delete all ${list.size} photos? This action cannot be undone.")
            .setPositiveButton("Delete All") { _, _ ->
                deleteAllPhotos()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAllPhotos() {
        val list = viewModel.photoPaths.value?.toMutableList() ?: mutableListOf()
        val count = list.size

        list.clear()
        viewModel.photoPaths.postValue(list)

        if (isAdded) {
            Toast.makeText(
                requireContext(),
                "All $count photos deleted",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun uploadImageToServer(bitmap: Bitmap) {
        if (!isAdded) return

        lifecycleScope.launch {
            try {
                progressBar?.visibility = View.VISIBLE
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                val byteArray = stream.toByteArray()
                val service = com.example.qceqapp.data.network.Service()
                val orderNum = viewModel.orderNum
                val boxId = viewModel.boxIdToInspect
                val result = withContext(Dispatchers.IO) {
                    service.uploadPhoto(orderNum, boxId, byteArray)
                }
                progressBar?.visibility = View.GONE
                if (result.isSuccess) {
                    val guidFile = result.getOrNull()
                    if (!guidFile.isNullOrEmpty()) {
                        withContext(Dispatchers.Main) {
                            val list = viewModel.photoPaths.value?.toMutableList() ?: mutableListOf()
                            list.add(guidFile)
                            viewModel.photoPaths.value = list
                            Toast.makeText(requireContext(), "Photo added successfully", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Error uploading photo", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                progressBar?.visibility = View.GONE
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadMultipleImagesToServer(uris: List<Uri>) {
        if (!isAdded) return

        lifecycleScope.launch {
            try {
                progressBar?.visibility = View.VISIBLE
                val service = com.example.qceqapp.data.network.Service()
                val orderNum = viewModel.orderNum
                val boxId = viewModel.boxIdToInspect

                val newGuids = mutableListOf<String>()

                for (uri in uris) {
                    val byteArray = withContext(Dispatchers.IO) {
                        val inputStream = requireContext().contentResolver.openInputStream(uri)
                        inputStream?.readBytes()?.also { inputStream.close() }
                    }

                    if (byteArray != null) {
                        val result = withContext(Dispatchers.IO) {
                            service.uploadPhoto(orderNum, boxId, byteArray)
                        }

                        if (result.isSuccess) {
                            val guidFile = result.getOrNull()
                            if (!guidFile.isNullOrEmpty()) {
                                newGuids.add(guidFile)
                            }
                        }
                    }
                }

                progressBar?.visibility = View.GONE

                if (newGuids.isNotEmpty()) {
                    val list = viewModel.photoPaths.value?.toMutableList() ?: mutableListOf()
                    list.addAll(newGuids)
                    viewModel.photoPaths.postValue(list)
                    Toast.makeText(requireContext(), "${newGuids.size} photos were added", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "No photos could be uploaded", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                progressBar?.visibility = View.GONE
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateUI() {
        val list = viewModel.photoPaths.value ?: emptyList()
        counter?.text = "Total: ${list.size}"
        adapter?.submitList(list.toList())
        updateArrowButtons(viewPager?.currentItem ?: 0)
    }

    private fun updateArrowButtons(currentPosition: Int) {
        val total = viewModel.photoPaths.value?.size ?: 0

        if (total <= 1) {
            btnPrev?.visibility = View.INVISIBLE
            btnNext?.visibility = View.INVISIBLE
        } else {
            btnPrev?.visibility = View.VISIBLE
            btnNext?.visibility = View.VISIBLE
        }
    }
}