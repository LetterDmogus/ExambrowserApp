# Setup Layout and Strings Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Update the app's strings and main layout to include a WebView, an exit button, and a setup overlay.

**Architecture:** Use `FrameLayout` as the root to overlay the setup screen and exit button on top of the `WebView`.

**Tech Stack:** Android XML (Layouts and Resources).

---

### Task 1: Update Strings

**Files:**
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Replace content of strings.xml**

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

### Task 2: Update Layout

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml`

- [ ] **Step 1: Replace content of activity_main.xml**

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

### Task 3: Commit Changes

- [ ] **Step 1: Stage and commit**

Run: `git add app/src/main/res/values/strings.xml app/src/main/res/layout/activity_main.xml`
Run: `git commit -m "feat: setup UI layout for Exambro"`
