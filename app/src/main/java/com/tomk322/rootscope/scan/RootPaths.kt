package com.tomk322.rootscope.scan

/** Known filesystem locations and package names the root probes look for. */
object RootPaths {

    /** Historic and current su locations, roughly most- to least-common. */
    val SU = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/system/sbin/su",
        "/sbin/su",
        "/vendor/bin/su",
        "/vendor/xbin/su",
        "/su/bin/su",
        "/debug_ramdisk/su",
        "/data/adb/su",
        "/data/local/su",
        "/data/local/bin/su",
        "/data/local/xbin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/system/bin/.ext/.su",
        "/system/usr/we-need-root/su",
        "/magisk/.core/bin/su",
        "/sbin/.magisk/busybox/su",
        "/cache/su",
        "/dev/su",
    )

    val MAGISK = listOf(
        "/data/adb/magisk",
        "/data/adb/magisk.db",
        "/data/adb/modules",
        "/data/adb/post-fs-data.d",
        "/data/adb/service.d",
        "/sbin/.magisk",
        "/cache/.disable_magisk",
        "/dev/.magisk.unblock",
    )

    val KERNELSU = listOf("/data/adb/ksu", "/data/adb/ksud", "/data/adb/ksu/modules")

    val APATCH = listOf("/data/adb/apd", "/data/adb/ap", "/data/adb/ap/modules")

    /**
     * Package name to display name. Managers repackaged under a random package name - which both
     * Magisk and KernelSU support - are invisible to this probe by design.
     */
    val MANAGER_PACKAGES = mapOf(
        "com.topjohnwu.magisk" to "Magisk",
        "io.github.huskydg.magisk" to "Magisk Delta",
        "me.weishu.kernelsu" to "KernelSU",
        "com.rifsxd.ksunext" to "KernelSU Next",
        "me.bmax.apatch" to "APatch",
        "eu.chainfire.supersu" to "SuperSU",
        "com.koushikdutta.superuser" to "Superuser (Koush)",
        "com.noshufou.android.su" to "Superuser (ChainsDD)",
        "com.thirdparty.superuser" to "Superuser (third party)",
        "com.yellowes.su" to "Yellowes su",
        "com.kingroot.kinguser" to "KingRoot",
        "com.kingo.root" to "KingoRoot",
        "com.zhiqupk.root.global" to "Root Master",
        "com.alephzain.framaroot" to "Framaroot",
    )

    /** Properties whose listed value indicates a relaxed, non-production build. */
    val DANGEROUS_PROPS = mapOf(
        "ro.debuggable" to "1",
        "ro.secure" to "0",
        "service.adb.root" to "1",
        "ro.adb.secure" to "0",
    )
}
