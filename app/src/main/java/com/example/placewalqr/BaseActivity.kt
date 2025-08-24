package com.example.placewalqr

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.fragment.app.Fragment

class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base)

        setupBottomNavigation()

        // Mostra la homepage di default se è la prima volta
        if (savedInstanceState == null) {
            navigateToFragment(HomepageFragment())
        }
    }

    private fun setupBottomNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val btnCamera = findViewById<ImageButton>(R.id.btn_camera)

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    navigateToFragment(HomepageFragment())
                    true
                }
                R.id.nav_map -> {
                    navigateToFragment(MapsFragment())
                    true
                }
                R.id.nav_achievements -> {
                    navigateToFragment(AchievementsFragment())
                    true
                }
                R.id.nav_leaderboard -> {
                    navigateToFragment(LeaderboardFragment())
                    true
                }
                else -> false
            }
        }

        // ATTENZIONE: modificato il contenuto di navigateToFragment
        // per gestire frammento convertito in grafica Jetpack Compose
        btnCamera.setOnClickListener {
            navigateToFragment(CameraComposeFragment())
        }
    }

    private fun navigateToFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, fragment)
            .commit()
    }
}
