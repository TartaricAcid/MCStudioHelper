package com.github.tartaricacid.nemc.util

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.filechooser.FileSystemView

class FileUtils {
    companion object {
        /**
         * 寻找开发启动器的可执行文件路径
         */
        fun findMinecraftExecutables(): List<String> {
            val paths = mutableListOf<String>()
            val fsv = FileSystemView.getFileSystemView()
            File.listRoots().filter { root ->
                !fsv.isFloppyDrive(root) && root.totalSpace > 0
            }.forEach { drive ->
                val basePath = File("${drive}MCStudioDownload\\game\\MinecraftPE_Netease")
                if (basePath.exists() && basePath.isDirectory) {
                    // 遍历版本目录
                    basePath.listFiles()?.forEach { versionDir ->
                        if (versionDir.isDirectory) {
                            val exePath = File(versionDir, "Minecraft.Windows.exe")
                            if (exePath.exists() && exePath.isFile) {
                                paths.add(exePath.absolutePath)
                            }
                        }
                    }
                }
            }
            return paths
        }

        /**
         * 清理行为包目录下所有符号链接
         */
        fun removeSymlinks(dir: Path?) {
            val removeDir = dir ?: return
            Files.list(removeDir).use { paths ->
                paths.forEach { path ->
                    if (Files.isSymbolicLink(path)) {
                        Files.deleteIfExists(path)
                    }
                }
            }
        }

        /**
         * 创建符号链接
         */
        fun createSymlink(target: Path, link: Path) {
            try {
                Files.createSymbolicLink(link, target)
            } catch (e: Exception) {
                // FIXME: Windows 下可能会因为权限问题创建失败，应该弹出 idea 弹窗提示用户
                e.printStackTrace()
            }
        }
    }
}