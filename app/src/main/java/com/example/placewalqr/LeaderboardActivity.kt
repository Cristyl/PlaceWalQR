package com.example.placewalqr

import android.os.Bundle
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class LeaderboardActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.leaderboard_activity)

        val leaderboardRecyclerView = findViewById<RecyclerView>(R.id.leaderboard_recycler_view)
        val currentUserTextView = findViewById<TextView>(R.id.current_user_rank)

        val leaderboardData = listOf(
            LeaderboardEntry(1, "Mario", 2500),
            LeaderboardEntry(2, "Luigi", 2300),
            LeaderboardEntry(3, "Anna", 2150),
            LeaderboardEntry(4, "Lucia", 2000),
            LeaderboardEntry(5, "Marco", 1980),
            LeaderboardEntry(6, "Francesca", 1850),
            LeaderboardEntry(7, "Stefano", 1750),
            LeaderboardEntry(8, "Elena", 1700),
            LeaderboardEntry(9, "Giorgio", 1650),
            LeaderboardEntry(10, "Sara", 1600)
        )

        leaderboardRecyclerView.layoutManager = LinearLayoutManager(this)
        leaderboardRecyclerView.adapter = LeaderboardAdapter(leaderboardData)

        val currentUserNickname = "Cristian"
        val currentUserScore = 1234
        val currentUserPosition = 11
        currentUserTextView.text = getString(
            R.string.current_user_info,
            currentUserPosition,
            currentUserNickname,
            currentUserScore
        )
    }
}
