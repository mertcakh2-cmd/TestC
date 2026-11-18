package com.ubuntu.terminal

import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import android.animation.*
import android.content.res.Configuration

class WelcomeScreen : Activity() {

    private val prefs by lazy {
        getSharedPreferences("ubuntu_settings", Context.MODE_PRIVATE)
    }

    private val orange = Color.rgb(233, 84, 32)

    private var isDark = true
    private lateinit var root: LinearLayout

    private fun bg(color: Int, radius: Float = 18f): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isDark = prefs.getBoolean(
            "dark_mode",
            true
        )

        applyTheme()
        showWelcome()
    }

    private fun applyTheme() {
        val dark = prefs.getBoolean("dark_mode", true)

        window.statusBarColor =
            if (dark) Color.rgb(18, 18, 22)
            else Color.WHITE

        window.navigationBarColor =
            if (dark) Color.rgb(18, 18, 22)
            else Color.WHITE

        window.decorView.systemUiVisibility =
            if (dark) 0
            else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                 View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
    }

    private fun background(): Int =
        if (isDark) Color.rgb(18, 18, 22)
        else Color.rgb(247, 247, 249)

    private fun surface(): Int =
        if (isDark) Color.rgb(30, 30, 36)
        else Color.WHITE

    private fun textColor(): Int =
        if (isDark) Color.WHITE
        else Color.rgb(30, 30, 35)

    private fun secondaryColor(): Int =
        if (isDark) Color.rgb(165, 165, 175)
        else Color.rgb(110, 110, 120)

    private fun createRoot(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 20, 24, 24)
            setBackgroundColor(background())
        }
    }

    private fun text(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            typeface = if (bold) {
                Typeface.create("sans-serif", Typeface.BOLD)
            } else {
                Typeface.create("sans-serif", Typeface.NORMAL)
            }
        }
    }

    private fun button(
        label: String,
        color: Int,
        textColor: Int = Color.WHITE
    ): TextView {
        return TextView(this).apply {
            text = label
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(textColor)
            typeface = Typeface.DEFAULT_BOLD
            background = bg(color, 16f)
            setPadding(20, 17, 20, 17)
            isClickable = true
            isFocusable = true
            elevation = 2f
        }
    }

    private fun showWelcome() {

        applyTheme()

        root = createRoot()

        val settings = text(
            "⚙ Settings",
            14f,
            secondaryColor(),
            true
        ).apply {
            gravity = Gravity.END
            setPadding(12, 12, 12, 12)
            setOnClickListener {
                showSettings()
            }
        }

        root.addView(
            settings,
            LinearLayout.LayoutParams(
                -1,
                -2
            )
        )

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        val logo = TextView(this).apply {
            text = "◉"
            textSize = 94f
            gravity = Gravity.CENTER
            setTextColor(orange)
            typeface = Typeface.DEFAULT_BOLD
        }

        content.addView(
            logo,
            LinearLayout.LayoutParams(-1, 150)
        )

        val title = text(
            "Ubuntu Terminal",
            30f,
            textColor(),
            true
        )

        title.gravity = Gravity.CENTER

        content.addView(title)

        val subtitle = text(
            "Your Linux environment.\nRight in your hands.",
            15f,
            secondaryColor()
        )

        subtitle.gravity = Gravity.CENTER
        subtitle.setLineSpacing(6f, 1f)

        val subtitleParams = LinearLayout.LayoutParams(
            -1,
            -2
        )

        subtitleParams.topMargin = 14

        content.addView(subtitle, subtitleParams)

        val terminalPreview = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 20, 18, 20)
            background = bg(surface(), 18f)
        }

        val terminalTitle = text(
            "TERMINAL PREVIEW",
            11f,
            secondaryColor(),
            true
        )

        terminalPreview.addView(terminalTitle)

        val terminalText = text(
            "root@ubuntu:~# uname -a\n" +
            "Linux ubuntu 6.0.0 aarch64\n" +
            "root@ubuntu:~# _",
            13f,
            if (isDark) Color.rgb(140, 230, 150)
            else Color.rgb(30, 140, 65)
        )

        terminalText.typeface = Typeface.MONOSPACE
        terminalText.setLineSpacing(8f, 1f)

        val terminalParams = LinearLayout.LayoutParams(
            -1,
            -2
        )

        terminalParams.topMargin = 18

        terminalPreview.addView(
            terminalText,
            terminalParams
        )

        val contentParams = LinearLayout.LayoutParams(
            -1,
            0,
            1f
        )

        contentParams.gravity = Gravity.CENTER

        root.addView(content, contentParams)

        val previewParams = LinearLayout.LayoutParams(
            -1,
            -2
        )

        previewParams.bottomMargin = 24

        root.addView(terminalPreview, previewParams)

        val start = button(
            "Get Started   →",
            orange
        )

        start.setOnClickListener {
            start.animate()
                .scaleX(0.96f)
                .scaleY(0.96f)
                .setDuration(90)
                .withEndAction {
                    openTerminal()
                }
                .start()
        }

        root.addView(
            start,
            LinearLayout.LayoutParams(-1, -2)
        )

        val footer = text(
            "POWERED BY UBUNTU",
            10f,
            secondaryColor(),
            true
        )

        footer.gravity = Gravity.CENTER

        val footerParams = LinearLayout.LayoutParams(
            -1,
            -2
        )

        footerParams.topMargin = 22

        root.addView(footer, footerParams)

        setContentView(root)

        root.alpha = 0f
        root.animate()
            .alpha(1f)
            .setDuration(600)
            .start()
    }

    private fun showSettings() {

        applyTheme()

        root = createRoot()

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val back = text(
            "‹",
            36f,
            textColor(),
            true
        )

        back.setOnClickListener {
            showWelcome()
        }

        header.addView(back)

        val title = text(
            "Settings",
            24f,
            textColor(),
            true
        )

        val titleParams = LinearLayout.LayoutParams(
            0,
            -2,
            1f
        )

        titleParams.leftMargin = 18

        header.addView(title, titleParams)

        root.addView(header)

        addSpace(root, 32)

        val appearanceTitle = text(
            "APPEARANCE",
            12f,
            secondaryColor(),
            true
        )

        root.addView(appearanceTitle)

        addSpace(root, 12)

        val themeCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = bg(surface(), 18f)
            setPadding(18, 18, 18, 18)
        }

        val themeTitle = text(
            "Theme",
            17f,
            textColor(),
            true
        )

        themeCard.addView(themeTitle)

        val themeDescription = text(
            "Choose how Ubuntu Terminal looks.",
            13f,
            secondaryColor()
        )

        val descriptionParams = LinearLayout.LayoutParams(
            -1,
            -2
        )

        descriptionParams.topMargin = 6

        themeCard.addView(
            themeDescription,
            descriptionParams
        )

        addSpace(themeCard, 20)

        addThemeOption(themeCard, "Dark", true)
        addThemeOption(themeCard, "Light", false)
        addThemeOption(themeCard, "System", null)

        val cardParams = LinearLayout.LayoutParams(
            -1,
            -2
        )

        root.addView(themeCard, cardParams)

        addSpace(root, 24)

        val info = text(
            "UBUNTU TERMINAL\nVersion 1.0\n\n" +
            "A Linux terminal environment for Android.",
            13f,
            secondaryColor()
        )

        info.gravity = Gravity.CENTER
        root.addView(info)

        setContentView(root)

        root.alpha = 0f
        root.animate().alpha(1f).setDuration(300).start()
    }

    private fun addThemeOption(
        parent: LinearLayout,
        label: String,
        dark: Boolean?
    ) {

        val selected = when (dark) {
            true -> prefs.getBoolean("dark_mode", true) &&
                    prefs.getString("theme", "dark") == "dark"

            false -> !prefs.getBoolean("dark_mode", true) &&
                    prefs.getString("theme", "dark") == "light"

            null -> prefs.getString("theme", "dark") == "system"
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(8, 14, 8, 14)
        }

        val name = text(
            label,
            15f,
            textColor()
        )

        row.addView(
            name,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        val indicator = text(
            if (selected) "●" else "○",
            20f,
            if (selected) orange else secondaryColor()
        )

        row.addView(indicator)

        row.setOnClickListener {

            when (dark) {

                true -> {
                    prefs.edit()
                        .putString("theme", "dark")
                        .putBoolean("dark_mode", true)
                        .apply()
                }

                false -> {
                    prefs.edit()
                        .putString("theme", "light")
                        .putBoolean("dark_mode", false)
                        .apply()
                }

                null -> {
                    prefs.edit()
                        .putString("theme", "system")
                        .putBoolean(
                            "dark_mode",
                            (resources.configuration.uiMode and
                                Configuration.UI_MODE_NIGHT_MASK) ==
                                Configuration.UI_MODE_NIGHT_YES
                        )
                        .apply()
                }
            }

            isDark = prefs.getBoolean("dark_mode", true)

            showSettings()
        }

        parent.addView(row)
    }

    private fun addSpace(
        parent: LinearLayout,
        height: Int
    ) {
        parent.addView(
            View(this),
            LinearLayout.LayoutParams(1, height)
        )
    }

    private fun openTerminal() {

        try {
            startActivity(
                android.content.Intent(
                    this,
                    MakeTerminal::class.java
                )
            )
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Unable to open terminal: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
