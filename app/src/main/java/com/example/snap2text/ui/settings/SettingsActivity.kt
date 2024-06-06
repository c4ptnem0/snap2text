package com.example.snap2text.ui.settings

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import com.example.snap2text.R

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Enable the back button

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }

    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed() // Handle back button click
        return true
    }

    class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            // Inflate the preferences from the XML resource file
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            val aboutPreference = findPreference<Preference>("about")
            val documentationPreference = findPreference<Preference>("documentation")

            aboutPreference?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                val url = "https://c4ptnem0.github.io/" // Replace with your desired URL
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(browserIntent)
                true
            }

            documentationPreference?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                val url = "https://c4ptnem0.github.io/" // Replace with your desired URL
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(browserIntent)
                true
            }

            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
            val darkModePreference = findPreference<SwitchPreferenceCompat>("dark_mode")

            // Set the initial state of the dark mode switch based on the shared preferences
            darkModePreference?.isChecked = sharedPreferences.getBoolean("dark_mode", false)
        }

        override fun onResume() {
            super.onResume()
            // Register the shared preference change listener
            preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            super.onPause()
            // Unregister the shared preference change listener
            preferenceScreen.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
            if (key == "dark_mode") {
                // Retrieve the updated value of the dark mode preference
                val isDarkModeEnabled = sharedPreferences.getBoolean(key, false)

                // Apply the selected night mode based on the preference value
                AppCompatDelegate.setDefaultNightMode(
                    if (isDarkModeEnabled) AppCompatDelegate.MODE_NIGHT_YES
                    else AppCompatDelegate.MODE_NIGHT_NO
                )
            }
        }
    }
}
