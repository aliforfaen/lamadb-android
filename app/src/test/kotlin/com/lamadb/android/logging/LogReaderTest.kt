package com.lamadb.android.logging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LogReaderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `parseLine extracts timestamp level tag and message`() {
        val entry = LogReader.parseLine("2026-07-08 10:00:00.000 [I/LamaDB] Presence service started")

        assertEquals("2026-07-08 10:00:00.000", entry?.timestamp)
        assertEquals("I", entry?.level)
        assertEquals("LamaDB", entry?.tag)
        assertEquals("Presence service started", entry?.message)
    }

    @Test
    fun `parseLine returns null for malformed lines`() {
        assertNull(LogReader.parseLine("not a log line"))
    }

    @Test
    fun `read filters by tag`() {
        val file = tempFolder.newFile("test.log")
        file.writeText(
            """
            2026-07-08 10:00:00.000 [I/LamaDB] service started
            2026-07-08 10:00:01.000 [D/Other] other message
            2026-07-08 10:00:02.000 [W/LamaDB] warning
            """.trimIndent()
        )

        val entries = LogReader.read(file, "LamaDB")

        assertEquals(2, entries.size)
        assertEquals("service started", entries[0].message)
        assertEquals("warning", entries[1].message)
    }
}
