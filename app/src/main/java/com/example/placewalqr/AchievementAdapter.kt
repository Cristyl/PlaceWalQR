package com.example.placewalqr

import android.graphics.BitmapFactory
import android.util.Log
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

        // Usa la stessa logica che funziona altrove
        val imageBytes = place.getImageBytes()

        if (imageBytes != null && imageBytes.isNotEmpty()) {
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            if (bitmap != null) {
                holder.imageView.setImageBitmap(bitmap)
                holder.imageView.visibility = View.VISIBLE
                Log.d("Adapter", "Loaded bitmap for ${place.name}, bytes=${imageBytes.size}")
            } else {
                Log.e("Adapter", "Bitmap decode failed for ${place.name}")
            }
        } else {
            Log.w("Adapter", "No image bytes for ${place.name}")
        }
    }

    override fun getItemCount(): Int = placeList.size
}
