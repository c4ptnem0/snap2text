package com.example.snap2text

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlin.math.sign

class EmailVerification : AppCompatActivity() {

    private lateinit var signIn: TextView
    private lateinit var otpEmail: TextView
    private lateinit var openGmailBtn: Button

    // sender of code after 60 secs
    private var resendEnabled = true;

    // resend code time
    private var resendTime = 60

    private var selectedPosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_verification)
        supportActionBar?.hide()

        signIn = findViewById(R.id.signInbtn)
        otpEmail = findViewById(R.id.otpEmail)
        openGmailBtn = findViewById(R.id.openGmailBtn)


        // getting email from Register activity through intent
        val getEmail = intent.getStringExtra("email")

        // setting email to TextView
        otpEmail.text = getEmail

        // if gmail app is available, it will open the app or browser redirecting to gmail.com
        openGmailBtn.setOnClickListener {
            val intent1 = packageManager.getLaunchIntentForPackage("com.google.android.gm")
            if (intent1 != null) {
                startActivity(intent1)
            } else {
                Toast.makeText(this, "Gmail app is not installed", Toast.LENGTH_SHORT).show()
                val intent2 = Intent(Intent.ACTION_VIEW, Uri.parse("https://mail.google.com"))
                startActivity(intent2)
            }
        }

        signIn.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }
    }
}
