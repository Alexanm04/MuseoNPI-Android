package com.ugr.npi.museo

// Esta clase es el "molde" de tus preguntas.
// Los nombres de las variables TIENEN que ser iguales al JSON.
data class Pregunta(
    val id: Int,
    val pregunta: String,
    val opciones: List<String>, // Una lista de textos para las respuestas
    val respuestaCorrecta: Int  // El índice de la respuesta correcta (0, 1, 2 o 3)
)