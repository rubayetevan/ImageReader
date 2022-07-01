package com.veriff.imagesdk

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
import androidx.camera.core.ImageCapture.FLASH_MODE_AUTO
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.veriff.imagesdk.databinding.ActivityImageReaderBinding
import com.veriff.imagesdk.util.RecognizeType
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class ImageReaderActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityImageReaderBinding
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityImageReaderBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        if (allPermissionsGranted()) {
            viewBinding.viewFinder.visibility = View.VISIBLE
            viewBinding.imageCaptureButton.visibility = View.VISIBLE
            viewBinding.progressBar.visibility = View.GONE
            viewBinding.textView.visibility = View.GONE
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this@ImageReaderActivity, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Set up the listeners for take photo and video capture buttons
        viewBinding.imageCaptureButton.setOnClickListener {
            viewBinding.imageCaptureButton.visibility = View.GONE
                takePhoto()
        }

        // initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun takePhoto() = lifecycleScope.launch {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return@launch

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, MIME_TYPE)
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, IMAGE_SAVE_PATH)
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this@ImageReaderActivity),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Log.d(TAG, msg)
                    runOnUiThread {
                        viewBinding.imageCaptureButton.visibility = View.GONE
                        viewBinding.viewFinder.visibility = View.GONE
                        viewBinding.progressBar.visibility = View.VISIBLE
                        viewBinding.textView.visibility = View.VISIBLE
                    }
                    processImage(output.savedUri)

                }
            }
        )
    }

    private fun processImage(savedUri: Uri?) {
        savedUri?.let { uri ->
            when (recognizeType) {
                RecognizeType.TEXT -> processImageToText(uri)
                RecognizeType.FACE -> processImageToFace(uri)
            }
        } ?: run {
            Log.e(TAG, "processImage: savedUri is null")
        }
    }

    private fun processImageToFace(uri: Uri) {
        val image: InputImage = InputImage.fromFilePath(this@ImageReaderActivity, uri)
        val highAccuracyOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
        val detector = FaceDetection.getClient(highAccuracyOpts)
        detector.process(image)
            .addOnSuccessListener { faces ->
                Log.d(TAG, "Number of face found in the image: ${faces.size}")
                val intent = Intent()
                if (faces.size == 1) {
                    intent.putExtra(KEY_RECOGNIZE_TYPE, recognizeType.toString())
                    intent.putExtra(KEY_DATA, uri.toString())
                    intent.putExtra(KEY_NUMBER_OF_FACES, faces.size)
                    setResult(RESULT_SUCCESS, intent)
                } else {
                    intent.putExtra(KEY_RECOGNIZE_TYPE, recognizeType.toString())
                    intent.putExtra(KEY_DATA, "")
                    intent.putExtra(KEY_NUMBER_OF_FACES, faces.size)
                    setResult(RESULT_ERROR, intent)
                }
                finish()
            }
            .addOnFailureListener { e ->
                e.message?.let { Log.e(TAG, it) }
            }
    }

    private fun processImageToText(uri: Uri) {
        val image: InputImage = InputImage.fromFilePath(this@ImageReaderActivity, uri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                Log.d(TAG, visionText.text)
                val intent = Intent()
                if(visionText.text.isNotEmpty()) {
                    intent.putExtra(KEY_RECOGNIZE_TYPE, recognizeType.toString())
                    intent.putExtra(KEY_DATA, visionText.text)
                    setResult(RESULT_SUCCESS, intent)
                }else{
                    intent.putExtra(KEY_RECOGNIZE_TYPE, recognizeType.toString())
                    intent.putExtra(KEY_DATA, "")
                    setResult(RESULT_ERROR, intent)
                }
                finish()
            }
            .addOnFailureListener { e ->
                e.message?.let { Log.e(TAG, it) }
            }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this@ImageReaderActivity)
        cameraProviderFuture.addListener({
            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setFlashMode(FLASH_MODE_AUTO)
                .setCaptureMode(CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this@ImageReaderActivity, cameraSelector, preview, imageCapture)

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this@ImageReaderActivity))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this@ImageReaderActivity,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        cameraExecutor.shutdown()
        super.onDestroy()
    }

    companion object {
        const val MIME_TYPE = "image/jpeg"
        const val IMAGE_SAVE_PATH = "Pictures/Veriff/Imagesdk"
        const val KEY_DATA ="data"
        const val RESULT_SUCCESS =1
        const val RESULT_ERROR =0
        const val KEY_RECOGNIZE_TYPE ="recognizeType"
        const val KEY_NUMBER_OF_FACES ="faces"
        private const val TAG = "imagesdk"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()

        private lateinit var recognizeType: RecognizeType

        fun start(
            context: Context,
            activityResultLauncher: ActivityResultLauncher<Intent>,
            recognizeType: RecognizeType,
        ) {
            ImageReaderActivity.recognizeType = recognizeType
            activityResultLauncher.launch(Intent(context, ImageReaderActivity::class.java))
        }
    }
}