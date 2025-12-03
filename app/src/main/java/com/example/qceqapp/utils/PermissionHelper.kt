// Crear archivo: PermissionHelper.kt en el paquete utils
package com.example.qceqapp.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

object PermissionHelper {

    const val CAMERA_PERMISSION_CODE = 100
    const val STORAGE_PERMISSION_CODE = 101
    const val ALL_PERMISSIONS_CODE = 102

    // Permisos necesarios según la versión de Android
    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10-12
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        } else { // Android 9 y anteriores
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    // Verificar si todos los permisos están concedidos
    fun hasAllPermissions(context: Context): Boolean {
        return getRequiredPermissions().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Verificar si tiene permiso de cámara
    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Solicitar permisos desde una Activity
    fun requestAllPermissions(activity: Activity, requestCode: Int = ALL_PERMISSIONS_CODE) {
        ActivityCompat.requestPermissions(
            activity,
            getRequiredPermissions(),
            requestCode
        )
    }

    // Solicitar permisos desde un Fragment
    fun requestAllPermissions(fragment: Fragment, requestCode: Int = ALL_PERMISSIONS_CODE) {
        fragment.requestPermissions(
            getRequiredPermissions(),
            requestCode
        )
    }

    // Solicitar solo permiso de cámara
    fun requestCameraPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }

    // Solicitar solo permiso de cámara desde Fragment
    fun requestCameraPermission(fragment: Fragment) {
        fragment.requestPermissions(
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }

    // Verificar resultado de permisos
    fun onPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean {
        if (requestCode == CAMERA_PERMISSION_CODE ||
            requestCode == STORAGE_PERMISSION_CODE ||
            requestCode == ALL_PERMISSIONS_CODE) {
            return grantResults.isNotEmpty() &&
                    grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        }
        return false
    }
}