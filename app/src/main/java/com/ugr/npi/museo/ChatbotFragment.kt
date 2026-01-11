package com.ugr.npi.museo

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.io.InputStreamReader

class ChatbotFragment : Fragment() {

    private lateinit var adapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var optionsLayout: LinearLayout
    private lateinit var avatarThot: ImageView
    private lateinit var recyclerView: RecyclerView
    
    private var isProcessing = false

    private val apiKey = "AIzaSyAqeijlkMjzg2f9kiMBgI8AUTYdx_G64BE"
    
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.0-flash",
        apiKey = apiKey
    )
    
    private val chatSession by lazy {
        val lang = SettingsManager.getLanguage(requireContext()).lowercase()
        generativeModel.startChat(
            history = listOf(
                content("user") { text(getSystemPrompt()) },
                content("model") { text(getInitialGreetingJson(lang)) }
            )
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chatbot, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.chat_recycler_view)
        val editMessage = view.findViewById<EditText>(R.id.edit_chat_message)
        val btnSend = view.findViewById<ImageButton>(R.id.btn_send)
        optionsLayout = view.findViewById(R.id.options_layout)
        avatarThot = view.findViewById(R.id.avatar_thot)

        avatarThot.setImageResource(R.drawable.thot_idle)
        
        adapter = ChatAdapter(messages)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        if (messages.isEmpty()) {
            val lang = SettingsManager.getLanguage(requireContext()).lowercase()
            processBotResponse(getInitialGreetingJson(lang))
        }

        btnSend.setOnClickListener {
            val text = editMessage.text.toString().trim()
            if (text.isNotEmpty() && !isProcessing) {
                sendMessageToGemini(text)
                editMessage.text.clear()
            }
        }
    }

    private fun getInitialGreetingJson(lang: String): String {
        // Normalizamos a los primeros 2 caracteres por si viene "en-US", etc.
        val shortLang = lang.take(2).lowercase()
        return when (shortLang) {
            "en" -> "{\"mensaje\": \"I am Thoth, guardian of wisdom. What would you like to know about our museum?\", \"opciones\": [\"What rooms are there?\", \"Who are you?\"]}"
            "fr" -> "{\"mensaje\": \"Je suis Thot, gardien de la sagesse. Que souhaitez-vous savoir sur notre musée ?\", \"opciones\": [\"Quelles salles y a-t-il ?\", \"Qui es-tu ?\"]}"
            "pt" -> "{\"mensaje\": \"Sou Thoth, guardião da sabedoria. O que você gostaria de saber sobre nosso museu?\", \"opciones\": [\"Quais salas existem?\", \"Quem é você?\"]}"
            else -> "{\"mensaje\": \"Soy Thot, guardián de la sabiduría. ¿Qué deseas conocer sobre nuestro museo?\", \"opciones\": [\"¿Qué salas hay?\", \"¿Quién eres?\"]}"
        }
    }

    private fun sendMessageToGemini(userText: String) {
        if (isProcessing) return
        isProcessing = true
        addUserMessage(userText)
        setAvatarState(isThinking = true)
        clearOptions()

        lifecycleScope.launch {
            try {
                val response = chatSession.sendMessage(userText)
                val responseText = response.text ?: ""
                processBotResponse(responseText)
            } catch (e: Exception) {
                Log.e("ChatbotFragment", "API Error: ", e)
                addBotMessage("Los dioses están en silencio. Inténtalo de nuevo en unos instantes.")
            } finally {
                if (isAdded) {
                    setAvatarState(isThinking = false)
                    isProcessing = false
                }
            }
        }
    }

    private fun processBotResponse(rawJson: String) {
        try {
            val cleanedJson = if (rawJson.contains("```json")) {
                rawJson.substringAfter("```json").substringBefore("```").trim()
            } else if (rawJson.contains("```")) {
                 rawJson.substringAfter("```").substringBeforeLast("```").trim()
            } else {
                rawJson.trim()
            }

            val responseData = Gson().fromJson(cleanedJson, GeminiResponse::class.java)
            addBotMessage(responseData.mensaje)
            updateOptions(responseData.opciones)
        } catch (e: Exception) {
            addBotMessage(rawJson)
        }
    }

    private fun addUserMessage(text: String) {
        messages.add(ChatMessage(text, true))
        adapter.notifyItemInserted(messages.size - 1)
        recyclerView.scrollToPosition(messages.size - 1)
    }

    private fun addBotMessage(text: String) {
        if (!isAdded) return
        messages.add(ChatMessage(text, false))
        adapter.notifyItemInserted(messages.size - 1)
        recyclerView.scrollToPosition(messages.size - 1)
    }

    private fun updateOptions(options: List<String>) {
        if (!isAdded) return
        clearOptions()
        val context = context ?: return
        options.take(2).forEach { option ->
            val button = com.google.android.material.button.MaterialButton(context).apply {
                text = option
                isAllCaps = false
                // Permitimos hasta 3 líneas para que crezca en altura si es necesario
                maxLines = 3
                ellipsize = TextUtils.TruncateAt.END
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                
                // Alineación centrada para que visualmente quede mejor
                gravity = Gravity.CENTER
                setPadding(12, 12, 12, 12)
                
                // Usamos weight 1.0f para que se repartan el ancho fijo (50% cada uno)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f).apply {
                    setMargins(8, 0, 8, 0)
                }
                setOnClickListener {
                    if (!isProcessing) sendMessageToGemini(option)
                }
            }
            optionsLayout.addView(button)
        }
    }

    private fun clearOptions() {
        optionsLayout.removeAllViews()
    }

    private fun setAvatarState(isThinking: Boolean) {
        if (!isAdded) return
        if (isThinking) {
            avatarThot.setImageResource(R.drawable.thot_thinking)
            avatarThot.animate().scaleX(1.1f).scaleY(1.1f).setDuration(500).withEndAction {
                if (isAdded) avatarThot.animate().scaleX(1.0f).scaleY(1.0f).setDuration(500).start()
            }.start()
        } else {
            avatarThot.animate().cancel()
            avatarThot.scaleX = 1.0f
            avatarThot.scaleY = 1.0f
            avatarThot.setImageResource(R.drawable.thot_idle)
        }
    }

    private fun getSystemPrompt(): String {
        val context = context ?: return "Responde como Thot."
        val lang = SettingsManager.getLanguage(context).lowercase()
        val museumData = loadMuseumData(lang)
        
        return """
            ROL: Eres Thot, dios egipcio de la sabiduría y guía experto de este museo.
            PERSONALIDAD: Solemne, mística y acogedora.
            
            MAPA DEL MUSEO:
            - Sala Inframundo: El viaje al más allá, juicio de Osiris, momificación y tumbas.
            - Sala Faraones: Reyes, máscaras de oro, sarcófagos y símbolos de poder.
            - Sala Pirámides: Ingeniería. Destacan: Gran Pirámide de Giza, Pirámide Escalonada (Saqqara) y Acodada (Dahshur).
            - Sala Dioses: Mitología, panteón egipcio y creencias.
            - Sala Río Nilo: Agricultura, fauna, inundaciones y vida cotidiana junto al río.
            
            SERVICIOS:
            - Baño: izquierda pasada la sala de los Dioses.
            - Ascensor: izquierda en la recepción.
            - Cafetería: derecha en la recepción.
            - Tienda de regalos: derecha pasada la sala del Río Nilo.
            
            INVENTARIO DE OBRAS EN EL MUSEO:
            ${"$"}{museumData}
            
            REGLAS DE RESPUESTA (JSON):
            1. Estructura estricta: { "mensaje": "...", "opciones": [...] }
            2. 'mensaje': Máximo 45 palabras. Usa los datos del inventario para ser preciso.
            3. 'opciones': EXACTAMENTE 2 preguntas muy cortas relacionadas.
            4. IMPORTANTE: Si proporcionas información sobre una obra o sala, una de las opciones DEBE ser una variación de "Cuéntame más" o "Saber más" en el idioma ${lang.uppercase()}.
            5. IDIOMA: Responde siempre en ${lang.uppercase()}.
        """.trimIndent()
    }

    private fun loadMuseumData(lang: String): String {
        return try {
            val inputStream = context?.assets?.open("objetos.json")
            val reader = InputStreamReader(inputStream)
            val type = object : TypeToken<List<Map<String, Any>>>() {}.type
            val fullData: List<Map<String, Any>> = Gson().fromJson(reader, type)
            
            fullData.joinToString("\n") { obj ->
                val nombre = obj["nombre_$lang"] ?: "Obra"
                val detalles = obj["detalles_tecnicos_$lang"] as? List<*>
                val ubicacion = detalles?.lastOrNull() ?: "Desconocida"
                "- ${"$"}{nombre} (Ubicación: ${"$"}{ubicacion})"
            }
        } catch (e: Exception) {
            "No hay datos de obras disponibles."
        }
    }

    data class GeminiResponse(val mensaje: String, val opciones: List<String>)
}
