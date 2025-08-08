package com.example.placewalqr

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraProvider
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.placewalqr.ui.theme.PlaceWalQRTheme
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.sql.Timestamp
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class CameraActivity : BaseActivity() {

    private lateinit var cameraView: PreviewView
    private lateinit var placeView: TextView
    private lateinit var placeInfoView: TextView
    private lateinit var visitBtn: Button

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var camera: androidx.camera.core.Camera

    private lateinit var lastRawValue: String
    private var isProcessingEnabled: Boolean = true

    // definizione dei permessi per accedere alla fotocamera, vedi AndroidManifest per dettagli
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.camera_activity)

        cameraView = findViewById(R.id.camera_view)
        cameraView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        // cameraView.visibility = View.VISIBLE

        placeView = findViewById(R.id.place_view)
        placeInfoView = findViewById(R.id.place_info)
        // placeView.visibility = View.GONE

        lastRawValue = ""

        if(allPermissionsGranted()){
            startCamera()
        } else {
            activityResultLauncher.launch(REQUIRED_PERMISSIONS)
        }

        visitBtn = findViewById(R.id.confirmation_button)

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        // listenableFuture per ottenere ProcessCameraProvider come ListenableFuture dal contesto attuale
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

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
                //setBackpressureStrategy gestisce il flusso di frame in modo da tenere
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
                    Toast.makeText(baseContext,
                        "Use case binding failed: ${exc.localizedMessage}",
                        Toast.LENGTH_SHORT).show()
                }
            }, ContextCompat.getMainExecutor(this)
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
                        Log.i("CameraActivity", "barcode: ${barcode}, rawValue: ${rawValue}")

                        if(rawValue!= "" && rawValue != lastRawValue)
                            lastRawValue = rawValue.toString()
                    }

                    Log.i("CameraActivity", "lastRawValue: ${lastRawValue}")
                    if(lastRawValue == "" || lastRawValue.toIntOrNull() == null){
                        placeView.setText("Invalid QR: ${lastRawValue}")
                    } else {
                        placeView.setText("${lastRawValue}")
                    }
                    imageProxy.close()
                }
                .addOnFailureListener {
                    Log.i( "CameraActivity", "Code detected")
            }
        } else {
            Log.e( "CameraActivity", "No code detected")
        }
    }

    private fun visitPlaceById() {
        isProcessingEnabled = false
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val userEmail = sharedPreferences.getString("email", "").toString()

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val current = LocalDateTime.now().format(formatter).toString()

        //Log.w("CameraActivity", "place_id: $lastRawValue, user_email: $userEmail, date_of_visit: $current")

        lifecycleScope.launch {
            try{
                val visitPlaceRequest = VisitPlaceRequest(lastRawValue.toInt(), userEmail, current)
                val response = RetrofitInstance.apiService.visitPlaceById(visitPlaceRequest)

                if(response.isSuccessful && response.body() != null){
                    val info = response.body()!!

                    Log.w("CameraActivity", "info_status = ${info.status}")
                    //Log.i("CameraActivity", "RESPONSE - Name: ${info.place_name}, Description: ${info.place_description}, Status: ${info.status}")

                    if(response.code() == 201){
                        Toast.makeText(baseContext, "Congratulation! You visited this place!", Toast.LENGTH_SHORT).show()
                    } else {
                        if(response.code() == 200) {
                            Toast.makeText(baseContext, "You already saw this place!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    runOnUiThread {
                        val info = response.body()
                        placeView.text = info?.name
                        placeInfoView.text = info?.information
                    }
                    stopCamera()
                }else {
                    Log.w("CameraActivity", "Bad request, response.body: ${response.code()}")
                    if(response.code() >= 400){
                        Toast.makeText(baseContext, "Bad request", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: IOException){
                Log.e("RegisterActivity", "IO Exception: ${e}")
                Toast.makeText(baseContext, "Connection error", Toast.LENGTH_SHORT).show()
            } catch (e: HttpException) {
                Log.e("MainActivity", "HTTP Exception: ${e.message}")
                Toast.makeText(baseContext, "Server error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopCamera() {
        try {
            // Ferma il processamento delle immagini
            isProcessingEnabled = false

            // Unbind tutti i use cases dalla fotocamera
            cameraProvider.unbindAll()

            // Shutdown dell'executor
            cameraExecutor.shutdown()

            Log.i("CameraActivity", "Camera stopped successfully")
        } catch (exc: Exception) {
            Log.e("CameraActivity", "Error stopping camera: ${exc.localizedMessage}")
        }
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
                Toast.makeText(baseContext,
                    "Permission request denied" ,
                    Toast.LENGTH_SHORT).show()
            } else { startCamera()}
        }

}