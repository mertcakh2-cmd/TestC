package com.ubuntu.terminal

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*

class WelcomeScreen : Activity() {

    private lateinit var preferences: SharedPreferences

    private var darkMode = true

    private var backgroundColor = Color.rgb(15, 17, 22)
    private var surfaceColor = Color.rgb(25, 28, 35)
    private var textColor = Color.WHITE
    private var secondaryColor = Color.rgb(155, 160, 175)
    private val accentColor = Color.rgb(233, 84, 32)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferences = getSharedPreferences("ubuntu_settings", MODE_PRIVATE)

        applySavedTheme()

        if (intent.getBooleanExtra("open_settings", false)) {
            showSettings()
        } else {
            showWelcome()
        }
    }

    // ------------------------------------------------
    // THEME
    // ------------------------------------------------

    private fun applySavedTheme() {
        val theme = preferences.getString("theme", "dark") ?: "dark"

        darkMode = when (theme) {
            "light" -> false
            "system" -> {
                val nightMode = resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK

                nightMode == Configuration.UI_MODE_NIGHT_YES
            }
            else -> true
        }

        backgroundColor = if (darkMode) {
            Color.rgb(15, 17, 22)
        } else {
            Color.rgb(245, 246, 249)
        }

        surfaceColor = if (darkMode) {
            Color.rgb(25, 28, 35)
        } else {
            Color.WHITE
        }

        textColor = if (darkMode) Color.WHITE else Color.rgb(25, 28, 35)

        secondaryColor = if (darkMode) {
            Color.rgb(155, 160, 175)
        } else {
            Color.rgb(105, 110, 125)
        }

        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor

        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            if (darkMode) 0
            else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
    }

    private fun saveTheme(theme: String) {
        preferences.edit()
            .putString("theme", theme)
            .apply()

        applySavedTheme()
        showSettings()
    }

    // ------------------------------------------------
    // UI HELPERS
    // ------------------------------------------------

    private fun createLayout(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
            setPadding(dp(24), dp(20), dp(24), dp(20))
        }
    }

    private fun createText(
        text: String,
        size: Float,
        color: Int = textColor,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            if (bold) {
                setTypeface(null, Typeface.BOLD)
            }
        }
    }

    private fun createButton(
        text: String,
        primary: Boolean = false
    ): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 15f
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            setTextColor(if (primary) Color.WHITE else textColor)

            background = roundedBackground(
                if (primary) accentColor else surfaceColor,
                14
            )

            setPadding(dp(18), dp(16), dp(18), dp(16))

            isClickable = true
            isFocusable = true
        }
    }

    private fun roundedBackground(
        color: Int,
        radius: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun addSpace(
        layout: LinearLayout,
        height: Int
    ) {
        layout.addView(
            View(this),
            LinearLayout.LayoutParams(
                1,
                dp(height)
            )
        )
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun addButton(
        layout: LinearLayout,
        button: TextView,
        action: () -> Unit
    ) {
        layout.addView(
            button,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(54)
            )
        )

        button.setOnClickListener {
            action()
        }
    }

    // ------------------------------------------------
    // WELCOME SCREEN
    // ------------------------------------------------

    private fun showWelcome() {
        val root = createLayout()

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(root)
        }

        val topBar = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
        }

        val logo = TextView(this).apply {
            text = "U"
            textSize = 27f
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = roundedBackground(accentColor, 16)
        }

        topBar.addView(
            logo,
            LinearLayout.LayoutParams(dp(48), dp(48))
        )

        val brand = createText("Ubuntu Terminal", 19f, textColor, true)
        brand.setPadding(dp(14), 0, 0, 0)

        topBar.addView(
            brand,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        val settingsButton = createText("SETTINGS", 12f, secondaryColor, true)
        settingsButton.setPadding(dp(12), dp(12), dp(4), dp(12))
        settingsButton.setOnClickListener {
            showSettings()
        }

        topBar.addView(settingsButton)

        root.addView(topBar)

        addSpace(root, 75)

        val label = createText(
            "LINUX ENVIRONMENT",
            12f,
            accentColor,
            true
        )

        root.addView(label)

        addSpace(root, 15)

        val title = createText(
            "Your terminal.\nYour environment.",
            35f,
            textColor,
            true
        )

        title.setLineSpacing(dp(4).toFloat(), 1f)
        root.addView(title)

        addSpace(root, 18)

        val description = createText(
            "A Linux terminal experience powered by Ubuntu and PRoot.",
            16f,
            secondaryColor
        )

        description.setLineSpacing(dp(5).toFloat(), 1f)
        root.addView(description)

        addSpace(root, 35)

        val preview = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBackground(surfaceColor, 18)
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }

        val previewHeader = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
        }

        val previewTitle = createText(
            "TERMINAL PREVIEW",
            11f,
            secondaryColor,
            true
        )

        previewHeader.addView(
            previewTitle,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        val status = createText("● READY", 10f, Color.rgb(85, 200, 130), true)
        previewHeader.addView(status)

        preview.addView(previewHeader)

        addSpace(preview, 20)

        val terminalText = TextView(this).apply {
            text = buildString {
                append("root@ubuntu:~# ")
                append("apt update\n")
                append("\n")
                append("Reading package lists...\n")
                append("Building dependency tree...\n")
                append("\n")
                append("root@ubuntu:~# ")
            }

            textSize = 13f
            typeface = Typeface.MONOSPACE
            setTextColor(Color.rgb(120, 210, 150))
            setLineSpacing(dp(5).toFloat(), 1f)
        }

        preview.addView(terminalText)
        root.addView(preview)

        addSpace(root, 35)

        val startButton = createButton("Open Terminal  →", true)

        addButton(root, startButton) {
            openTerminal()
        }

        addSpace(root, 12)

        val setupButton = createButton("Ubuntu Setup", false)

        addButton(root, setupButton) {
            showSetupInfo()
        }

        addSpace(root, 25)

        val footer = createText(
            "UBUNTU • PROOT • ANDROID",
            10f,
            secondaryColor,
            true
        )

        footer.gravity = Gravity.CENTER
        root.addView(footer)

        setContentView(scroll)
    }

    // ------------------------------------------------
    // SETTINGS SCREEN
    // ------------------------------------------------

    private fun showSettings() {
        val root = createLayout()

        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
        }

        val back = createText("‹", 34f, textColor, false)
        back.gravity = Gravity.CENTER
        back.setOnClickListener {
            showWelcome()
        }

        header.addView(
            back,
            LinearLayout.LayoutParams(dp(45), dp(48))
        )

        val heading = createText("Settings", 23f, textColor, true)

        header.addView(heading)

        root.addView(header)

        addSpace(root, 35)

        root.addView(
            createText("APPEARANCE", 11f, accentColor, true)
        )

        addSpace(root, 12)

        addSettingOption(
            root,
            "Dark theme",
            "A dark Ubuntu-inspired interface",
            "dark"
        )

        addSpace(root, 10)

        addSettingOption(
            root,
            "Light theme",
            "A clean light interface",
            "light"
        )

        addSpace(root, 10)

        addSettingOption(
            root,
            "System default",
            "Follow your Android device theme",
            "system"
        )

        addSpace(root, 35)

        root.addView(
            createText("ABOUT", 11f, accentColor, true)
        )

        addSpace(root, 12)

        val about = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBackground(surfaceColor, 16)
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }

        about.addView(
            createText("Ubuntu Terminal", 17f, textColor, true)
        )

        addSpace(about, 8)

        about.addView(
            createText(
                "Android Linux environment using Ubuntu and PRoot.",
                13f,
                secondaryColor
            )
        )

        addSpace(about, 12)

        about.addView(
            createText("Version 1.0.0", 11f, secondaryColor)
        )

        root.addView(about)

        setContentView(root)
    }

    private fun addSettingOption(
        root: LinearLayout,
        title: String,
        description: String,
        value: String
    ) {
        val selected = preferences.getString("theme", "dark") == value

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBackground(
                if (selected) {
                    if (darkMode) Color.rgb(55, 37, 34)
                    else Color.rgb(255, 235, 228)
                } else {
                    surfaceColor
                },
                15
            )

            setPadding(dp(18), dp(15), dp(18), dp(15))
            isClickable = true
            isFocusable = true
        }

        val titleRow = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
        }

        val titleView = createText(title, 15f, textColor, true)

        titleRow.addView(
            titleView,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        if (selected) {
            titleRow.addView(
                createText("✓", 20f, accentColor, true)
            )
        }

        card.addView(titleRow)

        addSpace(card, 5)

        card.addView(
            createText(description, 12f, secondaryColor)
        )

        card.setOnClickListener {
            saveTheme(value)
        }

        root.addView(
            card,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                -2
            )
        )
    }

    // ------------------------------------------------
    // SETUP INFORMATION
    // ------------------------------------------------

    private fun showSetupInfo() {
        val builder = android.app.AlertDialog.Builder(this)

        builder.setTitle("Ubuntu Setup")

        builder.setMessage(
            "Before opening the terminal, you need:\n\n" +
                "1. A compatible PRoot binary.\n" +
                "2. An ARM64 Ubuntu root filesystem.\n" +
                "3. The root filesystem extracted correctly.\n\n" +
                "Once installed, you can open the terminal."
        )

        builder.setPositiveButton("OK", null)

        builder.show()
    }

    // ------------------------------------------------
    // NAVIGATION
    // ------------------------------------------------

    private fun openTerminal() {
        startActivity(
            Intent(this, MakeTerminal::class.java)
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (preferences.getString("theme", "dark") == "system") {
            applySavedTheme()

            if (intent.getBooleanExtra("open_settings", false)) {
                showSettings()
            } else {
                showWelcome()
            }
        }
    }
}
