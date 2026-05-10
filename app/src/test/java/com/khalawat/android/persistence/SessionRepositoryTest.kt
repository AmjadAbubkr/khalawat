package com.khalawat.android.persistence

import com.google.common.truth.Truth.assertThat
import com.khalawat.android.escalation.EscalationStage
import org.junit.Test

/**
 * Issue #7: SessionPersistence — TDD
 *
 * Tests the SessionRepository contract using a fake in-memory implementation.
 * Room-backed implementation (RoomSessionRepository) is tested via
 * instrumented tests since Room needs Android context.
 */
class SessionRepositoryTest {

    private val repo = FakeSessionRepository()

    @Test
    fun `initial state is null`() {
        assertThat(repo.loadState()).isNull()
    }

    @Test
    fun `save and load state round-trips`() {
        val state = PersistentEscalationState(
            stage = EscalationStage.STAGE_2,
            lastRequestTime = 1000L,
            lastOverrideTime = 900L,
            cooldownEndTime = null
        )
        repo.saveState(state)
        assertThat(repo.loadState()).isEqualTo(state)
    }

    @Test
    fun `save state overwrites previous`() {
        repo.saveState(PersistentEscalationState(
            stage = EscalationStage.STAGE_1,
            lastRequestTime = 100L, lastOverrideTime = null, cooldownEndTime = null
        ))
        repo.saveState(PersistentEscalationState(
            stage = EscalationStage.COOLING,
            lastRequestTime = 200L, lastOverrideTime = 150L, cooldownEndTime = 800L
        ))
        assertThat(repo.loadState()!!.stage).isEqualTo(EscalationStage.COOLING)
    }

    @Test
    fun `clear state removes saved state`() {
        repo.saveState(PersistentEscalationState(
            stage = EscalationStage.STAGE_3,
            lastRequestTime = 500L, lastOverrideTime = 400L, cooldownEndTime = 1000L
        ))
        repo.clearState()
        assertThat(repo.loadState()).isNull()
    }

    @Test
    fun `log override and retrieve count`() {
        assertThat(repo.getOverrideCountSince(0L)).isEqualTo(0)
        repo.logOverride("example.com", EscalationStage.STAGE_1, 100L)
        repo.logOverride("test.com", EscalationStage.STAGE_2, 200L)
        repo.logOverride("bad.com", EscalationStage.STAGE_3, 300L)
        assertThat(repo.getOverrideCountSince(0L)).isEqualTo(3)
    }

    @Test
    fun `override count respects since timestamp`() {
        repo.logOverride("a.com", EscalationStage.STAGE_1, 100L)
        repo.logOverride("b.com", EscalationStage.STAGE_2, 200L)
        repo.logOverride("c.com", EscalationStage.STAGE_3, 300L)
        assertThat(repo.getOverrideCountSince(150L)).isEqualTo(2)
        assertThat(repo.getOverrideCountSince(250L)).isEqualTo(1)
        assertThat(repo.getOverrideCountSince(400L)).isEqualTo(0)
    }

    @Test
    fun `clear override logs removes all`() {
        repo.logOverride("a.com", EscalationStage.STAGE_1, 100L)
        repo.logOverride("b.com", EscalationStage.STAGE_2, 200L)
        repo.clearOverrideLogs()
        assertThat(repo.getOverrideCountSince(0L)).isEqualTo(0)
    }

    @Test
    fun `cooling state persists with cooldown end time`() {
        val state = PersistentEscalationState(
            stage = EscalationStage.COOLING,
            lastRequestTime = 500L,
            lastOverrideTime = 400L,
            cooldownEndTime = 1100L
        )
        repo.saveState(state)
        val loaded = repo.loadState()!!
        assertThat(loaded.stage).isEqualTo(EscalationStage.COOLING)
        assertThat(loaded.cooldownEndTime).isEqualTo(1100L)
    }
}

// --- Fake for unit tests (Room needs Android context) ---

class FakeSessionRepository : SessionRepository {
    private var state: PersistentEscalationState? = null
    private val overrideLogs = mutableListOf<Triple<String, EscalationStage, Long>>()

    override fun loadState(): PersistentEscalationState? = state

    override fun saveState(state: PersistentEscalationState) {
        this.state = state
    }

    override fun clearState() { state = null }

    override fun logOverride(domain: String, stage: EscalationStage, timestamp: Long) {
        overrideLogs.add(Triple(domain, stage, timestamp))
    }

    override fun getOverrideCountSince(sinceTimestamp: Long): Int =
        overrideLogs.count { it.third > sinceTimestamp }

    override fun clearOverrideLogs() { overrideLogs.clear() }
}
