package org.simbrain.util

import com.Ostermiller.util.CSVParser
import org.simbrain.util.Utils.USER_DIR
import org.simbrain.util.widgets.ProgressWindow
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

/**
 * Root is `src/main/resources`
 */
fun getFileFromResources(path: String): File? {
    val classLoader = ClassLoader.getSystemClassLoader()
    val resource: URL? = classLoader.getResource(path)
    return resource?.let { File(it.file) }
}

/**
 * Root is top level home directory that Simbrain is run from
 */
fun getFileFromRoot(relativePath: String): File {
    val homeDirectory = System.getProperty("user.dir")
    return File(homeDirectory, relativePath)
}

fun getFilesWithExtension(directoryPath: String, extension: String, searchInResources: Boolean = false) : Array<File> {
    val directory: File? = if (searchInResources) {
        getFileFromResources(directoryPath)
    } else {
        getFileFromRoot(directoryPath)
    }
    return directory?.let {
        it.listFiles { file -> file.isFile && file.extension == extension } ?: emptyArray()
    } ?: emptyArray()
}

fun fetchDataFromUrl(urlString: String): String? {
    var progressWindow: ProgressWindow? = null

    try {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"

        // Open progress window
        val contentLength = connection.contentLength
        progressWindow = ProgressWindow(contentLength, "Downloading...")

        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val inputStream = BufferedInputStream(connection.inputStream)
            val result = StringBuilder()
            val buffer = ByteArray(1024)
            var bytesRead: Int
            var totalBytesRead = 0

            // Read the data in chunks
            while (inputStream.read(buffer, 0, buffer.size).also { bytesRead = it } != -1) {
                result.append(String(buffer, 0, bytesRead))
                totalBytesRead += bytesRead

                // Update progress bar
                progressWindow.setValue(totalBytesRead)
                progressWindow.setText("Downloaded $totalBytesRead of $contentLength bytes")
            }

            progressWindow.close()
            return result.toString()
        } else {
            showWarningDialog("Failed to fetch data. Response code: $responseCode")
            progressWindow.close()
            return null
        }
    } catch (e: Exception) {
        progressWindow?.close()
        showWarningDialog("An error occurred: ${e.message}")
        return null
    }
}

fun csvToDouble2DArray(csvString: String): Array<DoubleArray> {
    try {
        // Parse the CSV string
        val parser = CSVParser(StringReader(csvString))
        val rows = parser.allValues
        val result = Array(rows.size) { DoubleArray(rows[0].size) }

        // Convert each value in the CSV to a Double
        for (i in rows.indices) {
            for (j in rows[i].indices) {
                val value = rows[i][j].trim()
                val doubleValue = value.toDoubleOrNull() ?: throw IllegalArgumentException("Non-numeric value found: '$value'")
                result[i][j] = doubleValue
            }
        }

        return result
    } catch (e: Exception) {
        throw IllegalArgumentException("Error parsing CSV: ${e.message}", e)
    }
}

// Helper to get the cache file based on the URL and cache directory
fun getCacheFile(urlString: String, cacheDir: String): File {

    // Create the cache directory if it doesn't exist
    File(cacheDir).mkdirs()

    val url = URI(urlString).toURL()

    val fileName = url.path.substringAfterLast('/')

    if (fileName.isEmpty()) throw IllegalArgumentException("Invalid URL: $urlString")

    return File(cacheDir, fileName)
}

// Function to check if the cache file is valid
fun isCacheValid(file: File): Boolean {
    // Example: you could add time-based cache expiration here if desired
    return file.exists() && file.length() > 0
}

// Function to read data from cache
fun readFromCache(file: File): String? {
    return try {
        file.readText()
    } catch (e: IOException) {
        null
    }
}

// Function to save downloaded data to the cache
fun saveToCache(file: File, data: String) {
    try {
        file.writeText(data)
    } catch (e: IOException) {
        showWarningDialog("Failed to save cache: ${e.message}")
    }
}

// Function to handle fetching data with caching
fun fetchDataWithCache(urlString: String): String? {
    val cacheFile = getCacheFile(urlString, USER_DIR / ".cache")

    // Check if cached file exists and is valid
    if (cacheFile.exists() && isCacheValid(cacheFile)) {
        return readFromCache(cacheFile)
    } else {
        // Download the data and cache it
        val data = fetchDataFromUrl(urlString)
        if (data != null) {
            saveToCache(cacheFile, data)
        }
        return data
    }
}

// TODO: Move to unit test
fun main() {
    val file = getFileFromRoot("build.gradle.kts")
    // val file = getFileFromResources( "tinylog.properties")
    // val file = getFileFromResources( "imageworld/bobcat.jpg")
    if (file != null && file.exists()) {
        println("File found: ${file.absolutePath}")
    } else {
        println("File not found")
    }
}