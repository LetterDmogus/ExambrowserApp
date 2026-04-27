package com.example.exambro

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private val PREFS_NAME = "ExambroPrefs"
    private val KEY_PIN = "session_pin"

    private var sessionPin: String? = null
    private var isExamStarted = false

    private lateinit var webView: WebView
    private lateinit var btnExit: ImageButton
    private lateinit var overlaySetup: LinearLayout
    private lateinit var etSetupPin: EditText
    private lateinit var btnStart: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize views
        webView = findViewById(R.id.webView)
        btnExit = findViewById(R.id.btnExit)
        overlaySetup = findViewById(R.id.overlaySetup)
        etSetupPin = findViewById(R.id.etSetupPin)
        btnStart = findViewById(R.id.btnStart)

        val savedPin = getSavedPin()
        if (savedPin != null) {
            sessionPin = savedPin
            isExamStarted = true
            // Hide setup fields for "Resume" mode
            etSetupPin.visibility = View.GONE
            btnStart.visibility = View.GONE
            overlaySetup.visibility = View.VISIBLE
            // Note: overlaySetup should contain a mechanism for "Resume PIN" entry, 
            // but the prompt says to KEEP the overlay visible for it.
        }

        setupWebView()

        btnStart.setOnClickListener {
            startExam()
        }

        btnExit.setOnClickListener {
            showExitDialog()
        }
    }

    private fun savePin(pin: String) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PIN, pin).apply()
    }

    private fun getSavedPin(): String? {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val pin = prefs.getString(KEY_PIN, "")
        return if (pin.isNullOrEmpty()) null else pin
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
        }
        webView.webViewClient = WebViewClient()
    }

    private fun startExam() {
        val pin = etSetupPin.text.toString()
        if (pin.isEmpty()) {
            Toast.makeText(this, "Please enter a PIN", Toast.LENGTH_SHORT).show()
            return
        }

        sessionPin = pin
        savePin(pin)
        isExamStarted = true

        // Apply FLAG_SECURE
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        // Native Kiosk Mode
        try {
            startLockTask()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        overlaySetup.visibility = View.GONE
        btnExit.visibility = View.VISIBLE

        webView.loadUrl("https://elsph.permataharapanku.sch.id/")
    }

    private fun showExitDialog() {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD

        AlertDialog.Builder(this)
            .setTitle("Exit Exam")
            .setMessage("Enter PIN to exit:")
            .setView(input)
            .setPositiveButton("Exit") { _, _ ->
                val enteredPin = input.text.toString()
                if (enteredPin == sessionPin) {
                    savePin("")
                    try {
                        stopLockTask()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    finishAffinity()
                } else {
                    Toast.makeText(this, "Wrong PIN", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        val savedPin = getSavedPin()
        if (savedPin != null) {
            isExamStarted = true
            sessionPin = savedPin
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
            
            // Native Kiosk Mode
            try {
                startLockTask()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Forcefully show overlay and hide WebView until unlocked
            overlaySetup.visibility = View.VISIBLE
            webView.visibility = View.GONE
            btnExit.visibility = View.GONE
            
            // Show fields for Resume entry
            etSetupPin.visibility = View.VISIBLE
            btnStart.visibility = View.VISIBLE
            etSetupPin.hint = "Enter PIN to Resume"
            btnStart.text = "Resume Exam"
            
            btnStart.setOnClickListener {
                val pin = etSetupPin.text.toString()
                if (pin == sessionPin) {
                    overlaySetup.visibility = View.GONE
                    webView.visibility = View.VISIBLE
                    btnExit.visibility = View.VISIBLE
                    etSetupPin.text.clear()
                    
                    // Reset to initial state if needed, but startExam will handle next start
                    // Ensure URL is loaded if first time resuming
                    if (webView.url == null) {
                        webView.loadUrl("https://elsph.permataharapanku.sch.id/")
                    }
                } else {
                    Toast.makeText(this, "Wrong PIN", Toast.LENGTH_SHORT).show()
                }
            }
        } else if (isExamStarted) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}