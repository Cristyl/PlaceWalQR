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

        seesights_label = findViewById<TextView>(R.id.seesights_text)
        points_label= findViewById<TextView>(R.id.points_text)
        lastplace_label= findViewById<TextView>(R.id.lastplace_text)
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
                        points_label.setText(points.toString())
                        see_sights=body?.count ?: 0
                        seesights_label.setText(see_sights.toString())
                    }

                    if(responsePlace.code()==200){
                        val body=responsePlace.body()
                        last_place=body?.name?:"None"
                        lastplace_label.setText(last_place)
                    }
                }
            }catch (e: Exception){

            }
        }
    }
}