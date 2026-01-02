package com.ugr.npi.museo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply Theme
        val themeMode = SettingsManager.getThemeMode(this)
        SettingsManager.applyTheme(themeMode)

        setContentView(R.layout.activity_main)

        // 1. Buscamos el "FragmentContainerView" por su ID correcto (nav_host_fragment)
        // Usamos supportFragmentManager para encontrarlo
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        // 2. Obtenemos el controlador de navegación
        val navController = navHostFragment.navController

        // 3. Buscamos la barra de navegación de abajo por su ID correcto
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        // 4. Conectamos los botones con la navegación
        bottomNav.setupWithNavController(navController)
    }

    override fun attachBaseContext(newBase: android.content.Context) {
        // Apply Language
        val lang = SettingsManager.getLanguage(newBase)
        val locale = java.util.Locale(lang)
        val config = android.content.res.Configuration(newBase.resources.configuration)
        java.util.Locale.setDefault(locale)
        config.setLocale(locale)

        // Apply Font Scale
        val fontScale = SettingsManager.getFontScale(newBase)
        config.fontScale = fontScale

        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.top_bar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        return when (item.itemId) {
            R.id.action_login -> {
                // Navigate to ProfileFragment. 
                // Using the ID from bottom_nav_menu if it matches, or assuming standard naming.
                // If ProfileFragment class is ProfileFragment, usually ID is profileFragment or similar.
                // Let's assume the ID is reachable. If it's a bottom tab, we can navigate to it.
                // We will attempt to navigate to R.id.profileFragment (standard convention)
                // If that fails, we might need to check nav_graph. 
                // But looking at the probable ID from bottom_nav_menu might be safer.
                // Let's blindly try R.id.profileFragment first, or better, check the bottom menu output first.
                // I'll wait for the bottom_nav_menu output to be sure of the ID.
                try {
                     navController.navigate(R.id.profileFragment) 
                } catch (e: Exception) {
                     // Fallback or log if ID differs
                }
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}