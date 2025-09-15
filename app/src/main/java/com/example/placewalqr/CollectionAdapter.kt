package com.example.placewalqr

import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Adapter for collection places
class CollectionAdapter(private val placeList: List<Place>) :
    RecyclerView.Adapter<CollectionAdapter.AchievementViewHolder>() {

    // View holder for item
    class AchievementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val placeNameTextView: TextView = itemView.findViewById(R.id.placeName) // place name
        val imageView: ImageView = itemView.findViewById(R.id.imageView) // place image
    }

    // create item view
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_achievement, parent, false)
        return AchievementViewHolder(view)
    }

    // bind data to item
    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        val place = placeList[position]
        holder.placeNameTextView.text = place.name

        // load image from bytes
        val imageBytes = place.getImageBytes()

        if (imageBytes != null && imageBytes.isNotEmpty()) {
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            if (bitmap != null) {
                holder.imageView.setImageBitmap(bitmap) // show bitmap
                holder.imageView.visibility = View.VISIBLE
                Log.d("Adapter", "Loaded bitmap for ${place.name}, bytes=${imageBytes.size}")
            } else {
                Log.e("Adapter", "Bitmap decode failed for ${place.name}")
            }
        } else {
            Log.w("Adapter", "No image bytes for ${place.name}")
        }
    }

    // number of items
    override fun getItemCount(): Int = placeList.size
}
