package com.example.placewalqr

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class HomepageFragment : Fragment(R.layout.homepage_activity){
    lateinit var user: String
    lateinit var email: String
    private var id=0
    private var see_sights=0
    private var points=0
    private var last_place=""
    private lateinit var points_label: TextView
    private lateinit var seesights_label: TextView
    private lateinit var lastplace_label: TextView
    private lateinit var photoImageView: ImageView
    private lateinit var logoutBtn: Button
    private lateinit var novisited_txt: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?){
        super.onViewCreated(view, savedInstanceState)
        loadHomePage()

        val welcome_label = view.findViewById<TextView>(R.id.welcome_label)
        val welcome_text=welcome_label.text.toString() +" "+ user
        welcome_label.setText(welcome_text)

        seesights_label = view.findViewById<TextView>(R.id.seesights)
        points_label= view.findViewById<TextView>(R.id.points)
        lastplace_label= view.findViewById<TextView>(R.id.lastplace)
        novisited_txt=view.findViewById<TextView>(R.id.no_visited)
        photoImageView=view.findViewById<ImageView>(R.id.photo_visited)
        photoImageView.visibility=View.GONE
        logoutBtn=view.findViewById<Button>(R.id.logout_btn)
        logoutBtn.setOnClickListener {
            val intent = Intent(requireContext(), MainActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }
    }

    private fun loadHomePage(){
        val sharedPreferences=requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        user=sharedPreferences.getString("nickname", "").toString()
        email=sharedPreferences.getString("email", "").toString()
        id=sharedPreferences.getString("id", "0").toString().toInt()

        lifecycleScope.launch {
            try {
                val responsePoint= RetrofitInstance.apiService.getPointsById(id)
                val responsePlace = RetrofitInstance.apiService.findLastPlaceById(id)

                if(responsePoint.code()==200){
                    seesights_label.visibility= View.VISIBLE
                    points_label.visibility= View.VISIBLE
                    lastplace_label.visibility= View.VISIBLE
                    novisited_txt.visibility= View.GONE
                    val body=responsePoint.body()
                    points= body?.points ?: 0
                    val pointsText=points_label.text.toString() + " " + points.toString()
                    points_label.setText(pointsText)
                    see_sights=body?.count ?: 0
                    val seeSightText=seesights_label.text.toString() + " " + see_sights.toString()
                    seesights_label.setText(seeSightText)
                }else if(responsePoint.code()==404){
                    seesights_label.visibility= View.GONE
                    points_label.visibility= View.GONE
                    lastplace_label.visibility= View.GONE
                    novisited_txt.visibility= View.VISIBLE
                    photoImageView.visibility= View.GONE
                }

                if(responsePlace.code()==200){
                    val body=responsePlace.body()
                    last_place=body?.name?:"None"
                    val lastPlaceText=lastplace_label.text.toString() + " " + last_place
                    lastplace_label.setText(lastPlaceText)

                    val imageBytes=body?.getImageBytes()
                    if(imageBytes!=null && imageBytes.isNotEmpty()){
                        val bitmap= BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        if(bitmap!=null){
                            photoImageView.setImageBitmap(bitmap)
                            photoImageView.visibility= View.VISIBLE
                        }
                    }
                }
            }catch (e: Exception){

            }
        }
    }
}