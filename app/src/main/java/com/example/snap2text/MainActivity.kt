package com.example.snap2text

import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.snap2text.databinding.ActivityMainBinding
import com.example.snap2text.ui.settings.SettingsActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.squareup.picasso.Transformation
import kotlin.math.min

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        // Get the Firebase Authentication instance
        val firebaseAuth = FirebaseAuth.getInstance()
        // Check if the user is logged in and verified
        val currentUser = firebaseAuth.currentUser
        // Get a reference to the Firebase Realtime Database
        val database = FirebaseDatabase.getInstance()
        // Get the current user's ID
        val uid = currentUser?.uid


        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        val logoutItem = binding.navView.menu.findItem(R.id.nav_logout)
        val settingsBtn = binding.navView.menu.findItem(R.id.nav_settings)

        // if the user is not logged in and not verified, redirect to Login Activity
        // else, proceed to the dashboard
        if (currentUser == null || !currentUser.isEmailVerified) {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
            return
        }
        else {
            val userRef = database.reference.child("Users").child(uid!!)

            // update the profile, username, and email in navigation header
            userRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val username = snapshot.child("username").value.toString()
                    val email = snapshot.child("email").value.toString()
                    val account = GoogleSignIn.getLastSignedInAccount(applicationContext)

                    val headerView = navView.getHeaderView(0)
                    val userNameTV = headerView.findViewById<TextView>(R.id.userNameTV)
                    val userEmailTV = headerView.findViewById<TextView>(R.id.userEmailTV)
                    val userImage = headerView.findViewById<ImageView>(R.id.imageView)

                    var personEmail: String? = null
                    var personDisplayName: String? = null

                    if (account != null)
                    {
                        personEmail = account.email
                        personDisplayName = account.displayName
                        val personPhotoUrl = account?.photoUrl?.toString()

                        if (!personPhotoUrl.isNullOrEmpty())
                        {
                            Picasso.get()
                                .load(personPhotoUrl)
                                .transform(transformation)
                                .error(R.mipmap.ic_launcher_round)
                                .into(userImage, object : Callback {
                                    override fun onSuccess() {
                                        Log.d(TAG, "Image loaded successfully")
                                    }

                                    override fun onError(e: Exception?) {
                                        Log.e(TAG, "Failed to load image", e)
                                    }
                                })
                        }
                        else
                        {
                            // Load a placeholder image if the user has no profile photo
                            Picasso.get()
                                .load(R.mipmap.ic_launcher_round)
                                .into(userImage)
                        }
                    }

                    // Update the TextViews with the user's display name and email
                    userNameTV.text = personDisplayName ?: username
                    userEmailTV.text = personEmail ?: email

                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Failed to read user data", error.toException())
                }
            })
        }

        settingsBtn.setOnMenuItemClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            true
        }

        // logout function
        logoutItem.setOnMenuItemClickListener {
            AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout") { _, _ ->
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(this, Login::class.java)
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
            true
        }

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_snap, R.id.nav_settings, R.id.nav_logout
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    // function for making the fetched profile to be circle
    val transformation = object : Transformation {
        override fun key(): String = "circle"

        override fun transform(source: Bitmap): Bitmap {
            val size = min(source.width, source.height)
            val x = (source.width - size) / 2
            val y = (source.height - size) / 2
            val squaredBitmap = Bitmap.createBitmap(source, x, y, size, size)
            if (squaredBitmap != source) {
                source.recycle()
            }
            val bitmap = Bitmap.createBitmap(size, size, source.config)
            val canvas = Canvas(bitmap)
            val paint = Paint()
            val shader = BitmapShader(squaredBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            paint.shader = shader
            paint.isAntiAlias = true
            val r = size / 2f
            canvas.drawCircle(r, r, r, paint)
            squaredBitmap.recycle()
            return bitmap
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                // Handle settings item click here
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}