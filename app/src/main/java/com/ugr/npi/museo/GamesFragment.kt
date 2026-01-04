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
    private lateinit var tvProgress: TextView
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

    // Guardamos el texto de la respuesta correcta para mostrarlo al fallar
    private var respuestaCorrectaActualString: String = ""

    private val TIEMPO_POR_PREGUNTA = 20000L

    private lateinit var sharedPreferences: SharedPreferences
    // Colores temática Egipto
    private val coloresOriginales = listOf("#A52A2A", "#1C39BB", "#D4AF37", "#2E8B57")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = requireActivity().getSharedPreferences("MisJuegos", Context.MODE_PRIVATE)

        tvScore = view.findViewById(R.id.tvScore)
        tvTimer = view.findViewById(R.id.tvTimer)
        tvHighScore = view.findViewById(R.id.tvHighScore)
        tvPregunta = view.findViewById(R.id.tvQuestion)
        tvProgress = view.findViewById(R.id.tvProgress)
        tvFeedback = view.findViewById(R.id.tvFeedback)
        feedbackOverlay = view.findViewById(R.id.feedbackOverlay)

        btnOpcion1 = view.findViewById(R.id.btnOption1)
        btnOpcion2 = view.findViewById(R.id.btnOption2)
        btnOpcion3 = view.findViewById(R.id.btnOption3)
        btnOpcion4 = view.findViewById(R.id.btnOption4)
        btnReiniciar = view.findViewById(R.id.btnReiniciar)

        btnReiniciar.setOnClickListener { iniciarJuego() }

        btnOpcion1.setOnClickListener { verificarRespuesta(btnOpcion1) }
        btnOpcion2.setOnClickListener { verificarRespuesta(btnOpcion2) }
        btnOpcion3.setOnClickListener { verificarRespuesta(btnOpcion3) }
        btnOpcion4.setOnClickListener { verificarRespuesta(btnOpcion4) }

        listaPreguntas = cargarPreguntasDesdeJson().shuffled()

        val record = sharedPreferences.getInt("RECORD_PUNTOS", 0)
        tvHighScore.text = record.toString()

        iniciarJuego()
    }

    private fun iniciarJuego() {
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

            tvPregunta.text = pregunta.pregunta
            tvProgress.text = getString(R.string.pregunta_progress, indicePreguntaActual + 1, listaPreguntas.size)

            respuestaCorrectaActualString = pregunta.opciones[pregunta.respuestaCorrecta]

            val opcionesBarajadas = pregunta.opciones.toMutableList()
            opcionesBarajadas.shuffle()

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

        val esCorrecto = (botonPulsado.text == respuestaCorrectaActualString)

        if (esCorrecto) {
            puntuacion += 10
            tvScore.text = puntuacion.toString()
            botonPulsado.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4CAF50"))

            // Solo mostramos "¡CORRECTO!"
            mostrarFeedbackOverlay(true, getString(R.string.correcto))

            // Add points if user is logged in
            if (UserManager.getCurrentUser() != null) {
                UserManager.addPoints(requireContext(), 10)
                // Optional: Show a small toast or log
                // CustomToast.show(requireContext(), "+10 Puntos!")
            }
        } else {
            botonPulsado.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F44336"))
            animarError(botonPulsado)

            // Mostramos "ERROR" + "La correcta era: X"
            val mensajeError = getString(R.string.error) + "\n\n" +
                    getString(R.string.la_correcta_era, respuestaCorrectaActualString)
            mostrarFeedbackOverlay(false, mensajeError)
        }

        irASiguientePreguntaConRetraso()
    }

    private fun manejarTiempoAgotado() {
        activarBotones(false)

        // Mostramos "TIEMPO" + "La correcta era: X"
        val mensajeTiempo = getString(R.string.tiempo_agotado) + "\n\n" +
                getString(R.string.la_correcta_era, respuestaCorrectaActualString)

        mostrarFeedbackOverlay(false, mensajeTiempo)
        irASiguientePreguntaConRetraso()
    }

    private fun irASiguientePreguntaConRetraso() {
        // Aumentamos el tiempo a 2.5 segundos para que de tiempo a leer la corrección
        Handler(Looper.getMainLooper()).postDelayed({
            feedbackOverlay.visibility = View.GONE
            indicePreguntaActual++
            mostrarPregunta()
        }, 1100)
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
            tvFeedback.setTextColor(Color.parseColor("#2E8B57")) // Verde oscuro
        } else {
            tvFeedback.setTextColor(Color.parseColor("#F44336")) // Rojo
        }
    }

    // --- FUNCIÓN FINALIZAR JUEGO (MODIFICADA) ---
    private fun finalizarJuego() {
        juegoTerminado = true
        timer?.cancel()

        // 1. Comprobamos si hay récord
        val recordActual = sharedPreferences.getInt("RECORD_PUNTOS", 0)
        val esNuevoRecord = puntuacion > recordActual

        if (esNuevoRecord) {
            // Guardamos el récord
            val editor = sharedPreferences.edit()
            editor.putInt("RECORD_PUNTOS", puntuacion)
            editor.apply()
            tvHighScore.text = puntuacion.toString()

            // AQUI ESTÁ EL CAMBIO:
            // Mostramos la animación de "NUEVO RÉCORD"
            tvFeedback.text = getString(R.string.nuevo_record)
            tvFeedback.setTextColor(Color.parseColor("#D4AF37")) // Dorado
            feedbackOverlay.visibility = View.VISIBLE

            // Esperamos 3 segundos y LUEGO quitamos la animación y mostramos el final
            Handler(Looper.getMainLooper()).postDelayed({
                feedbackOverlay.visibility = View.GONE
                mostrarPantallaFinal()
            }, 3000)

        } else {
            // Si no hay récord, mostramos la pantalla final directamente
            feedbackOverlay.visibility = View.GONE
            mostrarPantallaFinal()
        }
    }

    private fun mostrarPantallaFinal() {
        tvPregunta.text = getString(R.string.juego_completado)
        tvProgress.text = getString(R.string.finalizado)

        activarBotones(false)
        btnReiniciar.visibility = View.VISIBLE
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
            val language = java.util.Locale.getDefault().language
            val fileName = if (language == "en") "preguntas_en.json" else "preguntas.json"
            jsonString = requireContext().assets.open(fileName).bufferedReader().use { it.readText() }
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