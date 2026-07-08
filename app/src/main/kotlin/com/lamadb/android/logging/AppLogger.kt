package com.lamadb.android.logging

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * File-backed rotating logger for in-app debugging.
 *
 * The logger is intentionally simple: it keeps a bounded number of recent log
 * lines in a private file. It is **not** a replacement for logcat; it only
 * captures what the app explicitly logs through this class.
 *
 * Secrets (API keys, tokens, passwords) must never be passed to this logger.
 */
object AppLogger {

    private const val LOG_DIR = "logs"
    private const val LOG_FILE = "lamadb.log"
    private const val MAX_LINES = 1000
    private const val TRIM_TO_LINES = 500
    private const val MAX_FILE_BYTES = 512 * 1024L

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).apply {
        timeZone = TimeZone.getDefault()
    }

    @Volatile
    private var logFile: File? = null

    @Volatile
    private var enabled: Boolean = false

    @JvmStatic
    fun init(context: Context) {
        logFile = File(context.filesDir, "$LOG_DIR/$LOG_FILE").apply {
            parentFile?.mkdirs()
        }
        enabled = LogPreferences(context).loggingEnabled
    }

    @JvmStatic
    fun updateEnabled(context: Context, isEnabled: Boolean) {
        enabled = isEnabled
        LogPreferences(context).loggingEnabled = isEnabled
        if (isEnabled) {
            init(context)
        }
    }

    @JvmStatic
    fun d(tag: String, message: String) {
        Log.d(tag, message)
        write("D", tag, message)
    }

    @JvmStatic
    fun i(tag: String, message: String) {
        Log.i(tag, message)
        write("I", tag, message)
    }

    @JvmStatic
    fun w(tag: String, message: String) {
        Log.w(tag, message)
        write("W", tag, message)
    }

    @JvmStatic
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        val suffix = throwable?.let { "\n${Log.getStackTraceString(it)}" } ?: ""
        write("E", tag, "$message$suffix")
    }

    @JvmStatic
    @Synchronized
    fun getLogFile(): File? = logFile

    @Synchronized
    private fun write(level: String, tag: String, message: String) {
        if (!enabled) return
        val file = logFile ?: return
        try {
            val timestamp = dateFormat.format(Date())
            val line = "$timestamp [$level/$tag] $message\n"
            file.appendText(line, Charsets.UTF_8)
            rotateIfNeeded(file)
        } catch (e: Exception) {
            // Do not recurse into logging failures.
            Log.w("LamaDB", "Failed to write app log", e)
        }
    }

    private fun rotateIfNeeded(file: File) {
        if (file.length() <= MAX_FILE_BYTES) return
        try {
            val lines = file.readLines(Charsets.UTF_8)
            if (lines.size > MAX_LINES) {
                file.writeText(lines.takeLast(TRIM_TO_LINES).joinToString("\n") + "\n", Charsets.UTF_8)
            } else {
                // File is oversized due to long lines; clear it to avoid unbounded growth.
                file.writeText("", Charsets.UTF_8)
            }
        } catch (e: Exception) {
            Log.w("LamaDB", "Failed to rotate app log", e)
        }
    }
}
