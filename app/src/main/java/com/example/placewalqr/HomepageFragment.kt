package com.example.placewalqr

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class HomepageFragment : Fragment(R.layout.homepage_activity){
    lateinit var user: String
    lateinit var email: String
    private var id=0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    HomepageComposeContent()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?){
        super.onViewCreated(view, savedInstanceState)
        loadUserData()
    }

    private fun loadUserData() {
        val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        user = sharedPreferences.getString("nickname", "").toString()
        email = sharedPreferences.getString("email", "").toString()
        id = sharedPreferences.getString("id", "0").toString().toInt()
    }


    private suspend fun loadHomePageData(
        onDataLoaded: (points: Int, sights: Int, lastPlace: String, image: Bitmap?, place_points:Int) -> Unit
    ) {
        try {
            val responsePoint = RetrofitInstance.apiService.getPointsById(id)
            val responsePlace = RetrofitInstance.apiService.findLastPlaceById(id)

            var points = 0
            var sights = 0
            var lastPlace = ""
            var image: Bitmap? = null
            var place_points=0

            if (responsePoint.code() == 200) {
                val body = responsePoint.body()
                points = body?.points ?: 0
                sights = body?.count ?: 0
            }

            if (responsePlace.code() == 200) {
                val body = responsePlace.body()
                lastPlace = body?.name ?: "None"
                place_points=body?.point?:0

                val imageBytes = body?.getImageBytes()
                if (imageBytes != null && imageBytes.isNotEmpty()) {
                    image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                }
            }

            onDataLoaded(points, sights, lastPlace, image, place_points)
        } catch (e: Exception) {
            onDataLoaded(0, 0, "", null, 0)
        }
    }

    @Composable
    private fun HomepageComposeContent(){
        var isLoading by remember { mutableStateOf(true) }
        var hasVisitedPlaces by remember { mutableStateOf(false) }
        var userPoints by remember { mutableIntStateOf(0) }
        var userSights by remember { mutableIntStateOf(0) }
        var userLastPlace by remember { mutableStateOf("") }
        var userImage by remember { mutableStateOf<Bitmap?>(null) }
        var placePoints by remember { mutableStateOf(0) }

        LaunchedEffect(Unit) {
            loadHomePageData { points, sights, lastPlace, image, place_points ->
                userPoints = points
                userSights = sights
                userLastPlace = lastPlace
                userImage = image
                hasVisitedPlaces = points > 0 || sights > 0
                isLoading = false
                placePoints=place_points
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header con benvenuto
            WelcomeHeader(user = if (::user.isInitialized) user else "")

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(32.dp)
                )
            } else if (hasVisitedPlaces) {
                StatsSection(points = userPoints, sights = userSights)

                LastPlaceSection(
                    placeName = userLastPlace,
                    placeImage = userImage,
                    placePoints= placePoints
                )
            } else {
                NoVisitedPlacesSection()
            }

            LogoutButton {
                val intent = Intent(requireContext(), MainActivity::class.java)
                startActivity(intent)
                requireActivity().finish()
            }
        }
    }

    @Composable
    private fun LogoutButton(onLogout: () -> Unit) {
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Logout")
        }
    }

    @Composable
    private fun NoVisitedPlacesSection() {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .padding(bottom = 16.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "No places visited yet",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Start exploring to see your progress",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }

    @Composable
    private fun LastPlaceSection(placeName: String, placeImage: Bitmap?, placePoints:Int) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row {
                    Text(
                        text = "Last place visited",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Spacer(modifier = Modifier.padding(4.dp))

                    Text(
                        text = placeName.ifEmpty { "No place visited" },
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Spacer(modifier = Modifier.padding(8.dp))

                    Text(
                        text = "Points",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Spacer(modifier = Modifier.padding(4.dp))

                    Text(
                        text = placePoints.toString(),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                placeImage?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Last place visited photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }

    @Composable
    fun StatsSection(points: Int, sights: Int) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                icon = Icons.Default.Star,
                title = "Points",
                value = points.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = Icons.Default.LocationOn,
                title = "Places visited",
                value = sights.toString(),
                modifier = Modifier.weight(1f)
            )
        }
    }

    @Composable
    private fun StatCard(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        title: String,
        value: String,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(32.dp)
                        .padding(bottom = 8.dp)
                )
                Text(
                    text = value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    @Composable
    fun WelcomeHeader(user: String) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Welcome${if (user.isNotEmpty()) ", $user" else ""}!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}