package com.example.placewalqr

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Adapter for leaderboard RecyclerView
class LeaderboardAdapter(private val entries: List<LeaderboardEntry>) :
    RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder>() {

    // ViewHolder for leaderboard item
    class LeaderboardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rankText: TextView = itemView.findViewById(R.id.rank_text)       // Rank
        val usernameText: TextView = itemView.findViewById(R.id.username_text) // Username
        val scoreText: TextView = itemView.findViewById(R.id.score_text)      // Total points
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaderboardViewHolder {
        // Inflate item layout
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leaderboard, parent, false)
        return LeaderboardViewHolder(view)
    }

    override fun onBindViewHolder(holder: LeaderboardViewHolder, position: Int) {
        val entry = entries[position]
        val context = holder.itemView.context
        holder.rankText.text = context.getString(R.string.rank_format, entry.position) // Set rank
        holder.usernameText.text = entry.nickname                                       // Set nickname
        holder.scoreText.text = context.getString(R.string.score_format, entry.total_points) // Set score
    }

    override fun getItemCount(): Int = entries.size // Number of items
}