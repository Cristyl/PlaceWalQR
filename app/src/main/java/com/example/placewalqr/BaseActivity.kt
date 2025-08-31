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
import com.example.placewalqr.ui.theme.PlaceWalQRTheme

class BaseActivity : AppCompatActivity() {

    private var currentSelectedTab by mutableIntStateOf(0)
    private var lastSelectedTab by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Usa setContent per UI Compose
        setContent {
            PlaceWalQRTheme {
                MainScreen()
            }
        }

        // Mostra la homepage di default
        if (savedInstanceState == null) {
            lastSelectedTab = 0
            // IMPORTANTE: Aspetta che la UI sia pronta
            post {
                navigateToFragment(HomepageFragment())
            }
        }
    }

    private fun post(action: () -> Unit) {
        window.decorView.post(action)
    }

    @Composable
    private fun MainScreen() {
        Column(modifier = Modifier.fillMaxSize()) {
            // Container per i fragment usando AndroidView
            AndroidView(
                factory = { context ->
                    FragmentContainerView(context).apply {
                        id = View.generateViewId() // Aggiunto View.
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Occupa tutto lo spazio tranne la navbar
            ) { view ->
                // Salva l'ID generato per usarlo nei fragment
                savedFragmentContainerId = view.id
            }

            // Bottom Navigation personalizzata
            CustomBottomNavigation()
        }
    }

    // Variabile per salvare l'ID del container
    private var savedFragmentContainerId: Int = 0

    // Metodo per esporre l'ID del container ai fragment
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
                // Home Tab
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

                // Map Tab
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

                // Camera Button (Centrale)
                FloatingActionButton(
                    onClick = {
                        currentSelectedTab = -1 // Deseleziona tutti
                        navigateToFragment(CameraComposeFragment())
                    },
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera, // Cambiato da CameraAlt
                        contentDescription = "Camera",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Collection Tab (era Achievements)
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

                // Leaderboard Tab
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

    @Composable
    private fun RowScope.NavigationTab( // Aggiunto RowScope
        icon: ImageVector,
        label: String,
        isSelected: Boolean,
        onClick: () -> Unit
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f) // Ora funziona!
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

    private fun navigateToFragment(fragment: Fragment) {
        // Usa l'ID generato dinamicamente
        if (savedFragmentContainerId != 0) {
            supportFragmentManager.beginTransaction()
                .replace(savedFragmentContainerId, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    // Metodo pubblico per navigazione da altri fragment
    fun navigateToDetailFragment(fragment: Fragment) {
        navigateToFragment(fragment)
    }

    override fun onBackPressed() {
        val currentFragment = supportFragmentManager.findFragmentById(savedFragmentContainerId)

        // Se siamo già sulla homepage, chiudi l'app
        if (currentFragment is HomepageFragment && supportFragmentManager.backStackEntryCount <= 1) {
            finish() // Chiude l'applicazione
            return
        }

        // Controlla se c'è qualcosa nella back stack
        if (supportFragmentManager.backStackEntryCount > 1) {
            // C'è ancora qualcosa nella stack, procedi normalmente
            super.onBackPressed()
            currentSelectedTab = lastSelectedTab
        } else {
            // La stack è quasi vuota, vai alla homepage
            currentSelectedTab = 0
            lastSelectedTab = 0
            navigateToHomepage()
        }
    }

    private fun navigateToHomepage() {
        // Pulisce la back stack e va alla homepage
        supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)

        // Naviga alla homepage senza aggiungere alla back stack
        if (savedFragmentContainerId != 0) {
            supportFragmentManager.beginTransaction()
                .replace(savedFragmentContainerId, HomepageFragment())
                .commit() // Senza addToBackStack!
        }
    }
}