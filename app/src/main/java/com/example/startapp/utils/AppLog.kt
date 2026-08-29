package com.example.startapp.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Lightweight structured logger: Logcat + realtime append to a daily file
 * under filesDir/logs. Falls back to stdout when not initialized (unit tests).
 */
object AppLog {

    private const val TAG = "DaySurp"
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val fileNameFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val executor = Executors.newSingleThreadExecutor()

    @Volatile
    private var logDir: File? = null

    fun init(context: Context) {
        logDir = File(context.filesDir, "logs").apply { mkdirs() }
        i("AppLog", "Logging initialized")
    }

    fun i(module: String, message: String) = log("INFO", module, message, null)

    fun w(module: String, message: String, throwable: Throwable? = null) = log("WARN", module, message, throwable)

    fun e(module: String, message: String, throwable: Throwable? = null) = log("ERROR", module, message, throwable)

    private fun log(level: String, module: String, message: String, throwable: Throwable?) {
        val timestamp = timestampFormat.format(Date())
        val details = throwable?.let { " | ${it.stackTraceToString().lineSequence().firstOrNull() ?: ""}" } ?: ""
        val line = "$timestamp [$level] [$module] $message$details"

        val dir = logDir
        if (dir == null) {
            println("[$TAG] $line")
            return
        }

        Log.println(
            when (level) {
                "ERROR" -> Log.ERROR
                "WARN" -> Log.WARN
                else -> Log.INFO
            },
            "$TAG/$module",
            message
        )

        executor.execute {
            try {
                val file = File(dir, "daysurp-${fileNameFormat.format(Date())}.log")
                file.appendText(line + System.lineSeparator())
            } catch (_: Throwable) {
                // Never let logging crash the app.
            }
        }
    }
}
