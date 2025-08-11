package com.example.placewalqr

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AchievementAdapter(private val placeList: List<Place>) :
    RecyclerView.Adapter<AchievementAdapter.AchievementViewHolder>() {

    class AchievementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val placeNameTextView: TextView = itemView.findViewById(R.id.placeName)
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_achievement, parent, false)
        return AchievementViewHolder(view)
    }

    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        val place = placeList[position]
        holder.placeNameTextView.text = place.name

        val base64String = place.imageBase64

        // Se la stringa ha prefisso tipo "data:image/png;base64," rimuovilo
        val cleanBase64 = if (base64String.contains(",")) {
            base64String.substringAfter(",")
        } else {
            base64String
        }

        // Decodifica Base64 in byte[]
        val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)

        // Crea bitmap dai byte decodificati
        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

        // Setta la bitmap nell'imageView
        holder.imageView.setImageBitmap(bitmap)
    }

    override fun getItemCount(): Int = placeList.size
}
