package com.edududas.lanserver

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.net.Inet4Address
import java.net.NetworkInterface

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var url: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 7)
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 48, 36, 36)
            gravity = Gravity.CENTER_VERTICAL
        }
        layout.addView(TextView(this).apply { text = "EduDudas · Servidor local"; textSize = 24f })
        layout.addView(TextView(this).apply {
            text = "Conecta este teléfono y los clientes a la misma red Wi-Fi. No uses una IP pública."
            textSize = 16f
            setPadding(0, 18, 0, 18)
        })
        status = TextView(this).apply { textSize = 18f; text = "Estado: detenido" }
        layout.addView(status)
        url = TextView(this).apply {
            textSize = 16f
            setPadding(0, 14, 0, 14)
            text = "Dirección local: pulsa Iniciar servidor"
        }
        layout.addView(url)
        layout.addView(Button(this).apply {
            text = "Iniciar servidor"
            setOnClickListener {
                try {
                    ContextCompat.startForegroundService(
                        this@MainActivity,
                        Intent(this@MainActivity, LanServerService::class.java).setAction(LanServerService.ACTION_START)
                    )
                    status.text = "Estado: solicitud de inicio enviada; verifica la notificación del servidor."
                    updateUrl()
                } catch (e: Exception) {
                    status.text = "No se pudo iniciar: ${e.javaClass.simpleName}: ${e.message ?: "sin detalle"}"
                }
            }
        })
        layout.addView(Button(this).apply {
            text = "Detener servidor"
            setOnClickListener {
                stopService(Intent(this@MainActivity, LanServerService::class.java))
                status.text = "Estado: detenido"
                updateUrl()
            }
        })
        layout.addView(TextView(this).apply {
            text = "Prueba desde otro teléfono conectado a la misma Wi-Fi: abre la dirección local seguida de /health. Debe responder JSON con ok:true.\n\nAPI provisional; todavía no está conectada al APK original. No expongas este servidor a Internet."
            textSize = 14f
            setPadding(0, 24, 0, 0)
        })
        setContentView(layout)
    }

    private fun updateUrl() {
        val ip = findPrivateIpv4()
        url.text = if (ip == null) {
            "Dirección local: no se detectó una IP privada. Conecta el teléfono a la misma Wi-Fi que los clientes y vuelve a pulsar Iniciar servidor."
        } else {
            "Dirección local: http://$ip:8765\nPrueba: http://$ip:8765/health"
        }
    }

    private fun findPrivateIpv4(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            val addresses = mutableListOf<String>()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (!networkInterface.isUp || networkInterface.isLoopback || networkInterface.name.startsWith("tun")) continue
                val entries = networkInterface.inetAddresses
                while (entries.hasMoreElements()) {
                    val address = entries.nextElement()
                    if (address is Inet4Address && !address.isLoopbackAddress && address.isSiteLocalAddress) {
                        addresses.add(address.hostAddress ?: continue)
                    }
                }
            }
            addresses.firstOrNull { it.startsWith("192.168.") }
                ?: addresses.firstOrNull { it.startsWith("10.") }
                ?: addresses.firstOrNull { ip ->
                    val parts = ip.split(".")
                    parts.size == 4 && parts[0] == "172" && parts[1].toIntOrNull() in 16..31
                }
                ?: addresses.firstOrNull()
        } catch (_: Exception) {
            null
        }
    }

    override fun onResume() {
        super.onResume()
        updateUrl()
    }
}
