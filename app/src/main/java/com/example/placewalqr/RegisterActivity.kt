package com.example.placewalqr

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.placewalqr.ui.theme.PlaceWalQRTheme
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // stati delle variabili del form
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // variabili per gli errori nella validazione delle info nel form
    var nameError by remember { mutableStateOf("") }
    var surnameError by remember { mutableStateOf("") }
    var nicknameError by remember { mutableStateOf("") }
    var dobError by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var confirmPasswordError by remember { mutableStateOf("") }

    // selezionatore data di nascita
    fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            context,
            android.R.style.Theme_Holo_Light_Dialog,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(selectedYear, selectedMonth, selectedDay)

                val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                dateOfBirth = dateFormatter.format(selectedDate.time)
                dobError = "" // Clear error when date is selected
            },
            currentYear,
            currentMonth,
            currentDay
        )

        // imposto la data massima a partire da oggi
        val maxDate = Calendar.getInstance()
        datePickerDialog.datePicker.maxDate = maxDate.timeInMillis

        // imposto la data minima a 120 anni fa
        val minDate = Calendar.getInstance()
        minDate.add(Calendar.YEAR, -120)
        datePickerDialog.datePicker.minDate = minDate.timeInMillis

        datePickerDialog.show()
    }

    // definisco gli errori per la validazione dei campi del form
    fun validateForm(): Boolean {
        var isValid = true

        if (name.isBlank()) {
            nameError = "Name is required"
            isValid = false
        } else {
            nameError = ""
        }

        if (surname.isBlank()) {
            surnameError = "Surname is required"
            isValid = false
        } else {
            surnameError = ""
        }

        if (nickname.isBlank()) {
            nicknameError = "Nickname is required"
            isValid = false
        } else {
            nicknameError = ""
        }

        if (dateOfBirth.isBlank()) {
            dobError = "Date of birth is required"
            isValid = false
        } else {
            dobError = ""
        }

        if (email.isBlank()) {
            emailError = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError = "Please enter a valid email"
            isValid = false
        } else {
            emailError = ""
        }

        if (password.isBlank()) {
            passwordError = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            passwordError = "Password must be at least 6 characters"
            isValid = false
        } else {
            passwordError = ""
        }

        if (confirmPassword.isBlank()) {
            confirmPasswordError = "Please confirm your password"
            isValid = false
        } else if (password != confirmPassword) {
            confirmPasswordError = "Passwords don't match"
            isValid = false
        } else {
            confirmPasswordError = ""
        }

        return isValid
    }

    // funzione di registrazione
    fun performRegistration() {
        if (!validateForm()) return

        val registrationRequest = RegisterRequest(name, surname, nickname, dateOfBirth, email, password)

        coroutineScope.launch {
            isLoading = true

            try {
                val response = RetrofitInstance.apiService.register(registrationRequest)

                if (response.isSuccessful && response.body() != null) {
                    val info = response.body()!!

                    if (info.status == "ok") {
                        Toast.makeText(context, "Registration successful", Toast.LENGTH_SHORT).show()
                        onNavigateToLogin()
                    } else {
                        Toast.makeText(context, "Registration failed", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Registration failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                Log.e("RegisterScreen", "IO Exception: ${e}")
                Toast.makeText(context, "Connection error", Toast.LENGTH_SHORT).show()
            } catch (e: HttpException) {
                Log.e("RegisterScreen", "HTTP Exception: ${e.message}")
                Toast.makeText(context, "Server error", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    // interfaccia grafica
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(
                id = if (isSystemInDarkTheme()) R.drawable.placewalqr_logo_dark_lol
                else R.drawable.placewalqr_logo
            ),
            modifier = Modifier.width(250.dp).align(Alignment.CenterHorizontally),
            contentDescription = "App Logo"
        )
        // titolo
        Text(
            text = "Create Account",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
        )

        // campo per il nome
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                if (nameError.isNotEmpty()) nameError = ""
            },
            label = { Text("Name *") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true,
            isError = nameError.isNotEmpty(),
            supportingText = if (nameError.isNotEmpty()) {
                { Text(nameError, color = MaterialTheme.colorScheme.error) }
            } else null
        )

        Spacer(modifier = Modifier.height(16.dp))

        // campo per il cognome
        OutlinedTextField(
            value = surname,
            onValueChange = {
                surname = it
                if (surnameError.isNotEmpty()) surnameError = ""
            },
            label = { Text("Surname *") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true,
            isError = surnameError.isNotEmpty(),
            supportingText = if (surnameError.isNotEmpty()) {
                { Text(surnameError, color = MaterialTheme.colorScheme.error) }
            } else null
        )

        Spacer(modifier = Modifier.height(16.dp))

        // campo per il nickname
        OutlinedTextField(
            value = nickname,
            onValueChange = {
                nickname = it
                if (nicknameError.isNotEmpty()) nicknameError = ""
            },
            label = { Text("Nickname *") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true,
            isError = nicknameError.isNotEmpty(),
            supportingText = if (nicknameError.isNotEmpty()) {
                { Text(nicknameError, color = MaterialTheme.colorScheme.error) }
            } else null
        )

        Spacer(modifier = Modifier.height(16.dp))

        // campo per la data di nascita
        OutlinedTextField(
            value = dateOfBirth,
            onValueChange = { }, // SOLO IN LETTURA!
            label = { Text("Date of Birth *") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            readOnly = true,
            trailingIcon = {
                IconButton(
                    onClick = { if (!isLoading) showDatePicker() }
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = "Select date")
                }
            },
            isError = dobError.isNotEmpty(),
            supportingText = if (dobError.isNotEmpty()) {
                { Text(dobError, color = MaterialTheme.colorScheme.error) }
            } else null
        )

        Spacer(modifier = Modifier.height(16.dp))

        // campo per la mail
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                if (emailError.isNotEmpty()) emailError = ""
            },
            label = { Text("Email *") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true,
            isError = emailError.isNotEmpty(),
            supportingText = if (emailError.isNotEmpty()) {
                { Text(emailError, color = MaterialTheme.colorScheme.error) }
            } else null
        )

        Spacer(modifier = Modifier.height(16.dp))

        // campo per la password
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                if (passwordError.isNotEmpty()) passwordError = ""
            },
            label = { Text("Password *") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true,
            isError = passwordError.isNotEmpty(),
            supportingText = if (passwordError.isNotEmpty()) {
                { Text(passwordError, color = MaterialTheme.colorScheme.error) }
            } else null
        )

        Spacer(modifier = Modifier.height(16.dp))

        // campo di comferma password
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                if (confirmPasswordError.isNotEmpty()) confirmPasswordError = ""
            },
            label = { Text("Confirm Password *") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true,
            isError = confirmPasswordError.isNotEmpty(),
            supportingText = if (confirmPasswordError.isNotEmpty()) {
                { Text(confirmPasswordError, color = MaterialTheme.colorScheme.error) }
            } else null
        )

        Spacer(modifier = Modifier.height(32.dp))

        // bottone di registrazione
        Button(
            onClick = { performRegistration() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = if (isLoading) "Creating Account..." else "REGISTER",
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ritorno al login
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Already have an account? ",
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(
                onClick = onNavigateToLogin,
                enabled = !isLoading
            ) {
                Text(
                    text = "Login here",
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// classe per la registrazione
class RegisterActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PlaceWalQRTheme{
                val statusBarColor = MaterialTheme.colorScheme.primary
                SideEffect {
                    window.statusBarColor = statusBarColor.toArgb()
                    WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RegisterScreen(
                        onNavigateToLogin = {
                            val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    )
                }
            }
        }
    }
}