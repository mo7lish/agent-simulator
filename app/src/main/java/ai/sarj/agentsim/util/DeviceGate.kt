package ai.sarj.agentsim.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build

/**
 * Capability check for the on-device LLM. The game requires a 64-bit chip and enough RAM to host
 * the ~1.6 GB int8 model (~2.5–3 GB resident). Unsupported devices are blocked entirely.
 */
object DeviceGate {

    data class Result(val ok: Boolean, val reason: String?)

    fun check(context: Context): Result {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val totalRamGb = mi.totalMem / (1024.0 * 1024.0 * 1024.0)
        val is64 = Build.SUPPORTED_ABIS.any { it == "arm64-v8a" }

        return when {
            !is64 ->
                Result(false, "This game needs a 64-bit (arm64) phone.")
            Build.VERSION.SDK_INT < Build.VERSION_CODES.O ->
                Result(false, "This game needs Android 8.0 or newer.")
            totalRamGb < 5.8 ->
                Result(false, "This game needs about 6 GB of RAM to run its on-device AI. This phone has about ${"%.1f".format(totalRamGb)} GB.")
            else -> Result(true, null)
        }
    }
}
