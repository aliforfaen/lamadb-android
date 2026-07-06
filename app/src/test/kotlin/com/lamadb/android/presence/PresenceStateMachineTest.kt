package com.lamadb.android.presence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PresenceStateMachineTest {

    @Test
    fun `initial state is unknown and returns no event`() {
        val machine = PresenceStateMachine(homeSsid = "Valhalla-5G")
        assertEquals(PresenceState.Unknown, machine.currentState())
        assertNull(machine.evaluate("Valhalla-5G"))
    }

    @Test
    fun `home transition emits event after debounce`() {
        var now = 0L
        val machine = PresenceStateMachine(
            homeSsid = "Valhalla-5G",
            debounceMillis = 30_000,
            clock = { now }
        )

        assertNull(machine.evaluate("Valhalla-5G"))
        now += 29_999
        assertNull(machine.evaluate("Valhalla-5G"))
        now += 1
        val event = machine.evaluate("Valhalla-5G")

        assertEquals(PresenceState.Home, event?.state)
        assertEquals(PresenceState.Unknown, event?.previousState)
        assertEquals("valhalla-5g", event?.ssid)
        assertEquals(PresenceStateMachine.CONFIDENCE_SSID, event!!.confidence, 0.0)
    }

    @Test
    fun `flapping cancels pending transition`() {
        var now = 0L
        val machine = PresenceStateMachine(
            homeSsid = "Valhalla-5G",
            debounceMillis = 30_000,
            clock = { now }
        )

        machine.evaluate("Valhalla-5G")
        now += 20_000
        machine.evaluate("GuestWiFi")
        now += 30_000
        val event = machine.evaluate("GuestWiFi")

        assertEquals(PresenceState.Away, event?.state)
        assertEquals(PresenceState.Unknown, event?.previousState)
    }

    @Test
    fun `case insensitive SSID matching`() {
        var now = 0L
        val machine = PresenceStateMachine(
            homeSsid = "VALHALLA-5G",
            debounceMillis = 30_000,
            clock = { now }
        )

        machine.evaluate("valhalla-5g")
        now += 30_000
        val event = machine.evaluate("valhalla-5g")

        assertEquals(PresenceState.Home, event?.state)
    }

    @Test
    fun `quoted SSIDs are normalized`() {
        var now = 0L
        val machine = PresenceStateMachine(
            homeSsid = "Valhalla-5G",
            debounceMillis = 30_000,
            clock = { now }
        )

        machine.evaluate("\"Valhalla-5G\"")
        now += 30_000
        val event = machine.evaluate("\"Valhalla-5G\"")

        assertEquals(PresenceState.Home, event?.state)
        assertEquals("valhalla-5g", event?.ssid)
    }

    @Test
    fun `null SSID is treated as away with lower confidence`() {
        var now = 0L
        val machine = PresenceStateMachine(
            homeSsid = "Valhalla-5G",
            debounceMillis = 30_000,
            clock = { now }
        )

        // Move to home first.
        machine.evaluate("Valhalla-5G")
        now += 30_000
        machine.evaluate("Valhalla-5G")
        now += 30_000

        machine.evaluate(null)
        now += 30_000
        val event = machine.evaluate(null)

        assertEquals(PresenceState.Away, event?.state)
        assertEquals(PresenceState.Home, event?.previousState)
        assertNull(event?.ssid)
        assertEquals(PresenceStateMachine.CONFIDENCE_NO_SSID, event!!.confidence, 0.0)
    }

    @Test
    fun `stable state returns null without resetting timer`() {
        var now = 0L
        val machine = PresenceStateMachine(
            homeSsid = "Valhalla-5G",
            debounceMillis = 30_000,
            clock = { now }
        )

        machine.evaluate("Valhalla-5G")
        now += 30_000
        machine.evaluate("Valhalla-5G") // emits event

        assertNull(machine.evaluate("Valhalla-5G"))
        assertEquals(PresenceState.Home, machine.currentState())
    }
}
