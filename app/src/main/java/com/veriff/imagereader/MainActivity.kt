package com.veriff.imagereader

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.veriff.imagesdk.ImageReaderActivity
import com.veriff.imagesdk.util.RecognizeType

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ImageReaderActivity.start(this@MainActivity,getContent,RecognizeType.TEXT)

    }

    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { it ->
        Log.d("MainActivity",it.toString())
        Toast.makeText(this@MainActivity,it.toString(),Toast.LENGTH_SHORT).show()
    }
}