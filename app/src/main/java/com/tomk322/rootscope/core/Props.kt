package com.tomk322.rootscope.core

/**
 * System properties, read by shelling out to `getprop` once and parsing the dump.
 *
 * android.os.SystemProperties is a hidden API and is blocklisted for third-party apps, but the
 * `getprop` binary stays readable to everyone, so one exec gets the whole namespace at once.
 */
class Props(private val values: Map<String, String>) {

    operator fun get(key: String): String? = values[key]?.ifBlank { null }

    fun get(key: String, fallback: String): String = get(key) ?: fallback

    fun isTrue(key: String): Boolean = get(key) in TRUTHY

    fun firstOf(vararg keys: String): String? = keys.firstNotNullOfOrNull { get(it) }

    val size: Int get() = values.size

    fun all(): Map<String, String> = values

    companion object {
        private val TRUTHY = setOf("1", "true", "y", "yes", "on")

        // getprop prints one property per line as: [key]: [value]
        private val LINE = Regex("""^\[(.+?)]: \[(.*)]$""")

        suspend fun load(): Props {
            val result = Shell.exec(listOf("getprop"), timeoutMs = 6_000)
            if (!result.ok) return Props(emptyMap())
            val parsed = result.stdout.lineSequence()
                .mapNotNull { LINE.matchEntire(it.trim())?.destructured }
                .associate { (key, value) -> key to value }
            return Props(parsed)
        }
    }
}
