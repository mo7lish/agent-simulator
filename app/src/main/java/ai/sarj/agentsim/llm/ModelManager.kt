package ai.sarj.agentsim.llm

import android.content.Context
import android.net.Uri
import ai.sarj.agentsim.config.GameConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Locates / downloads the on-device LLM model and caches it. Checks if it's already present
 * (internal app storage OR the app's external files dir, where it can be adb-pushed for dev),
 * so it never re-downloads. After it's on disk the app runs fully offline.
 */
class ModelManager(context: Context) {

    private val appContext = context.applicationContext
    private val minValidBytes = 1_400_000_000L // sanity floor (model is ~1.6 GB)

    private val internalFile = File(File(appContext.filesDir, "models"), GameConfig.MODEL_FILE)
    private val externalFile: File? =
        appContext.getExternalFilesDir(null)?.let { File(File(it, "models"), GameConfig.MODEL_FILE) }

    /** The model file if already on disk and plausibly complete, else null. */
    fun existingModel(): File? {
        if (internalFile.exists() && internalFile.length() > minValidBytes) return internalFile
        externalFile?.let { if (it.exists() && it.length() > minValidBytes) return it }
        return null
    }

    fun isReady(): Boolean = existingModel() != null

    /** Where to adb-push the model for development (app-readable, no internet/hosting needed). */
    val sideloadPath: String
        get() = (externalFile ?: internalFile).absolutePath

    val hasDownloadUrl: Boolean get() = GameConfig.MODEL_URL.isNotBlank()

    /** Download the model from MODEL_URL into internal storage, reporting 0f..1f progress. */
    suspend fun download(onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        require(hasDownloadUrl) { "No MODEL_URL configured" }
        internalFile.parentFile?.mkdirs()
        val part = File(internalFile.parentFile, GameConfig.MODEL_FILE + ".part")

        // Follow redirects manually (HF resolve -> CDN can cross host/scheme, which
        // HttpURLConnection won't auto-follow across http<->https).
        var url = URL(GameConfig.MODEL_URL)
        var conn = openConn(url)
        var hops = 0
        while (conn.responseCode in 300..399 && hops < 5) {
            val loc = conn.getHeaderField("Location") ?: break
            conn.disconnect()
            url = URL(url, loc)
            conn = openConn(url)
            hops++
        }
        if (conn.responseCode !in 200..299) {
            val code = conn.responseCode
            conn.disconnect()
            throw IllegalStateException("Download failed (HTTP $code).")
        }

        val total = conn.contentLengthLong.takeIf { it > 0 } ?: GameConfig.MODEL_SIZE_BYTES
        conn.inputStream.use { input ->
            part.outputStream().use { out ->
                val buf = ByteArray(1 shl 16)
                var downloaded = 0L
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                    downloaded += n
                    onProgress((downloaded.toFloat() / total).coerceIn(0f, 1f))
                }
            }
        }
        conn.disconnect()
        if (part.length() < minValidBytes) {
            part.delete()
            throw IllegalStateException("Downloaded file too small (${part.length() / 1_000_000} MB).")
        }
        if (!part.renameTo(internalFile)) {
            part.copyTo(internalFile, overwrite = true)
            part.delete()
        }
        internalFile
    }

    private fun openConn(url: URL): HttpURLConnection =
        (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 60_000
            instanceFollowRedirects = false
            setRequestProperty("User-Agent", "AgentSimulator/1.0")
        }

    /** Import a user-picked .task file (e.g. AirDropped to Downloads) into app storage. No adb/internet. */
    suspend fun importFromUri(uri: Uri, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        internalFile.parentFile?.mkdirs()
        val resolver = appContext.contentResolver
        val total = runCatching { resolver.openFileDescriptor(uri, "r")?.use { it.statSize } }
            .getOrNull()?.takeIf { it > 0 } ?: GameConfig.MODEL_SIZE_BYTES
        val part = File(internalFile.parentFile, GameConfig.MODEL_FILE + ".part")
        val input = resolver.openInputStream(uri) ?: throw IllegalStateException("Can't open the selected file.")
        input.use { ins ->
            part.outputStream().use { out ->
                val buf = ByteArray(1 shl 16)
                var copied = 0L
                while (true) {
                    val n = ins.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                    copied += n
                    onProgress((copied.toFloat() / total).coerceIn(0f, 1f))
                }
            }
        }
        if (part.length() < minValidBytes) {
            part.delete()
            throw IllegalStateException("That file looks too small to be the model (${part.length() / 1_000_000} MB).")
        }
        if (!part.renameTo(internalFile)) {
            part.copyTo(internalFile, overwrite = true)
            part.delete()
        }
        internalFile
    }
}
