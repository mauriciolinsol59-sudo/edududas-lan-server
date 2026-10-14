package com.edududas.lanserver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import fi.iki.elonen.NanoHTTPD
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class LanServerService : Service() {
    companion object {
        const val ACTION_START = "com.edududas.lanserver.START"
        private const val CHANNEL = "edududas_lan_server"
        private const val PORT = 8765
        // Change this before using outside a trusted test network.
        private const val API_KEY = "cambia-esta-clave-lan"
    }

    private var server: ApiServer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        startForeground(101, notification("Preparando servidor local…"))
        if (server == null) {
            server = ApiServer(PORT, File(filesDir, "shared-data.json")).also {
                try { it.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false) }
                catch (e: IOException) { stopSelf(); return START_NOT_STICKY }
            }
        }
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(101, notification("Servidor activo en el puerto $PORT"))
        return START_STICKY
    }

    override fun onDestroy() { server?.stop(); server = null; super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL, "Servidor local EduDudas", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }
    private fun notification(text: String): Notification = NotificationCompat.Builder(this, CHANNEL)
        .setContentTitle("EduDudas LAN Server").setContentText(text).setSmallIcon(android.R.drawable.stat_sys_upload_done)
        .setOngoing(true).build()

    private inner class ApiServer(port: Int, private val store: File) : NanoHTTPD("0.0.0.0", port) {
        private val lock = Any()
        private val data = ConcurrentHashMap<String, JSONArray>()
        init { listOf("questions", "answers", "resources").forEach { data[it] = loadArray(it) } }

        override fun serve(session: IHTTPSession): Response {
            val path = session.uri.trimEnd('/').ifBlank { "/" }
            if (session.method == Method.OPTIONS) return newFixedLengthResponse(Response.Status.OK, "text/plain", "")
            if (path == "/health" && session.method == Method.GET) return json(JSONObject().put("ok", true).put("service", "EduDudas LAN API").put("version", 1))
            if (!path.startsWith("/api/")) return jsonError(Response.Status.NOT_FOUND, "Ruta no encontrada")
            if (session.headers["x-api-key"] != API_KEY) return jsonError(Response.Status.UNAUTHORIZED, "Falta o es incorrecta la clave X-API-Key")
            val parts = path.split('/').filter { it.isNotBlank() }
            if (parts.size != 2 || parts[0] != "api" || parts[1] !in listOf("questions", "answers", "resources")) return jsonError(Response.Status.NOT_FOUND, "Ruta no encontrada")
            val kind = parts[1]
            return when (session.method) {
                Method.GET -> synchronized(lock) { json(JSONObject().put("items", JSONArray(data[kind].toString()))) }
                Method.POST -> {
                    val files = HashMap<String, String>()
                    try { session.parseBody(files) } catch (_: Exception) { return jsonError(Response.Status.BAD_REQUEST, "Cuerpo inválido") }
                    val raw = files["postData"] ?: "{}"
                    val item = try { JSONObject(raw) } catch (_: Exception) { return jsonError(Response.Status.BAD_REQUEST, "Se esperaba un objeto JSON") }
                    if (item.length() == 0) return jsonError(Response.Status.BAD_REQUEST, "El objeto no puede estar vacío")
                    item.put("id", item.optString("id").ifBlank { java.util.UUID.randomUUID().toString() })
                    item.put("updatedAt", System.currentTimeMillis())
                    synchronized(lock) {
                        val arr = data[kind] ?: JSONArray()
                        arr.put(item); data[kind] = arr
                        saveAll()
                    }
                    json(JSONObject().put("ok", true).put("item", item))
                }
                else -> jsonError(Response.Status.METHOD_NOT_ALLOWED, "Método no permitido")
            }
        }

        private fun json(obj: JSONObject): Response = newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", obj.toString()).apply {
            addHeader("Access-Control-Allow-Origin", "*")
            addHeader("Access-Control-Allow-Headers", "Content-Type, X-API-Key")
            addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        }
        private fun jsonError(status: Response.IStatus, message: String): Response = newFixedLengthResponse(status, "application/json; charset=utf-8", JSONObject().put("error", message).toString())
        private fun loadArray(key: String): JSONArray = try {
            if (store.exists()) JSONObject(store.readText()).optJSONArray(key) ?: JSONArray() else JSONArray()
        } catch (_: Exception) { JSONArray() }
        private fun saveAll() {
            val obj = JSONObject(); data.forEach { (key, value) -> obj.put(key, value) }
            store.writeText(obj.toString())
        }
    }
}
