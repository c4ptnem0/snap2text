package com.example.snap2text

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.example.snap2text.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class Login : AppCompatActivity() {

    private var passwordShowing = false;
    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth

    companion object {
        private const val RC_SIGN_IN = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        val passwordIcon: ImageView = findViewById<ImageView>(R.id.passwordIcon)
        firebaseAuth = FirebaseAuth.getInstance()

        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)

        binding.forgotPasswordBtn.setOnClickListener {
            val intent = Intent(this, ForgotPassword::class.java)
            startActivity(intent)
        }

        binding.signUpbtn.setOnClickListener {
            val intent = Intent(this, Register::class.java)
            startActivity(intent)
        }

        binding.signInBtn.setOnClickListener {
            val email = binding.emailET.text.toString()
            val password = binding.passwordET.text.toString()

            googleSignInClient.signOut()

            // check if the input fields are empty
            if(email.isNotEmpty() && password.isNotEmpty())
            {
                // Show the progress dialog
                val progressDialog = showProgressDialog(this@Login)

                firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
                    if(it.isSuccessful)
                    {
                        val user = firebaseAuth.currentUser
                        // User is verified, proceed to main activity
                        if(user != null && user.isEmailVerified)
                        {
                            // hide the progress bar
                            progressDialog.dismiss()

                            Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                        }
                        else
                        {
                            // User is not verified, show error message
                            progressDialog.dismiss()
                            Toast.makeText(this, "Please verify your email address first!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    else
                    {
                        // hide the progress bar
                        progressDialog.dismiss()

                        Toast.makeText(this, "Email and Password Incorrect!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            else
            {
                Toast.makeText(this, "You must complete all the fields!", Toast.LENGTH_SHORT).show()
            }
        }

        // sign in with Google Account
        binding.signInWithGoogle.setOnClickListener {
            // signout the previous signed in account to sign in again
            googleSignInClient.signOut().addOnCompleteListener(this) {
                // sign in with google
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_SIGN_IN)
            }
        }

        // function for showing or hiding the password
        passwordIcon.setOnClickListener {
            val password = findViewById<EditText>(R.id.passwordET)

            if(passwordShowing)
            {
                passwordShowing = false;

                password.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                passwordIcon.setImageResource(R.drawable.password_show)
            }
            else
            {
                passwordShowing = true;

                password.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
                passwordIcon.setImageResource(R.drawable.password_hide)
            }
            // move the cursor to the last text
            password.setSelection(password.length())
        }
    }

    fun showProgressDialog(context: Context): ProgressDialog {
        val progressDialog = ProgressDialog(context)
        progressDialog.setTitle("Logging in")
        progressDialog.setMessage("Please wait while we check your account")
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.show()
        return progressDialog
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == RC_SIGN_IN)
        {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                return
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        // Show the progress dialog
        val progressDialog = showProgressDialog(this)

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    // Dismiss the progress dialog
                    progressDialog.dismiss()

                    Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }
                else
                {
                    // If sign in fails, display a message to the user.
                    // Dismiss the progress dialog
                    progressDialog.dismiss()
                    Toast.makeText(this, "Google sign in failed", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
