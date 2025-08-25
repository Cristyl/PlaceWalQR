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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
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
                PlaceWalQRTheme {
                    CameraScreen(
                        onNavigateToHome = {
                            parentFragmentManager.beginTransaction()
                                .replace(R.id.content_frame, HomepageFragment())
                                .commit()
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

    // stati per variabili
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

    // variabili per camera e posizione
    var lastRawValue by remember { mutableStateOf("") }
    var isProcessingEnabled by remember { mutableStateOf(true) }
    var latitude by remember { mutableStateOf(0.0) }
    var longitude by remember { mutableStateOf(0.0) }

    // variabili per la camera
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val cameraExecutor by remember { mutableStateOf(Executors.newSingleThreadExecutor()) }

    // variabili specifiche per posizione
    var fusedLocationClient by remember { mutableStateOf<FusedLocationProviderClient?>(null) }
    var locationCallback by remember { mutableStateOf<LocationCallback?>(null) }

    // permessi
    val requiredPermissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.VIBRATE)
    var permissionsGranted by remember { mutableStateOf(false) }

    // lancia i permessi e permette di ottenere popup per chiedere autorizzazioni
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions: Map<String, Boolean> ->
        permissionsGranted = permissions.values.all { it }
        if (!permissionsGranted) {
            Toast.makeText(context, "Permission request denied", Toast.LENGTH_SHORT).show()
        }
    }

    // lancia eventuale popup per i permessi
    LaunchedEffect(Unit) {
        permissionsGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (!permissionsGranted) {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    // inizializza la localizzazione se ottenuti i permessi
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

    // pulizia di tutto una volta che viene richiesta l'uscita dalla scehermata
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

    // trasforma un immagine ImageProxy in Bitmap per future lavorazioni
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

    // trasforma il bitmap in una stringa Base64 per inviarla al server
    fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.getEncoder().encodeToString(byteArray)
    }

    // trasforma la stringa Base64 per renderla Bitmap e "comprensibile"
    fun base64ToBitmap(base64String: String?): Bitmap? {
        return try {
            val decodedBytes = Base64.getDecoder().decode(base64String) // The error likely happens here
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: NullPointerException) {
            Log.e("CameraFragment", "Base64 decoding failed: ${e.message}")
            null
        }
    }


    // processa l'ImageProxy
    fun processImage(imageProxy: ImageProxy) {
        if (!isProcessingEnabled) {
            imageProxy.close()
            return
        }

        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()

        val mediaImage = imageProxy.image

        // se input contiene effettivamente un'immagine,
        // procede a usare BarcodeScanning per cercare un QR code valido;
        // se l'ha trovato, estrae la stringa contenuta in esso e assegna
        // tale valore a lastRawValue.
        // NB: lastRawValue contiene l'ULTIMO valore processato, NON il primo!!!
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

    // funzione per comunicare al server di voler visitare tale luogo
    fun visitPlaceById() {
        isProcessingEnabled = false
        val sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("id", "0")!!.toInt()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val current = LocalDateTime.now().format(formatter).toString()

        coroutineScope.launch {
            // effettua richiesta al backend inviando l'id del luogo e l'id dell'utente
            try {
                val visitPlaceRequest = VisitPlaceRequest(lastRawValue.toInt(), userId, current)
                val response = RetrofitInstance.apiService.visitPlaceById(visitPlaceRequest)

                if (response.isSuccessful && response.body() != null) {
                    // leggero feedback aptico alla ricezione di una risposta positiva
                    if (vibrator.hasVibrator()) {
                        vibrator.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
                    }

                    val info = response.body()!!

                    when (response.code()) {
                        // 201 per indicare l'inserimento della visita, non ancora avvenuta finora; verrà chiesto se si vuole scattare
                        // una foto ricordo per immortalare il momento
                        201 -> {
                            Toast.makeText(context, "Congratulation! You visited this place!", Toast.LENGTH_SHORT).show()
                            showSouvenirButton = true
                        }

                        // 200 per confermare che il luogo è stato già visitato, nessun inserimento e viene
                        // ricevuta l'immagine ricordo precedentemente scattata
                        200 -> {
                            Toast.makeText(context, "You already saw this place!", Toast.LENGTH_SHORT).show()
                            placeImage = base64ToBitmap(info.image)
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

                } else {
                    Toast.makeText(context, "Bad request", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                Toast.makeText(context, "Connection error", Toast.LENGTH_SHORT).show()
            } catch (e: HttpException) {
                Toast.makeText(context, "Server error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // funzione per scattare la foto
    fun takePhoto() {
        imageCapture?.takePicture(
            ContextCompat.getMainExecutor(context),

            // oggetto per la cattura dell'immagine, fa override di cattura
            // con successo per gestire l'immagine
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

    // funzione per invio dell'immagine ricordo al server
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

                // dopo aver trasformato immagine in Base64, procede all'invio della richiesta al backend
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
        // titolo app
        Text(
            text = "PlaceWalQR",
            fontSize = 24.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // box usato per mostrare cosa vede la fotocamera
        // oppure per visualizzare la foto ritornata dal backend
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 13f)
                .background(Color.Transparent)
                .clip(RoundedCornerShape(8.dp))
        ) {

            // immagine presente nella risposta HTTP(S)
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

            // se deve mostrare la camera, ha i permessi giusti ma NON
            // deve mostrare i dettagli del posto allora deve
            // o trovare un QR code valido o scattare la foto ricordo
            if (showCamera && permissionsGranted && !showPlaceDetails) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE

                            // inizializzazione e setup della camera
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                try {
                                    cameraProvider = cameraProviderFuture.get()

                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(surfaceProvider)
                                    }

                                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                                    if (souvenirMode) {
                                        // deve scattare foto ricordo
                                        imageCapture = ImageCapture.Builder()
                                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                                            .build()

                                        cameraProvider?.unbindAll()
                                        camera = cameraProvider?.bindToLifecycle(
                                            lifecycleOwner, cameraSelector, preview, imageCapture
                                        )
                                    } else {
                                        // deve trovare il QR code

                                        // invocazione di ImageAnalysis per ottenere oggetto in grado
                                        // di trovare il QR e prendere SEMPRE l'ultimo rilevato
                                        val imageAnalysis = ImageAnalysis.Builder()
                                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                            .build()

                                        // analisi "al volo" per ottenere il valore contenuto nel QR
                                        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                            processImage(imageProxy)
                                        }

                                        cameraProvider?.unbindAll()
                                        camera = cameraProvider?.bindToLifecycle(
                                            lifecycleOwner, cameraSelector, preview, imageAnalysis
                                        )
                                    }

                                    // autofocus, utile per mettere a fuoco meglio il codice
                                    val factory = meteringPointFactory
                                    val point = factory.createPoint(width / 2f, height / 2f)
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
                        // aggiornamento della box, stessa logica di prima
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
            
            // se immagine luogo non presente nella risposta,
            // mostra la camera per fare foto ricordo
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

            // una volta scattata foto ricordo,
            // mostrala per vedere se utente è soddisfatto
            capturedSouvenirBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Captured souvenir",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // semplice "rotella" di caricamento
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.Center),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // etichetta per luogo
        Text(
            text = "Info:",
            fontSize = 20.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card (
            modifier = Modifier
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            // nome luogo
            Text(
                text = if (showPlaceDetails) placeName else placeDetected,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )

            // informazioni sul luogo
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

        // bottoni vari
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // per visitare il luogo
            if (!showPlaceDetails && !souvenirMode) {
                Button(
                    onClick = { visitPlaceById() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("VISIT THIS PLACE")
                }
            }

            // per attivare funzionalità per foto ricordo
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

            // scattare foto ricordo
            if (souvenirMode && !showConfirmButtons) {
                Button(
                    onClick = { takePhoto() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("TAKE THE PHOTO")
                }
            }

            // conferma o ritenta foto ricordo
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