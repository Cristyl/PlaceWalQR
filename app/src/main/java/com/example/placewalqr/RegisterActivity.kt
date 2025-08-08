package com.example.placewalqr

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
import com.google.android.material.textfield.TextInputEditText
import java.util.Calendar
import android.app.DatePickerDialog
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.util.*
import java.text.SimpleDateFormat

class RegisterActivity : ComponentActivity() {

    private lateinit var nameField: TextInputEditText
    private lateinit var surnameField: TextInputEditText
    private lateinit var nicknameField: TextInputEditText
    private lateinit var dobField: TextInputEditText
    private lateinit var emailField: TextInputEditText
    private lateinit var pwdField: TextInputEditText
    private lateinit var pwdConfirmField: TextInputEditText
    private lateinit var registerBtn: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register_activity)

        // Inizializza i campi
        nameField = findViewById(R.id.name_field)
        surnameField = findViewById(R.id.surname_field)
        nicknameField = findViewById(R.id.nickname_field)
        dobField = findViewById(R.id.dob_field)
        emailField = findViewById(R.id.email_field)
        pwdField = findViewById(R.id.pwd_field)
        pwdConfirmField = findViewById(R.id.pwd_confirm_field)
        registerBtn = findViewById(R.id.btnLogin)

        dobField.setOnClickListener {
            showDatePicker()
        }

        registerBtn.setOnClickListener {
            performRegistration()
        }

    }

    private fun performRegistration() {
        val name = nameField.text.toString()
        val surname = surnameField.text.toString()
        val nickname = nicknameField.text.toString()
        val dob = dobField.text.toString()
        val email = emailField.text.toString()
        val password = pwdField.text.toString()
        val confirmPassword = pwdConfirmField.text.toString()

        val registrationRequest = RegisterRequest(name, surname, nickname, dob, email, password)

        lifecycleScope.launch {
            try{
                val response = RetrofitInstance.apiService.register(registrationRequest)
                if(response.isSuccessful && response.body() != null){
                    val info = response.body()!!

                    if(info.status == "ok"){
                        Toast.makeText(baseContext, "Registration successful", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(baseContext, "Registration failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: IOException){
                Log.e("RegisterActivity", "IO Exception: ${e}")
                Toast.makeText(baseContext, "Connection error", Toast.LENGTH_SHORT).show()
            } catch (e: HttpException) {
                Log.e("MainActivity", "HTTP Exception: ${e.message}")
                Toast.makeText(baseContext, "Server error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(selectedYear, selectedMonth, selectedDay)

                val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                dobField.setText(dateFormatter.format(selectedDate.time))
            },
            currentYear,
            currentMonth,
            currentDay
        )

        val maxDate = Calendar.getInstance()
        maxDate.add(Calendar.YEAR, 0)
        datePickerDialog.datePicker.maxDate = maxDate.timeInMillis

        val minDate = Calendar.getInstance()
        minDate.add(Calendar.YEAR, -120)
        datePickerDialog.datePicker.minDate = minDate.timeInMillis

        datePickerDialog.show()
    }
}