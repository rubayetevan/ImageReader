package com.veriff.imagesdk

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import java.util.*
import kotlin.concurrent.timerTask


class ImageReaderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_reader)
        Timer().schedule(timerTask {
            val intent = Intent()
            intent.putExtra("text","Hello World")
            setResult(RESULT_OK, intent)
            finish()
        }, 2000)

    }



    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

        fun start(context:Context,activityResultLauncher: ActivityResultLauncher<Intent>) {
            activityResultLauncher.launch(Intent(context, ImageReaderActivity::class.java))
        }
    }
}