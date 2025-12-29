package com.ugr.npi.museo

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Locale

@Parcelize
data class MuseoObject(
    val id: Int,
    val nombre_es: String,
    val nombre_en: String,
    val nombre_fr: String,
    val nombre_pt: String,
    val categoria_es: String,
    val categoria_en: String,
    val categoria_fr: String,
    val categoria_pt: String,
    val descripcion_es: String,
    val descripcion_en: String,
    val descripcion_fr: String,
    val descripcion_pt: String,
    val imagen: String,
    val info_extra_es: List<String>,
    val info_extra_en: List<String>,
    val info_extra_fr: List<String>,
    val info_extra_pt: List<String>,
    val detalles_tecnicos_es: List<String>,
    val detalles_tecnicos_en: List<String>,
    val detalles_tecnicos_fr: List<String>,
    val detalles_tecnicos_pt: List<String>
) : Parcelable {

    fun getNombre(): String = when (getLanguage()) {
        "es" -> nombre_es
        "fr" -> nombre_fr
        "pt" -> nombre_pt
        else -> nombre_en
    }

    fun getCategoria(): String = when (getLanguage()) {
        "es" -> categoria_es
        "fr" -> categoria_fr
        "pt" -> categoria_pt
        else -> categoria_en
    }

    fun getDescripcion(): String = when (getLanguage()) {
        "es" -> descripcion_es
        "fr" -> descripcion_fr
        "pt" -> descripcion_pt
        else -> descripcion_en
    }

    fun getInfoExtra(): List<String> = when (getLanguage()) {
        "es" -> info_extra_es
        "fr" -> info_extra_fr
        "pt" -> info_extra_pt
        else -> info_extra_en
    }

    fun getDetallesTecnicos(): List<String> = when (getLanguage()) {
        "es" -> detalles_tecnicos_es
        "fr" -> detalles_tecnicos_fr
        "pt" -> detalles_tecnicos_pt
        else -> detalles_tecnicos_en
    }

    private fun getLanguage(): String {
        return Locale.getDefault().language
    }
}