package com.example.placewalqr

import android.content.Intent
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.widget.RelativeLayout

abstract class BaseActivity : AppCompatActivity() {

    override fun setContentView(layoutResID: Int) {
        val fullLayout = layoutInflater.inflate(R.layout.activity_base, null) as RelativeLayout
        val activityContainer = fullLayout.findViewById<FrameLayout>(R.id.content_frame)
        layoutInflater.inflate(layoutResID, activityContainer, true)
        super.setContentView(fullLayout)

        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val btnCamera = findViewById<ImageButton>(R.id.btn_camera)

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    if (this !is HomepageActivity) {
                        bottomNavigationView.menu.findItem(R.id.nav_home).isChecked = true
                        startActivity(Intent(this, HomepageActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        })
                    }
                    true
                }
                R.id.nav_map -> {
                    if (this !is MapsActivity) {
                        bottomNavigationView.menu.findItem(R.id.nav_map).isChecked = true
                        startActivity(Intent(this, MapsActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        })
                    }
                    true
                }
                R.id.nav_achievements -> {
                    if (this !is AchievementsActivity) {
                        bottomNavigationView.menu.findItem(R.id.nav_achievements).isChecked = true
                        startActivity(Intent(this, AchievementsActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        })
                    }
                    true
                }
                R.id.nav_leaderboard -> {
                    if (this !is LeaderboardActivity) {
                        bottomNavigationView.menu.findItem(R.id.nav_leaderboard).isChecked = true
                        startActivity(Intent(this, LeaderboardActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        })
                    }
                    true
                }
                else -> false
            }
        }

        btnCamera.setOnClickListener {
            startActivity(Intent(this, CameraActivity::class.java))
        }
    }
}
