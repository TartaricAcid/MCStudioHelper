package com.github.tartaricacid.mcshelper.util

import com.google.gson.JsonParser
import java.nio.file.Files
import java.nio.file.Path
import java.util.*


class PackUtils {
    companion object {
        /**
         * 解析指定路径下的资源包和行为包，找到 manifest.json 文件并提取相关信息，存储在 packMap 中
         *
         * 如果找到至少一个有效的包，返回 true；否则返回 false
         */
        fun parsePack(path: Path, packMap: EnumMap<PackType, MutableList<PackInfo>>): Boolean {
            var foundPack = false

            // 遍历当前目录下的子目录，检查是否存在 manifest.json 文件
            Files.walk(path, 2).use { stream ->
                stream.filter { Files.isDirectory(it) }.forEach { dir ->
                    val manifestPath = dir.resolve("manifest.json")
                    if (Files.isRegularFile(manifestPath)) {
                        val packInfo = parseManifest(manifestPath)
                        if (packInfo != null) {
                            val list = packMap.getOrPut(packInfo.type) { mutableListOf() }
                            list.add(packInfo)
                            foundPack = true
                        }
                    }
                }
            }

            return foundPack
        }

        fun parseManifest(manifestPath: Path): PackInfo? {
            val manifestContent = Files.readString(manifestPath)
            val jsonObject = JsonParser.parseString(manifestContent).asJsonObject

            val header = jsonObject.getAsJsonObject("header")
            val uuid = header.get("uuid").asString
            val versionArray = header.getAsJsonArray("version")
            val version = versionArray.joinToString(".") { it.asInt.toString() }

            val modules = jsonObject.getAsJsonArray("modules")
            var packType: PackType? = null
            for (moduleElement in modules) {
                val moduleObj = moduleElement.asJsonObject
                val type = moduleObj.get("type").asString
                when (type) {
                    "data" -> packType = PackType.BEHAVIOR
                    "resources" -> packType = PackType.RESOURCE
                }
            }

            return if (packType != null) {
                val path = manifestPath.parent
                PackInfo(packType, uuid, version, path)
            } else {
                null
            }
        }
    }

    data class PackInfo(
        val type: PackType,
        val uuid: String,
        val version: String,
        val path: Path
    )

    enum class PackType {
        BEHAVIOR,
        RESOURCE
    }
}
