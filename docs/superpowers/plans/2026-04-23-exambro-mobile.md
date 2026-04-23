# Exambro Mobile Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a secure exam browser (Exambro) with session-based PIN lock, native kiosk mode (startLockTask), screen recording/screenshot blocking (FLAG_SECURE), and a Moodle-capable WebView.

**Architecture:** Use `FrameLayout` for layering the UI. Layer 0: `WebView` for the exam content. Layer 1: `ImageButton` for the exit trigger. Layer 2: `LinearLayout` overlay for session setup. Use Android's `startLockTask()` for kiosk mode.

**Tech Stack:** Kotlin, Android SDK (WebView, LockTask API, WindowManager).

---

### Task 1: Setup Layout and Strings

**Files:**
- Create: `app/src/main/res/values/strings.xml` (Update existing)
- Modify: `app/src/main/res/layout/activity_main.xml`

- [ ] **Step 1: Update strings.xml**

```xml
<resources>
    <string name="app_name">ExamBro</string>
    <string name="setup_title">Pengaturan Sesi Ujian</string>
    <string name="input_pin_hint">Masukkan PIN Pengawas</string>
    <string name="start_exam">Mulai Ujian</string>
    <string name="exit_prompt_title">Keluar Ujian</string>
    <string name="exit_prompt_msg">Masukkan PIN Pengawas untuk keluar</string>
    <string name="confirm">Konfirmasi</string>
    <string name="cancel">Batal</string>
    <string name="wrong_pin">PIN Salah!</string>
</resources>
```

- [ ] **Step 2: Update activity_main.xml**
Replace ConstraintLayout with FrameLayout for layering.

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/main"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".MainActivity">

    <!-- Layer 0: WebView -->
    <WebView
        android:id="@+id/webView"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

    <!-- Layer 1: Floating Exit Button (Transparent Icon) -->
    <ImageButton
        android:id="@+id/btnExit"
        android:layout_width="48dp"
        android:layout_height="48dp"
        android:layout_gravity="top|end"
        android:layout_margin="8dp"
        android:background="?attr/selectableItemBackgroundBorderless"
        android:src="@android:drawable/ic_menu_close_clear_cancel"
        android:visibility="gone" />

    <!-- Layer 2: Setup Overlay -->
    <LinearLayout
        android:id="@+id/overlaySetup"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="#FFFFFF"
        android:gravity="center"
        android:orientation="vertical"
        android:padding="32dp">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/setup_title"
            android:textSize="24sp"
            android:textStyle="bold" />

        <EditText
            android:id="@+id/etSetupPin"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            android:hint="@string/input_pin_hint"
            android:inputType="numberPassword" />

        <Button
            android:id="@+id/btnStart"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            android:text="@string/start_exam" />
    </LinearLayout>

</FrameLayout>
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/layout/activity_main.xml app/src/main/res/values/strings.xml
git commit -m "feat: setup UI layout for Exambro"
```

---

### Task 2: Implement Core Logic and Kiosk Mode

**Files:**
- Modify: `app/src/main/java/com/example/exambro/MainActivity.kt`

- [ ] **Step 1: Declare variables and Setup WebView**
Configure WebView settings for Moodle (JavaScript enabled, DOM storage, etc.).

```kotlin
private var sessionPin: String? = null
private var isExamStarted = false

// Inside onCreate:
val webView = findViewById<WebView>(R.id.webView)
webView.settings.apply {
    javaScriptEnabled = true
    domStorageEnabled = true
    loadWithOverviewMode = true
    useWideViewPort = true
}
webView.webViewClient = WebViewClient()
```

- [ ] **Step 2: Implement startExam logic**
When `btnStart` is clicked: save PIN, start LockTask, block screenshots, hide overlay.

```kotlin
val btnStart = findViewById<Button>(R.id.btnStart)
val etSetupPin = findViewById<EditText>(R.id.etSetupPin)
val overlaySetup = findViewById<View>(R.id.overlaySetup)
val btnExit = findViewById<ImageButton>(R.id.btnExit)

btnStart.setOnClickListener {
    val pin = etSetupPin.text.toString()
    if (pin.isNotEmpty()) {
        sessionPin = pin
        isExamStarted = true
        
        // Security: Block screenshots
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        
        // Kiosk Mode: Lock task
        startLockTask()
        
        // UI Updates
        overlaySetup.visibility = View.GONE
        btnExit.visibility = View.VISIBLE
        
        // Load URL
        webView.loadUrl("https://elsph.permataharapanku.sch.id/")
    }
}
```

- [ ] **Step 3: Implement Exit logic with PIN validation**
When `btnExit` is clicked, show AlertDialog to input PIN.

```kotlin
btnExit.setOnClickListener {
    val builder = AlertDialog.Builder(this)
    builder.setTitle(getString(R.string.exit_prompt_title))
    val input = EditText(this)
    input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
    builder.setView(input)
    
    builder.setPositiveButton(getString(R.string.confirm)) { _, _ ->
        if (input.text.toString() == sessionPin) {
            stopLockTask()
            finishAffinity()
        } else {
            Toast.makeText(this, getString(R.string.wrong_pin), Toast.LENGTH_SHORT).show()
        }
    }
    builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.cancel() }
    builder.show()
}
```

- [ ] **Step 4: Handle Lifecycle (onResume/onPause)**
Ensure app stays locked if it somehow loses focus.

```kotlin
override fun onResume() {
    super.onResume()
    if (isExamStarted) {
        startLockTask()
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/exambro/MainActivity.kt
git commit -m "feat: implement kiosk mode, security flags and exit logic"
```

---

### Task 3: Final Polishing and Permissions

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Add Internet Permission**

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

- [ ] **Step 2: Commit and Run**

```bash
git add app/src/main/AndroidManifest.xml
git commit -m "chore: add internet permission"
```
