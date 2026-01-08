package com.ugr.npi.museo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.ugr.npi.museo.databinding.FragmentScannerBinding
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScannerFragment : Fragment() {

    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()

        setupUI()
        checkCameraPermission()

        binding.btnInventory.setOnClickListener {
            findNavController().navigate(R.id.action_scannerFragment_to_inventoryFragment)
        }
    }

    private fun setupUI() {
        val lang = Locale.getDefault().language
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
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    viewLifecycleOwner, cameraSelector, preview, imageAnalyzer
                )
            } catch (exc: Exception) {
                Log.e("ScannerFragment", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val scanner = BarcodeScanning.getClient()

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val rawValue = barcode.rawValue
                        if (rawValue != null) {
                            handleQRCode(rawValue)
                            // Detenemos el escaneo una vez encontrado un código válido para evitar múltiples navegaciones
                            cameraProvider?.unbindAll()
                            break
                        }
                    }
                }
                .addOnFailureListener {
                    Log.e("ScannerFragment", "Barcode scanning failed", it)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun handleQRCode(content: String) {
        try {
            val gson = Gson()
            val mapType = object : TypeToken<Map<String, String>>() {}.type
            val data: Map<String, String> = gson.fromJson(content, mapType)

            val type = data["t"]
            val id = data["id"]

            if (type == "o" && id != null) {
                navigateToDetail(id)
            } else if (type == "u" && id != null) {
                // Tratamiento de ubicación (futuro)
                activity?.runOnUiThread {
                    Toast.makeText(context, "Ubicación detectada: $id", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e("ScannerFragment", "Error parsing QR JSON", e)
        }
    }

    private fun navigateToDetail(objectIdStr: String) {
        val objectId = objectIdStr.toIntOrNull() ?: return
        
        // Cargar objetos desde JSON para encontrar el objeto por ID
        val jsonString = context?.assets?.open("objetos.json")?.bufferedReader().use { it?.readText() }
        if (jsonString != null) {
            val listType = object : TypeToken<List<MuseoObject>>() {}.type
            val objects: List<MuseoObject> = Gson().fromJson(jsonString, listType)
            val museoObject = objects.find { it.id == objectId }

            if (museoObject != null) {
                activity?.runOnUiThread {
                    val bundle = Bundle().apply {
                        putParcelable("museoObject", museoObject)
                    }
                    findNavController().navigate(R.id.action_scannerFragment_to_objectDetailFragment, bundle)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }
}