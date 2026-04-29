package com.example.exambro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val PREFS_NAME = "ExambroPrefs"
    private val KEY_PIN = "session_pin"

    private var sessionPin: String? = null
    private var isExamStarted = false
    private var studentName: String? = null
    private var publicKey: String? = null
    private var privateKey: String? = "9999" // Mock Private Key for testing
    private var isBlocked = false

    private lateinit var webView: WebView
    private lateinit var headerBar: LinearLayout
    private lateinit var btnExitAction: Button
    private lateinit var overlaySetup: LinearLayout
    private lateinit var etStudentName: EditText
    private lateinit var etSetupPin: EditText
    private lateinit var btnStart: Button
    private lateinit var overlayBlocker: FrameLayout
    private lateinit var etPrivateKey: EditText
    private lateinit var btnUnblock: Button

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF && isExamStarted) {
                triggerBlocker("Screen Turned Off")
            }
        }
    }

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
        headerBar = findViewById(R.id.headerBar)
        btnExitAction = findViewById(R.id.btnExitAction)
        overlaySetup = findViewById(R.id.overlaySetup)
        etStudentName = findViewById(R.id.etStudentName)
        etSetupPin = findViewById(R.id.etSetupPin)
        btnStart = findViewById(R.id.btnStart)
        overlayBlocker = findViewById(R.id.overlayBlocker)
        etPrivateKey = findViewById(R.id.etPrivateKey)
        btnUnblock = findViewById(R.id.btnUnblock)

        val savedPin = getSavedPin()
        if (savedPin != null) {
            sessionPin = savedPin
            isExamStarted = true
            // Hide setup fields for "Resume" mode
            etStudentName.visibility = View.GONE
            etSetupPin.visibility = View.GONE
            btnStart.visibility = View.GONE
            overlaySetup.visibility = View.VISIBLE
        }

        setupWebView()

        btnStart.setOnClickListener {
            startExam()
        }

        btnExitAction.setOnClickListener {
            showExitDialog()
        }

        btnUnblock.setOnClickListener {
            unblockExam(etPrivateKey.text.toString())
        }

        // Register Screen Off Receiver
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenOffReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
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

    private suspend fun validatePinWithBackend(pin: String): Boolean {
        // TODO: Implement Retrofit/API call here later
        // For now, return pin == sessionPin
        return pin == getSavedPin()
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
        val name = etStudentName.text.toString()
        val pin = etSetupPin.text.toString()
        
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
            return
        }
        if (pin.isEmpty()) {
            Toast.makeText(this, "Please enter a PIN", Toast.LENGTH_SHORT).show()
            return
        }

        studentName = name
        publicKey = pin
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
        headerBar.visibility = View.VISIBLE

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
                lifecycleScope.launch {
                    if (validatePinWithBackend(enteredPin)) {
                        savePin("")
                        isExamStarted = false // Set to false before stopping lock task
                        try {
                            stopLockTask()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        finishAffinity()
                    } else {
                        Toast.makeText(this@MainActivity, "Wrong PIN", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun triggerBlocker(reason: String) {
        if (isBlocked) return
        
        isBlocked = true
        logCheatingToBackend(reason)
        
        runOnUiThread {
            overlayBlocker.visibility = View.VISIBLE
            webView.visibility = View.GONE
            headerBar.visibility = View.GONE
            overlaySetup.visibility = View.GONE
            Log.d("ExamBro", "Exam Blocked: $reason")
        }
    }

    private fun unblockExam(inputKey: String) {
        if (inputKey == privateKey) {
            isBlocked = false
            overlayBlocker.visibility = View.GONE
            etPrivateKey.text.clear()
            
            if (isExamStarted) {
                webView.visibility = View.VISIBLE
                headerBar.visibility = View.VISIBLE
                try {
                    startLockTask()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                overlaySetup.visibility = View.VISIBLE
            }
        } else {
            Toast.makeText(this, "Wrong Private Key", Toast.LENGTH_SHORT).show()
        }
    }

    private fun logCheatingToBackend(reason: String) {
        // Skeleton for future API
        Log.d("ExamBro", "LOG TO BACKEND: Student $studentName cheated. Reason: $reason")
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus && isExamStarted && !isBlocked) {
            // If focus is lost while exam is running and not already blocked,
            // it might be an attempt to use multitasking or system dialogs.
            // We trigger the blocker for safety.
            triggerBlocker("App Lost Focus / System Escape")
        }
    }

    override fun onResume() {
        super.onResume()
        
        if (isBlocked) {
            overlayBlocker.visibility = View.VISIBLE
            webView.visibility = View.GONE
            headerBar.visibility = View.GONE
            overlaySetup.visibility = View.GONE
            return
        }

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
            headerBar.visibility = View.GONE
            
            // Show fields for Resume entry
            etStudentName.visibility = View.GONE // Hide name field on resume if already started
            etSetupPin.visibility = View.VISIBLE
            btnStart.visibility = View.VISIBLE
            etSetupPin.hint = "Public Key to Resume"
            btnStart.text = "Resume Exam"
            
            btnStart.setOnClickListener {
                val enteredPin = etSetupPin.text.toString()
                lifecycleScope.launch {
                    if (validatePinWithBackend(enteredPin)) {
                        overlaySetup.visibility = View.GONE
                        webView.visibility = View.VISIBLE
                        headerBar.visibility = View.VISIBLE
                        etSetupPin.text.clear()
                        
                        // Reset to initial state if needed, but startExam will handle next start
                        // Ensure URL is loaded if first time resuming
                        if (webView.url == null) {
                            webView.loadUrl("https://elsph.permataharapanku.sch.id/")
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Wrong PIN", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else if (isExamStarted) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}