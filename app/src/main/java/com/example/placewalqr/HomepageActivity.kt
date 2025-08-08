package com.example.placewalqr

import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.homepage_activity)
        loadHomePage()

        val welcome_label = findViewById<TextView>(R.id.welcome_label)
        val welcome_text=welcome_label.text.toString() +" "+ user
        welcome_label.setText(welcome_text)

        val seesights_label = findViewById<TextView>(R.id.seesights_text)
        seesights_label.setText(see_sights.toString())

        val points_label= findViewById<TextView>(R.id.points_text)
        points_label.setText(points.toString())

        val lastplace_label= findViewById<TextView>(R.id.lastplace_text)
        lastplace_label.setText(last_place)
    }

    private fun loadHomePage(){
        val sharedPreferences=getSharedPreferences("UserPrefs", MODE_PRIVATE)
        user=sharedPreferences.getString("nickname", "").toString()
        email=sharedPreferences.getString("email", "").toString()
        id=sharedPreferences.getString("id", "0").toString().toInt()

        lifecycleScope.launch {
            try {
                val request= UserIdRequest(id)
                val responsePoint= RetrofitInstance.apiService.getPointsById(request)

                val responsePlace = RetrofitInstance.apiService.findLastPlaceById(request)
                runOnUiThread {
                    if(responsePoint.code()==404){
                        val body=responsePoint.body()
                        print(body)
                        points= body?.points ?: 0
                        see_sights=body?.count ?: 0
                    }

                    if(responsePlace.code()==404){
                        val body=responsePlace.body()
                        print(body)
                        last_place=body?.name?:"None"
                    }
                }
            }catch (e: Exception){

            }
        }
    }
}