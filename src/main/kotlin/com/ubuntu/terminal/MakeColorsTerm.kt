package com.ubuntu.terminal

import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.BackgroundColorSpan
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.widget.TextView

object MakeColorsTerm {

    // Ubuntu terminal colors
    object Palette {
        const val BLACK = 0xFF171421.toInt()
        const val RED = 0xFFE95420.toInt()
        const val GREEN = 0xFF8AE234.toInt()
        const val YELLOW = 0xFFFCE94F.toInt()
        const val BLUE = 0xFF729FCF.toInt()
        const val MAGENTA = 0xFFAD7FA8.toInt()
        const val CYAN = 0xFF34E2E2.toInt()
        const val WHITE = 0xFFEEEEEC.toInt()

        const val BRIGHT_BLACK = 0xFF555753.toInt()
        const val BRIGHT_RED = 0xFFEF2929.toInt()
        const val BRIGHT_GREEN = 0xFF8AE234.toInt()
        const val BRIGHT_YELLOW = 0xFFFCE94F.toInt()
        const val BRIGHT_BLUE = 0xFF729FCF.toInt()
        const val BRIGHT_MAGENTA = 0xFFAD7FA8.toInt()
        const val BRIGHT_CYAN = 0xFF34E2E2.toInt()
        const val BRIGHT_WHITE = 0xFFFFFFFF.toInt()

        const val BACKGROUND = 0xFF171421.toInt()
        const val FOREGROUND = 0xFFFFFFFF.toInt()
        const val UBUNTU_ORANGE = 0xFFE95420.toInt()
    }

    // ANSI escape codes
    fun ansi(code: Int): String {
        return "\u001B[${code}m"
    }

    fun reset(): String = ansi(0)

    fun red(text: String) = "\u001B[31m$text\u001B[0m"
    fun green(text: String) = "\u001B[32m$text\u001B[0m"
    fun yellow(text: String) = "\u001B[33m$text\u001B[0m"
    fun blue(text: String) = "\u001B[34m$text\u001B[0m"
    fun magenta(text: String) = "\u001B[35m$text\u001B[0m"
    fun cyan(text: String) = "\u001B[36m$text\u001B[0m"
    fun white(text: String) = "\u001B[37m$text\u001B[0m"

    fun bold(text: String) = "\u001B[1m$text\u001B[0m"
    fun dim(text: String) = "\u001B[2m$text\u001B[0m"
    fun underline(text: String) = "\u001B[4m$text\u001B[0m"

    // Ubuntu prompt
    fun ubuntuPrompt(
        user: String = "root",
        hostname: String = "ubuntu",
        directory: String = "~"
    ): String {
        return "\u001B[32m$user@$hostname\u001B[0m:" +
                "\u001B[34m$directory\u001B[0m# "
    }

    // Convert basic ANSI colors to Android Spannable
    fun colorize(text: String): SpannableString {

        val result = SpannableString(text)

        val regex = Regex("\u001B\\[([0-9;]*)m")

        var currentColor = Palette.FOREGROUND
        var bold = false
        var start = 0

        val matches = regex.findAll(text).toList()

        for (match in matches) {

            val end = match.range.first

            if (end > start) {
                result.setSpan(
                    ForegroundColorSpan(currentColor),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                if (bold) {
                    result.setSpan(
                        StyleSpan(Typeface.BOLD),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }

            val codes = match.groupValues[1]
                .split(";")
                .mapNotNull { it.toIntOrNull() }

            for (code in codes) {
                when (code) {
                    0 -> {
                        currentColor = Palette.FOREGROUND
                        bold = false
                    }
                    1 -> bold = true
                    22 -> bold = false
                    30 -> currentColor = Palette.BLACK
                    31 -> currentColor = Palette.RED
                    32 -> currentColor = Palette.GREEN
                    33 -> currentColor = Palette.YELLOW
                    34 -> currentColor = Palette.BLUE
                    35 -> currentColor = Palette.MAGENTA
                    36 -> currentColor = Palette.CYAN
                    37 -> currentColor = Palette.WHITE
                    90 -> currentColor = Palette.BRIGHT_BLACK
                    91 -> currentColor = Palette.BRIGHT_RED
                    92 -> currentColor = Palette.BRIGHT_GREEN
                    93 -> currentColor = Palette.BRIGHT_YELLOW
                    94 -> currentColor = Palette.BRIGHT_BLUE
                    95 -> currentColor = Palette.BRIGHT_MAGENTA
                    96 -> currentColor = Palette.BRIGHT_CYAN
                    97 -> currentColor = Palette.BRIGHT_WHITE
                }
            }

            start = match.range.last + 1
        }

        if (start < text.length) {
            result.setSpan(
                ForegroundColorSpan(currentColor),
                start,
                text.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        return result
    }

    // Android TextView helper
    fun apply(
        textView: TextView,
        text: String
    ) {
        textView.append(colorize(text))
    }

    // RGB true color
    fun rgb(
        red: Int,
        green: Int,
        blue: Int
    ): Int {
        return Color.rgb(
            red.coerceIn(0, 255),
            green.coerceIn(0, 255),
            blue.coerceIn(0, 255)
        )
    }

    // 256 color palette
    fun color256(code: Int): Int {

        val value = code.coerceIn(0, 255)

        if (value < 16) {
            return basicColor(value)
        }

        if (value >= 232) {
            val gray = 8 + (value - 232) * 10
            return Color.rgb(gray, gray, gray)
        }

        val index = value - 16

        val red = index / 36
        val green = (index % 36) / 6
        val blue = index % 6

        fun channel(v: Int) =
            if (v == 0) 0 else 55 + v * 40

        return Color.rgb(
            channel(red),
            channel(green),
            channel(blue)
        )
    }

    private fun basicColor(index: Int): Int {
        val colors = intArrayOf(
            Palette.BLACK,
            Palette.RED,
            Palette.GREEN,
            Palette.YELLOW,
            Palette.BLUE,
            Palette.MAGENTA,
            Palette.CYAN,
            Palette.WHITE,
            Palette.BRIGHT_BLACK,
            Palette.BRIGHT_RED,
            Palette.BRIGHT_GREEN,
            Palette.BRIGHT_YELLOW,
            Palette.BRIGHT_BLUE,
            Palette.BRIGHT_MAGENTA,
            Palette.BRIGHT_CYAN,
            Palette.BRIGHT_WHITE
        )

        return colors[index]
    }

    // Command highlighting
    fun highlightCommand(command: String): SpannableString {

        val result = SpannableString(command)

        val commands = listOf(
            "sudo", "apt", "apt-get", "cd",
            "ls", "pwd", "mkdir", "rm",
            "cp", "mv", "cat", "echo",
            "python3", "git", "nano", "vim"
        )

        for (word in commands) {

            val regex = Regex("\\b${Regex.escape(word)}\\b")

            for (match in regex.findAll(command)) {
                result.setSpan(
                    ForegroundColorSpan(Palette.GREEN),
                    match.range.first,
                    match.range.last + 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        return result
    }
}
