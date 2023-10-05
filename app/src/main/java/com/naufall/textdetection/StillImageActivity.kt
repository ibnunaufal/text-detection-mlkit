package com.naufall.textdetection

import android.app.Activity
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Pair
import android.view.View
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.naufall.textdetection.objectdetector.ObjectDetectorProcessor
import com.naufall.textdetection.preferences.PreferenceUtils
import com.naufall.textdetection.textdetector.TextRecognitionProcessor
import java.io.IOException

class StillImageActivity : AppCompatActivity() {

    private var preview: ImageView? = null
    private var graphicOverlay: GraphicOverlay? = null
    private var selectedMode = OBJECT_DETECTION_CUSTOM
    private var selectedSize: String? = SIZE_SCREEN
    private var isLandScape = false
    private var imageUri: Uri? = null
    // Max width (portrait mode)
    private var imageMaxWidth = 0
    // Max height (portrait mode)
    private var imageMaxHeight = 0
    private var imageProcessor: VisionImageProcessor? = null
    var objectType: String? = null
    var objectNumber: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_still_image)
        basicAlert()
        preview = findViewById(R.id.preview)
        graphicOverlay = findViewById(R.id.graphic_overlay)

        isLandScape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        if (savedInstanceState != null) {
            imageUri = savedInstanceState.getParcelable(KEY_IMAGE_URI)
            imageMaxWidth = savedInstanceState.getInt(KEY_IMAGE_MAX_WIDTH)
            imageMaxHeight = savedInstanceState.getInt(KEY_IMAGE_MAX_HEIGHT)
            selectedSize = savedInstanceState.getString(KEY_SELECTED_SIZE)
        }

        val rootView = findViewById<View>(R.id.root)
        rootView.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    rootView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    imageMaxWidth = rootView.width
                    imageMaxHeight = rootView.height - findViewById<View>(R.id.control).height
                    if (SIZE_SCREEN == selectedSize) {
                        tryReloadAndDetectInImage()
                    }
                }
            }
        )

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            tryReloadAndDetectInImage()
        } else if (requestCode == REQUEST_CHOOSE_IMAGE && resultCode == Activity.RESULT_OK) {
            // In this case, imageUri is returned by the chooser, save it.
            imageUri = data!!.data
            tryReloadAndDetectInImage()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun tryReloadAndDetectInImage() {
        Log.d(TAG, "Try reload and detect image")
        try {
            if (imageUri == null) {
                return
            }

            if (SIZE_SCREEN == selectedSize && imageMaxWidth == 0) {
                // UI layout has not finished yet, will reload once it's ready.
                return
            }

            val imageBitmap = BitmapUtils.getBitmapFromContentUri(contentResolver, imageUri) ?: return
            // Clear the overlay first
            graphicOverlay!!.clear()

            val resizedBitmap: Bitmap
            resizedBitmap =
                if (selectedSize == SIZE_ORIGINAL) {
                    imageBitmap
                } else {
                    // Get the dimensions of the image view
                    val targetedSize: Pair<Int, Int> = targetedWidthHeight

                    // Determine how much to scale down the image
                    val scaleFactor =
                        Math.max(
                            imageBitmap.width.toFloat() / targetedSize.first.toFloat(),
                            imageBitmap.height.toFloat() / targetedSize.second.toFloat()
                        )
                    Bitmap.createScaledBitmap(
                        imageBitmap,
                        (imageBitmap.width / scaleFactor).toInt(),
                        (imageBitmap.height / scaleFactor).toInt(),
                        true
                    )
                }

            preview!!.setImageBitmap(resizedBitmap)
            if (imageProcessor != null) {
                graphicOverlay!!.setImageSourceInfo(
                    resizedBitmap.width,
                    resizedBitmap.height,
                    /* isFlipped= */ false
                )
                imageProcessor!!.processBitmap(resizedBitmap, graphicOverlay)
            } else {
                Log.e(TAG, "Null imageProcessor, please check adb logs for imageProcessor creation error")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error retrieving saved image")
            imageUri = null
        }
    }

    public override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        createImageProcessor()
        tryReloadAndDetectInImage()
    }

    public override fun onPause() {
        super.onPause()
        imageProcessor?.run { this.stop() }
    }

    public override fun onDestroy() {
        super.onDestroy()
        imageProcessor?.run { this.stop() }
    }

    private fun createImageProcessor() {
        try {
            when (selectedMode) {
                OBJECT_DETECTION -> {
                    Log.i(TAG, "Using Object Detector Processor")
                    val objectDetectorOptions = PreferenceUtils.getObjectDetectorOptionsForStillImage(this)
                    imageProcessor = ObjectDetectorProcessor(this, objectDetectorOptions)
                }
                OBJECT_DETECTION_CUSTOM -> {
                    Log.i(TAG, "Using Custom Object Detector Processor")
                    val localModel =
                        LocalModel.Builder().setAssetFilePath("custom_models/object_labeler.tflite").build()
                    val customObjectDetectorOptions =
                        PreferenceUtils.getCustomObjectDetectorOptionsForStillImage(this, localModel)
                    imageProcessor = ObjectDetectorProcessor(this, customObjectDetectorOptions)
                }
                CUSTOM_AUTOML_OBJECT_DETECTION -> {
                    Log.i(TAG, "Using Custom AutoML Object Detector Processor")
                    val customAutoMLODTLocalModel =
                        LocalModel.Builder().setAssetManifestFilePath("automl/manifest.json").build()
                    val customAutoMLODTOptions =
                        PreferenceUtils.getCustomObjectDetectorOptionsForStillImage(
                            this,
                            customAutoMLODTLocalModel
                        )
                    imageProcessor = ObjectDetectorProcessor(this, customAutoMLODTOptions)
                }
                TEXT_RECOGNITION_LATIN ->
                    imageProcessor = TextRecognitionProcessor(this, TextRecognizerOptions.Builder().build())
                else -> Log.e(TAG, "Unknown selectedMode: $selectedMode")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Can not create image processor: $selectedMode", e)
            Toast.makeText(
                applicationContext,
                "Can not create image processor: " + e.message,
                Toast.LENGTH_LONG
            )
                .show()
        }
    }


    private val targetedWidthHeight: Pair<Int, Int>
        get() {
            val targetWidth: Int
            val targetHeight: Int
            when (selectedSize) {
                SIZE_SCREEN -> {
                    targetWidth = imageMaxWidth
                    targetHeight = imageMaxHeight
                }
                SIZE_640_480 -> {
                    targetWidth = if (isLandScape) 640 else 480
                    targetHeight = if (isLandScape) 480 else 640
                }
                SIZE_1024_768 -> {
                    targetWidth = if (isLandScape) 1024 else 768
                    targetHeight = if (isLandScape) 768 else 1024
                }
                else -> throw IllegalStateException("Unknown size")
            }
            return Pair(targetWidth, targetHeight)
        }
    fun basicAlert(){

        val builder = AlertDialog.Builder(this)

        with(builder)
        {
            setTitle("Select Image")
            setMessage("You should image first")
            setPositiveButton("Camera", DialogInterface.OnClickListener { _, _ -> startCameraIntentForResult() })
            setNegativeButton("Gallery") { _, _ -> startChooseImageIntentForResult() }
            setNeutralButton("Close") { _, _ -> finish() }
            setCancelable(false)
            show()
        }
    }

    private fun startCameraIntentForResult() { // Clean up last time's image
        imageUri = null
        preview!!.setImageBitmap(null)
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            val values = ContentValues()
            values.put(MediaStore.Images.Media.TITLE, "New Picture")
            values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera")
            imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

    private fun startChooseImageIntentForResult() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_CHOOSE_IMAGE)
    }

    companion object {
        private const val TAG = "StillImageActivity"
        private const val OBJECT_DETECTION = "Object Detection"
        private const val OBJECT_DETECTION_CUSTOM = "Custom Object Detection"
        private const val CUSTOM_AUTOML_OBJECT_DETECTION = "Custom AutoML Object Detection (Flower)"
        private const val FACE_DETECTION = "Face Detection"
        private const val BARCODE_SCANNING = "Barcode Scanning"
        private const val TEXT_RECOGNITION_LATIN = "Text Recognition Latin"
        private const val TEXT_RECOGNITION_CHINESE = "Text Recognition Chinese"
        private const val TEXT_RECOGNITION_DEVANAGARI = "Text Recognition Devanagari"
        private const val TEXT_RECOGNITION_JAPANESE = "Text Recognition Japanese"
        private const val TEXT_RECOGNITION_KOREAN = "Text Recognition Korean"
        private const val IMAGE_LABELING = "Image Labeling"
        private const val IMAGE_LABELING_CUSTOM = "Custom Image Labeling (Birds)"
        private const val CUSTOM_AUTOML_LABELING = "Custom AutoML Image Labeling (Flower)"
        private const val POSE_DETECTION = "Pose Detection"
        private const val SELFIE_SEGMENTATION = "Selfie Segmentation"
        private const val FACE_MESH_DETECTION = "Face Mesh Detection (Beta)"

        private const val SIZE_SCREEN = "w:screen" // Match screen width
        private const val SIZE_1024_768 = "w:1024" // ~1024*768 in a normal ratio
        private const val SIZE_640_480 = "w:640" // ~640*480 in a normal ratio
        private const val SIZE_ORIGINAL = "w:original" // Original image size
        private const val KEY_IMAGE_URI = "com.google.mlkit.vision.demo.KEY_IMAGE_URI"
        private const val KEY_IMAGE_MAX_WIDTH = "com.google.mlkit.vision.demo.KEY_IMAGE_MAX_WIDTH"
        private const val KEY_IMAGE_MAX_HEIGHT = "com.google.mlkit.vision.demo.KEY_IMAGE_MAX_HEIGHT"
        private const val KEY_SELECTED_SIZE = "com.google.mlkit.vision.demo.KEY_SELECTED_SIZE"
        private const val REQUEST_IMAGE_CAPTURE = 1001
        private const val REQUEST_CHOOSE_IMAGE = 1002
    }
}