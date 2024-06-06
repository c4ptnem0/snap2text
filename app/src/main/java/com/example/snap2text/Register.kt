package com.example.snap2text

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.example.snap2text.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class Register : AppCompatActivity() {

    private var passwordShowing = false;
    private var conpasswordShowing = false;
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        firebaseAuth = FirebaseAuth.getInstance()

        val pass = findViewById<EditText>(R.id.passwordET)
        val conpass = findViewById<EditText>(R.id.conpasswordET)
        val passwordIcon = findViewById<ImageView>(R.id.passwordIcon)
        val conpasswordIcon = findViewById<ImageView>(R.id.conpasswordIcon)

        binding.signInBtn.setOnClickListener{
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }

        binding.signUpBtn.setOnClickListener {
            val username = binding.usernameET.text.toString()
            val email = binding.emailET.text.toString()
            val password = binding.passwordET.text.toString()
            val conpassword = binding.conpasswordET.text.toString()

            // check if the input fields are empty
            if(username.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && conpassword.isNotEmpty())
            {
                // check if email is already registered
                firebaseAuth.fetchSignInMethodsForEmail(email).addOnCompleteListener { task ->
                    if (task.isSuccessful)
                    {
                        val result = task.result?.signInMethods ?: emptyList<String>()
                        if (result.isNotEmpty())
                        {
                            Toast.makeText(this, "This email is already registered!", Toast.LENGTH_SHORT).show()
                        }
                        else
                        {
                            if(password.equals(conpassword))
                            {
                                // Show the progress bar
                                val progressDialog = ProgressDialog(this)
                                progressDialog.setTitle("Registering User")
                                progressDialog.setMessage("Please wait while we register your account")
                                progressDialog.setCanceledOnTouchOutside(false)
                                progressDialog.show()

                                firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener{
                                    if(task.isSuccessful)
                                    {
                                        val user = firebaseAuth.currentUser
                                        val uid = user?.uid

                                        // create a new entry in the "Users" node with uid as key
                                        val database = FirebaseDatabase.getInstance()
                                        val usersRef = database.getReference("Users")
                                        val userValues = mapOf("email" to email,
                                            "password" to password,
                                            "username" to username,)
                                        uid?.let { usersRef.child(it).setValue(userValues) }

                                        // send email verification
                                        user?.sendEmailVerification()?.addOnCompleteListener { emailTask ->
                                            if(emailTask.isSuccessful)
                                            {
                                                Toast.makeText(this, "Signup Successful! Verification email sent to $email", Toast.LENGTH_SHORT).show()
                                            }
                                            else
                                            {
                                                Toast.makeText(this, "Signup Failed!", Toast.LENGTH_SHORT).show()
                                            }
                                        }

                                        // hide the progress bar
                                        progressDialog.dismiss()

                                        // redirect to login activity, after signing up
                                        val intent = Intent(this, EmailVerification::class.java)
                                        intent.putExtra("email", email)
                                        startActivity(intent)
                                    }
                                    else
                                    {
                                        Toast.makeText(this, "Error: Registering account failed!", Toast.LENGTH_SHORT).show()
                                        // hide the progress bar
                                        progressDialog.dismiss()
                                    }
                                }
                            }
                            else
                            {
                                Toast.makeText(this, "Password and Confirm password did not match!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    else
                    {
                        Toast.makeText(this, "Error: Something went wrong!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            else
            {
                Toast.makeText(this, "You must complete all the fields!", Toast.LENGTH_SHORT).show()
            }
        }


        // function for showing or hiding the password
        passwordIcon.setOnClickListener {
            if(passwordShowing)
            {
                passwordShowing = false

                pass.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                passwordIcon.setImageResource(R.drawable.password_show)
            }
            else
            {
                passwordShowing = true

                pass.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
                passwordIcon.setImageResource(R.drawable.password_hide)
            }
            // move the cursor to the last text
            pass.setSelection(pass.length())
        }

        conpasswordIcon.setOnClickListener {
            if(conpasswordShowing)
            {
                conpasswordShowing = false

                conpass.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                conpasswordIcon.setImageResource(R.drawable.password_show)
            }
            else
            {
                conpasswordShowing = true

                conpass.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
                conpasswordIcon.setImageResource(R.drawable.password_hide)
            }
            // move the cursor to the last text
            conpass.setSelection(pass.length())
        }
    }
}