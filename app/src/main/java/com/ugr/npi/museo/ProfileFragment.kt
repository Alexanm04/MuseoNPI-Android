package com.ugr.npi.museo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Spinner
import android.widget.SeekBar
import android.widget.RadioGroup
import android.widget.RadioButton
import android.widget.ArrayAdapter
import android.widget.AdapterView
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class ProfileFragment : Fragment() {

    private lateinit var layoutLogin: LinearLayout
    private lateinit var layoutRegister: LinearLayout
    private lateinit var layoutAuthenticated: LinearLayout

    private var isInitializing = true


    private lateinit var etLoginUser: EditText
    private lateinit var etLoginPass: EditText
    private lateinit var etRegisterUser: EditText
    private lateinit var etRegisterEmail: EditText
    private lateinit var etRegisterPass: EditText

    // Settings Views
    private lateinit var btnSettingsToggle: ImageButton
    private lateinit var layoutSettingsPanel: LinearLayout
    private lateinit var btnBackSettings: ImageButton
    private lateinit var spinnerLanguage: Spinner
    private lateinit var seekbarFontSize: SeekBar
    private lateinit var radioGroupTheme: RadioGroup
    private lateinit var rbThemeLight: RadioButton
    private lateinit var rbThemeDark: RadioButton
    private lateinit var rbThemeColorblind: RadioButton

    private lateinit var tvWelcome: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        
        initViews(view)
        
        if (savedInstanceState != null && savedInstanceState.getBoolean("IS_SETTINGS_OPEN", false)) {
            layoutSettingsPanel.visibility = View.VISIBLE
            btnSettingsToggle.visibility = View.GONE
        }
        
        setupListeners()
        updateUI()

        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("IS_SETTINGS_OPEN", layoutSettingsPanel.visibility == View.VISIBLE)
    }

    private fun initViews(view: View) {
        layoutLogin = view.findViewById(R.id.layout_login)
        layoutRegister = view.findViewById(R.id.layout_register)
        layoutAuthenticated = view.findViewById(R.id.layout_authenticated)

        etLoginUser = view.findViewById(R.id.et_login_username)
        etLoginPass = view.findViewById(R.id.et_login_password)
        
        etRegisterUser = view.findViewById(R.id.et_register_username)
        etRegisterEmail = view.findViewById(R.id.et_register_email)
        etRegisterPass = view.findViewById(R.id.et_register_password)

        tvWelcome = view.findViewById(R.id.tv_welcome)

        // Settings Init
        btnSettingsToggle = view.findViewById(R.id.btn_settings_toggle)
        layoutSettingsPanel = view.findViewById(R.id.layout_settings_panel)
        btnBackSettings = view.findViewById(R.id.btn_back_settings)
        spinnerLanguage = view.findViewById(R.id.spinner_language)
        seekbarFontSize = view.findViewById(R.id.seekbar_font_size)
        radioGroupTheme = view.findViewById(R.id.radio_group_theme)
        rbThemeLight = view.findViewById(R.id.rb_theme_light)
        rbThemeDark = view.findViewById(R.id.rb_theme_dark)
        rbThemeColorblind = view.findViewById(R.id.rb_theme_colorblind)

        resizeIcons()
    }

    private fun resizeIcons() {
        val fontScale = SettingsManager.getFontScale(requireContext())
        
        // Explicit Step Scaling Logic for Icons in Profile
        // Small: < 0.9
        // Large: > 1.1
        // Normal: else
        
        // Base Sizes (Standard)
        // Settings Toggle: 48dp
        // Back Arrow: 40dp
        // Fingerprint: 64dp
        
        // Multipliers
        val multiplier = when {
            fontScale > 1.1f -> 1.5f // 50% bigger for Large
            fontScale < 0.9f -> 0.8f // 20% smaller for Small
            else -> 1.0f
        }

        // Helper to scale generic view
        fun scaleView(view: View, baseSizeDp: Float) {
            val sizePx = android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_DIP,
                baseSizeDp * multiplier,
                resources.displayMetrics
            ).toInt()
            val params = view.layoutParams
            params.width = sizePx
            params.height = sizePx
            view.layoutParams = params
        }

        // Apply to Settings Toggle (Base 48dp)
        scaleView(btnSettingsToggle, 48f)
        
        // Apply to Fingerprint/Lock Icon (Base 64dp)
        // Note: It's inside layout_login, verify valid reference
        val btnFinger = layoutLogin.findViewById<ImageButton>(R.id.btn_fingerprint)
        if (btnFinger != null) {
            scaleView(btnFinger, 64f)
        }
        
        // Apply to Back Settings (Base 40dp)
        scaleView(btnBackSettings, 40f)
    }

    private fun setupSettings() {
        isInitializing = true
        // pendingRecreate = false // No longer needed
        
        // --- Language ---
        val languages = arrayOf("Español", "English")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLanguage.adapter = adapter
        
        // Set current selection
        val currentLang = SettingsManager.getLanguage(requireContext())
        if (currentLang == "en") spinnerLanguage.setSelection(1) else spinnerLanguage.setSelection(0)

        spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (isInitializing) return
                
                val selectedCode = if (position == 0) "es" else "en"
                if (selectedCode != SettingsManager.getLanguage(requireContext())) {
                    SettingsManager.setLanguage(requireContext(), selectedCode)
                    // Immediate apply
                    requireActivity().recreate()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // --- Font Size ---
        val currentScale = SettingsManager.getFontScale(requireContext())
        val progress = when {
             currentScale < 0.9f -> 0
             currentScale > 1.2f -> 2
             else -> 1
        }
        seekbarFontSize.progress = progress

        seekbarFontSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // For Seekbar, we might want to wait for "StopTracking" to avoid spamming recreate,
                // BUT user requested "when I change... immediately". 
                // However, spamming recreate on every tick of slide is bad UX and performance.
                // Let's do it on StopTrackingTouch for sliding, or debounce. 
                // But simplified requirement interpretation: Apply on change. 
                // To be safe and usable, I'll apply on StopTrackingTouch OR explicit click.
                // Actually, standard behavior for font size sliders is often "apply on release".
                // I will move logic to onStopTrackingTouch to prevent crash/lag loop while sliding.
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                 if (seekBar == null) return
                 val progress = seekBar.progress
                 val newScale = when(progress) {
                     0 -> 0.80f
                     2 -> 1.30f
                     else -> 1.0f
                 }
                 if (newScale != SettingsManager.getFontScale(requireContext())) {
                     SettingsManager.setFontScale(requireContext(), newScale)
                     requireActivity().recreate()
                 }
            }
        })

        // --- Theme ---
        val currentTheme = SettingsManager.getThemeMode(requireContext())
        when(currentTheme) {
            1 -> rbThemeDark.isChecked = true
            2 -> rbThemeColorblind.isChecked = true
            else -> rbThemeLight.isChecked = true
        }

        radioGroupTheme.setOnCheckedChangeListener { _, checkedId ->
            if (isInitializing) return@setOnCheckedChangeListener
            
            val newMode = when(checkedId) {
                R.id.rb_theme_dark -> 1
                R.id.rb_theme_colorblind -> 2
                else -> 0
            }
            if (newMode != SettingsManager.getThemeMode(requireContext())) {
                SettingsManager.setThemeMode(requireContext(), newMode)
                // Immediate apply
                requireActivity().recreate()
            }
        }
        
        isInitializing = false
    }

    private fun closeSettings() {
        layoutSettingsPanel.visibility = View.GONE
        btnSettingsToggle.visibility = View.VISIBLE
    }

    private var startBiometricEnrollment = false

    private fun setupListeners() {
        // Login Flow
        etLoginUser.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                 val bioUser = UserManager.getBiometricUser(requireContext())
                 if (bioUser != null) {
                     startBiometricEnrollment = false
                     showBiometricPrompt()
                 }
            }
        }

        layoutLogin.findViewById<Button>(R.id.btn_login).setOnClickListener {
            val user = etLoginUser.text.toString()
            val pass = etLoginPass.text.toString()
            if (UserManager.login(requireContext(), user, pass)) {
                updateUI()
                CustomToast.show(requireContext(), "Login Correcto")
            } else {
                CustomToast.show(requireContext(), "Credenciales inválidas", true)
            }
        }

        layoutLogin.findViewById<TextView>(R.id.tv_go_to_register).setOnClickListener {
            layoutLogin.visibility = View.GONE
            layoutRegister.visibility = View.VISIBLE
        }

        layoutLogin.findViewById<ImageButton>(R.id.btn_fingerprint).setOnClickListener {
            startBiometricEnrollment = false
            showBiometricPrompt()
        }

        // Register Flow
        layoutRegister.findViewById<Button>(R.id.btn_register).setOnClickListener {
            val user = etRegisterUser.text.toString()
            val pass = etRegisterPass.text.toString()
            val email = etRegisterEmail.text.toString()

            if (user.isEmpty() || pass.isEmpty() || email.isEmpty()) {
                CustomToast.show(requireContext(), "Rellena todos los campos", true)
                return@setOnClickListener
            }

            val newUser = User(user, pass, email)
            if (UserManager.register(requireContext(), newUser)) {
                CustomToast.show(requireContext(), "Cuenta creada")
                updateUI() 
            } else {
                CustomToast.show(requireContext(), "El usuario ya existe", true)
            }
        }

        layoutRegister.findViewById<TextView>(R.id.tv_go_to_login).setOnClickListener {
            layoutRegister.visibility = View.GONE
            layoutLogin.visibility = View.VISIBLE
        }

        // Authenticated Flow
        layoutAuthenticated.findViewById<Button>(R.id.btn_logout).setOnClickListener {
            UserManager.logout()
            updateUI()
        }

        layoutAuthenticated.findViewById<Button>(R.id.btn_enable_biometric).setOnClickListener {
             // Now we show prompt to "verify" (simulated enrollment) the fingerprint
             startBiometricEnrollment = true
             showBiometricPrompt()
        }
        
        // Settings Listeners
        btnSettingsToggle.setOnClickListener {
            layoutSettingsPanel.visibility = View.VISIBLE
            btnSettingsToggle.visibility = View.GONE
        }

        btnBackSettings.setOnClickListener {
            closeSettings()
        }
        
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, 
            object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (layoutSettingsPanel.visibility == View.VISIBLE) {
                        closeSettings()
                    } else {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )
        
        setupSettings()
    }

    private fun updateUI() {
        val currentUser = UserManager.getCurrentUser()
        if (currentUser != null) {
            layoutLogin.visibility = View.GONE
            layoutRegister.visibility = View.GONE
            layoutAuthenticated.visibility = View.VISIBLE
            tvWelcome.text = "Bienvenido, ${currentUser.username}"
            
            val btnEnableBio = layoutAuthenticated.findViewById<Button>(R.id.btn_enable_biometric)
            if (currentUser.biometricEnabled) {
                btnEnableBio.visibility = View.GONE
            } else {
                btnEnableBio.visibility = View.VISIBLE
            }

        } else {
            layoutLogin.visibility = View.VISIBLE
            layoutRegister.visibility = View.GONE
            layoutAuthenticated.visibility = View.GONE
        }
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(requireContext())
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    
                    if (startBiometricEnrollment) {
                        // ENROLL MODE
                        UserManager.enableBiometric(requireContext())
                        CustomToast.show(requireContext(), "Huella vinculada correctamente")
                        updateUI()
                    } else {
                        // LOGIN MODE
                        val user = UserManager.getBiometricUser(requireContext())
                        if (user != null) {
                            etLoginUser.setText(user.username)
                            etLoginPass.setText(user.password)
                            if (UserManager.login(requireContext(), user.username, user.password)) {
                                updateUI()
                                CustomToast.show(requireContext(), "Login con Huella Exitoso")
                            }
                        } else {
                             CustomToast.show(requireContext(), "No hay usuario vinculado a la huella", true)
                        }
                    }
                    startBiometricEnrollment = false
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    startBiometricEnrollment = false
                    if (errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS || errorCode == 11) { // 11 is generic no enrolled
                         CustomToast.show(requireContext(), "Debes registrar una huella en los Ajustes de Android primero", true)
                    } else {
                        CustomToast.show(requireContext(), "Error: $errString", true)
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    CustomToast.show(requireContext(), "Huella no reconocida", true)
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(if (startBiometricEnrollment) "Vincular Huella" else "Iniciar sesión con huella")
            .setSubtitle("Toca el sensor")
            .setNegativeButtonText("Cancelar")
            .build()
        
        val biometricManager = BiometricManager.from(requireContext())
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                biometricPrompt.authenticate(promptInfo)
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                 CustomToast.show(requireContext(), "IMPORTANTE: Registra una huella en Ajustes de Android.", true)
            }
            else -> {
                // Try anyway or show error
                biometricPrompt.authenticate(promptInfo)
            }
        }
    }
    
    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ProfileFragment().apply { }
    }
}