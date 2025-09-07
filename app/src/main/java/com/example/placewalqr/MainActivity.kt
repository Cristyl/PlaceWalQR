package com.example.placewalqr

import com.google.android.gms.auth.api.signin.GoogleSignIn
import RetrofitInstance
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.placewalqr.ui.theme.PlaceWalQRTheme
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToBase: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Effettua il logout da Google se l'utente torna a questa schermata dopo un login Google
    LaunchedEffect(Unit) {
        GoogleSignIn.getClient(context, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
    }

    // definizione client per effettuare accesso tramite login con google
    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestId()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    // gestisce interazione con interfaccia per accedere con account google
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            // Processa il login Google
            coroutineScope.launch {
                handleGoogleSignIn(account, context, onNavigateToBase)
            }
        } catch (e: ApiException) {
            Log.e("GoogleSignIn", "Login failed: ${e.statusCode}")
            Toast.makeText(context, "Google login failed", Toast.LENGTH_SHORT).show()
        }
    }

    // stati delle variabili
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }

    // recupera valori precedentemente usati per loggare se persistenti
    LaunchedEffect(Unit) {
        val preferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

        // salva i dati prima di rimuoverli dalla memoria
        val editor = preferences.edit()
        val savedRememberMe = preferences.getBoolean("remember_me", true)
        val savedEmail = preferences.getString("email", "")
        val savedPassword = preferences.getString("password", "")

        editor.clear().apply()
        editor.putBoolean("remember_me", savedRememberMe).apply()

        if (savedRememberMe) {
            rememberMe = true
            email = savedEmail ?: ""
            password = savedPassword ?: ""
        }
    }

    // funzione di login
    fun performLogin() {
        val editor = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).edit()

        if (rememberMe) {
            editor.putString("email", email)
            editor.putString("password", password)
            editor.putBoolean("remember_me", true)
        } else {
            editor.putBoolean("remember_me", false)
        }

        val loginRequest = LoginRequest(email, password)

        coroutineScope.launch {
            isLoading = true

            try {
                val response = RetrofitInstance.apiService.login(loginRequest)

                if (response.isSuccessful && response.body() != null) {
                    val userInfo = response.body()!!

                    // recupera info dell'utente storate in precedenza
                    editor.putString("id", userInfo.id.toString())
                    editor.putString("name", userInfo.name)
                    editor.putString("surname", userInfo.surname)
                    editor.putString("dob", userInfo.dob.toString())
                    editor.putString("email", userInfo.email)
                    editor.putString("nickname", userInfo.nickname)
                    editor.apply()

                    onNavigateToBase()
                } else {
                    Log.e("LoginScreen", "Error during login: ${response.errorBody()?.string()}")
                    Toast.makeText(context, "Error during data fetching", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                Log.e("LoginScreen", "IO Exception: ${e.message}")
                Toast.makeText(context, "Connection error", Toast.LENGTH_SHORT).show()
            } catch (e: HttpException) {
                Log.e("LoginScreen", "HTTP Exception: ${e.message}")
                Toast.makeText(context, "Server error", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    // UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // titolo app
        Image(
            painter = painterResource(
                id = if (isSystemInDarkTheme()) R.drawable.placewalqr_logo_dark_lol
                else R.drawable.placewalqr_logo
            ),
            modifier = Modifier.width(250.dp).align(Alignment.CenterHorizontally),
            contentDescription = "App Logo"
        )

        Spacer(modifier = Modifier.height(48.dp))

        // campo per email
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        var showPassword by remember { mutableStateOf(false) }
        // campo per password
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true,
            visualTransformation =
                if(showPassword){
                    VisualTransformation.None
                }
                else {
                    PasswordVisualTransformation()
                },
            trailingIcon = {
                if (showPassword) {
                    IconButton(onClick = { showPassword = false }) {
                        Icon(
                            imageVector = Icons.Filled.Visibility,
                            contentDescription = "hide_password"
                        )
                    }
                } else {
                    IconButton(
                        onClick = { showPassword = true }) {
                        Icon(
                            imageVector = Icons.Filled.VisibilityOff,
                            contentDescription = "hide_password"
                        )
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // switch per rememberMe
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Switch(
                checked = rememberMe,
                onCheckedChange = {
                    rememberMe = it
                    context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean("remember_me", it)
                        .apply()
                },
                enabled = !isLoading,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Remember me",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // bottone di login
        Button(
            onClick = { performLogin() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank()
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
                text = if (isLoading) "Logging in..." else "LOGIN",
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AGGIUNGI: Divisore "OR"
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                text = "OR",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            androidx.compose.material3.HorizontalDivider(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AGGIUNGI: Bottone Google Sign-In
        androidx.compose.material3.OutlinedButton(
            onClick = {
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text(
                text = "Continue with Google",
                fontSize = 16.sp
            )
        }

        // link per password dimenticata, da rivedere
//        TextButton(
//            onClick = {
//                // TODO: Implement forgot password
//                Toast.makeText(context, "Forgot password feature coming soon", Toast.LENGTH_SHORT).show()
//            },
//            enabled = !isLoading
//        ) {
//            Text(
//                text = "Forgot password?",
//                textDecoration = TextDecoration.Underline
//            )
//        }

        Spacer(modifier = Modifier.height(32.dp))

        // link di registrazione
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Don't have an account? ",
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(
                onClick = onNavigateToRegister,
                enabled = !isLoading
            ) {
                Text(
                    text = "Register here",
                    textDecoration = TextDecoration.Underline,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Loading indicator overlay
        if (isLoading) {
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// funzione asincrona per gestione accesso con google
suspend fun handleGoogleSignIn(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount, context: Context, onNavigateToBase: () -> Unit) {
    try {
        val userData = GoogleUserData(
            google_id = account.id ?: "",
            email = account.email ?: "",
            name = account.givenName ?: "",
            surname = account.familyName ?: "",
            dob = null.toString(),
            nickname = account.givenName + account.familyName
        )

        val response = RetrofitInstance.apiService.googleAuth(userData)

        if (response.isSuccessful) {
            val userInfo = response.body()!!

            // salva i dati dell'utente
            val editor = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).edit()
            editor.putString("id", userInfo.id.toString())
            editor.putString("name", userData.name)
            editor.putString("surname", userData.surname)
            editor.putString("email", userData.email)
            editor.putString("nickname", userData.nickname)
            editor.putString("google_auth", "false")
            editor.putBoolean("remember_me", false)
            editor.apply()

            when(response.code()!!) {
                200 ->
                    Toast.makeText(context, "Successful login!", Toast.LENGTH_SHORT).show()

                201 ->
                    Toast.makeText(context, "Successful registration!", Toast.LENGTH_SHORT).show()
            }

            // ritorna a schermata antecedente al launcher
            onNavigateToBase()
        } else {
            Toast.makeText(context, "Google auth failed", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Log.e("GoogleAuth", "Error: ${e.message}")
        Toast.makeText(context, "Connection error", Toast.LENGTH_SHORT).show()
    }
}

// classe per login
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PlaceWalQRTheme() {
                val statusBarColor = MaterialTheme.colorScheme.primary
                SideEffect {
                    window.statusBarColor = statusBarColor.toArgb()
                    WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LoginScreen(
                        onNavigateToRegister = {
                            val intent = Intent(this@MainActivity, RegisterActivity::class.java)
                            startActivity(intent)
                        },
                        onNavigateToBase = {
                            val intent = Intent(this@MainActivity, BaseActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    )
                }
            }
        }
    }
}