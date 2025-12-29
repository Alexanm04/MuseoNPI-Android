package com.ugr.npi.museo

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import com.ugr.npi.museo.databinding.FragmentScannerBinding
import java.util.Locale

class ScannerFragment : Fragment() {

    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val lang = Locale.getDefault().language
        
        // Traducción de textos estáticos
        binding.tvScannerTitle.text = when(lang) {
            "es" -> "Escáner de Objetos"
            "fr" -> "Scanner d'objets"
            "pt" -> "Scanner de objetos"
            else -> "Object Scanner"
        }

        binding.btnInventory.text = when(lang) {
            "es" -> "Ver inventario"
            "fr" -> "Voir l'inventaire"
            "pt" -> "Ver inventário"
            else -> "View inventory"
        }

        // Placeholder de la cámara
        val cameraPlaceholder = binding.scannerPlaceholder.getChildAt(0) as? TextView
        cameraPlaceholder?.text = when(lang) {
            "es" -> "[ Cámara ]"
            "fr" -> "[ Caméra ]"
            "pt" -> "[ Câmera ]"
            else -> "[ Camera ]"
        }

        binding.btnInventory.setOnClickListener {
            findNavController().navigate(R.id.action_scannerFragment_to_inventoryFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}