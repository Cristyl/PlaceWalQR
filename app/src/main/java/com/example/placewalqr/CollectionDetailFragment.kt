package com.example.placewalqr

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CollectionDetailFragment : Fragment() {

    private var collectionId: Int = -1

    companion object {
        private const val ARG_COLLECTION_ID = "collection_id"

        fun newInstance(collectionId: Int): CollectionDetailFragment {
            return CollectionDetailFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_COLLECTION_ID, collectionId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        collectionId = arguments?.getInt(ARG_COLLECTION_ID) ?: -1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    CollectionDetailScreen()
                }
            }
        }
    }

    @Composable
    fun CollectionDetailScreen() {
        var isLoading by remember { mutableStateOf(true) }
        var collection by remember { mutableStateOf<Collection?>(null) }
        var places by remember { mutableStateOf<List<CollectionPlace>>(emptyList()) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(collectionId) {
            if (collectionId == -1) {
                errorMessage = "Invalid collection ID"
                isLoading = false
                return@LaunchedEffect
            }

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
                    RetrofitInstance.apiService.getCollectionPlaces(collectionId, userId)
                }

                if (response.isSuccessful) {
                    val data = response.body()
                    if (data != null) {
                        collection = data.collection
                        places = data.places

                        if (places.isEmpty()) {
                            errorMessage = "No places found in this collection"
                        }
                    } else {
                        errorMessage = "Invalid response data"
                    }
                } else {
                    errorMessage = when (response.code()) {
                        404 -> "Collection not found"
                        500 -> "Server error, please try again later"
                        else -> "Error loading collection details"
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Connection error, please check your internet"
            }

            isLoading = false
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Header con back button e info collezione
            CollectionHeader(
                collection = collection,
                onBackClick = {
                    parentFragmentManager.popBackStack()
                }
            )

            // Contenuto principale
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
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    parentFragmentManager.popBackStack()
                                }
                            ) {
                                Text("Go Back")
                            }
                        }
                    }
                }

                places.isNotEmpty() -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(places) { place ->
                            CollectionPlaceCard(place)
                        }

                        // Spazio extra per la navbar
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun CollectionHeader(
        collection: Collection?,
        onBackClick: () -> Unit
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = collection?.displayName ?: "Loading...",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    collection?.let {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${it.progressText} completed",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            if (it.isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Completed",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Punti della collezione
                collection?.let {
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "${it.points}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "points",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun CollectionPlaceCard(place: CollectionPlace) {
        val backgroundColor = if (place.isVisited) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }

        val statusColor = if (place.isVisited) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Immagine del luogo
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    if (place.displayImage != null) {
                        val bitmap = remember(place.displayImage) {
                            try {
                                val bytes = Base64.decode(place.displayImage, Base64.DEFAULT)
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        bitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "Place image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } ?: SecretPlaceImage()
                    } else {
                        SecretPlaceImage()
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Nome del luogo
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = place.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (place.isVisited)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Status badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = statusColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = if (place.isVisited) "VISITED" else "NOT VISITED",
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }

    @Composable
    private fun SecretPlaceImage() {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Secret place",
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}