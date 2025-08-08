package com.example.placewalqr

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.example.placewalqr.ui.theme.PlaceWalQRTheme
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import kotlin.math.log

class MainActivity : ComponentActivity() {

    private lateinit var mainLabel: TextView
    private lateinit var emailField: TextView
    private lateinit var pwdField: TextView
    private lateinit var loginBtn: Button
    private lateinit var cameraBtn: Button
    private lateinit var forgotPwd: TextView
    private lateinit var registerText: TextView
    private lateinit var homepageBtn: Button
    private lateinit var mapBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainLabel = findViewById(R.id.main_label)
        emailField = findViewById(R.id.email_field)
        pwdField = findViewById(R.id.pwd_field)
        forgotPwd = findViewById(R.id.forgot_pwd_text)
        registerText = findViewById(R.id.register_text)
        loginBtn = findViewById(R.id.btnLogin)
        cameraBtn = findViewById(R.id.camera_btn)
        homepageBtn=findViewById(R.id.homepage_btn)
        mapBtn=findViewById(R.id.map_btn)

        forgotPwd.setOnClickListener {
            //
        }

        loginBtn.setOnClickListener {
            remoteLogin()
        }

        registerText.setOnClickListener {
            var intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        cameraBtn.setOnClickListener {
            var intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        }

        homepageBtn.setOnClickListener {
            var intent = Intent(this, HomepageActivity::class.java)
            startActivity(intent)
        }

        mapBtn.setOnClickListener {
            var intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        }

    }

    private fun remoteLogin() {
        val email = emailField.text.toString()
        val password = pwdField.text.toString()

        val loginRequest = LoginRequest(email, password)

        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.apiService.login(loginRequest)
                if (response.isSuccessful && response.body() != null) {
                    val userInfo = response.body()!!

                    // storing user information for future uses
                    val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                    val editor = sharedPreferences.edit()

                    editor.putString("name", userInfo.name)
                    editor.putString("surname", userInfo.surname)
                    editor.putString("dob", userInfo.dob.toString())
                    editor.putString("email", userInfo.email)
                    editor.putString("nickname", userInfo.nickname)
                    editor.apply()

                    var intent = Intent(this@MainActivity, HomepageActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Log.e("MainActivity", "Error during login: ${response.errorBody()?.string()}")
                    Toast.makeText(baseContext, "Error during data fetching", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                Log.e("MainActivity", "IO Exception: ${e.message}")
                Toast.makeText(baseContext, "Connection error", Toast.LENGTH_SHORT).show()
            } catch (e: HttpException) {
                Log.e("MainActivity", "HTTP Exception: ${e.message}")
                Toast.makeText(baseContext, "Server error", Toast.LENGTH_SHORT).show()
            }
        }

    }

}