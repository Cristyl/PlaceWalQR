package com.example.placewalqr

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class LeaderboardActivity : BaseActivity() {

    private lateinit var leaderboardRecyclerView: RecyclerView
    private lateinit var adapter: LeaderboardAdapter

    // Views per la card dell’utente
    private lateinit var userPositionText: TextView
    private lateinit var userNicknameText: TextView
    private lateinit var userScoreText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.leaderboard_activity)

        // Inizializza RecyclerView
        leaderboardRecyclerView = findViewById(R.id.leaderboard_recycler_view)
        leaderboardRecyclerView.layoutManager = LinearLayoutManager(this)

        // Inizializza card utente
        userPositionText = findViewById(R.id.user_position)
        userNicknameText = findViewById(R.id.user_nickname)
        userScoreText = findViewById(R.id.user_score)

        fetchLeaderboard()
    }

    private fun fetchLeaderboard() {
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val nickname = sharedPreferences.getString("nickname", null)

        if (nickname == null) {
            showToast("Utente non loggato")
            return
        }

        lifecycleScope.launch {
            try {
                // Chiamata API con parametro nickname
                val response = RetrofitInstance.apiService.getLeaderboard(nickname)
                if (response.isSuccessful && response.body() != null) {
                    val leaderboardData = response.body()!!

                    if (leaderboardData.isNotEmpty()) {
                        // Ultima entry = utente loggato
                        val userEntry = leaderboardData.last()

                        userPositionText.text = "#${userEntry.position}"
                        userNicknameText.text = userEntry.nickname
                        userScoreText.text = "${userEntry.total_points} pts"

                        // Tutti gli altri utenti (senza l’utente loggato)
                        val others = leaderboardData.dropLast(1)

                        adapter = LeaderboardAdapter(others)
                        leaderboardRecyclerView.adapter = adapter
                    }

                } else {
                    showToast("Errore nel caricamento dati")
                }
            } catch (e: IOException) {
                showToast("Errore di connessione")
            } catch (e: HttpException) {
                showToast("Errore nel server")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
