package com.lamadb.android.logging

import java.io.File

/**
 * Reads and filters the in-app log file.
 */
object LogReader {

    private val LOG_LINE_REGEX =
        Regex("^(.+?) \\[(.+?)/(.+?)\\] (.*)$")

    fun read(file: File, tagFilter: String = ""): List<LogEntry> {
        if (!file.exists()) return emptyList()
        return try {
            file.readLines(Charsets.UTF_8)
                .mapNotNull { parseLine(it) }
                .filter { tagFilter.isBlank() || it.tag.contains(tagFilter, ignoreCase = true) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun parseLine(line: String): LogEntry? {
        val match = LOG_LINE_REGEX.matchEntire(line) ?: return null
        return LogEntry(
            timestamp = match.groupValues[1],
            level = match.groupValues[2],
            tag = match.groupValues[3],
            message = match.groupValues[4]
        )
    }
}
