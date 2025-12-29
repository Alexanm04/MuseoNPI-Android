package com.ugr.npi.museo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
}