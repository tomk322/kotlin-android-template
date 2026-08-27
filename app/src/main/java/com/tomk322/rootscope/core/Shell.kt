package com.tomk322.rootscope.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.IOException
import java.io.InputStream

/** Outcome of a single command. [timedOut] commands are killed, not left running. */
data class CmdResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val timedOut: Boolean,
) {
    val ok: Boolean get() = !timedOut && exitCode == 0

    /** stdout when there is any, otherwise stderr, so callers can show *something* useful. */
    val text: String get() = stdout.ifBlank { stderr }.trim()

    companion object {
        fun failure(message: String) = CmdResult(-1, "", message, timedOut = false)
    }
}

/**
 * Runs commands as the app user or through `su`.
 *
 * Every call spawns its own process rather than holding one long-lived root shell: a persistent
 * shell is faster, but a single hung command would then wedge every later probe. Detection has to
 * survive a broken or half-installed su, so isolation wins over speed here.
 */
object Shell {

    const val DEFAULT_TIMEOUT_MS = 4_000L

    /** How long to wait for the reader threads to finish draining after the process exits. */
    private const val DRAIN_JOIN_MS = 500L

    suspend fun exec(argv: List<String>, timeoutMs: Long = DEFAULT_TIMEOUT_MS): CmdResult {
        var process: Process? = null
        val result = withTimeoutOrNull(timeoutMs) {
            runInterruptible(Dispatchers.IO) {
                // Missing binary, EACCES and SELinux denials all surface here as one of these
                // two; both mean "no result", so they are reported identically.
                val started = try {
                    ProcessBuilder(argv).redirectErrorStream(false).start()
                } catch (e: IOException) {
                    return@runInterruptible CmdResult.failure(e.message ?: "IOException")
                } catch (e: SecurityException) {
                    return@runInterruptible CmdResult.failure(e.message ?: "SecurityException")
                }
                process = started

                // stdout and stderr must drain concurrently: a command that fills either pipe
                // buffer while we block on the other deadlocks until the timeout fires.
                val out = StringBuilder()
                val err = StringBuilder()
                val outThread = pump(started.inputStream, out)
                val errThread = pump(started.errorStream, err)

                val code = started.waitFor()
                outThread.join(DRAIN_JOIN_MS)
                errThread.join(DRAIN_JOIN_MS)
                CmdResult(code, out.toString(), err.toString(), timedOut = false)
            }
        }

        if (result == null) {
            runCatching { process?.destroy() }
            return CmdResult(-1, "", "timed out after ${timeoutMs}ms", timedOut = true)
        }
        return result
    }

    suspend fun sh(command: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS): CmdResult =
        exec(listOf("sh", "-c", command), timeoutMs)

    /**
     * Runs [command] through su. The first call typically raises the superuser prompt, so callers
     * should give the user a longer [timeoutMs] to react to it.
     */
    suspend fun su(command: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS): CmdResult =
        exec(listOf("su", "-c", command), timeoutMs)

    private fun pump(stream: InputStream, sink: StringBuilder): Thread =
        Thread {
            runCatching {
                stream.bufferedReader().use { reader ->
                    reader.forEachLine { line ->
                        synchronized(sink) { sink.append(line).append('\n') }
                    }
                }
            }
        }.apply { isDaemon = true; start() }
}

/** Reads a small pseudo-file, returning null instead of throwing on the usual EACCES/ENOENT. */
fun readFileOrNull(path: String): String? = runCatching {
    val file = File(path)
    if (!file.exists() || !file.canRead()) null else file.readText().trim().ifBlank { null }
}.getOrNull()

/** `exists()` returns false for paths we merely cannot stat, so absence is never proof. */
fun pathExists(path: String): Boolean = runCatching { File(path).exists() }.getOrDefault(false)
