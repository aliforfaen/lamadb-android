package com.lamadb.android.logging

/**
 * A single line parsed from the in-app log file.
 */
data class LogEntry(
    val timestamp: String,
    val level: String,
    val tag: String,
    val message: String
)
