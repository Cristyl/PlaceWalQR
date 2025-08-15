package com.example.placewalqr

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.w3c.dom.Text

class HomepageActivity : BaseActivity(){
    lateinit var user: String
    lateinit var email: String
    private var id=0
    private var see_sights=0
    private var points=0
    private var last_place=""
    private lateinit var points_label: TextView
    private lateinit var seesights_label: TextView
    private lateinit var lastplace_label: TextView

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.homepage_activity)
        loadHomePage()

        val welcome_label = findViewById<TextView>(R.id.welcome_label)
        val welcome_text=welcome_label.text.toString() +" "+ user
        welcome_label.setText(welcome_text)

        seesights_label = findViewById<TextView>(R.id.seesights)
        points_label= findViewById<TextView>(R.id.points)
        lastplace_label= findViewById<TextView>(R.id.lastplace)
    }

    private fun loadHomePage(){
        val sharedPreferences=getSharedPreferences("UserPrefs", MODE_PRIVATE)
        user=sharedPreferences.getString("nickname", "").toString()
        email=sharedPreferences.getString("email", "").toString()
        id=sharedPreferences.getString("id", "0").toString().toInt()

        lifecycleScope.launch {
            try {
                val responsePoint= RetrofitInstance.apiService.getPointsById(id)
                val responsePlace = RetrofitInstance.apiService.findLastPlaceById(id)
                runOnUiThread {
                    if(responsePoint.code()==200){
                        val body=responsePoint.body()
                        points= body?.points ?: 0
                        val pointsText=points_label.text.toString() + " " + points.toString()
                        points_label.setText(pointsText)
                        see_sights=body?.count ?: 0
                        val seeSightText=seesights_label.text.toString() + " " + see_sights.toString()
                        seesights_label.setText(seeSightText)
                    }

                    if(responsePlace.code()==200){
                        val body=responsePlace.body()
                        last_place=body?.name?:"None"
                        val lastPlaceText=lastplace_label.text.toString() + " " + last_place
                        lastplace_label.setText(lastPlaceText)
                    }
                }
            }catch (e: Exception){

            }
        }
    }
}