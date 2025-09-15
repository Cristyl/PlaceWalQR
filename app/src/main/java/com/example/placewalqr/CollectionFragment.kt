package com.example.placewalqr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CollectionFragment : Fragment() {

    companion object {
        private var lastSelectedTab: Int = 0 // remember last selected tab
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    CollectionScreen()
                }
            }
        }
    }

    @Composable
    fun CollectionScreen() {
        var selectedTab by remember { mutableIntStateOf(lastSelectedTab) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // logo
            Image(
                painter = painterResource(
                    id = if (isSystemInDarkTheme()) R.drawable.placewalqr_logo_dark_lol
                    else R.drawable.placewalqr_logo
                ),
                modifier = Modifier
                    .width(250.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 32.dp),
                contentDescription = "App Logo"
            )

            // tabs: places / collections
            TabSwitcher(
                selectedTab = selectedTab,
                onTabSelected = {
                    selectedTab = it
                    lastSelectedTab = it
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // show tab content
            when (selectedTab) {
                0 -> PlacesContent()
                1 -> CollectionsContent()
            }
        }
    }

    @Composable
    private fun TabSwitcher(
        selectedTab: Int,
        onTabSelected: (Int) -> Unit
    ) {
        val tabs = listOf("Places", "Collections")

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    val backgroundColor by animateColorAsState(
                        targetValue = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            Color.Transparent,
                        animationSpec = tween(300),
                        label = "background"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(300),
                        label = "text"
                    )

                    // tab button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(4.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(backgroundColor)
                            .clickable { onTabSelected(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = textColor,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun PlacesContent() {
        var isLoading by remember { mutableStateOf(true) }
        var places by remember { mutableStateOf<List<Place>>(emptyList()) }
        var nickname by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        // load user places
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
                        errorMessage = "No places visited yet"
                    }
                } else {
                    errorMessage = "Error loading places"
                }
            } catch (e: Exception) {
                errorMessage = "Connection error, please check your internet"
            }

            isLoading = false
        }

        // show content
        when {
            isLoading -> LoadingScreen()
            errorMessage != null -> ErrorScreen(errorMessage!!)
            places.isNotEmpty() -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(places) { place ->
                        PlaceCard(place)
                    }
                }
            }
        }
    }

    @Composable
    private fun CollectionsContent() {
        var isLoading by remember { mutableStateOf(true) }
        var collections by remember { mutableStateOf<List<Collection>>(emptyList()) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        // load user collections
        LaunchedEffect(Unit) {
            val sharedPreferences = requireActivity().getSharedPreferences(
                "UserPrefs",
                Context.MODE_PRIVATE
            )
            val userId = sharedPreferences.getString("id", null)

            if (userId == null) {
                errorMessage = "User not logged in"
                isLoading = false
                return@LaunchedEffect
            }

            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitInstance.apiService.getUserCollections(userId)
                }

                if (response.isSuccessful) {
                    collections = response.body() ?: emptyList()
                    if (collections.isEmpty()) {
                        errorMessage = "No collections available"
                    }
                } else {
                    errorMessage = "Error loading collections"
                }
            } catch (e: Exception) {
                errorMessage = "Connection error, please check your internet"
            }

            isLoading = false
        }

        // show content
        when {
            isLoading -> LoadingScreen()
            errorMessage != null -> ErrorScreen(errorMessage!!)
            collections.isNotEmpty() -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(collections) { collection ->
                        CollectionCard(
                            collection = collection,
                            onClick = { openCollectionDetail(collection.id) }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun PlaceCard(place: Place) {
        var bitmap by remember { mutableStateOf<Bitmap?>(null) }

        // decode image from base64
        LaunchedEffect(place.imageBase64) {
            bitmap = withContext(Dispatchers.IO) {
                try {
                    place.imageBase64?.let {
                        val bytes = Base64.decode(it, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }
                } catch (e: Exception) {
                    null
                }
            }
        }

        // card UI
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

                if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = "Place image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // placeholder while loading image
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Gray),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }

    @Composable
    private fun CollectionCard(
        collection: Collection,
        onClick: () -> Unit
    ) {
        var bitmap by remember { mutableStateOf<Bitmap?>(null) }

        // decode image
        LaunchedEffect(collection.displayImage) {
            bitmap = withContext(Dispatchers.IO) {
                try {
                    collection.displayImage?.let {
                        val bytes = Base64.decode(it, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }
                } catch (e: Exception) {
                    null
                }
            }
        }

        val backgroundColor = if (collection.isCompleted) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        }

        // card UI
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // image
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap!!.asImageBitmap(),
                            contentDescription = "Collection image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // collection info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = collection.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = collection.progressText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LinearProgressIndicator(
                        progress = { collection.visitedPlaces.toFloat() / collection.totalPlaces.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // points
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${collection.points}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "points",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    @Composable
    private fun DefaultCollectionImage() {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Secret collection",
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    @Composable
    private fun LoadingScreen() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }

    @Composable
    private fun ErrorScreen(message: String) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }
    }

    // open detail fragment
    private fun openCollectionDetail(collectionId: Int) {
        val detailFragment = CollectionDetailFragment.newInstance(collectionId)
        val activity = requireActivity() as BaseActivity
        activity.navigateToDetailFragment(detailFragment)
    }
}