package com.example.placewalqr

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class AchievementsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.achievements_activity)

        val nicknameTextView = findViewById<TextView>(R.id.nicknameTextView)

        // Simula dinamicamente il nickname (potresti passarlo da un'altra Activity)
        val nickname = "MarioRossi"
        nicknameTextView.text = getString(R.string.nickname_placeholder, nickname)

        val recyclerView = findViewById<RecyclerView>(R.id.achievementsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            val response = RetrofitInstance.apiService.getPlacesByUser(nickname)
            if (response.isSuccessful) {
                val placesList = response.body() ?: emptyList()
                // Qui devi avere un adapter che accetta una lista di Place (o adattarlo)
                recyclerView.adapter = AchievementAdapter(placesList)
            } else {
                // Gestione errore (es. Toast o log)
            }
        }
    }
}
