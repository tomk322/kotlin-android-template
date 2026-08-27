package com.tomk322.rootscope.scan

import android.content.pm.PackageManager
import android.os.Build
import com.tomk322.rootscope.core.CmdResult
import com.tomk322.rootscope.core.Props
import com.tomk322.rootscope.core.Shell
import com.tomk322.rootscope.core.pathExists
import com.tomk322.rootscope.core.readFileOrNull

/**
 * Everything the root probes observed, gathered once and then interpreted separately.
 *
 * Keeping collection and interpretation apart means the signal list, the verdict and the rendered
 * sections all read the same evidence instead of each re-running its own probes.
 */
data class RootEvidence(
    val suOnDisk: List<String>,
    val suInPath: CmdResult,
    val magiskFiles: List<String>,
    val kernelSuFiles: List<String>,
    val apatchFiles: List<String>,
    val managers: Map<String, String>,
    val writableMounts: List<String>,
    val testKeys: Boolean,
    val buildType: String,
    val dangerousProps: Map<String, String>,
    /** null when root was never requested, so "not attempted" stays distinct from "refused". */
    val idResult: CmdResult?,
) {
    val uidZero: Boolean get() = idResult?.stdout?.contains("uid=0") == true

    /** Any artefact suggesting a superuser solution is installed, regardless of whether it works. */
    val hasArtefacts: Boolean
        get() = suOnDisk.isNotEmpty() || magiskFiles.isNotEmpty() ||
            kernelSuFiles.isNotEmpty() || apatchFiles.isNotEmpty() || managers.isNotEmpty()

    companion object {

        private const val ROOT_PROMPT_TIMEOUT_MS = 30_000L

        // /proc/mounts columns: device, mountpoint, fstype, options, ...
        private const val MOUNT_POINT_COLUMN = 1
        private const val MOUNT_FSTYPE_COLUMN = 2
        private const val MOUNT_OPTIONS_COLUMN = 3
        private const val MOUNT_MIN_COLUMNS = 4

        suspend fun collect(
            pm: PackageManager,
            props: Props,
            requestRoot: Boolean,
        ): RootEvidence = RootEvidence(
            suOnDisk = RootPaths.SU.filter { pathExists(it) },
            suInPath = Shell.sh("command -v su || which su"),
            magiskFiles = RootPaths.MAGISK.filter { pathExists(it) },
            kernelSuFiles = RootPaths.KERNELSU.filter { pathExists(it) },
            apatchFiles = RootPaths.APATCH.filter { pathExists(it) },
            managers = RootPaths.MANAGER_PACKAGES.filterKeys { isInstalled(pm, it) },
            writableMounts = writableSystemMounts(),
            testKeys = Build.TAGS?.contains("test-keys") == true,
            buildType = props.get("ro.build.type", "unknown"),
            dangerousProps = RootPaths.DANGEROUS_PROPS.filter { (key, bad) -> props[key] == bad },
            // The decisive probe, and the only one that can block: it is attempted only on
            // request so opening the app never throws a superuser prompt at the user.
            idResult = if (requestRoot) Shell.su("id", ROOT_PROMPT_TIMEOUT_MS) else null,
        )

        /** Reads the mount table and returns system partitions that are mounted writable. */
        private fun writableSystemMounts(): List<String> {
            val table = readFileOrNull("/proc/mounts") ?: return emptyList()
            val watched = setOf("/", "/system", "/system_ext", "/vendor", "/product", "/odm")
            return table.lineSequence().mapNotNull { line ->
                val parts = line.split(" ")
                if (parts.size < MOUNT_MIN_COLUMNS) return@mapNotNull null
                val mountPoint = parts[MOUNT_POINT_COLUMN]
                if (mountPoint !in watched) return@mapNotNull null
                val options = parts[MOUNT_OPTIONS_COLUMN].split(",")
                if ("rw" in options) "$mountPoint (${parts[MOUNT_FSTYPE_COLUMN]})" else null
            }.toList()
        }

        @Suppress("DEPRECATION")
        private fun isInstalled(pm: PackageManager, packageName: String): Boolean = runCatching {
            pm.getPackageInfo(packageName, 0)
            true
        }.getOrDefault(false)
    }
}
