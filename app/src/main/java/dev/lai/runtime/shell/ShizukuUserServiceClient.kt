package dev.lai.runtime.shell

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import dev.lai.runtime.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import rikka.shizuku.Shizuku

internal class ShizukuUserServiceClient(context: Context) {
    private val service = MutableStateFlow<IPrivilegedService?>(null)
    private val bindMutex = Mutex()
    @Volatile private var binding = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder?) {
            binding = false
            service.value = binder?.takeIf(IBinder::pingBinder)?.let(IPrivilegedService.Stub::asInterface)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            binding = false
            service.value = null
        }
    }

    private val args = Shizuku.UserServiceArgs(
        ComponentName(context.packageName, PrivilegedUserService::class.java.name),
    )
        .daemon(false)
        .processNameSuffix("privileged")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)

    suspend fun execute(argv: List<String>, timeoutMs: Long): ShellResult = withContext(Dispatchers.IO) {
        val remote = connect()
        val response = remote.execute(argv.toTypedArray(), timeoutMs, OUTPUT_LIMIT)
        val json = JSONObject(response)
        ShellResult(
            exitCode = json.getInt("exitCode"),
            stdout = json.optString("stdout"),
            stderr = json.optString("stderr"),
            timedOut = json.optBoolean("timedOut"),
        )
    }

    private suspend fun connect(): IPrivilegedService {
        service.value?.takeIf { it.asBinder().isBinderAlive }?.let { return it }
        bindMutex.withLock {
            if (service.value?.asBinder()?.isBinderAlive != true && !binding) {
                binding = true
                runCatching { Shizuku.bindUserService(args, connection) }
                    .onFailure { binding = false; throw it }
            }
        }
        return withTimeout(BIND_TIMEOUT_MS) { service.filterNotNull().first() }
    }

    companion object {
        private const val BIND_TIMEOUT_MS = 8_000L
        private const val OUTPUT_LIMIT = 64 * 1024
    }
}
