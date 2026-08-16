package dev.lai.runtime.core

import android.content.ComponentCallbacks2
import android.content.Context
import dev.lai.runtime.agent.AgentRuntime
import dev.lai.runtime.audit.ToolAuditRepository
import dev.lai.runtime.device.AndroidRuntimeEnvironmentProvider
import dev.lai.runtime.inference.ModelRepository
import dev.lai.runtime.inference.NativeInferenceEngine
import dev.lai.runtime.model.RemoteModelCatalogRepository
import dev.lai.runtime.ocr.BanglaOcrService
import dev.lai.runtime.scheduler.InferenceScheduler
import dev.lai.runtime.scheduler.ModelMemoryEstimator
import dev.lai.runtime.shell.ElevatedShell
import dev.lai.runtime.shell.ShizukuController
import dev.lai.runtime.workspace.WorkspaceDiscovery
import dev.lai.runtime.workspace.WorkspaceRepository
import dev.lai.runtime.workspace.WorkspaceSettingsStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed interface AppRuntimeEvent {
    data class ModelUnloadedForMemory(val level: Int) : AppRuntimeEvent
}

class AppContainer(context: Context) {
    val shizukuController = ShizukuController()
    val elevatedShell = ElevatedShell(context, shizukuController)
    val modelRepository = ModelRepository(context)
    val modelCatalogRepository = RemoteModelCatalogRepository(context)
    val toolAuditRepository = ToolAuditRepository(context)
    val inferenceEngine = NativeInferenceEngine()
    val inferenceScheduler = InferenceScheduler()
    val memoryEstimator = ModelMemoryEstimator()
    val runtimeEnvironment = AndroidRuntimeEnvironmentProvider(context)
    val ocrService = BanglaOcrService()
    val agentRuntime = AgentRuntime(elevatedShell, shizukuController, ocrService)
    val workspaceRepository = WorkspaceRepository(context)
    val workspaceSettingsStore = WorkspaceSettingsStore(workspaceRepository)
    val workspaceDiscovery = WorkspaceDiscovery(workspaceRepository)

    private val _events = MutableSharedFlow<AppRuntimeEvent>(extraBufferCapacity = 4)
    val events = _events.asSharedFlow()

    fun onTrimMemory(level: Int) {
        if (
            level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL ||
            level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE
        ) {
            inferenceEngine.close()
            _events.tryEmit(AppRuntimeEvent.ModelUnloadedForMemory(level))
        }
    }

    fun onLowMemory() {
        inferenceEngine.close()
        _events.tryEmit(AppRuntimeEvent.ModelUnloadedForMemory(Int.MAX_VALUE))
    }
}
