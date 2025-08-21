package com.example.placewalqr

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.placewalqr.ui.theme.PlaceWalQRTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import retrofit2.HttpException
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.concurrent.TimeUnit
import kotlin.math.round

class CameraFragment : Fragment() {

    private lateinit var cameraView: PreviewView
    private lateinit var placeView: TextView
    private lateinit var placeInfoView: TextView
    private lateinit var visitBtn: Button
    private lateinit var souvenirBtn: Button

    private lateinit var confirmSouvenirBtn: Button
    private lateinit var discardSouvenirBtn: Button
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var camera: androidx.camera.core.Camera

    private lateinit var lastRawValue: String
    private var isProcessingEnabled: Boolean = true

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private val LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION

    private var longitude = 0.0
    private var latitude = 0.0

    // definizione dei permessi per accedere alla fotocamera, vedi AndroidManifest per dettagli
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

    private lateinit var composeProgressIndicator: ComposeView
    private var isLoadingState by mutableStateOf(false)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.camera_activity, container, false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraView = view.findViewById(R.id.camera_view)
        cameraView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE

        placeView = view.findViewById(R.id.place_view)
        placeInfoView = view.findViewById(R.id.place_info)

        placeInfoView.visibility = View.GONE

        lastRawValue = ""

        // inizializzazione e avvio tracciamento posizione
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        locationRequest = LocationRequest.Builder(500L).apply{
            setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            setMinUpdateIntervalMillis(1000L)
        }.build()

        locationCallback = object: LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                Log.i("LocationFragment", "onLocationResult")
                locationResult?.locations?.forEach{ location ->
                    Log.i("LocationFragment", " === Position: ${location.latitude} - ${location.longitude} === ")
                    latitude = round(location.latitude * 10000) / 10000
                    longitude = round(location.longitude * 10000) / 10000
                }
            }
        }

        if(allPermissionsGranted()){
            startLocationUpdates()
            startCamera()
        } else {
            activityResultLauncher.launch(REQUIRED_PERMISSIONS)
        }

        visitBtn = view.findViewById(R.id.confirmation_button)

        souvenirBtn = view.findViewById(R.id.take_souvenir_photo)
        confirmSouvenirBtn = view.findViewById(R.id.confirm_souvenir_photo)
        discardSouvenirBtn = view.findViewById(R.id.discard_souvenir_photo)
        souvenirBtn.setOnClickListener { updateCamera() }
        souvenirBtn.visibility = View.GONE
        confirmSouvenirBtn.visibility = View.GONE
        discardSouvenirBtn.visibility = View.GONE

        composeProgressIndicator = view.findViewById(R.id.compose_progress_indicator)
        composeProgressIndicator.setContent {
            PlaceWalQRTheme {
                IndeterminateCircularIndicator(isLoading = isLoadingState)
            }
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        // listenableFuture per ottenere ProcessCameraProvider come ListenableFuture dal contesto attuale
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        //aggiungo un listener con runnable ed executor
        cameraProviderFuture.addListener(
            {
                //ottengo il cameraProvider attuale
                cameraProvider = cameraProviderFuture.get()

                // definisco la preview che mi serve a visualizzare cosa vede la fotocamera posteriore
                val preview = androidx.camera.core.Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(cameraView.surfaceProvider)
                    }

                // ImageAnalysis permette di fornire immagini direttamente dalla fotocamera
                // per effettuare l'elaborazione asincronamente.
                // setBackpressureStrategy gestisce il flusso di frame in modo da tenere
                // solo gli ultimi che potrebbero accodarsi in situazioni di sovraccarico
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor, {imageProxy ->
                    processImage(imageProxy)
                })

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // rimuovo ogni possibile bind precedente della fotocamera e
                // lo lego al lifecycle attuale
                try {
                    cameraProvider.unbindAll()
                    camera = cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageAnalysis
                    )

                    val factory = cameraView.meteringPointFactory
                    val point = factory.createPoint(cameraView.width / 2f, cameraView.height / 2f)

                    val autoFocusAction = FocusMeteringAction.Builder(point)
                        .setAutoCancelDuration(10, TimeUnit.SECONDS)
                        .build()

                    camera.cameraControl.startFocusAndMetering(autoFocusAction)
                    visitBtn.setOnClickListener {
                        visitPlaceById()
                    }

                } catch (exc: Exception) {
                    Toast.makeText(requireContext(),
                        "Use case binding failed: ${exc.localizedMessage}",
                        Toast.LENGTH_SHORT).show()
                }
            }, ContextCompat.getMainExecutor(requireContext())
        )
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImage(imageProxy: ImageProxy) {

        if(!isProcessingEnabled){
            imageProxy.close()
            return
        }

        // definisce impostazioni per utilizzo dello scanner
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)  // cerca di trovare qr code
            //.enableAllPotentialBarcodes()   // abilita potenziali codici da scansionare
            .build()

        val mediaImage = imageProxy.image

        if(mediaImage != null){
            //Toast.makeText(this, "Processing image...", Toast.LENGTH_LONG).show()

            // dall'immagine ottiene informazioni che possono essere usate per la scansione
            val img = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val scanner = BarcodeScanning.getClient(options)

            // estrazione dati da tutti i codici trovati
            val result = scanner.process(img)
                .addOnSuccessListener{
                        barcodes ->
                    for (barcode in barcodes) {

                        // estrae il valore della stringa contenuta nel qr code
                        val rawValue = barcode.rawValue

                        // Toast.makeText(this, "RawValue: ${rawValue}, ValueType: ${valueType}", Toast.LENGTH_LONG).show()
                        Log.i("CameraFragment", "barcode: ${barcode}, rawValue: ${rawValue}")

                        if(rawValue!= "" && rawValue != lastRawValue)
                            lastRawValue = rawValue.toString()
                    }

                    Log.i("CameraFragment", "lastRawValue: ${lastRawValue}")

                    if(lastRawValue == "" || lastRawValue.toIntOrNull() == null){
                        placeView.setText("Scanning for a valid QR code...")
                    } else {
                        placeView.setText("Place detected, now click the button!")
                    }
                    imageProxy.close()
                }
                .addOnFailureListener {
                    Log.i( "CameraFragment", "Code detected")
                }
        } else {
            Log.e( "CameraFragment", "No code detected")
        }
    }

    private fun visitPlaceById() {
        isProcessingEnabled = false
        val sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("id", "0")!!.toInt()

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val current = LocalDateTime.now().format(formatter).toString()

        //Log.w("CameraFragment", "place_id: $lastRawValue, user_email: $userEmail, date_of_visit: $current")

        lifecycleScope.launch {
            try{
                val visitPlaceRequest = VisitPlaceRequest(lastRawValue.toInt(), userId, current)
                val response = RetrofitInstance.apiService.visitPlaceById(visitPlaceRequest)

                if(response.isSuccessful && response.body() != null){
                    val info = response.body()!!

                    Log.w("CameraFragment", "info_status = ${info.status}")
                    //Log.i("CameraFragment", "RESPONSE - Name: ${info.place_name}, Description: ${info.place_description}, Status: ${info.status}")

                    if(response.code() == 201){
                        Toast.makeText(requireContext(), "Congratulation! You visited this place!", Toast.LENGTH_SHORT).show()
                        souvenirBtn.visibility = View.VISIBLE
                    } else {
                        if(response.code() == 200) {
                            Toast.makeText(requireContext(), "You already saw this place!", Toast.LENGTH_SHORT).show()
                            stopLocationUpdates()
                        }
                    }
                    Log.i("CameraFragment", "Before setting UI elements")
                    placeView.text = info?.name
                    placeInfoView.text = info?.information
                    placeInfoView.visibility = View.VISIBLE
                    cameraView.visibility = View.GONE
                    visitBtn.visibility = View.GONE
                    Log.i("CameraFragment", "UI elements set, stopping camera and location")

                    stopCamera()
                    stopLocationUpdates()
                    Log.i("CameraFragment", "Camera and location stopped - should stay on this screen")

                } else {
                    Log.w("CameraFragment", "Bad request, response.body: ${response.code()}")
                    if(response.code() >= 400){
                        Toast.makeText(requireContext(), "Bad request", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: IOException){
                Log.e("CameraFragment", "IO Exception: ${e}")
                Toast.makeText(requireContext(), "Connection error", Toast.LENGTH_SHORT).show()
            } catch (e: HttpException) {
                Log.e("CameraFragment", "HTTP Exception: ${e.message}")
                Toast.makeText(requireContext(), "Server error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendTakenSouvenir(imageString: String) {
        val sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("id", "")

        if(userId == ""){
            Log.i("CameraFragment", "Invalid user!")
            return
        }

        lifecycleScope.launch {
            isLoadingState = true
            composeProgressIndicator.visibility = View.VISIBLE

            try {
                val souvenirRequest = SouvenirRequest(lastRawValue.toInt(), userId!!.toInt(), imageString)
                val response = RetrofitInstance.apiService.saveSouvenir(souvenirRequest)

                if (response.isSuccessful && response.body() != null) {

                    val info = response.body()!!
                    Log.i("CameraFragment", "info.status: ${info.status} - response.code(): ${response.code()}")
                    if (response.code() == 201) {
                        Toast.makeText(requireContext(), "Photo saved!", Toast.LENGTH_SHORT).show()
                        confirmSouvenirBtn.visibility = View.GONE
                        discardSouvenirBtn.visibility = View.GONE
                        souvenirBtn.visibility = View.VISIBLE

                        val intent = Intent(requireContext(), HomepageFragment::class.java)
                        startActivity(intent)
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Error occurred: photo not saved!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: IOException) {
                Log.e("CameraFragment", "IO Exception: ${e}")
                Toast.makeText(requireContext(), "Connection error", Toast.LENGTH_SHORT).show()
            } catch (e: HttpException) {
                Log.e("CameraFragment", "HTTP Exception: ${e.message}")
                Toast.makeText(requireContext(), "Server error", Toast.LENGTH_SHORT).show()
            } finally {
                isLoadingState = false
                composeProgressIndicator.visibility = View.GONE
            }
        }
    }

    private fun stopCamera() {
        try {
            // Ferma il processamento delle immagini
            isProcessingEnabled = false

            // Unbind tutti i use cases dalla fotocamera
            if (::cameraProvider.isInitialized) {
                cameraProvider.unbindAll()
            }

            // Shutdown dell'executor
            if (::cameraExecutor.isInitialized) {
                cameraExecutor.shutdown()
            }

            Log.i("CameraFragment", "Camera stopped successfully")
        } catch (exc: Exception) {
            Log.e("CameraFragment", "Error stopping camera: ${exc.localizedMessage}")
        }
    }

    private fun updateCamera(){
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener(
            {
                try {
                    // ottengo il camera provider
                    cameraProvider = cameraProviderFuture.get()

                    val preview = androidx.camera.core.Preview.Builder()
                        .build()
                        .also {
                            it.setSurfaceProvider(cameraView.surfaceProvider)
                        }

                    // solita selezione della camera posteriore
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .setTargetRotation(requireActivity().windowManager.defaultDisplay.rotation)
                        .build()

                    // rimuovo vecchi bind e lego al lifecycle
                    cameraProvider.unbindAll()

                    // IMPORTANTE: mettere imageCapture dentro al provider della camera,
                    // se non lo metti non ti scatta la foto!!!
                    camera = cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        imageCapture,
                        preview
                    )

                    // imposto autofocus al centro
                    val factory = cameraView.meteringPointFactory
                    val point = factory.createPoint(cameraView.width / 2f, cameraView.height / 2f)

                    val autoFocusAction = FocusMeteringAction.Builder(point)
                        .setAutoCancelDuration(10, TimeUnit.SECONDS)
                        .build()

                    camera.cameraControl.startFocusAndMetering(autoFocusAction)

                    placeInfoView.visibility = View.GONE

                    Log.i("CameraFragment", "Camera reinitialized for souvenir")

                } catch (exc: Exception) {
                    Log.e("CameraFragment", "Error updating camera: ${exc.localizedMessage}")
                    Toast.makeText(requireContext(),
                        "Camera initialization failed: ${exc.localizedMessage}",
                        Toast.LENGTH_SHORT).show()
                }
            },
            ContextCompat.getMainExecutor(requireContext())
        )

        cameraView.visibility = View.VISIBLE
        visitBtn.visibility = View.GONE
        souvenirBtn.setText("TAKE THE PHOTO")
        souvenirBtn.setOnClickListener { takePhoto() }
    }

    fun checkPermission(permission: String): Boolean {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.i("LocationFragment", "Location permission granted")
            return true
        } else {
            Log.e("LocationFragment", "Location permission denied")
            return false
        }
    }

    private fun takePhoto(){
        placeInfoView.visibility = View.GONE
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    Log.i("CameraFragment", "Photo captured successfully! Image: ${image}")

                    // imageProxy mantiene rotazione del sensore con cui immagine è acquisita
                    // necessario renderla bitmap e ruotarla
                    val rotatedBitmap = imageProxyToBitmap(image)
                    cameraView.foreground = android.graphics.drawable.BitmapDrawable(resources, rotatedBitmap)
                    val imageString = bitmapToBase64(rotatedBitmap)

                    Log.i("CameraFragment", "imageString: ${imageString}")
                    Toast.makeText(requireContext(), "Photo taken!", Toast.LENGTH_SHORT).show()

                    image.close()
                    confirmSouvenirBtn.setOnClickListener { sendTakenSouvenir(imageString) }
                    confirmSouvenirBtn.visibility = View.VISIBLE
                    discardSouvenirBtn.visibility = View.VISIBLE
                    souvenirBtn.visibility = View.GONE

                    discardSouvenirBtn.setOnClickListener {
                        confirmSouvenirBtn.visibility = View.GONE
                        discardSouvenirBtn.visibility = View.GONE
                        souvenirBtn.visibility = View.VISIBLE
                        cameraView.foreground = null
                        updateCamera()
                    }

                }
            })
        Log.i("CameraFragment", "I am here!!")
    }

    private fun startLocationUpdates() {
        if(checkPermission(LOCATION_PERMISSION)) {
            try {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    android.os.Looper.getMainLooper()
                )
                Log.i("LocationFragment", "Location updates started")
            } catch (e: SecurityException) {
                Log.e("LocationFragment", "Error starting location updates", e)
            }
        }
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        // Applica la rotazione fisica al bitmap
        val rotationDegrees = image.imageInfo.rotationDegrees
        return if (rotationDegrees != 0) {
            val matrix = Matrix()
            matrix.postRotate(rotationDegrees.toFloat())
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String{
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.getEncoder().encodeToString(byteArray)
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.i("LocationFragment", "Location updates stopped")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopCamera()
        stopLocationUpdates()
    }

    @Composable
    fun IndeterminateCircularIndicator(isLoading: Boolean) {
        if (!isLoading) return

        CircularProgressIndicator(
            modifier = Modifier.width(16.dp),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }

    // richiedo i permessi, tramite popup, nel caso in cui non siano stati grantati
    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            var permissionsGranted = true
            permissions.entries.forEach {
                if(it.key in REQUIRED_PERMISSIONS && it.value == false)
                    permissionsGranted = false
            }
            if(!permissionsGranted){
                Toast.makeText(requireContext(),
                    "Permission request denied" ,
                    Toast.LENGTH_SHORT).show()
            } else {
                startLocationUpdates()
                startCamera()
            }
        }
}