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

class LeaderboardFragment : Fragment(R.layout.leaderboard_fragment) {

    private lateinit var leaderboardRecyclerView: RecyclerView
    private lateinit var adapter: LeaderboardAdapter

    private lateinit var userPositionText: TextView
    private lateinit var userNicknameText: TextView
    private lateinit var userScoreText: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        leaderboardRecyclerView = view.findViewById(R.id.leaderboard_recycler_view)
        leaderboardRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        userPositionText = view.findViewById(R.id.user_position)
        userNicknameText = view.findViewById(R.id.user_nickname)
        userScoreText = view.findViewById(R.id.user_score)

        fetchLeaderboard()
    }

    private fun fetchLeaderboard() {
        val sharedPreferences = requireActivity().getSharedPreferences(
            "UserPrefs",
            android.content.Context.MODE_PRIVATE
        )
        val nickname = sharedPreferences.getString("nickname", null)

        if (nickname == null) {
            showToast("User not logged in")
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val leaderboardData: List<LeaderboardEntry>

            try {
                val response = RetrofitInstance.apiService.getLeaderboard(nickname)
                if (!response.isSuccessful) {
                    showToast("Failed to load leaderboard: ${response.code()}")
                    return@launch
                }
                leaderboardData = response.body() ?: emptyList()
            } catch (e: IOException) {
                showToast("Connection error, please check your internet")
                return@launch
            } catch (e: HttpException) {
                showToast("Server error")
                return@launch
            }

            if (leaderboardData.isEmpty()) {
                showToast("No leaderboard entries found")
                return@launch
            }

            // Trova l’entry dell’utente loggato
            val userEntry = leaderboardData.find { it.nickname == nickname }

            // Top 10 escluso l’utente loggato
            val topEntries = leaderboardData.filter { it.nickname != nickname }

            adapter = LeaderboardAdapter(topEntries)
            leaderboardRecyclerView.adapter = adapter

            // Mostra la card dell’utente loggato separata
            userEntry?.let {
                userPositionText.text = getString(R.string.rank_format, it.position)
                userNicknameText.text = it.nickname
                userScoreText.text = getString(R.string.score_format, it.total_points)
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
