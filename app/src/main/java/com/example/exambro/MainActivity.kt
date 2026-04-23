package com.example.exambro

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

        setupWebView()

        btnStart.setOnClickListener {
            startExam()
        }

        btnExit.setOnClickListener {
            showExitDialog()
        }
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
        isExamStarted = true

        // Apply FLAG_SECURE
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

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
        if (isExamStarted) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}