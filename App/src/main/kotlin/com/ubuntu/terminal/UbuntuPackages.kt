package com.ubuntu.terminal

import android.content.Context
import kotlinx.coroutines.*
import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader

class UbuntuPackages(context: Context) {

    private val scope = CoroutineScope(
        Dispatchers.IO + SupervisorJob()
    )

    private val ubuntuDir = File(
        context.getExternalFilesDir(null),
        "ubuntu"
    )

    private val rootfs = File(ubuntuDir, "rootfs")
    private val proot = File(ubuntuDir, "proot")

    private val apt = "/usr/bin/apt-get"

    interface Callback {
        fun onOutput(output: String)
        fun onSuccess(message: String)
        fun onError(error: String)
    }

    private fun execute(
        command: List<String>,
        callback: Callback
    ) {

        scope.launch {

            try {

                if (!proot.exists() || !proot.canExecute()) {
                    throw Exception("PROOT is not installed.")
                }

                if (!File(rootfs, "usr/bin/apt-get").exists()) {
                    throw Exception("Ubuntu APT is not installed.")
                }

                val processCommand = mutableListOf(
                    proot.absolutePath,
                    "--link2symlink",
                    "-0",
                    "-r",
                    rootfs.absolutePath,
                    "-w",
                    "/root"
                )

                processCommand.addAll(command)

                val process = ProcessBuilder(processCommand)
                    .redirectErrorStream(true)
                    .start()

                val reader = BufferedReader(
                    InputStreamReader(process.inputStream)
                )

                while (true) {

                    val line = reader.readLine() ?: break

                    withContext(Dispatchers.Main) {
                        callback.onOutput(line)
                    }
                }

                val exitCode = process.waitFor()

                withContext(Dispatchers.Main) {

                    if (exitCode == 0) {
                        callback.onSuccess("Operation completed successfully.")
                    } else {
                        callback.onError("APT exited with code $exitCode")
                    }
                }

            } catch (e: Exception) {

                withContext(Dispatchers.Main) {
                    callback.onError(
                        e.message ?: "Unknown package manager error"
                    )
                }
            }
        }
    }

    // Configure Ubuntu repositories
    fun configureRepositories(callback: Callback) {

        val sources = """
deb http://ports.ubuntu.com/ubuntu-ports noble main restricted universe multiverse
deb http://ports.ubuntu.com/ubuntu-ports noble-updates main restricted universe multiverse
deb http://ports.ubuntu.com/ubuntu-ports noble-security main restricted universe multiverse
""".trimIndent()

        val script = """
mkdir -p /etc/apt
printf '%s\n' '${sources.lines().joinToString("' '")}' > /etc/apt/sources.list
""".trimIndent()

        execute(
            listOf("/bin/sh", "-c", script),
            callback
        )
    }

    // Update package lists
    fun update(callback: Callback) {

        execute(
            listOf(apt, "update"),
            callback
        )
    }

    // Upgrade installed packages
    fun upgrade(callback: Callback) {

        execute(
            listOf(apt, "upgrade", "-y"),
            callback
        )
    }

    // Install a package
    fun install(
        packageName: String,
        callback: Callback
    ) {

        if (!validPackageName(packageName)) {
            callback.onError("Invalid package name.")
            return
        }

        execute(
            listOf(
                apt,
                "install",
                "-y",
                packageName
            ),
            callback
        )
    }

    // Remove a package
    fun remove(
        packageName: String,
        callback: Callback
    ) {

        if (!validPackageName(packageName)) {
            callback.onError("Invalid package name.")
            return
        }

        execute(
            listOf(
                apt,
                "remove",
                "-y",
                packageName
            ),
            callback
        )
    }

    // Completely remove a package
    fun purge(
        packageName: String,
        callback: Callback
    ) {

        if (!validPackageName(packageName)) {
            callback.onError("Invalid package name.")
            return
        }

        execute(
            listOf(
                apt,
                "purge",
                "-y",
                packageName
            ),
            callback
        )
    }

    // Search packages
    fun search(
        query: String,
        callback: Callback
    ) {

        if (!validPackageName(query)) {
            callback.onError("Invalid search query.")
            return
        }

        execute(
            listOf(
                "/usr/bin/apt-cache",
                "search",
                query
            ),
            callback
        )
    }

    // Show package information
    fun show(
        packageName: String,
        callback: Callback
    ) {

        if (!validPackageName(packageName)) {
            callback.onError("Invalid package name.")
            return
        }

        execute(
            listOf(
                "/usr/bin/apt-cache",
                "show",
                packageName
            ),
            callback
        )
    }

    // List installed packages
    fun listInstalled(callback: Callback) {

        execute(
            listOf(
                "/usr/bin/dpkg",
                "--get-selections"
            ),
            callback
        )
    }

    // Clean package cache
    fun clean(callback: Callback) {

        execute(
            listOf(apt, "clean"),
            callback
        )
    }

    private fun validPackageName(name: String): Boolean {
        return name.matches(
            Regex("[a-zA-Z0-9][a-zA-Z0-9.+:-]*")
        )
    }

    fun cancel() {
        scope.cancel()
    }
}
