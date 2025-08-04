package com.example.placewalqr

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
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
import com.example.placewalqr.ui.theme.PlaceWalQRTheme

class MainActivity : ComponentActivity() {

    private lateinit var mainLabel: TextView
    private lateinit var emailField: TextView
    private lateinit var pwdField: TextView
    private lateinit var loginBtn: Button
    private lateinit var forgotPwd: TextView
    private lateinit var registerText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainLabel = findViewById(R.id.main_label)
        emailField = findViewById(R.id.email_field)
        pwdField = findViewById(R.id.pwd_field)
        forgotPwd = findViewById(R.id.forgot_pwd_text)
        registerText = findViewById(R.id.register_text)
        loginBtn = findViewById(R.id.btnLogin)

        forgotPwd.setOnClickListener {
            //
        }

        registerText.setOnClickListener {
            var intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        }
    }