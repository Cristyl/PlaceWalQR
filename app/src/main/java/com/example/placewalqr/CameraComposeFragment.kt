package com.example.placewalqr

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.placewalqr.ui.theme.PlaceWalQRTheme
import com.google.android.gms.location.*
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.round

class CameraComposeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                PlaceWalQRTheme (darkTheme = false) {
                    CameraScreen(
                        onNavigateToHome = {
                            val activity = activity as? BaseActivity
                            if (activity != null && !activity.isFinishing && !activity.isDestroyed && isAdded) {
                                try {
                                    val containerId = activity.getFragmentContainerId()
                                    if (containerId != 0) {
                                        parentFragmentManager.beginTransaction()
                                            .replace(containerId, HomepageFragment()) // Usa l'ID corretto!
                                            .commitAllowingStateLoss()
                                    }
                                } catch (e: Exception) {
                                    Log.e("Navigation", "Fragment transaction failed: ${e.message}")
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraScreen(
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    // variables for states
    var placeDetected by remember { mutableStateOf("Scanning for a valid QR code...") }
    var placeInfo by remember { mutableStateOf("") }
    var placeName by remember { mutableStateOf("") }
    var placeImage by remember { mutableStateOf<Bitmap?>(null) }
    var showPlaceDetails by remember { mutableStateOf(false) }
    var showCamera by remember { mutableStateOf(true) }
    var showSouvenirButton by remember { mutableStateOf(false) }
    var showConfirmButtons by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var souvenirMode by remember { mutableStateOf(false) }
    var capturedSouvenirBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // side variables for qr codes processing and position
    var lastRawValue by remember { mutableStateOf("") }
    var isProcessingEnabled by remember { mutableStateOf(true) }
    var latitude by remember { mutableStateOf(0.0) }
    var longitude by remember { mutableStateOf(0.0) }

    // variables for camera
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val cameraExecutor by remember { mutableStateOf(Executors.newSingleThreadExecutor()) }

    // specific variables for position
    var fusedLocationClient by remember { mutableStateOf<FusedLocationProviderClient?>(null) }
    var locationCallback by remember { mutableStateOf<LocationCallback?>(null) }

    // permissions
    val requiredPermissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.VIBRATE)
    var permissionsGranted by remember { mutableStateOf(false) }

    // throw permissions, through popup asks for permission
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions: Map<String, Boolean> ->
        permissionsGranted = permissions.values.all { it }
        if (!permissionsGranted) {
            Toast.makeText(context, "Permission request denied", Toast.LENGTH_SHORT).show()
        }
    }

    // able to open permissions popup
    LaunchedEffect(Unit) {
        permissionsGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (!permissionsGranted) {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    // initialize location scan if permissions are granted
    LaunchedEffect(permissionsGranted) {
        if (permissionsGranted) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

            val locationRequest = LocationRequest.Builder(500L).apply {
                setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                setMinUpdateIntervalMillis(1000L)
            }.build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)
                    locationResult.locations.forEach { location ->
                        // arrotonda la posizione decimale
                        latitude = round(location.latitude * 10000) / 10000
                        longitude = round(location.longitude * 10000) / 10000
                    }
                }
            }

            try {
                fusedLocationClient?.requestLocationUpdates(
                    locationRequest,
                    locationCallback!!,
                    Looper.getMainLooper()
                )
            } catch (e: SecurityException) {
                Log.e("CameraScreen", "Error starting location updates", e)
            }
        }
    }

    // cleanup after screen exiting
    DisposableEffect(Unit) {
        onDispose {
            isProcessingEnabled = false
            cameraProvider?.unbindAll()
            cameraExecutor.shutdown()
            locationCallback?.let { callback ->
                fusedLocationClient?.removeLocationUpdates(callback)
            }
        }
    }

    // transform imageProxy into bitmap:
    // extracts image as array of bytes then decodes it as bitmap,
    // finally rotates it if needed
    fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        val rotationDegrees = image.imageInfo.rotationDegrees
        return if (rotationDegrees != 0) {
            val matrix = Matrix()
            matrix.postRotate(rotationDegrees.toFloat())
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    }

    // converts bitmap into base64 string for sending it to server
    fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        // bitmap compression
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.getEncoder().encodeToString(byteArray)
    }

    // converts base64 string into bitmap
    fun base64ToBitmap(base64String: String?): Bitmap? {
        return try {
            val decodedBytes = Base64.getDecoder().decode(base64String) // The error likely happens here
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: NullPointerException) {
            Log.e("CameraFragment", "Base64 decoding failed: ${e.message}")
            null
        }
    }


    // imageProxy processing:
    // if no processing is executed, set to qr code the barcode scan
    // then executes the processing
    fun processImage(imageProxy: ImageProxy) {
        if (!isProcessingEnabled) {
            imageProxy.close()
            return
        }

        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()

        val mediaImage = imageProxy.image

        // if input actually contains an image,
        // uses BarcodeScanning for recognizing a qr code,
        // then extract the value contained in it and set lastRawValue as it
        if (mediaImage != null) {
            val img = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val scanner = BarcodeScanning.getClient(options)

            scanner.process(img)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val rawValue = barcode.rawValue
                        if (rawValue != "" && rawValue != lastRawValue) {
                            lastRawValue = rawValue.toString()
                        }
                    }

                    placeDetected = if (lastRawValue == "" || lastRawValue.toIntOrNull() == null) {
                        "Scanning for a valid QR code..."
                    } else {
                        "Place detected, now click the button!"
                    }
                    imageProxy.close()
                }
                .addOnFailureListener {
                    Log.i("CameraScreen", "Code detection failed")
                    imageProxy.close()
                }
        }
    }

    // tells to server that user wants to visit the place
    fun visitPlaceById() {
        isProcessingEnabled = false
        val sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("id", "0")!!.toInt()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val current = LocalDateTime.now().format(formatter).toString()

        coroutineScope.launch {
            isLoading = true
            try {
                // tries to send a request to the server for telling the visit,
                // it specifies id of place, userId, date, position
                val visitPlaceRequest = VisitPlaceRequest(lastRawValue.toInt(), userId, current, latitude.toDouble(), longitude.toDouble())
                val response = RetrofitInstance.apiService.visitPlaceById(visitPlaceRequest)

                if (response.body() != null) {
                    if (response.isSuccessful){
                        // aptic feedback if success
                        if (vibrator.hasVibrator()) {
                            vibrator.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
                        }

                        val info = response.body()!!

                        when (response.code()) {
                            // 200 as confirmation code of place has been already seen in the past;
                            // if possible, the souvenir photo is showed in the screen
                            200 -> {
                                Toast.makeText(context, "You already saw this place!", Toast.LENGTH_SHORT).show()
                                placeImage = base64ToBitmap(info.image)
                            }

                            // 201 as new place visited, it will ask for taking a souvenir photo
                            201 -> {
                                Toast.makeText(context, "Congratulation! You visited this place!", Toast.LENGTH_SHORT).show()
                                showSouvenirButton = true
                            }
                        }

                        placeName = info.name ?: ""
                        placeInfo = info.information ?: ""
                        if(info.image == null){
                            placeImage = BitmapFactory.decodeResource(context.getResources(),
                                R.drawable.null_place_image);
                        }

                        placeImage = base64ToBitmap(info.image)
                        showPlaceDetails = true
                        // showCamera = false

                    }
                } else {
                    val errorMessage = when(response.code()){
                        400 -> "Incorrect request"
                        404 -> "Not found"
                        406 -> {
                            placeName = "Too far from the place, check your position!"
                            placeImage = BitmapFactory.decodeResource(context.resources, R.drawable.null_place_image)
                            showPlaceDetails = true
                            "Too far from the place, check your position!"
                        }
                        else -> "Bad request: ${response.code()}"
                    }
                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                    if (response.code() != 406) {
                        placeName = "Error"
                        placeImage = BitmapFactory.decodeResource(context.resources,
                            R.drawable.null_place_image);
                    }
                }
            } catch (e: IOException) {
                Toast.makeText(context, "Connection error", Toast.LENGTH_SHORT).show()
            } catch (e: HttpException) {
                Toast.makeText(context, "Server error", Toast.LENGTH_SHORT).show()
            } finally{
                isLoading = false
            }
        }
    }

    fun takePhoto() {
        imageCapture?.takePicture(
            ContextCompat.getMainExecutor(context),

            // such object is used to capture the image, overrides the capture
            // with success for managing the image
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    // immagine ottenuta viene convertita in Bitmap
                    val rotatedBitmap = imageProxyToBitmap(image)
                    capturedSouvenirBitmap = rotatedBitmap
                    showConfirmButtons = true
                    showSouvenirButton = false

                    Toast.makeText(context, "Photo taken!", Toast.LENGTH_SHORT).show()
                    image.close()
                }
            }
        )
    }

    fun sendTakenSouvenir(bitmap: Bitmap) {
        val sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("id", "")

        if (userId == "") {
            Log.i("CameraScreen", "Invalid user!")
            return
        }

        coroutineScope.launch {
            isLoading = true
            try {
                val imageString = bitmapToBase64(bitmap)

                // after transforming the image in Base64, proceeds to send the request to the server
                val souvenirRequest = SouvenirRequest(lastRawValue.toInt(), userId!!.toInt(), imageString)
                val response = RetrofitInstance.apiService.saveSouvenir(souvenirRequest)

                if (response.isSuccessful && response.body() != null) {
                    if (response.code() == 201) {
                        Toast.makeText(context, "Photo saved!", Toast.LENGTH_SHORT).show()
                        onNavigateToHome()
                    }
                } else {
                    Toast.makeText(context, "Error occurred: photo not saved!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                Toast.makeText(context, "Connection error", Toast.LENGTH_SHORT).show()
            } catch (e: HttpException) {
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
            .padding(16.dp)
    ) {
        // image as logo
        Image(painter = painterResource(
            id = if (isSystemInDarkTheme()) R.drawable.placewalqr_logo_dark_lol
            else R.drawable.placewalqr_logo
        ),
            modifier = Modifier.width(250.dp).align(Alignment.CenterHorizontally),
            contentDescription = "App Logo"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // box for showing what the camera sees or
        // for showing the image coming from the backend
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 13f)
                .background(Color.Transparent)
                .clip(RoundedCornerShape(8.dp))
        ) {

            // image inside the request is being extracted
            placeImage?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Place image",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            // if it has to show the camera, it has permissions but
            // it must not show details of place then find a qr code or
            // take a souvenir photo
            if (showCamera && permissionsGranted && !showPlaceDetails) {
                AndroidView(
                    factory = { ctx ->
                        // it sets the preview in a mode that is compatible with the sensor
                        PreviewView(ctx).apply {
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE

                            // obtains an instance of the cameraprovider in order to handle
                            // the camera and its lifecycle
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                try {
                                    cameraProvider = cameraProviderFuture.get()

                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(surfaceProvider)
                                    }

                                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                                    if (souvenirMode) {
                                        // take a souvenir photo
                                        imageCapture = ImageCapture.Builder()
                                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                                            .build()

                                        cameraProvider?.unbindAll()
                                        camera = cameraProvider?.bindToLifecycle(
                                            lifecycleOwner, cameraSelector, preview, imageCapture
                                        )
                                    } else {
                                        // it has to find the qr code

                                        // invokes imageanalysis in order to get the object
                                        // capable to find the code and takes ALWAYS the last one detected
                                        val imageAnalysis = ImageAnalysis.Builder()
                                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                            .build()

                                        // quick analysis for extracting info inside the qr
                                        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                            processImage(imageProxy)
                                        }

                                        cameraProvider?.unbindAll()
                                        camera = cameraProvider?.bindToLifecycle(
                                            lifecycleOwner, cameraSelector, preview, imageAnalysis
                                        )
                                    }

                                    // autofocus
                                    val factory = meteringPointFactory
                                    val point = factory.createPoint(width / 2f, height / 2f)
                                    // autofocus set for 10 seconds
                                    val autoFocusAction = FocusMeteringAction.Builder(point)
                                        .setAutoCancelDuration(10, TimeUnit.SECONDS)
                                        .build()
                                    camera?.cameraControl?.startFocusAndMetering(autoFocusAction)

                                } catch (exc: Exception) {
                                    Log.e("CameraScreen", "Use case binding failed", exc)
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { view ->
                        // preview update, same logic as before:
                        // find the qd from camera, uses the imageanalysis for extracting code content
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                        cameraProviderFuture.addListener({
                            try {
                                val provider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(view.surfaceProvider)
                                }
                                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                                if (souvenirMode) {
                                    imageCapture = ImageCapture.Builder()
                                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                                        .build()

                                    provider.unbindAll()
                                    camera = provider.bindToLifecycle(
                                        lifecycleOwner, cameraSelector, preview, imageCapture
                                    )
                                } else {
                                    val imageAnalysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()

                                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                        processImage(imageProxy)
                                    }

                                    provider.unbindAll()
                                    camera = provider.bindToLifecycle(
                                        lifecycleOwner, cameraSelector, preview, imageAnalysis
                                    )
                                }
                            } catch (exc: Exception) {
                                Log.e("CameraScreen", "Camera update failed", exc)
                            }
                        }, ContextCompat.getMainExecutor(context))
                    }
                )
            }

            // if image of place not found in response, then enable the camera!
            if (souvenirMode && permissionsGranted) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE

                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                try {
                                    cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(surfaceProvider)
                                    }
                                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                                    imageCapture = ImageCapture.Builder()
                                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                                        .build()

                                    cameraProvider?.unbindAll()
                                    camera = cameraProvider?.bindToLifecycle(
                                        lifecycleOwner, cameraSelector, preview, imageCapture
                                    )
                                } catch (exc: Exception) {
                                    Log.e("CameraScreen", "Souvenir camera setup failed", exc)
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // once taken the souvenir photo, shows it to user
            // and let him decide if it is ok or not
            capturedSouvenirBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Captured souvenir",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Info:",
            fontSize = 20.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card (
            modifier = Modifier
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            // colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            // place name
            Text(
                text = if (showPlaceDetails) placeName else placeDetected,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )

            // place name
            if (showPlaceDetails && placeInfo.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = placeInfo,
                    fontSize = 16.sp,
                    fontStyle = FontStyle.Italic,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 8.dp,
                            end = 8.dp,
                            bottom = 8.dp
                        )
                )
        }
}
        Spacer(modifier = Modifier.weight(1f))

        // various buttons
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // "rotella" of loading
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(30.dp)
                        .align(Alignment.CenterHorizontally),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            // button for visiting the place
            if (!showPlaceDetails && !souvenirMode) {
                Button(
                    onClick = { visitPlaceById() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("VISIT THIS PLACE")
                }
            }

            // activating souvenir photo mode
            if (showSouvenirButton && !souvenirMode) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        souvenirMode = true
                        showCamera = true
                        showPlaceDetails = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("TAKE SOUVENIR PHOTO")
                }
            }

            // take souvenir photo
            if (souvenirMode && !showConfirmButtons) {
                placeDetected = "Save this moment with a photo!"
                Button(
                    onClick = {
                        takePhoto()
                              },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("TAKE THE PHOTO")
                }
            }

            // confir or retry photo
            if (showConfirmButtons) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            capturedSouvenirBitmap?.let { bitmap ->
                                sendTakenSouvenir(bitmap)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("CONFIRM")
                    }

                    Button(
                        onClick = {
                            showConfirmButtons = false
                            showSouvenirButton = true
                            capturedSouvenirBitmap = null
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("RETRY")
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}