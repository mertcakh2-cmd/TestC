package com.ubuntu.terminal

import android.content.Context
import kotlinx.coroutines.*
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream

class DownloadRootfs(context: Context) {

    private val scope = CoroutineScope(
        Dispatchers.IO + SupervisorJob()
    )

    private val ubuntuDir = File(
        context.getExternalFilesDir(null),
        "ubuntu"
    )

    private val archive = File(ubuntuDir, "ubuntu-rootfs.tar.gz")
    private val rootfs = File(ubuntuDir, "rootfs")
    private val temporaryRootfs = File(ubuntuDir, "rootfs-temp")

    companion object {
        const val UBUNTU_VERSION = "24.04"
        const val ARCHITECTURE = "arm64"

        /*
         * Official Ubuntu OCI image registry.
         * The OCI manifest must be resolved before downloading layers.
         * Supply a verified rootfs archive URL to downloadFrom().
         */
    }

    interface Callback {
        fun onProgress(progress: Int)
        fun onStatus(status: String)
        fun onSuccess(rootfs: File)
        fun onError(error: String)
    }

    fun downloadFrom(
        downloadUrl: String,
        callback: Callback
    ) {

        scope.launch {

            try {

                ubuntuDir.mkdirs()

                callback.onStatus("Checking device architecture...")

                if (!android.os.Build.SUPPORTED_ABIS.any {
                        it == "arm64-v8a"
                    }) {
                    throw Exception("This rootfs requires ARM64 Android.")
                }

                callback.onStatus("Checking storage...")

                val available = ubuntuDir.usableSpace

                if (available < 1024L * 1024L * 1024L) {
                    throw Exception(
                        "At least 1 GB of free storage is recommended."
                    )
                }

                callback.onStatus("Connecting to Ubuntu server...")

                val connection =
                    URL(downloadUrl).openConnection() as HttpURLConnection

                connection.apply {
                    requestMethod = "GET"
                    connectTimeout = 20000
                    readTimeout = 60000
                    setRequestProperty(
                        "User-Agent",
                        "UbuntuTerminal-Android"
                    )
                }

                if (connection.responseCode != 200) {
                    throw Exception(
                        "Download failed: HTTP ${connection.responseCode}"
                    )
                }

                val total = connection.contentLengthLong

                callback.onStatus("Downloading Ubuntu rootfs...")

                archive.delete()

                connection.inputStream.use { input ->

                    FileOutputStream(archive).use { output ->

                        val buffer = ByteArray(32768)
                        var downloaded = 0L

                        while (true) {

                            val count = input.read(buffer)

                            if (count == -1) break

                            output.write(buffer, 0, count)
                            downloaded += count

                            if (total > 0) {

                                val progress =
                                    (downloaded * 100 / total).toInt()

                                withContext(Dispatchers.Main) {
                                    callback.onProgress(progress)
                                }
                            }
                        }
                    }
                }

                connection.disconnect()

                if (!archive.exists() || archive.length() == 0L) {
                    throw Exception("Downloaded archive is empty.")
                }

                callback.onStatus("Verifying archive...")

                // Check gzip header
                FileInputStream(archive).use {
                    val header = ByteArray(2)
                    if (it.read(header) != 2 ||
                        header[0] != 0x1f.toByte() ||
                        header[1] != 0x8b.toByte()
                    ) {
                        throw Exception("Invalid gzip archive.")
                    }
                }

                callback.onStatus("Extracting Ubuntu rootfs...")

                if (temporaryRootfs.exists()) {
                    temporaryRootfs.deleteRecursively()
                }

                temporaryRootfs.mkdirs()

                extractTarGz(archive, temporaryRootfs)

                // Some rootfs archives contain a rootfs/ directory.
                val actualRoot = File(temporaryRootfs, "rootfs")
                val extractedRoot = if (
                    File(actualRoot, "bin").exists()
                ) actualRoot else temporaryRootfs

                if (!File(extractedRoot, "bin").exists() ||
                    !File(extractedRoot, "etc").exists()
                ) {
                    throw Exception("Invalid Ubuntu root filesystem.")
                }

                callback.onStatus("Installing Ubuntu rootfs...")

                val installDir = File(ubuntuDir, "rootfs-new")

                installDir.deleteRecursively()

                if (!extractedRoot.copyRecursively(
                        installDir,
                        overwrite = true
                    )
                ) {
                    throw Exception("Could not install rootfs.")
                }

                if (!File(installDir, "usr/bin").exists()) {
                    throw Exception("Ubuntu filesystem is incomplete.")
                }

                val oldRoot = File(ubuntuDir, "rootfs-old")

                oldRoot.deleteRecursively()

                if (rootfs.exists() && !rootfs.renameTo(oldRoot)) {
                    throw Exception("Could not replace existing rootfs.")
                }

                if (!installDir.renameTo(rootfs)) {
                    if (oldRoot.exists()) {
                        oldRoot.renameTo(rootfs)
                    }

                    throw Exception("Could not activate Ubuntu rootfs.")
                }

                oldRoot.deleteRecursively()
                temporaryRootfs.deleteRecursively()
                archive.delete()

                withContext(Dispatchers.Main) {
                    callback.onProgress(100)
                    callback.onStatus("Ubuntu installed successfully!")
                    callback.onSuccess(rootfs)
                }

            } catch (e: Exception) {

                withContext(Dispatchers.Main) {
                    callback.onError(
                        e.message ?: "Unknown installation error"
                    )
                }
            }
        }
    }

    private fun extractTarGz(
        archive: File,
        destination: File
    ) {

        GZIPInputStream(
            BufferedInputStream(FileInputStream(archive))
        ).use { gzip ->

            val header = ByteArray(512)

            while (true) {

                readFully(gzip, header)

                if (header.all { it == 0.toByte() }) break

                val name = String(
                    header,
                    0,
                    100
                ).trimEnd('\u0000')

                val prefix = String(
                    header,
                    345,
                    155
                ).trimEnd('\u0000')

                val fullName = if (prefix.isNotEmpty()) {
                    "$prefix/$name"
                } else {
                    name
                }

                val sizeText = String(
                    header,
                    124,
                    12
                ).trim('\u0000', ' ')

                val size = sizeText.ifEmpty {
                    "0"
                }.trim().toLongOrNull(8)
                    ?: throw Exception("Invalid TAR entry size.")

                val type = header[156].toInt().toChar()

                val target = File(destination, fullName)

                if (!target.canonicalPath.startsWith(
                        destination.canonicalPath + File.separator
                    )
                ) {
                    throw Exception("Unsafe archive path detected.")
                }

                when (type) {

                    '5' -> {
                        target.mkdirs()
                    }

                    '0', '\u0000' -> {

                        target.parentFile?.mkdirs()

                        FileOutputStream(target).use { output ->
                            copyExact(gzip, output, size)
                        }
                    }

                    '2', '1' -> {
                        throw Exception(
                            "This extractor does not support symbolic or hard links. " +
                            "Use a rootfs installer that preserves Linux links."
                        )
                    }

                    else -> {
                        skipExact(gzip, size)
                    }
                }

                val padding = (512 - size % 512) % 512
                skipExact(gzip, padding)
            }
        }
    }

    private fun readFully(
        input: InputStream,
        buffer: ByteArray
    ) {
        var offset = 0

        while (offset < buffer.size) {
            val count = input.read(
                buffer,
                offset,
                buffer.size - offset
            )

            if (count == -1) {
                throw Exception("Unexpected end of TAR archive.")
            }

            offset += count
        }
    }

    private fun copyExact(
        input: InputStream,
        output: OutputStream,
        size: Long
    ) {

        var remaining = size
        val buffer = ByteArray(32768)

        while (remaining > 0) {

            val count = input.read(
                buffer,
                0,
                minOf(buffer.size.toLong(), remaining).toInt()
            )

            if (count == -1) {
                throw Exception("Incomplete TAR file.")
            }

            output.write(buffer, 0, count)
            remaining -= count
        }
    }

    private fun skipExact(
        input: InputStream,
        size: Long
    ) {

        var remaining = size

        while (remaining > 0) {

            val skipped = input.skip(remaining)

            if (skipped <= 0) {
                if (input.read() == -1) {
                    throw Exception("Unexpected end of TAR archive.")
                }

                remaining--
            } else {
                remaining -= skipped
            }
        }
    }

    fun cancel() {
        scope.cancel()
    }
}
