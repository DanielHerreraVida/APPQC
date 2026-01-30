package com.example.qceqapp.utils

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.qceqapp.data.model.GithubRelease
import com.example.qceqapp.data.remote.GithubApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File

class UpdateManager(private val context: Context) {

    companion object {
        private const val GITHUB_OWNER = "DanielHerreraVida"
        private const val GITHUB_REPO = "APPQC"
        private const val BASE_URL = "https://api.github.com/"
    }

    private val githubApi: GithubApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GithubApiService::class.java)
    }

    private var downloadId: Long = -1

    /**
     * Verifica si hay una nueva versión disponible
     * @return Triple(hayActualizacion, versionNueva, release)
     */
    suspend fun checkForUpdates(): Triple<Boolean, String?, GithubRelease?> {
        return withContext(Dispatchers.IO) {
            try {
                val response = githubApi.getLatestRelease(GITHUB_OWNER, GITHUB_REPO)

                if (response.isSuccessful) {
                    val release = response.body()
                    release?.let {
                        val latestVersion = parseVersion(it.tagName)
                        val currentVersion = getCurrentAppVersion()
                        val isNewVersion = compareVersions(latestVersion, currentVersion) > 0

                        Triple(isNewVersion, it.tagName, it)
                    } ?: Triple(false, null, null)
                } else {
                    Triple(false, null, null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Triple(false, null, null)
            }
        }
    }

    /**
     * Obtiene la versión actual de la app
     */
    private fun getCurrentAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    /**
     * Descarga e instala la actualización
     */
    @SuppressLint("UnspecifiedRegisterReceiverFlag")  // ⭐ AGREGAR ESTA LÍNEA
    fun downloadAndInstallUpdate(downloadUrl: String, versionName: String) {
        val fileName = "QCEQAPP_$versionName.apk"

        val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
            setTitle("Actualizando QCEQAPP")
            setDescription("Descargando versión $versionName")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = downloadManager.enqueue(request)

        // Registrar BroadcastReceiver para cuando termine la descarga
        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    installApk(context, fileName)
                    try {
                        context.unregisterReceiver(this)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        // Registrar el receiver con compatibilidad para diferentes versiones de Android
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                onComplete,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            context.registerReceiver(
                onComplete,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
        }
    }

    /**
     * Instala el APK descargado
     */
    private fun installApk(context: Context, fileName: String) {
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            fileName
        )

        if (!file.exists()) return

        val intent = Intent(Intent.ACTION_VIEW).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val apkUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                setDataAndType(apkUri, "application/vnd.android.package-archive")
            } else {
                setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
            }
        }

        context.startActivity(intent)
    }

    /**
     * Compara dos versiones (formato: v1.0.0 o 1.0.0)
     * @return 1 si version1 > version2, -1 si version1 < version2, 0 si son iguales
     */
    private fun compareVersions(version1: String, version2: String): Int {
        val v1Parts = version1.replace("v", "").split(".")
        val v2Parts = version2.replace("v", "").split(".")

        val maxLength = maxOf(v1Parts.size, v2Parts.size)

        for (i in 0 until maxLength) {
            val v1Part = v1Parts.getOrNull(i)?.toIntOrNull() ?: 0
            val v2Part = v2Parts.getOrNull(i)?.toIntOrNull() ?: 0

            if (v1Part > v2Part) return 1
            if (v1Part < v2Part) return -1
        }

        return 0
    }

    /**
     * Parsea la versión eliminando la 'v' si existe
     */
    private fun parseVersion(version: String): String {
        return version.replace("v", "")
    }
}