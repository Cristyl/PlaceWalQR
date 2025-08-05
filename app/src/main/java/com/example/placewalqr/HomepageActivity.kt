package com.example.placewalqr

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.activity.ComponentActivity
import org.w3c.dom.Text

class HomepageActivity : BaseActivity(){
    lateinit var user: String
    lateinit var see_sights: Number
    lateinit var points: Number
    lateinit var last_place: String

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.homepage_activity)
        val welcome_label: TextView = findViewById<TextView>(R.id.welcome_label)
        val welcome_text=welcome_label.text.toString() +" "+ user
        welcome_label.setText(user)

        val seesights_label: TextView = findViewById<TextView>(R.id.seesights_text)
        seesights_label.setText(see_sights.toString())

        val points_label: TextView= findViewById<TextView>(R.id.points_text)
        points_label.setText(points.toString())

        val lastplace_label: TextView= findViewById<TextView>(R.id.lastplace_text)
        lastplace_label.setText(last_place)
    }

    fun loadHomePage(){
        //chiamate api per i dati richeisti
    }
}