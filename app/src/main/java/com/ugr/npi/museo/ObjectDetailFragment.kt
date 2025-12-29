package com.ugr.npi.museo

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.ugr.npi.museo.databinding.FragmentObjectDetailBinding
import java.io.InputStream
import java.util.Locale

class ObjectDetailFragment : Fragment() {

    private var _binding: FragmentObjectDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentObjectDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val museoObject = arguments?.getParcelable<MuseoObject>("museoObject")
        val lang = Locale.getDefault().language
        
        museoObject?.let { obj ->
            binding.tvDetailName.text = obj.getNombre()
            
            val categoryLabel = when(lang) {
                "es" -> "Categoría: "
                "fr" -> "Catégorie : "
                "pt" -> "Categoria: "
                else -> "Category: "
            }
            binding.tvDetailCategory.text = categoryLabel + obj.getCategoria()
            binding.tvDetailDescription.text = obj.getDescripcion()

            // Carga de imagen desde Assets con soporte para múltiples formatos
            val context = requireContext()
            val extensions = listOf(".webp", ".png", ".jpg", ".jpeg")
            var loaded = false

            for (ext in extensions) {
                try {
                    val inputStream: InputStream = context.assets.open("${obj.imagen}$ext")
                    val drawable = Drawable.createFromStream(inputStream, null)
                    binding.ivDetailImage.setImageDrawable(drawable)
                    loaded = true
                    break
                } catch (e: Exception) {
                    // Probar siguiente extensión
                }
            }

            if (!loaded) {
                binding.ivDetailImage.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            binding.btnLearnMore.text = when(lang) {
                "es" -> "Aprender más sobre esta obra"
                "fr" -> "En savoir plus sur cette œuvre"
                "pt" -> "Saiba mais sobre esta obra"
                else -> "Learn more about this work"
            }
            
            binding.btnTechnicalDetails.text = when(lang) {
                "es" -> "Detalles técnicos"
                "fr" -> "Détails techniques"
                "pt" -> "Detalhes técnicos"
                else -> "Technical details"
            }

            binding.btnLearnMore.setOnClickListener {
                val title = when(lang) {
                    "es" -> "Aprender más sobre la obra"
                    "fr" -> "En savoir plus sur l'œuvre"
                    "pt" -> "Saiba mais sobre a obra"
                    else -> "Learn more about the work"
                }
                showIterativePopup(title, obj.getInfoExtra())
            }

            binding.btnTechnicalDetails.setOnClickListener {
                val title = when(lang) {
                    "es" -> "Detalles técnicos"
                    "fr" -> "Détails techniques"
                    "pt" -> "Detalhes técnicos"
                    else -> "Technical details"
                }
                showIterativePopup(title, obj.getDetallesTecnicos())
            }
        }
    }

    private fun showIterativePopup(title: String, infoList: List<String>) {
        if (infoList.isEmpty()) return

        val lang = Locale.getDefault().language
        var currentIndex = 0
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_info, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tvDialogMessage)
        val btnClose = dialogView.findViewById<MaterialButton>(R.id.btnDialogClose)
        val btnMore = dialogView.findViewById<MaterialButton>(R.id.btnDialogMore)

        tvTitle.text = title
        tvMessage.text = infoList[currentIndex]

        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
            .setView(dialogView)
            .create()

        if (infoList.size <= 1) {
            btnMore.visibility = View.GONE
        } else {
            btnMore.visibility = View.VISIBLE
            btnMore.text = when(lang) {
                "es" -> "Mostrar más información"
                "fr" -> "Afficher plus d'informations"
                "pt" -> "Mostrar mais informações"
                else -> "Show more information"
            }
        }

        btnMore.setOnClickListener {
            currentIndex++
            if (currentIndex < infoList.size) {
                tvMessage.text = infoList[currentIndex]
                if (currentIndex == infoList.size - 1) {
                    btnMore.visibility = View.GONE
                }
            }
        }

        btnClose.text = when(lang) {
            "es" -> "Cerrar"
            "fr" -> "Fermer"
            "pt" -> "Fechar"
            else -> "Close"
        }
        btnClose.setOnClickListener { dialog.dismiss() }
        
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}