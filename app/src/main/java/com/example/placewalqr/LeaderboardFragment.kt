package com.example.placewalqr

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class LeaderboardFragment : Fragment(R.layout.leaderboard_activity) {

    private lateinit var leaderboardRecyclerView: RecyclerView
    private lateinit var adapter: LeaderboardAdapter

    // Views per la card dell’utente
    private lateinit var userPositionText: TextView
    private lateinit var userNicknameText: TextView
    private lateinit var userScoreText: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inizializza RecyclerView
        leaderboardRecyclerView = view.findViewById(R.id.leaderboard_recycler_view)
        leaderboardRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Inizializza card utente
        userPositionText = view.findViewById(R.id.user_position)
        userNicknameText = view.findViewById(R.id.user_nickname)
        userScoreText = view.findViewById(R.id.user_score)

        fetchLeaderboard()
    }

    private fun fetchLeaderboard() {
        val sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE)
        val nickname = sharedPreferences.getString("nickname", null)

        if (nickname == null) {
            showToast("Utente non loggato")
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Chiamata API con parametro nickname
                val response = RetrofitInstance.apiService.getLeaderboard(nickname)
                if (response.isSuccessful && response.body() != null) {
                    val leaderboardData = response.body()!!

                    if (leaderboardData.isNotEmpty()) {
                        // Trova l’entry corrispondente all’utente loggato
                        val userEntry = leaderboardData.find { it.nickname == nickname }

                        userEntry?.let {
                            userPositionText.text = getString(R.string.rank_format, it.position)
                            userNicknameText.text = it.nickname
                            userScoreText.text = getString(R.string.score_format, it.total_points)
                        }

                        // Mostra tutti gli altri (la lista intera)
                        adapter = LeaderboardAdapter(leaderboardData)
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
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
