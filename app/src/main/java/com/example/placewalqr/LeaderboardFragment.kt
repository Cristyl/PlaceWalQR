package com.example.placewalqr

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import android.os.Bundle

class LeaderboardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    LeaderboardScreen()
                }
            }
        }
    }

    @Composable
    fun LeaderboardScreen() {
        var isLoading by remember { mutableStateOf(true) }
        var leaderboard by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }
        var userEntry by remember { mutableStateOf<LeaderboardEntry?>(null) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        // Caricamento dati
        LaunchedEffect(Unit) {
            val sharedPreferences = requireActivity().getSharedPreferences(
                "UserPrefs",
                Context.MODE_PRIVATE
            )
            val nickname = sharedPreferences.getString("nickname", null)

            if (nickname == null) {
                errorMessage = "User not logged in"
                isLoading = false
                return@LaunchedEffect
            }

            lifecycleScope.launch {
                try {
                    val response = RetrofitInstance.apiService.getLeaderboard(nickname)
                    if (!response.isSuccessful) {
                        errorMessage = "Failed to load leaderboard: ${response.code()}"
                    } else {
                        leaderboard = response.body() ?: emptyList()
                        userEntry = leaderboard.find { it.nickname == nickname }
                            ?: LeaderboardEntry(
                                position = -1, // fuori classifica
                                nickname = nickname,
                                total_points = 0
                            )
                    }
                } catch (e: IOException) {
                    errorMessage = "Connection error, please check your internet"
                } catch (e: HttpException) {
                    errorMessage = "Server error"
                }
                isLoading = false
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Leaderboard",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                errorMessage != null -> {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                leaderboard.isEmpty() -> {
                    Text("No leaderboard entries found")
                }

                else -> {
                    // Mostro l'utente loggato in alto
                    userEntry?.let {
                        UserCard(it)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Lista classifica
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(leaderboard) { entry ->
                            LeaderboardCard(entry)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun UserCard(entry: LeaderboardEntry) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Your Position", fontWeight = FontWeight.Bold)
                Text(
                    text = if (entry.position != -1) "Rank #${entry.position}" else "Not ranked",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${entry.nickname} - ${entry.total_points} pts",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }

    @Composable
    fun LeaderboardCard(entry: LeaderboardEntry) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("#${entry.position}", fontWeight = FontWeight.Bold)
                Text(entry.nickname)
                Text("${entry.total_points} pts")
            }
        }
    }
}
