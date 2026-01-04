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

/**
 * Fragment that handles the User Profile interaction.
 * It manages three distinct states via visibility toggling:
 * 1. Login (Credentials or Biometric)
 * 2. Register (New Account)
 * 3. Authenticated (User Profile, Rewards, Settings)
 */
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

    // Rewards Views
    private lateinit var btnRedeemRewards: Button
    private lateinit var layoutRewardsPanel: LinearLayout
    private lateinit var btnBackRewards: ImageButton
    private lateinit var tvUserPoints: TextView
    private lateinit var tvRewardsPoints: TextView
    private lateinit var btnRedeem1: Button
    private lateinit var btnRedeem2: Button
    private lateinit var btnRedeem3: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        
        initViews(view)
        
        if (savedInstanceState != null) {
             if (savedInstanceState.getBoolean("IS_SETTINGS_OPEN", false)) {
                layoutSettingsPanel.visibility = View.VISIBLE
                btnSettingsToggle.visibility = View.GONE
             }
             if (savedInstanceState.getBoolean("IS_REWARDS_OPEN", false)) {
                 layoutRewardsPanel.visibility = View.VISIBLE
             }
        }
        
        setupListeners()
        updateUI()

        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("IS_SETTINGS_OPEN", layoutSettingsPanel.visibility == View.VISIBLE)
        outState.putBoolean("IS_REWARDS_OPEN", layoutRewardsPanel.visibility == View.VISIBLE)
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
        tvUserPoints = view.findViewById(R.id.tv_user_points)

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
        
        // Rewards Init
        btnRedeemRewards = view.findViewById(R.id.btn_redeem_rewards)
        layoutRewardsPanel = view.findViewById(R.id.layout_rewards_panel)
        btnBackRewards = view.findViewById(R.id.btn_back_rewards)
        tvRewardsPoints = view.findViewById(R.id.tv_rewards_points)
        btnRedeem1 = view.findViewById(R.id.btn_redeem_1)
        btnRedeem2 = view.findViewById(R.id.btn_redeem_2)
        btnRedeem3 = view.findViewById(R.id.btn_redeem_3)

        resizeIcons()
    }

    private fun resizeIcons() {
        val fontScale = SettingsManager.getFontScale(requireContext())
        
        // Manual scaling for Icons to ensure they match the text size preferences.
        // Standard Android icons don't always scale with FontScale automatically.
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
        
        // Apply to Back Rewards (Base 40dp)
        scaleView(btnBackRewards, 40f)
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
    
    private fun closeRewards() {
        layoutRewardsPanel.visibility = View.GONE
    }
    
    private fun updatePointsDisplay() {
        val user = UserManager.getCurrentUser()
        val points = user?.points ?: 0
        tvUserPoints.text = getString(R.string.mis_puntos, points)
        tvRewardsPoints.text = getString(R.string.mis_puntos, points)
    }

    private fun attemptRedeem(cost: Int) {
        if (UserManager.deductPoints(requireContext(), cost)) {
            CustomToast.show(requireContext(), getString(R.string.recompensa_canjeada))
            updatePointsDisplay()
        } else {
            CustomToast.show(requireContext(), getString(R.string.puntos_insuficientes), true)
        }
    }

    private var startBiometricEnrollment = false

    private fun setupListeners() {
        // --- Login Flow Listeners ---
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
                CustomToast.show(requireContext(), getString(R.string.login_correcto))
            } else {
                CustomToast.show(requireContext(), getString(R.string.credenciales_invalidas), true)
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

        // --- Register Flow Listeners ---
        layoutRegister.findViewById<Button>(R.id.btn_register).setOnClickListener {
            val user = etRegisterUser.text.toString()
            val pass = etRegisterPass.text.toString()
            val email = etRegisterEmail.text.toString()

            if (user.isEmpty() || pass.isEmpty() || email.isEmpty()) {
                CustomToast.show(requireContext(), getString(R.string.rellena_campos), true)
                return@setOnClickListener
            }

            val newUser = User(user, pass, email)
            if (UserManager.register(requireContext(), newUser)) {
                CustomToast.show(requireContext(), getString(R.string.cuenta_creada))
                updateUI() 
            } else {
                CustomToast.show(requireContext(), getString(R.string.usuario_existe), true)
            }
        }

        layoutRegister.findViewById<TextView>(R.id.tv_go_to_login).setOnClickListener {
            layoutRegister.visibility = View.GONE
            layoutLogin.visibility = View.VISIBLE
        }

        // --- Authenticated Flow Listeners ---
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
        
        // Rewards Listeners
        btnRedeemRewards.setOnClickListener {
            layoutRewardsPanel.visibility = View.VISIBLE
            updatePointsDisplay()
        }
        
        btnBackRewards.setOnClickListener {
            closeRewards()
        }
        
        btnRedeem1.setOnClickListener { attemptRedeem(50) }
        btnRedeem2.setOnClickListener { attemptRedeem(200) }
        btnRedeem3.setOnClickListener { attemptRedeem(5000) }
        
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, 
            object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (layoutSettingsPanel.visibility == View.VISIBLE) {
                        closeSettings()
                    } else if (layoutRewardsPanel.visibility == View.VISIBLE) {
                        closeRewards()
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
            tvWelcome.text = getString(R.string.bienvenido_user, currentUser.username)
            updatePointsDisplay()
            
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
                        // ENROLL MODE: Link current user to this device/fingerprint
                        UserManager.enableBiometric(requireContext())
                        CustomToast.show(requireContext(), getString(R.string.huella_vinculada))
                        updateUI()
                    } else {
                        // LOGIN MODE
                        val user = UserManager.getBiometricUser(requireContext())
                        if (user != null) {
                            etLoginUser.setText(user.username)
                            etLoginPass.setText(user.password)
                            if (UserManager.login(requireContext(), user.username, user.password)) {
                                updateUI()
                                CustomToast.show(requireContext(), getString(R.string.login_huella_exitoso))
                            }
                        } else {
                             CustomToast.show(requireContext(), getString(R.string.no_usuario_huella), true)
                        }
                    }
                    startBiometricEnrollment = false
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    startBiometricEnrollment = false
                    if (errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS || errorCode == 11) { // 11 is generic no enrolled
                         CustomToast.show(requireContext(), getString(R.string.registra_huella_android), true)
                    } else {
                        CustomToast.show(requireContext(), getString(R.string.error_prefix, errString), true)
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    CustomToast.show(requireContext(), getString(R.string.huella_no_reconocida), true)
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(if (startBiometricEnrollment) getString(R.string.vincular_huella_titulo) else getString(R.string.iniciar_sesion_huella_titulo))
            .setSubtitle(getString(R.string.toca_sensor))
            .setNegativeButtonText(getString(R.string.cancelar))
            .build()
        
        val biometricManager = BiometricManager.from(requireContext())
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                biometricPrompt.authenticate(promptInfo)
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                 CustomToast.show(requireContext(), getString(R.string.importante_registra_huella), true)
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