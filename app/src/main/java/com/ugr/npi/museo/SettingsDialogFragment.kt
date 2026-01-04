package com.ugr.npi.museo

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import androidx.fragment.app.DialogFragment

/**
 * Dialog Fragment that presents the Settings UI.
 * Allows users to change Language, Font Scale, and Theme.
 * Changes are applied via `SettingsManager` and require Activity recreation.
 */
class SettingsDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_settings, null)

        val seekBarFont = view.findViewById<SeekBar>(R.id.seekbar_font_size)
        val rgLanguage = view.findViewById<RadioGroup>(R.id.rg_language)
        val rgTheme = view.findViewById<RadioGroup>(R.id.rg_theme)
        val btnSave = view.findViewById<Button>(R.id.btn_save_settings)

        // Load current settings
        val currentFontScale = SettingsManager.getFontScale(requireContext())
        seekBarFont.progress = when (currentFontScale) {
            0.8f -> 0
            1.0f -> 1
            1.2f -> 2
            else -> 1
        }

        val currentLang = SettingsManager.getLanguage(requireContext())
        if (currentLang == "es") {
            rgLanguage.check(R.id.rb_spanish)
        } else {
            rgLanguage.check(R.id.rb_english)
        }

        // Theme logic - simplistic for now as we don't persist theme in ID form in Manager yet, 
        // assuming standard is default.
        // For now, let's just make UI consistent.
        rgTheme.check(R.id.rb_theme_standard)


        btnSave.setOnClickListener {
            // Save Language
            val selectedLangId = rgLanguage.checkedRadioButtonId
            val langCode = if (selectedLangId == R.id.rb_spanish) "es" else "en"
            SettingsManager.setLanguage(requireContext(), langCode)

            // Save Font Scale
            val scale = when (seekBarFont.progress) {
                0 -> 0.8f
                1 -> 1.0f
                2 -> 1.2f
                else -> 1.0f
            }
            SettingsManager.setFontScale(requireContext(), scale)

            // Save Theme (Mock implementation for now)
            // val selectedThemeId = rgTheme.checkedRadioButtonId
            // TODO: Implement theme switch logic

            dismiss()
            // Recreate activity to apply changes immediately
            requireActivity().recreate()
        }

        builder.setView(view)
        return builder.create()
    }
}
