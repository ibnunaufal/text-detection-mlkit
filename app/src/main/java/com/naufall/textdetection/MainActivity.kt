package com.naufall.textdetection

import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.CompoundButton
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.core.content.ContextCompat
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.naufall.textdetection.preferences.SharedPref
import com.naufall.textdetection.textdetector.TextRecognitionProcessor
import java.io.IOException

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btn_live = findViewById<Button>(R.id.btn_live)
        val btn_still = findViewById<Button>(R.id.btn_still)

        btn_live.setOnClickListener {
            startActivity(Intent(this, LivePreviewActivity::class.java))
        }

        btn_still.setOnClickListener {
            startActivity(Intent(this, StillImageActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        val res = findViewById<TextView>(R.id.txt_result)
        val localText = SharedPref.getTextResult(this)
        localText?.let { res.text = it }
    }

}