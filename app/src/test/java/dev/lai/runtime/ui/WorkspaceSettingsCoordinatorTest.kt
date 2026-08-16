package dev.lai.runtime.ui

import dev.lai.runtime.settings.ContextPolicy
import dev.lai.runtime.settings.LlmSettings
import dev.lai.runtime.settings.SettingsDocumentV1
import dev.lai.runtime.workspace.DiscoveredModel
import dev.lai.runtime.workspace.DiscoveryLimits
import dev.lai.runtime.workspace.ModelDiscoveryPort
import dev.lai.runtime.workspace.ModelDiscoveryStatus
import dev.lai.runtime.workspace.SettingsLoadOutcome
import dev.lai.runtime.workspace.SettingsStorePort
import dev.lai.runtime.workspace.WorkspaceGrantPort
import dev.lai.runtime.workspace.WorkspaceGrantState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fake-port tests for the Phase 2A composition seam.
 *
 * These run as plain JVM unit tests: the coordinator depends only on the pure ports, so the whole
 * settings/workspace vertical slice is verifiable without SAF, Robolectric, or a device.
 */
class WorkspaceSettingsCoordinatorTest {

    private class FakeGrant(override var state: WorkspaceGrantState) : WorkspaceGrantPort

    private class FakeStore(
        var outcome: SettingsLoadOutcome = SettingsLoadOutcome(
            document = SettingsDocumentV1(),
            fromFile = false,
            fellBackToDefaults = true,
            warnings = listOf("workspace not granted"),
        ),
        var saveResult: Result<Unit> = Result.success(Unit),
    ) : SettingsStorePort {
        var saved: SettingsDocumentV1? = null
        var saveCount = 0

        override suspend fun load(): SettingsLoadOutcome = outcome

        override suspend fun save(document: SettingsDocumentV1): Result<Unit> {
            saveCount++
            return saveResult.onSuccess {
                saved = document
                outcome = SettingsLoadOutcome(document, fromFile = true, fellBackToDefaults = false, warnings = emptyList())
            }
        }
    }

    private class FakeDiscovery(
        var result: Result<List<DiscoveredModel>> = Result.success(emptyList()),
    ) : ModelDiscoveryPort {
        var calls = 0
        override suspend fun discoverModels(
            reviewedBySha256: Map<String, String>,
            limits: DiscoveryLimits,
        ): Result<List<DiscoveredModel>> {
            calls++
            return result
        }
    }

    private fun coordinator(
        grant: FakeGrant = FakeGrant(WorkspaceGrantState.NOT_GRANTED),
        store: FakeStore = FakeStore(),
        discovery: FakeDiscovery = FakeDiscovery(),
    ) = WorkspaceSettingsCoordinator(grant, store, discovery)

    private fun model(status: ModelDiscoveryStatus, name: String) = DiscoveredModel(
        relativePath = "models/$name",
        fileName = name,
        sizeBytes = 1_024,
        sha256 = "a".repeat(64),
        modelFormat = "gguf",
        status = status,
    )

    @Test
    fun `an absent workspace starts on safe defaults`() = runBlocking {
        val coordinator = coordinator()

        coordinator.refresh()

        val state = coordinator.state.value
        assertFalse(state.granted)
        assertEquals(SettingsDocumentV1(), state.session.saved)
        assertEquals(0.7f, state.effectiveLlm.temperature)
        assertTrue(state.settingsStatus.contains("built-in defaults"))
    }

    @Test
    fun `a valid settings file restores typed values`() = runBlocking {
        val stored = SettingsDocumentV1(llm = LlmSettings(temperature = 0.25f, topP = 0.4f, maxNewTokens = 320))
        val store = FakeStore(SettingsLoadOutcome(stored, fromFile = true, fellBackToDefaults = false, warnings = emptyList()))
        val coordinator = coordinator(FakeGrant(WorkspaceGrantState.GRANTED), store)

        coordinator.refresh()

        val state = coordinator.state.value
        assertTrue(state.granted)
        assertEquals(0.25f, state.session.saved.llm.temperature)
        assertEquals(320, state.session.saved.llm.maxNewTokens)
        assertTrue(state.settingsStatus.contains("workspace folder"))
    }

    @Test
    fun `a malformed or oversized settings file falls back without crashing`() = runBlocking {
        val store = FakeStore(
            SettingsLoadOutcome(
                document = SettingsDocumentV1(),
                fromFile = true,
                fellBackToDefaults = true,
                warnings = listOf("settings exceeded 32768 bytes"),
            ),
        )
        val coordinator = coordinator(FakeGrant(WorkspaceGrantState.GRANTED), store)

        coordinator.refresh()

        val state = coordinator.state.value
        assertEquals(SettingsDocumentV1(), state.session.saved)
        assertTrue(state.session.fellBackToDefaults)
        assertTrue(state.settingsStatus.contains("unreadable"))
    }

    @Test
    fun `apply once never mutates saved defaults and is spent by one request`() = runBlocking {
        val store = FakeStore()
        val coordinator = coordinator(FakeGrant(WorkspaceGrantState.GRANTED), store)
        coordinator.refresh()

        coordinator.applyOnce(LlmSettings(temperature = 1.5f))

        assertEquals(1.5f, coordinator.state.value.effectiveLlm.temperature)
        assertEquals(0.7f, coordinator.state.value.session.saved.llm.temperature)
        assertTrue(coordinator.state.value.overrideArmed)
        assertEquals("nothing was persisted", 0, store.saveCount)

        assertEquals(1.5f, coordinator.consumeForRequest().temperature)
        // The next request is back on saved defaults.
        assertEquals(0.7f, coordinator.consumeForRequest().temperature)
        assertFalse(coordinator.state.value.overrideArmed)
    }

    @Test
    fun `an invalid quick setting is rejected without arming an override`() = runBlocking {
        val coordinator = coordinator(FakeGrant(WorkspaceGrantState.GRANTED))
        coordinator.refresh()

        val message = coordinator.applyOnce(LlmSettings(temperature = 42f))

        assertTrue(message.contains("temperature"))
        assertFalse(coordinator.state.value.overrideArmed)
        assertEquals(0.7f, coordinator.state.value.effectiveLlm.temperature)
    }

    @Test
    fun `save default persists and becomes the new baseline`() = runBlocking {
        val store = FakeStore()
        val coordinator = coordinator(FakeGrant(WorkspaceGrantState.GRANTED), store)
        coordinator.refresh()
        coordinator.applyOnce(LlmSettings(temperature = 1.2f))

        val document = SettingsDocumentV1(llm = LlmSettings(temperature = 0.4f, maxNewTokens = 200))
        val message = coordinator.saveDefaults(document)

        assertTrue(message.contains("Saved"))
        assertEquals(document, store.saved)
        assertEquals(0.4f, coordinator.state.value.session.saved.llm.temperature)
        // Promoting a choice to a default clears the one-request override.
        assertFalse(coordinator.state.value.overrideArmed)
    }

    @Test
    fun `a rejected document is never written`() = runBlocking {
        val store = FakeStore()
        val coordinator = coordinator(FakeGrant(WorkspaceGrantState.GRANTED), store)
        coordinator.refresh()

        val invalid = SettingsDocumentV1(
            llm = LlmSettings(maxNewTokens = 4096, context = ContextPolicy(maxContextTokens = 512)),
        )
        val message = coordinator.saveDefaults(invalid)

        assertTrue(message.contains("maxNewTokens"))
        assertEquals(0, store.saveCount)
        assertNull(store.saved)
        assertEquals(SettingsDocumentV1(), coordinator.state.value.session.saved)
    }

    @Test
    fun `a failed write keeps the previous saved defaults`() = runBlocking {
        val store = FakeStore(saveResult = Result.failure(IllegalStateException("Workspace not granted")))
        val coordinator = coordinator(FakeGrant(WorkspaceGrantState.GRANTED), store)
        coordinator.refresh()

        val message = coordinator.saveDefaults(SettingsDocumentV1(llm = LlmSettings(temperature = 0.1f)))

        assertTrue(message.contains("not saved"))
        assertEquals(0.7f, coordinator.state.value.session.saved.llm.temperature)
        assertFalse(coordinator.state.value.savingSettings)
    }

    @Test
    fun `reset round-trips back to defaults and persists when granted`() = runBlocking {
        val store = FakeStore()
        val coordinator = coordinator(FakeGrant(WorkspaceGrantState.GRANTED), store)
        coordinator.refresh()
        coordinator.saveDefaults(SettingsDocumentV1(llm = LlmSettings(temperature = 0.1f)))

        coordinator.resetDefaults()

        assertEquals(SettingsDocumentV1(), coordinator.state.value.session.saved)
        assertEquals(SettingsDocumentV1(), store.saved)
    }

    @Test
    fun `reset without a workspace stays in app and writes nothing`() = runBlocking {
        val store = FakeStore()
        val coordinator = coordinator(FakeGrant(WorkspaceGrantState.NOT_GRANTED), store)
        coordinator.refresh()

        val message = coordinator.resetDefaults()

        assertTrue(message.contains("this device"))
        assertEquals(0, store.saveCount)
    }

    @Test
    fun `discovery publishes coarse counts only and never leaks identifiers`() = runBlocking {
        val discovery = FakeDiscovery(
            Result.success(
                listOf(
                    model(ModelDiscoveryStatus.REVIEWED, "qwen.gguf"),
                    model(ModelDiscoveryStatus.LOCAL_UNREVIEWED, "private-notes.gguf"),
                    model(ModelDiscoveryStatus.LOCAL_UNREVIEWED, "other.gguf"),
                    model(ModelDiscoveryStatus.REJECTED, "huge.bin"),
                ),
            ),
        )
        val coordinator = coordinator(FakeGrant(WorkspaceGrantState.GRANTED), FakeStore(), discovery)
        coordinator.refresh()

        val message = coordinator.discoverModels(emptyMap())

        val state = coordinator.state.value
        assertEquals(1, state.reviewedModelCount)
        assertEquals(2, state.localUnreviewedModelCount)
        assertFalse("no file name may reach the UI", message.contains("private-notes"))
        assertFalse("no digest may reach the UI", message.contains("a".repeat(64)))
        assertTrue(message.contains("nothing was loaded"))
    }

    @Test
    fun `discovery is refused without a grant`() = runBlocking {
        val discovery = FakeDiscovery()
        val coordinator = coordinator(FakeGrant(WorkspaceGrantState.NOT_GRANTED), FakeStore(), discovery)
        coordinator.refresh()

        val message = coordinator.discoverModels(emptyMap())

        assertEquals(0, discovery.calls)
        assertTrue(message.contains("Grant a workspace folder"))
    }

    @Test
    fun `a revoked grant clears previous discovery counts`() = runBlocking {
        val grant = FakeGrant(WorkspaceGrantState.GRANTED)
        val discovery = FakeDiscovery(Result.success(listOf(model(ModelDiscoveryStatus.REVIEWED, "qwen.gguf"))))
        val coordinator = coordinator(grant, FakeStore(), discovery)
        coordinator.refresh()
        coordinator.discoverModels(emptyMap())
        assertEquals(1, coordinator.state.value.reviewedModelCount)

        grant.state = WorkspaceGrantState.REVOKED
        coordinator.refresh()

        assertEquals(0, coordinator.state.value.reviewedModelCount)
        assertEquals(0, coordinator.state.value.localUnreviewedModelCount)
        assertFalse(coordinator.state.value.granted)
    }

    @Test
    fun `a discovery failure is reported without crashing`() = runBlocking {
        val discovery = FakeDiscovery(Result.failure(IllegalStateException("Workspace not granted")))
        val coordinator = coordinator(FakeGrant(WorkspaceGrantState.GRANTED), FakeStore(), discovery)
        coordinator.refresh()

        val message = coordinator.discoverModels(emptyMap())

        assertTrue(message.contains("scan failed"))
        assertFalse(coordinator.state.value.discovering)
    }

    @Test
    fun `quick sheet visibility is explicit state`() = runBlocking {
        val coordinator = coordinator()
        coordinator.refresh()
        assertFalse(coordinator.state.value.quickSettingsVisible)

        coordinator.setQuickSettingsVisible(true)
        assertTrue(coordinator.state.value.quickSettingsVisible)

        // A successful apply closes the sheet; a rejected one keeps it open to show the reason.
        coordinator.applyOnce(LlmSettings(temperature = 1.0f))
        assertFalse(coordinator.state.value.quickSettingsVisible)

        coordinator.setQuickSettingsVisible(true)
        coordinator.applyOnce(LlmSettings(temperature = 99f))
        assertTrue(coordinator.state.value.quickSettingsVisible)
    }
}
