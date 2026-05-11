package com.khalawat.android.escalation

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class EscalationEngineTest {

    private lateinit var clock: MutableClock
    private lateinit var engine: EscalationEngine

    @Before
    fun setUp() {
        clock = MutableClock(Instant.parse("2026-01-01T00:00:00Z"))
        engine = EscalationEngineImpl(clock)
    }

    // --- Stage progression ---

    @Test
    fun `first blocked request returns Stage 1`() {
        val state = engine.onBlockedRequest("bad.com")

        assertThat(state.stage).isEqualTo(EscalationStage.STAGE_1)
    }

    @Test
    fun `override from Stage 1 advances to Stage 2`() {
        engine.onBlockedRequest("bad.com")

        val state = engine.override("bad.com")

        assertThat(state.stage).isEqualTo(EscalationStage.STAGE_2)
    }

    @Test
    fun `override from Stage 2 advances to Stage 3`() {
        engine.onBlockedRequest("bad.com")
        engine.override("bad.com")
        engine.onBlockedRequest("bad.com")

        val state = engine.override("bad.com")

        assertThat(state.stage).isEqualTo(EscalationStage.STAGE_3)
    }

    @Test
    fun `override from Stage 3 starts cooling period`() {
        engine.onBlockedRequest("bad.com")
        engine.override("bad.com") // → Stage 2
        engine.onBlockedRequest("bad.com")
        engine.override("bad.com") // → Stage 3

        // Stage 3 timer completes → unlock → cooling
        val state = engine.onStage3Complete("bad.com")

        assertThat(state.stage).isEqualTo(EscalationStage.COOLING)
        assertThat(state.cooldownEndTime).isNotNull()
    }

    // --- Cooling period behavior ---

    @Test
    fun `during cooling, blocked request goes straight to Stage 3`() {
        // Reach cooling state
        reachCoolingState()

        val state = engine.onBlockedRequest("other-bad.com")

        assertThat(state.stage).isEqualTo(EscalationStage.STAGE_3)
    }

    @Test
    fun `after cooling period expires, blocked request returns to Stage 1`() {
        reachCoolingState()

        // Advance past 10-minute cooling period
        clock.advanceMinutes(11)

        val state = engine.onBlockedRequest("bad.com")

        assertThat(state.stage).isEqualTo(EscalationStage.STAGE_1)
    }

    // --- Sliding window reset ---

    @Test
    fun `30 minutes idle resets Stage 2 back to Stage 1`() {
        engine.onBlockedRequest("bad.com")
        engine.override("bad.com") // Now at Stage 2

        // Advance 31 minutes (past idle threshold)
        clock.advanceMinutes(31)

        val state = engine.onBlockedRequest("bad.com")

        assertThat(state.stage).isEqualTo(EscalationStage.STAGE_1)
    }

    @Test
    fun `30 minutes idle resets Stage 3 back to Stage 2`() {
        // Reach Stage 3
        engine.onBlockedRequest("bad.com")
        engine.override("bad.com") // → Stage 2
        engine.onBlockedRequest("bad.com")
        engine.override("bad.com") // → Stage 3

        // Advance 31 minutes
        clock.advanceMinutes(31)

        val state = engine.onBlockedRequest("bad.com")

        assertThat(state.stage).isEqualTo(EscalationStage.STAGE_2)
    }

    @Test
    fun `no reset during 10-minute post-override window`() {
        reachCoolingState()

        // Only 5 minutes into cooling — no reset
        clock.advanceMinutes(5)

        val state = engine.onBlockedRequest("bad.com")

        // Still in cooling, so straight to Stage 3
        assertThat(state.stage).isEqualTo(EscalationStage.STAGE_3)
    }

    // --- Reset ---

    @Test
    fun `full reset clears all state`() {
        reachCoolingState()
        engine.reset()

        val state = engine.onBlockedRequest("bad.com")

        assertThat(state.stage).isEqualTo(EscalationStage.STAGE_1)
    }

    // --- Multiple domains in same session ---

    @Test
    fun `different blocked domains share escalation state`() {
        engine.onBlockedRequest("bad.com")
        engine.override("bad.com") // → Stage 2

        // Different domain inherits the session's escalation level
        val state = engine.onBlockedRequest("other-bad.com")

        assertThat(state.stage).isEqualTo(EscalationStage.STAGE_2)
    }

    // --- Helpers ---

    private fun reachCoolingState() {
        engine.onBlockedRequest("bad.com")
        engine.override("bad.com") // → Stage 2
        engine.onBlockedRequest("bad.com")
        engine.override("bad.com") // → Stage 3
        engine.onStage3Complete("bad.com") // → Cooling
    }
}

/**
 * Testable clock that allows manual time advancement.
 */
class MutableClock(initialInstant: Instant) : Clock() {
    private var instant = initialInstant

    override fun instant(): Instant = instant
    override fun getZone(): ZoneId = ZoneId.of("UTC")
    override fun withZone(zone: ZoneId): Clock = throw UnsupportedOperationException()

    fun advanceMinutes(minutes: Long) {
        instant = instant.plusSeconds(minutes * 60)
    }

    fun advanceSeconds(seconds: Long) {
        instant = instant.plusSeconds(seconds)
    }
}
