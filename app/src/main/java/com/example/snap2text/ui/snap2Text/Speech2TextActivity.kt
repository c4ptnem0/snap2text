package com.example.snap2text.ui.snap2Text

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.Editable
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.FileProvider
import com.example.snap2text.R
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class Speech2TextActivity : AppCompatActivity() {
    private lateinit var copyTextBtn: ImageView
    private lateinit var openMicBtn: Button
    private lateinit var recognizedTextET: EditText
    private lateinit var selectImageBtn: Button
    private lateinit var imageResultIV: ImageView
    private lateinit var saveToTxtBtn: Button

    // to handle result of camera/gallery/mic permission
    private companion object {
        private const val CAMERA_REQUEST_CODE = 100
        private const val STORAGE_REQUEST_CODE = 101
        private const val REQUEST_CODE_SPEECH_INPUT = 102
    }

    private var imageUri: Uri? = null

    private lateinit var cameraPermissions: Array<String>
    private lateinit var storagePermissions: Array<String>
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var recognizerIntent: Intent

    private var previousRecognizedText: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speech2_text)

        // Get the support action bar
        val actionBar = supportActionBar
        // Enable the back button
        actionBar?.setDisplayHomeAsUpEnabled(true)
        // Change navigation title
        supportActionBar?.title = "Speech to Text"

        copyTextBtn = findViewById(R.id.copyTextBtn)
        openMicBtn = findViewById(R.id.openMicBtn)
        selectImageBtn = findViewById(R.id.selectImageBtn)
        imageResultIV = findViewById(R.id.imageResultIV)
        recognizedTextET = findViewById(R.id.recognizedTextET)
        saveToTxtBtn = findViewById(R.id.saveToTxtBtn)

//        // init arrays of permissions required for Camera/Gallery
//        cameraPermissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
//        storagePermissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)

        openMicBtn.setOnClickListener {
            micSpeak()

        }

        // if the copy icon is clicked, it will select and copy all the recognized text
        copyTextBtn.setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val text = recognizedTextET.text.toString().trim()
            if (text.isNotEmpty())
            {
                recognizedTextET.setSelection(recognizedTextET.text.length) // move cursor to the end
                clipboard.setPrimaryClip(ClipData.newPlainText("text", text))
                showToast("Text copied to clipboard")
            }
        }

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

        selectImageBtn.setOnClickListener {
            showSelectImageDialog()
        }

    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun micSpeak() {
        // intent to show Google Mic Speak
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hi! speak something")

        try
        {
            // if there is no error show Google Mic Speak
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)

            // clear the previous recognized text
            previousRecognizedText = ""
        }
        catch (e: Exception)
        {
            // if there is any error get message error and show toast
            showToast("Error!")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode) {
            REQUEST_CODE_SPEECH_INPUT -> {
                if (resultCode == Activity.RESULT_OK && null != data)
                {
                    // get text from result
                    val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)

                    // get the current text from the editText
                    val currentText = recognizedTextET.text.toString()

                    // concatenate the new text with the previous text
                    val newText = "$currentText ${result?.getOrNull(0)}"

                    // convert the string to editable
                    val newEditable = Editable.Factory.getInstance().newEditable(newText)
                    // pass the editable to the editText
                    recognizedTextET.text = newEditable
                }
            }
        }
    }

    // dialog for selecting CAMERA or GALLERY
    private fun showSelectImageDialog() {
        val popupMenu = PopupMenu(this, selectImageBtn)

        popupMenu.menu.add(Menu.NONE, 1, 2, "CAMERA")
        popupMenu.menu.add(Menu.NONE, 2, 2, "GALLERY")

        popupMenu.show()

        popupMenu.setOnMenuItemClickListener { menuItem ->
            val id = menuItem.itemId

            if(id == 1)
            {
                // camera is clicked, check if camera permissions granted or not
                if(checkCameraPermission())
                {
                    selectImageCamera()
                }
                else
                {
                    requestCameraPermission()
                }
            }
            else if(id == 2)
            {
                // Gallery is clicked, check if camera permissions granted or not
                if(checkStoragePermission())
                {
                    selectImageGallery()
                }
                else
                {
                    requestStoragePermission()
                }
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun selectImageGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryActivityResultLauncher.launch(intent)
    }

    private val galleryActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts
            .StartActivityForResult()){ result ->

        if (result.resultCode == RESULT_OK)
        {
            val data = result.data
            imageUri = data!!.data

            imageResultIV.setImageURI(imageUri)
        }
        else
        {
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
        // get the image data to store in MediaStore
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "Sample Title")
        values.put(MediaStore.Images.Media.DESCRIPTION, "Sample Description")

        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraActivityResultLauncher.launch(intent)

    }

    private val cameraActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.
        StartActivityForResult()){ result ->
        if(result.resultCode == RESULT_OK)
        {
            imageResultIV.setImageURI(imageUri)
        }
        else
        {
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

    private fun checkStoragePermission() : Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkCameraPermission() : Boolean {
        val cameraResult = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val storageResult = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

        return cameraResult && storageResult
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(this, storagePermissions,
            Speech2TextActivity.STORAGE_REQUEST_CODE
        )
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, cameraPermissions,
            Speech2TextActivity.CAMERA_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // handle permissions results
        when(requestCode) {
            CAMERA_REQUEST_CODE -> {
                if(grantResults.isNotEmpty())
                {
                    val cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    val storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED

                    if(cameraAccepted && storageAccepted)
                    {
                        selectImageCamera()
                    }
                    else
                    {
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
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Check for the home button ID
        if (item.itemId == android.R.id.home)
        {
            // Navigate back to Snap2TextFragment
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}