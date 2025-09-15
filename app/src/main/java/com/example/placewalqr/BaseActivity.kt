package com.example.placewalqr

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import android.view.View
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material3.Surface
import androidx.core.view.WindowCompat
import com.example.placewalqr.ui.theme.PlaceWalQRTheme

class BaseActivity : AppCompatActivity() {

    // Track selected tab
    private var currentSelectedTab by mutableIntStateOf(0)
    private var lastSelectedTab by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Use Jetpack Compose for UI
        setContent {
            PlaceWalQRTheme {
                // Main background color
                Surface(color = MaterialTheme.colorScheme.background) {
                    val statusBarColor = MaterialTheme.colorScheme.primary
                    SideEffect {
                        window.statusBarColor = statusBarColor.toArgb()
                        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
                    }
                    MainScreen()
                }
            }
        }

        // Show homepage on first launch
        if (savedInstanceState == null) {
            lastSelectedTab = 0
            post {
                navigateToFragment(HomepageFragment())
            }
        }
    }

    // Helper to post actions after UI is ready
    private fun post(action: () -> Unit) {
        window.decorView.post(action)
    }

    @Composable
    private fun MainScreen() {
        Column(modifier = Modifier.fillMaxSize()) {
            // Container for fragments
            AndroidView(
                factory = { context ->
                    FragmentContainerView(context).apply {
                        id = View.generateViewId()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Takes all space except bottom navigation
            ) { view ->
                savedFragmentContainerId = view.id
            }

            // Custom bottom navigation bar
            CustomBottomNavigation()
        }
    }

    // Save fragment container ID
    private var savedFragmentContainerId: Int = 0

    fun getFragmentContainerId(): Int = savedFragmentContainerId

    @Composable
    private fun CustomBottomNavigation() {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Button for Home
                NavigationTab(
                    icon = Icons.Default.Home,
                    label = "Home",
                    isSelected = currentSelectedTab == 0,
                    onClick = {
                        currentSelectedTab = 0
                        lastSelectedTab = 0
                        navigateToFragment(HomepageFragment())
                    }
                )

                // Button for Map
                NavigationTab(
                    icon = Icons.Default.LocationOn,
                    label = "Map",
                    isSelected = currentSelectedTab == 1,
                    onClick = {
                        currentSelectedTab = 1
                        lastSelectedTab = 1
                        navigateToFragment(MapsFragment())
                    }
                )

                // Center floating button for Camera
                FloatingActionButton(
                    onClick = {
                        currentSelectedTab = -1
                        navigateToFragment(CameraComposeFragment())
                    },
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Camera",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Button for Collection
                NavigationTab(
                    icon = Icons.Default.CollectionsBookmark,
                    label = "Collection",
                    isSelected = currentSelectedTab == 2,
                    onClick = {
                        currentSelectedTab = 2
                        lastSelectedTab = 2
                        navigateToFragment(CollectionFragment())
                    }
                )

                // Button for Leaderboard
                NavigationTab(
                    icon = Icons.Default.Leaderboard,
                    label = "Leaderboard",
                    isSelected = currentSelectedTab == 3,
                    onClick = {
                        currentSelectedTab = 3
                        lastSelectedTab = 3
                        navigateToFragment(LeaderboardFragment())
                    }
                )
            }
        }
    }

    // Reusable navigation tab
    @Composable
    private fun RowScope.NavigationTab(
        icon: ImageVector,
        label: String,
        isSelected: Boolean,
        onClick: () -> Unit
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Replace current fragment with a new one
    private fun navigateToFragment(fragment: Fragment) {
        if (savedFragmentContainerId != 0) {
            supportFragmentManager.beginTransaction()
                .replace(savedFragmentContainerId, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    // Public navigation for other fragments
    fun navigateToDetailFragment(fragment: Fragment) {
        navigateToFragment(fragment)
    }

    override fun onBackPressed() {
        val currentFragment = supportFragmentManager.findFragmentById(savedFragmentContainerId)

        // If on homepage and no back stack, exit app
        if (currentFragment is HomepageFragment && supportFragmentManager.backStackEntryCount <= 1) {
            finish()
            return
        }

        // If there are fragments in the stack, go back
        if (supportFragmentManager.backStackEntryCount > 1) {
            super.onBackPressed()
            currentSelectedTab = lastSelectedTab
        } else {
            // Otherwise go back to homepage
            currentSelectedTab = 0
            lastSelectedTab = 0
            navigateToHomepage()
        }
    }

    // Go to homepage and clear back stack
    private fun navigateToHomepage() {
        supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)

        if (savedFragmentContainerId != 0) {
            supportFragmentManager.beginTransaction()
                .replace(savedFragmentContainerId, HomepageFragment())
                .commit()
        }
    }
}
