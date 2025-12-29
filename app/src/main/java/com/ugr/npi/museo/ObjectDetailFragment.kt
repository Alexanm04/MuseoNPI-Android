package com.ugr.npi.museo

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.os.BundleCompat
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

        val museoObject = arguments?.let {
            BundleCompat.getParcelable(it, "museoObject", MuseoObject::class.java)
        }
        val lang = Locale.getDefault().language
        
        museoObject?.let { obj ->
            binding.tvDetailName.text = obj.getNombre()
            
            // Corrección: Usar strings con placeholders en lugar de concatenar
            binding.tvDetailCategory.text = getString(R.string.categoria_label, obj.getCategoria())
            binding.tvDetailDescription.text = obj.getDescripcion()

            // Carga de imagen desde Assets
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
                } catch (ignored: Exception) {
                    // Ignorado: Probar siguiente extensión
                }
            }

            if (!loaded) {
                binding.ivDetailImage.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            // Usar strings de recursos para los botones
            binding.btnLearnMore.setText(R.string.aprender_mas_obra)
            binding.btnTechnicalDetails.setText(R.string.detalles_tecnicos_label)

            binding.btnLearnMore.setOnClickListener {
                showIterativePopup(getString(R.string.aprender_mas_titulo), obj.getInfoExtra())
            }

            binding.btnTechnicalDetails.setOnClickListener {
                showIterativePopup(getString(R.string.detalles_tecnicos_label), obj.getDetallesTecnicos())
            }
        }
    }

    private fun showIterativePopup(title: String, infoList: List<String>) {
        if (infoList.isEmpty()) return

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
            btnMore.setText(R.string.mostrar_mas_info)
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

        btnClose.setText(R.string.cerrar)
        btnClose.setOnClickListener { dialog.dismiss() }
        
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
