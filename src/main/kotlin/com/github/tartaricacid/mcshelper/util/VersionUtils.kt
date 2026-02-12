package com.github.tartaricacid.mcshelper.util

/**
 * 获取启动器的版本号，用于判断是否支持断点调试功能
 */
val VERSION = Regex(
    """
    (?x)                                                # 启用扩展模式：忽略空白并允许使用 '#' 注释
    ^.*MinecraftPE_Netease[\\/]
    (\d+\.\d+\.\d+\.\d+)[\\/]                           # 版本号：比如 3.7.0.222545
    Minecraft\.Windows\.exe$
    """.trimIndent()
)

class VersionUtils {
    companion object {
        /**
         * 网易在 3.7.0.222545 及以上版本中内置了 ptvsd 库，支持 LSP4IJ 的断点调试功能
         */
        fun canSupportBreakpointDebug(launcherPath: String?): Boolean {
            if (launcherPath == null) {
                return false
            }

            val versionMatch = VERSION.find(launcherPath) ?: return false
            val versionStr = versionMatch.groupValues[1]
            val versionParts = versionStr.split(".").mapNotNull { it.toIntOrNull() }
            return if (versionParts.size >= 4) {
                val major = versionParts[0]
                val minor = versionParts[1]
                val patch = versionParts[2]
                val build = versionParts[3]
                // 判断版本是否大于等于 3.7.0.222545
                (major > 3) ||
                        (major == 3 && minor > 7) ||
                        (major == 3 && minor == 7 && patch > 0) ||
                        (major == 3 && minor == 7 && patch == 0 && build >= 222545)
            } else {
                false
            }
        }
    }
}