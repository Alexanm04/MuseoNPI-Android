package com.ugr.npi.museo

import android.animation.ObjectAnimator
import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException

class GamesFragment : Fragment(R.layout.fragment_games) {

    // --- UI ---
    private lateinit var tvPregunta: TextView
    private lateinit var tvProgress: TextView // NUEVO: Contador
    private lateinit var tvScore: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvHighScore: TextView
    private lateinit var tvFeedback: TextView
    private lateinit var feedbackOverlay: FrameLayout

    private lateinit var btnOpcion1: MaterialButton
    private lateinit var btnOpcion2: MaterialButton
    private lateinit var btnOpcion3: MaterialButton
    private lateinit var btnOpcion4: MaterialButton
    private lateinit var btnReiniciar: Button

    // --- VARIABLES JUEGO ---
    private var listaPreguntas: List<Pregunta> = emptyList()
    private var indicePreguntaActual = 0
    private var puntuacion = 0
    private var timer: CountDownTimer? = null
    private var juegoTerminado = false

    // NUEVO: Guardamos el TEXTO de la respuesta correcta, no el índice
    private var respuestaCorrectaActualString: String = ""

    private val TIEMPO_POR_PREGUNTA = 20000L

    private lateinit var sharedPreferences: SharedPreferences

    // Colores Temática Egipto
    private val coloresOriginales = listOf("#A52A2A", "#1C39BB", "#D4AF37", "#2E8B57")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = requireActivity().getSharedPreferences("MisJuegos", Context.MODE_PRIVATE)

        // Vincular vistas
        tvScore = view.findViewById(R.id.tvScore)
        tvTimer = view.findViewById(R.id.tvTimer)
        tvHighScore = view.findViewById(R.id.tvHighScore)
        tvPregunta = view.findViewById(R.id.tvQuestion)
        tvProgress = view.findViewById(R.id.tvProgress) // Vincular el nuevo contador
        tvFeedback = view.findViewById(R.id.tvFeedback)
        feedbackOverlay = view.findViewById(R.id.feedbackOverlay)

        btnOpcion1 = view.findViewById(R.id.btnOption1)
        btnOpcion2 = view.findViewById(R.id.btnOption2)
        btnOpcion3 = view.findViewById(R.id.btnOption3)
        btnOpcion4 = view.findViewById(R.id.btnOption4)
        btnReiniciar = view.findViewById(R.id.btnReiniciar)

        btnReiniciar.setOnClickListener { iniciarJuego() }

        // IMPORTANTE: Ahora pasamos el botón directamente, no el índice
        btnOpcion1.setOnClickListener { verificarRespuesta(btnOpcion1) }
        btnOpcion2.setOnClickListener { verificarRespuesta(btnOpcion2) }
        btnOpcion3.setOnClickListener { verificarRespuesta(btnOpcion3) }
        btnOpcion4.setOnClickListener { verificarRespuesta(btnOpcion4) }

        // Cargar y barajar la lista completa de preguntas
        listaPreguntas = cargarPreguntasDesdeJson().shuffled()

        val record = sharedPreferences.getInt("RECORD_PUNTOS", 0)
        tvHighScore.text = record.toString()

        iniciarJuego()
    }

    private fun iniciarJuego() {
        // Volver a barajar si reiniciamos
        listaPreguntas = listaPreguntas.shuffled()

        indicePreguntaActual = 0
        puntuacion = 0
        juegoTerminado = false
        tvScore.text = "0"

        btnReiniciar.visibility = View.GONE
        feedbackOverlay.visibility = View.GONE
        activarBotones(true)
        restaurarColoresBotones()

        tvTimer.setTextColor(Color.parseColor("#FF6F00"))

        if (listaPreguntas.isNotEmpty()) {
            mostrarPregunta()
        }
    }

    private fun restaurarColoresBotones() {
        btnOpcion1.backgroundTintList = ColorStateList.valueOf(Color.parseColor(coloresOriginales[0]))
        btnOpcion2.backgroundTintList = ColorStateList.valueOf(Color.parseColor(coloresOriginales[1]))
        btnOpcion3.backgroundTintList = ColorStateList.valueOf(Color.parseColor(coloresOriginales[2]))
        btnOpcion4.backgroundTintList = ColorStateList.valueOf(Color.parseColor(coloresOriginales[3]))
    }

    private fun startQuestionTimer() {
        timer?.cancel()
        timer = object : CountDownTimer(TIEMPO_POR_PREGUNTA, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                tvTimer.text = (millisUntilFinished / 1000).toString()
                if (millisUntilFinished < 5000) {
                    tvTimer.setTextColor(Color.RED)
                } else {
                    tvTimer.setTextColor(Color.parseColor("#FF6F00"))
                }
            }

            override fun onFinish() {
                tvTimer.text = "0"
                manejarTiempoAgotado()
            }
        }.start()
    }

    private fun mostrarPregunta() {
        if (indicePreguntaActual < listaPreguntas.size) {
            val pregunta = listaPreguntas[indicePreguntaActual]

            // Actualizar textos
            tvPregunta.text = pregunta.pregunta

            // Actualizar contador (Ej: "Pregunta 1 / 20")
            tvProgress.text = "Pregunta ${indicePreguntaActual + 1} / ${listaPreguntas.size}"

            // LÓGICA DE BARAJAR RESPUESTAS
            // 1. Obtenemos el texto correcto usando el índice original del JSON
            respuestaCorrectaActualString = pregunta.opciones[pregunta.respuestaCorrecta]

            // 2. Creamos una copia de las opciones y las barajamos
            val opcionesBarajadas = pregunta.opciones.toMutableList()
            opcionesBarajadas.shuffle()

            // 3. Asignamos los textos barajados a los botones
            btnOpcion1.text = opcionesBarajadas[0]
            btnOpcion2.text = opcionesBarajadas[1]
            btnOpcion3.text = opcionesBarajadas[2]
            btnOpcion4.text = opcionesBarajadas[3]

            restaurarColoresBotones()
            activarBotones(true)
            startQuestionTimer()

        } else {
            finalizarJuego()
        }
    }

    private fun verificarRespuesta(botonPulsado: MaterialButton) {
        if (juegoTerminado) return

        timer?.cancel()
        activarBotones(false)

        // Comprobamos si el texto del botón pulsado coincide con la respuesta correcta guardada
        val esCorrecto = (botonPulsado.text == respuestaCorrectaActualString)

        if (esCorrecto) {
            puntuacion += 10
            tvScore.text = puntuacion.toString()
            botonPulsado.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4CAF50"))
            mostrarFeedbackOverlay(true, "¡CORRECTO!")
        } else {
            botonPulsado.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F44336"))
            animarError(botonPulsado)
            mostrarFeedbackOverlay(false, "¡OH NO!")
        }

        irASiguientePreguntaConRetraso()
    }

    private fun manejarTiempoAgotado() {
        activarBotones(false)
        mostrarFeedbackOverlay(false, "¡TIEMPO!")
        irASiguientePreguntaConRetraso()
    }

    private fun irASiguientePreguntaConRetraso() {
        Handler(Looper.getMainLooper()).postDelayed({
            feedbackOverlay.visibility = View.GONE
            indicePreguntaActual++
            mostrarPregunta()
        }, 1500)
    }

    private fun animarError(view: View) {
        val animator = ObjectAnimator.ofFloat(view, "translationX", 0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f)
        animator.duration = 500
        animator.start()
    }

    private fun mostrarFeedbackOverlay(esCorrecto: Boolean, mensaje: String) {
        feedbackOverlay.visibility = View.VISIBLE
        tvFeedback.text = mensaje
        if (esCorrecto) {
            tvFeedback.setTextColor(Color.parseColor("#4CAF50"))
        } else {
            tvFeedback.setTextColor(Color.parseColor("#F44336"))
        }
    }

    private fun finalizarJuego() {
        juegoTerminado = true
        timer?.cancel()
        feedbackOverlay.visibility = View.GONE

        tvPregunta.text = "¡JUEGO COMPLETADO!"
        tvProgress.text = "Finalizado"

        activarBotones(false)
        btnReiniciar.visibility = View.VISIBLE

        val recordActual = sharedPreferences.getInt("RECORD_PUNTOS", 0)
        if (puntuacion > recordActual) {
            val editor = sharedPreferences.edit()
            editor.putInt("RECORD_PUNTOS", puntuacion)
            editor.apply()
            tvHighScore.text = puntuacion.toString()

            tvFeedback.text = "¡NUEVO RÉCORD!"
            tvFeedback.setTextColor(Color.parseColor("#D4AF37"))
            feedbackOverlay.visibility = View.VISIBLE
        }
    }

    private fun activarBotones(activar: Boolean) {
        btnOpcion1.isEnabled = activar
        btnOpcion2.isEnabled = activar
        btnOpcion3.isEnabled = activar
        btnOpcion4.isEnabled = activar
    }

    private fun cargarPreguntasDesdeJson(): List<Pregunta> {
        val jsonString: String
        try {
            jsonString = requireContext().assets.open("preguntas.json").bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            return emptyList()
        }
        val gson = Gson()
        return gson.fromJson(jsonString, object : TypeToken<List<Pregunta>>() {}.type)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer?.cancel()
    }
}