package com.example.placewalqr

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.os.Bundle

class AchievementsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    AchievementsScreen()
                }
            }
        }
    }

    @Composable
    fun AchievementsScreen() {
        var isLoading by remember { mutableStateOf(true) }
        var places by remember { mutableStateOf<List<Place>>(emptyList()) }
        var nickname by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            val sharedPreferences = requireActivity().getSharedPreferences(
                "UserPrefs",
                Context.MODE_PRIVATE
            )
            val userId = sharedPreferences.getString("id", null)
            nickname = sharedPreferences.getString("nickname", "") ?: ""

            if (userId == null) {
                errorMessage = "User not logged in"
                isLoading = false
                return@LaunchedEffect
            }

            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitInstance.apiService.getPlacesByUser(userId)
                }

                if (response.isSuccessful) {
                    places = response.body() ?: emptyList()
                    if (places.isEmpty()) {
                        errorMessage = "No achievements yet"
                    }
                } else {
                    errorMessage = when (response.code()) {
                        400 -> "User ID parameter is missing"
                        500 -> "Server error, please try again later"
                        else -> "Unknown error occurred"
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Connection error, please check your internet"
            }

            isLoading = false
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Achievements of $nickname",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(32.dp)
                    )
                }

                errorMessage != null -> {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                places.isNotEmpty() -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(places) { place ->
                            AchievementCard(place)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun AchievementCard(place: Place) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.titleMedium
                )

                place.imageBase64?.let { base64Image ->
                    val bitmap = remember(base64Image) {
                        try {
                            val bytes = Base64.decode(base64Image, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    bitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Place image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
