package com.example.placewalqr

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
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
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.example.placewalqr.ui.theme.PlaceWalQRTheme
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.io.ByteArrayOutputStream
import java.sql.Timestamp
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.json.JSONObject

class CameraActivity : BaseActivity() {

    private lateinit var cameraView: PreviewView
    private lateinit var placeView: TextView

    private lateinit var takeShotBtn: Button
    private lateinit var imageCapture: ImageCapture

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var outputStream: ByteArrayOutputStream

    // definizione dei permessi per accedere alla fotocamera, vedi AndroidManifest per dettagli
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

    data class CapturedImage(
        val bitmap: Bitmap,
        val timestamp: Timestamp
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.camera_activity)

        cameraView = findViewById(R.id.camera_view)
        cameraView.visibility = View.VISIBLE

        placeView = findViewById(R.id.place_view)
        placeView.visibility = View.GONE

        takeShotBtn = findViewById(R.id.camera_btn)

        if(allPermissionsGranted()){
            startCamera()
        } else {
            activityResultLauncher.launch(REQUIRED_PERMISSIONS)
        }

        takeShotBtn.setOnClickListener { takePhotoOnClick() }
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

                // imageCapture per gestire la fotocamera e lo scatto della foto
                imageCapture = ImageCapture.Builder().build()
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // rimuovo ogni possibile bind precedente della fotocamera e
                // lo lego al lifecycle attuale
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture
                    )
                } catch (exc: Exception) {
                    Toast.makeText(baseContext,
                        "Use case binding failed: ${exc.localizedMessage}",
                        Toast.LENGTH_SHORT).show()
                }
            }, ContextCompat.getMainExecutor(this)
        )
    }

    private fun takePhotoOnClick() {

        // scatta foto, genera ImageProxy e la processa
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object: ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)
                    Toast.makeText(baseContext, "Capture success", Toast.LENGTH_SHORT).show()
                    processImage(image)
                    image.close()
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    Toast.makeText(baseContext, "Capture error: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImage(imageProxy: ImageProxy) {

        // definisce impostazioni per utilizzo dello scanner
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)  // cerca di trovare qr code
            .enableAllPotentialBarcodes()   // abilita potenziali codici da scansionare
            .build()

        val mediaImage = imageProxy.image

        if(mediaImage != null){
            Toast.makeText(this, "Processing image...", Toast.LENGTH_LONG).show()

            // dall'immagine ottiene informazioni che possono essere usate per la scansione
            val img = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val scanner = BarcodeScanning.getClient(options)

            // estrazione dati da tutti i codici trovati
            val result = scanner.process(img)
                .addOnSuccessListener{
                        barcodes ->
                    for (barcode in barcodes) {

                        val bounds = barcode.boundingBox
                        val corners = barcode.cornerPoints

                        val rawValue = barcode.rawValue
                        val valueType = barcode.valueType

                        Toast.makeText(this, "Type: ${valueType}", Toast.LENGTH_LONG).show()

                        // switch della visibilità delle view
                        cameraView.visibility = View.GONE
                        placeView.visibility = View.VISIBLE
                    }

                }
                .addOnFailureListener {
                    Toast.makeText(
                        this,
                        "An error occurred", Toast.LENGTH_SHORT
                    ).show()

                }
        } else {
            Toast.makeText(
                this,
                "Lol, non worka :_(", Toast.LENGTH_SHORT
            ).show()
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