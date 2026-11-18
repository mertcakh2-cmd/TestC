package com.ubuntu.terminal

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.File

class MakeTerminal : Activity() {

    private lateinit var terminalOutput: TextView
    private lateinit var commandInput: EditText
    private lateinit var scrollView: ScrollView

    private val handler = Handler(Looper.getMainLooper())

    private val history = mutableListOf<String>()
    private var historyIndex = 0

    private var process: Process? = null
    private var writer: OutputStreamWriter? = null

    private val terminalGreen = Color.rgb(80, 220, 120)
    private val background = Color.rgb(12, 14, 18)
    private val foreground = Color.rgb(220, 225, 230)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = background
        window.navigationBarColor = background

        createTerminal()
        startUbuntu()
    }

    private fun createTerminal() {

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(background)
        }

        // Terminal toolbar
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 12, 16, 12)
        }

        val title = TextView(this).apply {
            text = "● Ubuntu Terminal"
            textSize = 17f
            setTextColor(terminalGreen)
            typeface = Typeface.MONOSPACE
        }

        toolbar.addView(title)

        // Terminal output
        scrollView = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(background)
        }

        terminalOutput = TextView(this).apply {
            textSize = 13f
            setTextColor(foreground)
            typeface = Typeface.MONOSPACE
            setPadding(14, 14, 14, 14)
            text = """
Ubuntu Terminal
Initializing environment...

""".trimIndent()
        }

        scrollView.addView(terminalOutput)

        // Input container
        val inputContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(10, 8, 10, 8)
        }

        val prompt = TextView(this).apply {
            text = "root@ubuntu:~# "
            textSize = 12f
            setTextColor(terminalGreen)
            typeface = Typeface.MONOSPACE
        }

        commandInput = EditText(this).apply {
            textSize = 13f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            hint = "Enter command..."
            typeface = Typeface.MONOSPACE
            singleLine = true
            setBackgroundColor(Color.TRANSPARENT)
            imeOptions = EditorInfo.IME_ACTION_SEND
            isFocusableInTouchMode = true
        }

        commandInput.setOnEditorActionListener { _, actionId, event ->

            if (
                actionId == EditorInfo.IME_ACTION_SEND ||
                event?.keyCode == KeyEvent.KEYCODE_ENTER
            ) {
                executeCommand(commandInput.text.toString())
                commandInput.text.clear()
                true
            } else {
                false
            }
        }

        inputContainer.addView(prompt)

        inputContainer.addView(
            commandInput,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        root.addView(toolbar)

        root.addView(
            scrollView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        root.addView(inputContainer)

        setContentView(root)
    }

    private fun startUbuntu() {

        Thread {

            try {
                val files = getExternalFilesDir(null)
                    ?: throw Exception("App storage unavailable")

                val ubuntuRoot = File(files, "ubuntu/rootfs")
                val proot = File(files, "ubuntu/proot")

                if (!ubuntuRoot.exists() || !File(ubuntuRoot, "bin/bash").exists()) {
                    appendOutput(
                        "\nUbuntu root filesystem not found.\n" +
                        "Install an Ubuntu rootfs and proot first.\n"
                    )
                    return@Thread
                }

                if (!proot.exists()) {
                    appendOutput("\nPROOT executable not found.\n")
                    return@Thread
                }

                val command = listOf(
                    proot.absolutePath,
                    "--link2symlink",
                    "-0",
                    "-r",
                    ubuntuRoot.absolutePath,
                    "-w",
                    "/root",
                    "/usr/bin/env",
                    "-i",
                    "HOME=/root",
                    "USER=root",
                    "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
                    "TERM=xterm-256color",
                    "/bin/bash",
                    "--noprofile",
                    "--norc",
                    "-i"
                )

                process = ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start()

                writer = OutputStreamWriter(process!!.outputStream)

                val reader = BufferedReader(
                    InputStreamReader(process!!.inputStream)
                )

                handler.post {
                    terminalOutput.append(
                        "\nUbuntu environment started.\n\n"
                    )
                }

                while (true) {
                    val line = reader.readLine() ?: break
                    appendOutput(line + "\n")
                }

                appendOutput("\nUbuntu session ended.\n")

            } catch (e: Exception) {
                appendOutput("\nTerminal error: ${e.message}\n")
            }

        }.start()
    }

    private fun executeCommand(command: String) {

        if (command.isBlank()) return

        history.add(command)
        historyIndex = history.size

        appendOutput("root@ubuntu:~# $command\n")

        when (command.trim()) {

            "clear" -> {
                handler.post {
                    terminalOutput.text = ""
                }
                return
            }

            "exit" -> {
                stopUbuntu()
                appendOutput("Ubuntu session closed.\n")
                return
            }
        }

        Thread {
            try {
                val currentWriter = writer

                if (currentWriter == null || process?.isAlive != true) {
                    appendOutput("Ubuntu is not running.\n")
                    return@Thread
                }

                synchronized(currentWriter) {
                    currentWriter.write(command)
                    currentWriter.write("\n")
                    currentWriter.flush()
                }

            } catch (e: Exception) {
                appendOutput("Execution error: ${e.message}\n")
            }
        }.start()
    }

    private fun appendOutput(text: String) {

        handler.post {
            terminalOutput.append(text)

            scrollView.post {
                scrollView.fullScroll(View.FOCUS_DOWN)
            }
        }
    }

    private fun stopUbuntu() {
        try {
            writer?.close()
            process?.destroy()
            writer = null
            process = null
        } catch (_: Exception) {
        }
    }

    override fun onDestroy() {
        stopUbuntu()
        super.onDestroy()
    }
}
