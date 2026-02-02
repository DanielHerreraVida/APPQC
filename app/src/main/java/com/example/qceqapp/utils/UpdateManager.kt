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
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.qceqapp.data.model.GithubRelease
import com.example.qceqapp.data.remote.GithubApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import android.provider.Settings
import androidx.annotation.RequiresApi
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

    private fun getCurrentAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    fun downloadAndInstallUpdate(downloadUrl: String, versionName: String) {
        val fileName = "QCEQAPP_$versionName.apk"

        val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
            setTitle("Actualizando QCEQAPP")
            setDescription("Descargando versión $versionName")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
            setMimeType("application/vnd.android.package-archive")
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = downloadManager.enqueue(request)

        android.util.Log.d("UpdateManager", " Download ID: $downloadId")

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)

                android.util.Log.d("UpdateManager", " Broadcast recibido - ID: $id")

                if (id == downloadId) {
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    val cursor = downloadManager.query(query)

                    if (cursor.moveToFirst()) {
                        val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val status = cursor.getInt(statusIndex)

                        val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)

                        android.util.Log.d("UpdateManager", " Estado de descarga: $status")

                        when (status) {
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                android.util.Log.d("UpdateManager", " Descarga exitosa")

                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                    installApk(context, fileName)
                                }, 5000)
                            }
                            DownloadManager.STATUS_FAILED -> {
                                val reason = if (reasonIndex >= 0) cursor.getInt(reasonIndex) else -1
                                android.util.Log.e("UpdateManager", " Descarga falló. Razón: $reason")
                                android.widget.Toast.makeText(
                                    context,
                                    "Error al descargar actualización",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                            else -> {
                                android.util.Log.w("UpdateManager", "Estado desconocido: $status")
                            }
                        }
                    }

                    cursor.close()

                    try {
                        context.unregisterReceiver(this)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

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

        android.widget.Toast.makeText(
            context,
            "Iniciando descarga...",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    private fun installApk(context: Context, fileName: String) {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = downloadManager.getUriForDownloadedFile(downloadId)

            if (uri == null) {
                android.util.Log.e("UpdateManager", " No se pudo obtener URI del archivo")
                Toast.makeText(context, "Error: No se encontró el archivo descargado", Toast.LENGTH_LONG).show()
                return
            }

            android.util.Log.d("UpdateManager", " URI del archivo: $uri")

            android.util.Log.d("UpdateManager", " Copiando APK a directorio interno...")
            val internalFile = File(context.cacheDir, fileName)

            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    internalFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                android.util.Log.d("UpdateManager", "APK copiado exitosamente")
                android.util.Log.d("UpdateManager", " Nueva ubicación: ${internalFile.absolutePath}")
                android.util.Log.d("UpdateManager", " Tamaño copiado: ${internalFile.length()} bytes")
            } catch (e: Exception) {
                android.util.Log.e("UpdateManager", "Error al copiar archivo", e)
                Toast.makeText(context, "Error al procesar APK: ${e.message}", Toast.LENGTH_LONG).show()
                return
            }

            installApkFromInternalStorage(context, internalFile)

        } catch (e: Exception) {
            android.util.Log.e("UpdateManager", "Error al instalar APK", e)
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun installApkFromInternalStorage(context: Context, file: File) {
        try {
            android.util.Log.d("UpdateManager", "Instalando desde almacenamiento interno")

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            android.util.Log.d("UpdateManager", "URI: $uri")

            val intent = Intent(Intent.ACTION_VIEW).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                setDataAndType(uri, "application/vnd.android.package-archive")
            }

            // Dar permisos explícitos
            context.grantUriPermission(
                "com.android.packageinstaller",
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            context.grantUriPermission(
                "com.google.android.packageinstaller",
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            android.util.Log.d("UpdateManager", " Abriendo instalador...")
            context.startActivity(intent)

            Toast.makeText(context, "Instalando actualización...", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            android.util.Log.e("UpdateManager", " Error al instalar", e)
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

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


    private fun parseVersion(version: String): String {
        return version.replace("v", "")
    }
}