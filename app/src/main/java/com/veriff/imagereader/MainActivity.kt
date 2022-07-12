package com.veriff.imagereader

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.squareup.picasso.Picasso
import com.veriff.imagereader.databinding.ActivityMainBinding
import com.veriff.imagesdk.ImageReaderActivity
import com.veriff.imagesdk.util.RecognizeType

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        viewBinding.btnDetectFace.visibility= View.VISIBLE
        viewBinding.btnReadText.visibility= View.VISIBLE

        viewBinding.btnDetectFace.setOnClickListener {
            viewBinding.imageFace.setImageResource(0)
            ImageReaderActivity.start(this@MainActivity,
                getContent,
                RecognizeType.FACE)
        }
        viewBinding.btnReadText.setOnClickListener {
            viewBinding.txtResult.text = ""
            ImageReaderActivity.start(this@MainActivity,
                getContent,
                RecognizeType.TEXT)
        }

    }

    private val getContent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { it ->
            Log.d("MainActivity", it.toString())

            it.data?.getStringExtra(ImageReaderActivity.KEY_RECOGNIZE_TYPE)?.let { rtype ->
                val data = it.data?.getStringExtra(ImageReaderActivity.KEY_DATA)
                when (RecognizeType.valueOf(rtype)) {
                    RecognizeType.FACE -> {
                        if (it.resultCode == ImageReaderActivity.RESULT_SUCCESS) {
                            val uri = Uri.parse(data)
                            Picasso.get().load(uri).into(viewBinding.imageFace)
                            viewBinding.btnDetectFace.visibility= View.GONE
                        } else if (it.resultCode == ImageReaderActivity.RESULT_ERROR) {
                            viewBinding.btnDetectFace.visibility= View.VISIBLE
                            val numberOfFaces =
                                it.data?.getIntExtra(ImageReaderActivity.KEY_NUMBER_OF_FACES, 0)
                            Toast.makeText(this@MainActivity,
                                "$numberOfFaces face(s) detected in the image",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                    RecognizeType.TEXT -> {
                        if (it.resultCode == ImageReaderActivity.RESULT_SUCCESS) {
                            viewBinding.txtResult.text = data
                            viewBinding.btnReadText.visibility= View.GONE
                        } else if (it.resultCode == ImageReaderActivity.RESULT_ERROR) {
                            viewBinding.btnReadText.visibility= View.VISIBLE
                            Toast.makeText(this@MainActivity,
                                "Unable to detect $rtype in the image",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

}