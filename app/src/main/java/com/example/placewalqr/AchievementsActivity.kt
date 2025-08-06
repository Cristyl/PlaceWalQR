package com.example.placewalqr

import android.os.Bundle
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AchievementsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.achievements_activity)

        val nickname = "MarioRossi" // simulazione dinamica

        val nicknameTextView = findViewById<TextView>(R.id.nicknameTextView)
        nicknameTextView.text = getString(R.string.nickname_placeholder, nickname)

        val achievements = listOf(
            Achievement("Colosseo", R.drawable.colosseo_mock),
            Achievement("Piazza San Marco", R.drawable.san_marco_mock),
            Achievement("Duomo di Milano", R.drawable.duomo_milano_mock)
        )

        val recyclerView = findViewById<RecyclerView>(R.id.achievementsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = AchievementAdapter(achievements)
    }
}
