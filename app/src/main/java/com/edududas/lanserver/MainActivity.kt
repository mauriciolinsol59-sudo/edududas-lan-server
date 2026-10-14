package com.edududas.lanserver

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.text.format.Formatter
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var url: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 7)
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(36, 48, 36, 36); gravity = Gravity.CENTER_VERTICAL
        }
        layout.addView(TextView(this).apply { text = "EduDudas · Servidor local"; textSize = 24f })
        layout.addView(TextView(this).apply { text = "Conecta este teléfono y los clientes a la misma red Wi-Fi."; textSize = 16f; setPadding(0, 18, 0, 18) })
        status = TextView(this).apply { textSize = 18f; text = "Estado: detenido" }
        layout.addView(status)
        url = TextView(this).apply { textSize = 16f; setPadding(0, 14, 0, 14); text = "Dirección: (inicia el servidor)" }
        layout.addView(url)
        layout.addView(Button(this).apply { text = "Iniciar servidor"; setOnClickListener {
            ContextCompat.startForegroundService(this@MainActivity, Intent(this@MainActivity, LanServerService::class.java).setAction(LanServerService.ACTION_START))
            status.text = "Estado: iniciando…"
            updateUrl()
        } })
        layout.addView(Button(this).apply { text = "Detener servidor"; setOnClickListener {
            stopService(Intent(this@MainActivity, LanServerService::class.java)); status.text = "Estado: detenido"
        } })
        layout.addView(TextView(this).apply {
            text = "API de prueba: GET /health, GET /api/questions, POST /api/questions, GET /api/answers, POST /api/answers, GET /api/resources, POST /api/resources.\n\nEsta es una API provisional; todavía no está conectada al APK original. No la expongas a Internet."
            textSize = 14f; setPadding(0, 24, 0, 0)
        })
        setContentView(layout)
    }

    private fun updateUrl() {
        val wm = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION")
        val ip = Formatter.formatIpAddress(wm.connectionInfo.ipAddress)
        url.text = if (ip == "0.0.0.0") "Dirección: revisa que esté conectado a Wi-Fi" else "Dirección: http://$ip:8765"
    }

    override fun onResume() { super.onResume(); updateUrl() }
}
