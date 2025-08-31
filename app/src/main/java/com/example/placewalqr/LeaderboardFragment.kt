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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import android.os.Bundle
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource

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

        // Caricamento dati (CORRETTO: senza lifecycleScope ridondante)
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

            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitInstance.apiService.getLeaderboard(nickname)
                }

                if (response.isSuccessful) {
                    val fullLeaderboard = response.body() ?: emptyList()

                    // Trova l'utente nella lista completa
                    userEntry = fullLeaderboard.find { it.nickname == nickname }

                    // Se l'utente non esiste, crea entry vuota
                    if (userEntry == null) {
                        userEntry = LeaderboardEntry(
                            position = -1,
                            nickname = nickname,
                            total_points = 0
                        )
                    }

                    // La leaderboard è già filtrata dal server (top 10 + utente se fuori top 10)
                    leaderboard = fullLeaderboard

                } else {
                    errorMessage = "Failed to load leaderboard: ${response.code()}"
                }
            } catch (e: IOException) {
                errorMessage = "Connection error, please check your internet"
            } catch (e: HttpException) {
                errorMessage = "Server error"
            }

            isLoading = false
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Image(
            painter = painterResource(id = R.drawable.placewalqr_logo),
            modifier = Modifier.width(250.dp).align(Alignment.CenterHorizontally).padding(bottom = 32.dp),
            contentDescription = "App Logo"
            )

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
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                leaderboard.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No leaderboard entries found")
                    }
                }

                else -> {
                    // MOSTRO SOLO la posizione utente se è FUORI dalla top 10
                    userEntry?.let { user ->
                        if (user.position > 10) {
                            UserPositionCard(user)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Lista classifica (include già l'utente se nella top 10)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(leaderboard) { entry ->
                            val isCurrentUser = entry.nickname == userEntry?.nickname
                            LeaderboardCard(
                                entry = entry,
                                isCurrentUser = isCurrentUser
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun UserPositionCard(entry: LeaderboardEntry) {
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
                Text(
                    text = "Your Position",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Rank #${entry.position}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${entry.nickname} - ${entry.total_points} pts",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }

    @Composable
    fun LeaderboardCard(entry: LeaderboardEntry, isCurrentUser: Boolean = false) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = if (isCurrentUser) {
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            } else {
                CardDefaults.cardColors()
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Posizione
                Text(
                    text = "#${entry.position}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (entry.position) {
                        1 -> Color(0xFFFFD700) // Oro
                        2 -> Color(0xFFC0C0C0) // Argento
                        3 -> Color(0xFFCD7F32) // Bronzo
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )

                // Nickname
                Text(
                    text = entry.nickname + if (isCurrentUser) " (You)" else "",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Normal
                )

                // Punti
                Text(
                    text = "${entry.total_points} pts",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}