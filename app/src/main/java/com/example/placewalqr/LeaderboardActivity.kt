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
    private lateinit var currentUserTextView: TextView
    private lateinit var adapter: LeaderboardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.leaderboard_activity)

        leaderboardRecyclerView = findViewById(R.id.leaderboard_recycler_view)

        leaderboardRecyclerView.layoutManager = LinearLayoutManager(this)

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
                val response = RetrofitInstance.apiService.getLeaderboard(nickname)
                if (response.isSuccessful && response.body() != null) {
                    val leaderboardData = response.body()!!


                    adapter = LeaderboardAdapter(leaderboardData)
                    leaderboardRecyclerView.adapter = adapter

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
