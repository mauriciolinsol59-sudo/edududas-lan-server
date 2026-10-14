package com.edududas.user

import android.app.Activity
import android.os.Bundle
import android.graphics.Typeface
import android.view.View
import android.view.Gravity
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var baseUrl: EditText
    private lateinit var apiKey: EditText
    private lateinit var status: TextView
    private lateinit var list: LinearLayout
    private lateinit var titleInput: EditText
    private lateinit var bodyInput: EditText
    private var questions = JSONArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("edududas", MODE_PRIVATE)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(0xFFF4F7FB.toInt())
        }
        val scroller = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(TextView(this).apply {
            text = "EduDudas"
            textSize = 30f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFF163A63.toInt())
        })
        content.addView(TextView(this).apply {
            text = "Comunidad de preguntas y respuestas"
            textSize = 15f
            setPadding(0, 4, 0, 20)
        })
        content.addView(label("Dirección del servidor (IP del teléfono administrador)"))
        baseUrl = EditText(this).apply {
            hint = "http://192.168.1.50:8765"
            setSingleLine(true)
            setText(prefs.getString("baseUrl", ""))
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
        }
        content.addView(baseUrl)
        content.addView(label("Clave de acceso"))
        apiKey = EditText(this).apply {
            setSingleLine(true)
            setText(prefs.getString("apiKey", "cambia-esta-clave-lan"))
        }
        content.addView(apiKey)
        val connect = Button(this).apply {
            text = "Guardar conexión y cargar preguntas"
            setOnClickListener {
                saveConnection()
                loadQuestions()
            }
        }
        content.addView(connect)
        status = TextView(this).apply {
            text = "Introduce la dirección que aparece en EduDudas Administrador."
            textSize = 14f
            setPadding(0, 12, 0, 16)
        }
        content.addView(status)
        content.addView(TextView(this).apply {
            text = "Publicar una pregunta"
            textSize = 21f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFF163A63.toInt())
        })
        titleInput = EditText(this).apply { hint = "Título de la pregunta"; setSingleLine(true) }
        bodyInput = EditText(this).apply { hint = "Explica tu pregunta"; minLines = 3; gravity = Gravity.TOP }
        content.addView(titleInput)
        content.addView(bodyInput)
        content.addView(Button(this).apply {
            text = "Publicar pregunta"
            setOnClickListener {
                val title = titleInput.text.toString().trim()
                val body = bodyInput.text.toString().trim()
                if (title.isBlank() || body.isBlank()) {
                    status.text = "Escribe un título y el contenido de la pregunta."
                } else {
                    request("POST", "/api/questions", JSONObject().put("title", title).put("body", body).put("author", "Usuario")) { result ->
                        runOnUiThread {
                            if (result.optBoolean("ok")) {
                                titleInput.setText(""); bodyInput.setText("")
                                status.text = "Pregunta publicada."
                                loadQuestions()
                            } else status.text = "No se pudo publicar: ${result.optString("error", "revisa la conexión")}"
                        }
                    }
                }
            }
        })
        content.addView(TextView(this).apply {
            text = "Preguntas recientes"
            textSize = 21f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFF163A63.toInt())
            setPadding(0, 22, 0, 8)
        })
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(list)
        scroller.addView(content)
        root.addView(scroller, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        if (prefs.getString("baseUrl", "").orEmpty().isNotBlank()) loadQuestions()
    }

    private fun label(value: String) = TextView(this).apply {
        text = value
        textSize = 14f
        setPadding(0, 10, 0, 4)
    }

    private fun saveConnection() {
        getSharedPreferences("edududas", MODE_PRIVATE).edit()
            .putString("baseUrl", baseUrl.text.toString().trim().trimEnd('/'))
            .putString("apiKey", apiKey.text.toString().trim()).apply()
    }

    private fun request(method: String, path: String, payload: JSONObject? = null, done: (JSONObject) -> Unit) {
        saveConnection()
        val host = baseUrl.text.toString().trim().trimEnd('/')
        val key = apiKey.text.toString().trim()
        if (host.isBlank()) {
            status.text = "Escribe la dirección del servidor."
            return
        }
        executor.execute {
            var connection: HttpURLConnection? = null
            val result = try {
                connection = (URL(host + path).openConnection() as HttpURLConnection).apply {
                    requestMethod = method
                    connectTimeout = 6000
                    readTimeout = 6000
                    setRequestProperty("X-API-Key", key)
                    setRequestProperty("Accept", "application/json")
                    if (payload != null) {
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    }
                }
                if (payload != null) OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(payload.toString()) }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() } ?: "{}"
                JSONObject(text)
            } catch (e: Exception) {
                JSONObject().put("error", e.message ?: "Error de conexión")
            } finally { connection?.disconnect() }
            done(result)
        }
    }

    private fun loadQuestions() {
        request("GET", "/api/questions") { result ->
            runOnUiThread {
                if (result.has("error")) {
                    status.text = "No se pudo conectar: ${result.optString("error")}. Comprueba IP, Wi-Fi y clave."
                    return@runOnUiThread
                }
                questions = result.optJSONArray("items") ?: JSONArray()
                status.text = "Conectado. Preguntas disponibles: ${questions.length()}."
                renderQuestions()
            }
        }
    }

    private fun renderQuestions() {
        list.removeAllViews()
        if (questions.length() == 0) {
            list.addView(TextView(this).apply { text = "Todavía no hay preguntas."; setPadding(0, 12, 0, 12) })
            return
        }
        for (i in questions.length() - 1 downTo 0) {
            val item = questions.optJSONObject(i) ?: continue
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 14, 16, 14)
                setBackgroundColor(0xFFFFFFFF.toInt())
            }
            card.addView(TextView(this).apply {
                text = item.optString("title", "Pregunta sin título")
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
                setTextColor(0xFF163A63.toInt())
            })
            card.addView(TextView(this).apply {
                text = item.optString("body", "")
                textSize = 15f
                setPadding(0, 8, 0, 8)
            })
            val answer = EditText(this).apply { hint = "Escribe una respuesta"; minLines = 2; gravity = Gravity.TOP }
            card.addView(answer)
            card.addView(Button(this).apply {
                text = "Responder"
                setOnClickListener {
                    val value = answer.text.toString().trim()
                    if (value.isBlank()) {
                        status.text = "Escribe una respuesta antes de enviarla."
                    } else {
                        request("POST", "/api/answers", JSONObject()
                            .put("questionId", item.optString("id"))
                            .put("body", value)
                            .put("author", "Usuario")) { result ->
                            runOnUiThread {
                                if (result.optBoolean("ok")) {
                                    answer.setText("")
                                    status.text = "Respuesta enviada."
                                } else status.text = "No se pudo enviar: ${result.optString("error", "revisa la conexión")}"
                            }
                        }
                    }
                }
            })
            val answers = TextView(this).apply {
                text = "Respuestas guardadas en el servidor se compartirán entre los dispositivos."
                textSize = 12f
                setPadding(0, 4, 0, 0)
            }
            card.addView(answers)
            list.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 14 })
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}