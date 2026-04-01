package com.example.qceqapp

import android.app.Application
import android.util.Log
import com.example.qceqapp.data.local.QCEQDatabase

/**
 * Application class de QCEQAPP.
 *
 * Responsabilidad única: inicializar singletons que requieren Application context
 * antes de que cualquier Activity, Fragment o ViewModel sea creado.
 *
 * Actualmente inicializa:
 * - [QCEQDatabase]: base de datos Room para persistencia local de pending items.
 *
 * Registro en AndroidManifest.xml:
 *   android:name=".QCEQApplication"
 */
class QCEQApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        QCEQDatabase.init(this)
        Log.d("QCEQApplication", "Application initialized")
    }
}
