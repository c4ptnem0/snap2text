package com.example.snap2text.ui.snap2Text

import android.app.Activity
import android.app.ProgressDialog
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.*
import android.os.Environment
import android.provider.MediaStore
import android.view.MenuItem
import android.widget.*
import androidx.core.content.FileProvider
import com.example.snap2text.R
import com.google.android.material.button.MaterialButton
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class Image2TextActivity : AppCompatActivity() {

    private lateinit var selectImageBtn: MaterialButton
    private lateinit var recognizeTextBtn: MaterialButton
    private lateinit var imageResultIV: ImageView
    private lateinit var recognizedTextET: EditText
    private lateinit var copyTextBtn: ImageView
    private lateinit var saveToTxtBtn: Button

    private lateinit var cameraPermissions: Array<String>
    private lateinit var storagePermissions: Array<String>
    private lateinit var progressDialog: ProgressDialog
    private lateinit var textRecognizer: TextRecognizer
    private var imageUri: Uri? = null

    companion object {
        private const val CAMERA_REQUEST_CODE = 100
        private const val STORAGE_REQUEST_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image2_text)

        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Image to Text"

        selectImageBtn = findViewById(R.id.selectImageBtn)
        recognizeTextBtn = findViewById(R.id.recognizeTextBtn)
        imageResultIV = findViewById(R.id.imageResultIV)
        recognizedTextET = findViewById(R.id.recognizedTextET)
        copyTextBtn = findViewById(R.id.copyTextBtn)
        saveToTxtBtn = findViewById(R.id.saveToTxtBtn)

        cameraPermissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        storagePermissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)

        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        // save to text file
        saveToTxtBtn.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "text/plain"
                intent.putExtra(Intent.EXTRA_TITLE, "Snap2Text.txt")
                saveActivityResultLauncher.launch(intent)
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    STORAGE_REQUEST_CODE
                )
            }
        }

        // copy all the text recognized when button is clicked
        copyTextBtn.setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val text = recognizedTextET.text.toString().trim()
            if (text.isNotEmpty()) {
                recognizedTextET.setSelection(recognizedTextET.text.length)
                clipboard.setPrimaryClip(ClipData.newPlainText("text", text))
                showToast("Text copied to clipboard")
            }
        }


        selectImageBtn.setOnClickListener {
            showSelectImageDialog()
        }

        recognizeTextBtn.setOnClickListener {
            if (imageUri == null) {
                showToast("Select an image first!")
            } else {
                recognizeTextFromImage()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    // function for recognizing text from image
    private fun recognizeTextFromImage() {
        progressDialog.setMessage("Processing Image")
        progressDialog.show()

        try {
            val inputImage = InputImage.fromFilePath(this, imageUri!!)
            progressDialog.setMessage("Recognizing Text")

            textRecognizer.process(inputImage)
                .addOnSuccessListener { text ->
                    val recognizedText = text.text
                    progressDialog.dismiss()
                    recognizedTextET.setText(recognizedText)
                }
                .addOnFailureListener { e ->
                    progressDialog.dismiss()
                    showToast("Failed to recognize text due to ${e.message}")
                }
        } catch (e: Exception) {
            progressDialog.dismiss()
            showToast("Failed to prepare image due to ${e.message}")
        }
    }

    // popup menu
    private fun showSelectImageDialog() {
        val popupMenu = PopupMenu(this, selectImageBtn)
        popupMenu.menu.add(Menu.NONE, 1, 2, "CAMERA")
        popupMenu.menu.add(Menu.NONE, 2, 2, "GALLERY")
        popupMenu.show()

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                1 -> {
                    if (checkCameraPermission()) {
                        selectImageCamera()
                    } else {
                        requestCameraPermission()
                    }
                }
                2 -> {
                    if (checkStoragePermission()) {
                        selectImageGallery()
                    } else {
                        requestStoragePermission()
                    }
                }
            }
            true
        }
    }

    // function for selecting image from gallery
    private fun selectImageGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryActivityResultLauncher.launch(intent)
    }

    private val galleryActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                imageUri = data?.data
                imageResultIV.setImageURI(imageUri)
            } else {
                showToast("Cancelled!")
            }
        }

    private val saveActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                if (data != null && data.data != null) {
                    val txtUri = data.data
                    if (txtUri != null) {
                        writeTextToTxt(recognizedTextET.text.toString(), txtUri)
                        showToast("Text file saved successfully!")
                    } else {
                        showToast("Failed to create text file")
                    }
                }
            } else {
                showToast("Failed to create text file")
            }
        }

    private fun selectImageCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "Sample Title")
        values.put(MediaStore.Images.Media.DESCRIPTION, "Sample Description")

        imageUri =
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraActivityResultLauncher.launch(intent)
    }

    private val cameraActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                imageResultIV.setImageURI(imageUri)
            } else {
                showToast("Cancelled!")
            }
        }

    private fun writeTextToTxt(text: String, txtUri: Uri) {
        val outputStream = contentResolver.openOutputStream(txtUri)
        try {
            outputStream?.use { it.write(text.toByteArray()) }
            showToast("Text file saved successfully!")
        } catch (e: IOException) {
            e.printStackTrace()
            showToast("Failed to save text file")
        }
    }

    // function for creating text file and save to storage
    private fun createTxtFile(): Uri? {
        val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val txtFileName = "Snap2Text_$timeStamp.txt"
        val folder = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val txtFile = File(folder, txtFileName)

        try {
            txtFile.createNewFile()
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }

        return FileProvider.getUriForFile(
            this,
            "com.example.snap2text.fileprovider",
            txtFile
        )
    }

    private fun checkStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkCameraPermission(): Boolean {
        val cameraResult =
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val storageResult =
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        return cameraResult && storageResult
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            storagePermissions,
            STORAGE_REQUEST_CODE
        )
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            cameraPermissions,
            CAMERA_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()) {
                    val cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    val storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED
                    if (cameraAccepted && storageAccepted) {
                        selectImageCamera()
                    } else {
                        showToast("Camera & Storage permission are required.")
                    }
                }
            }
            STORAGE_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()) {
                    val storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    val recognizedText = recognizedTextET.text.toString()
                    if (storageAccepted) {
                        selectImageGallery()
                        val txtFile = createTxtFile()
                        if (txtFile != null) {
                            writeTextToTxt(recognizedText, txtFile)
                            showToast("Text file saved successfully!")
                        } else {
                            showToast("Failed to create text file")
                        }
                    } else {
                        showToast("Storage permission is required.")
                    }
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
