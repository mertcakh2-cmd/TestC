package com.ubuntu.terminal

import android.content.Context
import android.os.Build
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class FetchProot(private val context: Context) {

    companion object {
        private const val GITHUB_API =
            "https://api.github.com/repos/termux/proot/releases/latest"

        private const val USER_AGENT = "UbuntuTerminal-Android"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val ubuntuDir =
        File(context.getExternalFilesDir(null), "ubuntu")

    private val prootFile =
        File(ubuntuDir, "proot")

    interface Callback {
        fun onProgress(progress: Int)
        fun onSuccess(file: File)
        fun onError(error: String)
    }

    fun fetch(callback: Callback) {

        scope.launch {

            try {
                ubuntuDir.mkdirs()

                callback.onProgress(0)

                // Detect architecture
                val architecture = detectArchitecture()

                // Fetch GitHub release information
                val connection =
                    URL(GITHUB_API).openConnection() as HttpURLConnection

                connection.apply {
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", USER_AGENT)
                    setRequestProperty(
                        "Accept",
                        "application/vnd.github+json"
                    )
                    connectTimeout = 15000
                    readTimeout = 15000
                }

                val response = connection.inputStream
                    .bufferedReader()
                    .use { it.readText() }

                connection.disconnect()

                val release = JSONObject(response)
                val assets = release.getJSONArray("assets")

                var downloadUrl: String? = null

                // Find matching binary
                for (i in 0 until assets.length()) {

                    val asset = assets.getJSONObject(i)

                    val name = asset.getString("name")
                        .lowercase()

                    val url = asset.getString("browser_download_url")

                    if (
                        name.contains("proot") &&
                        !name.endsWith(".sha256") &&
                        !name.endsWith(".sig") &&
                        matchesArchitecture(name, architecture)
                    ) {
                        downloadUrl = url
                        break
                    }
                }

                if (downloadUrl == null) {
                    throw Exception(
                        "No compatible PROOT binary found for $architecture. " +
                        "Check the GitHub release assets."
                    )
                }

                callback.onProgress(5)

                // Download binary
                val downloadConnection =
                    URL(downloadUrl).openConnection() as HttpURLConnection

                downloadConnection.apply {
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", USER_AGENT)
                    connectTimeout = 20000
                    readTimeout = 30000
                }

                val totalSize = downloadConnection.contentLengthLong

                downloadConnection.inputStream.use { input ->

                    prootFile.outputStream().use { output ->

                        val buffer = ByteArray(8192)
                        var downloaded = 0L
                        var count: Int

                        while (input.read(buffer).also { count = it } != -1) {

                            output.write(buffer, 0, count)
                            downloaded += count

                            if (totalSize > 0) {
                                val progress =
                                    5 + ((downloaded * 95) / totalSize).toInt()

                                withContext(Dispatchers.Main) {
                                    callback.onProgress(progress)
                                }
                            }
                        }

                        output.flush()
                    }
                }

                downloadConnection.disconnect()

                // Make executable
                if (!prootFile.setExecutable(true, true)) {
                    throw Exception("Could not make PROOT executable.")
                }

                if (!prootFile.exists() || prootFile.length() == 0L) {
                    throw Exception("Downloaded PROOT file is invalid.")
                }

                withContext(Dispatchers.Main) {
                    callback.onProgress(100)
                    callback.onSuccess(prootFile)
                }

            } catch (e: Exception) {

                prootFile.delete()

                withContext(Dispatchers.Main) {
                    callback.onError(
                        e.message ?: "Unknown download error"
                    )
                }
            }
        }
    }

    private fun detectArchitecture(): String {

        return when {
            Build.SUPPORTED_ABIS.any {
                it.contains("arm64")
            } -> "aarch64"

            Build.SUPPORTED_ABIS.any {
                it.startsWith("armeabi")
            } -> "arm"

            Build.SUPPORTED_ABIS.any {
                it.contains("x86_64")
            } -> "x86_64"

            Build.SUPPORTED_ABIS.any {
                it == "x86"
            } -> "x86"

            else -> throw Exception("Unsupported CPU architecture")
        }
    }

    private fun matchesArchitecture(
        filename: String,
        architecture: String
    ): Boolean {

        return when (architecture) {

            "aarch64" ->
                filename.contains("aarch64") ||
                filename.contains("arm64")

            "arm" ->
                filename.contains("armv7") ||
                filename.contains("armhf")

            "x86_64" ->
                filename.contains("x86_64") ||
                filename.contains("amd64")

            "x86" ->
                filename.contains("i686") ||
                filename.contains("i386")

            else -> false
        }
    }

    fun cancel() {
        scope.cancel()
    }
}
