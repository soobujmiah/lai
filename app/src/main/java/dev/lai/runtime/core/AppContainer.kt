package dev.lai.runtime.core

import android.content.Context
import dev.lai.runtime.agent.AgentRuntime
import dev.lai.runtime.inference.ModelRepository
import dev.lai.runtime.inference.NativeInferenceEngine
import dev.lai.runtime.ocr.BanglaOcrService
import dev.lai.runtime.shell.ElevatedShell
import dev.lai.runtime.shell.ShizukuController

class AppContainer(context: Context) {
    val shizukuController = ShizukuController()
    val elevatedShell = ElevatedShell(context, shizukuController)
    val modelRepository = ModelRepository(context)
    val inferenceEngine = NativeInferenceEngine()
    val ocrService = BanglaOcrService()
    val agentRuntime = AgentRuntime(elevatedShell, shizukuController, ocrService)
}
