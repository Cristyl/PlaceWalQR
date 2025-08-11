package com.example.placewalqr

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class AchievementsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.achievements_activity)

        val recyclerView = findViewById<RecyclerView>(R.id.achievementsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val nickname = sharedPreferences.getString("nickname", null)

        if (nickname != null) {
            lifecycleScope.launch {
                val response = RetrofitInstance.apiService.getPlacesByUser(nickname)
                if (response.isSuccessful) {
                    val placesList = response.body() ?: emptyList()
                    recyclerView.adapter = AchievementAdapter(placesList)
                } else {
                    // Gestione errore
                }
            }
        } else {
            // Nessun nickname salvato → probabilmente l'utente non è loggato
        }

    }
}
