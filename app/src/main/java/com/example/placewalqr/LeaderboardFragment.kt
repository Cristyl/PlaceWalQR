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
import androidx.compose.foundation.isSystemInDarkTheme

// Fragment to display the leaderboard
class LeaderboardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Compose UI container
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
        var isLoading by remember { mutableStateOf(true) }                      // Loading state
        var leaderboard by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) } // Leaderboard list
        var userEntry by remember { mutableStateOf<LeaderboardEntry?>(null) }   // Current user entry
        var errorMessage by remember { mutableStateOf<String?>(null) }          // Error message

        // Load leaderboard data
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

                    // Find current user in leaderboard
                    userEntry = fullLeaderboard.find { it.nickname == nickname }

                    // If user not found, create a default entry
                    if (userEntry == null) {
                        userEntry = LeaderboardEntry(
                            position = -1,
                            nickname = nickname,
                            total_points = 0
                        )
                    }

                    leaderboard = fullLeaderboard // Full leaderboard

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
            // App logo
            Image(
                painter = painterResource(
                    id = if (isSystemInDarkTheme()) R.drawable.placewalqr_logo_dark_lol
                    else R.drawable.placewalqr_logo
                ),
                modifier = Modifier.width(250.dp).align(Alignment.CenterHorizontally).padding(bottom = 32.dp),
                contentDescription = "App Logo"
            )

            // Title
            Text(
                text = "Leaderboard",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            when {
                isLoading -> {
                    // Show loading indicator
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                errorMessage != null -> {
                    // Show error message
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
                    // No entries
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No leaderboard entries found")
                    }
                }

                else -> {
                    // Show current user position if outside top 10
                    userEntry?.let { user ->
                        if (user.position > 10) {
                            UserPositionCard(user) // Current user card
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Leaderboard list
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(leaderboard) { entry ->
                            val isCurrentUser = entry.nickname == userEntry?.nickname
                            LeaderboardCard(
                                entry = entry,       // Leaderboard entry
                                isCurrentUser = isCurrentUser // Highlight if current user
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun UserPositionCard(entry: LeaderboardEntry) {
        // Card for current user position outside top 10
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
        // Card for leaderboard entry
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
                // Rank
                Text(
                    text = "#${entry.position}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (entry.position) {
                        1 -> Color(0xFFFFD700) // Gold
                        2 -> Color(0xFFC0C0C0) // Silver
                        3 -> Color(0xFFCD7F32) // Bronze
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )

                // Nickname
                Text(
                    text = entry.nickname + if (isCurrentUser) " (You)" else "",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Normal
                )

                // Points
                Text(
                    text = "${entry.total_points} pts",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}