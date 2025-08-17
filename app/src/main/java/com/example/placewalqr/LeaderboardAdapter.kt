package com.example.placewalqr

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LeaderboardAdapter(private val entries: List<LeaderboardEntry>) :
    RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder>() {

    class LeaderboardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rankText: TextView = itemView.findViewById(R.id.rank_text)
        val usernameText: TextView = itemView.findViewById(R.id.username_text)
        val scoreText: TextView = itemView.findViewById(R.id.score_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaderboardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leaderboard, parent, false)
        return LeaderboardViewHolder(view)
    }

    override fun onBindViewHolder(holder: LeaderboardViewHolder, position: Int) {
        val entry = entries[position]
        val context = holder.itemView.context
        holder.rankText.text = context.getString(R.string.rank_format, entry.position)
        holder.usernameText.text = entry.nickname
        holder.scoreText.text = context.getString(R.string.score_format, entry.total_points)
    }

    override fun getItemCount(): Int = entries.size
}
