package com.github.tartaricacid.nemc.run

import com.github.tartaricacid.nemc.setting.MCRunConfigurationOptions
import com.github.tartaricacid.nemc.util.FileUtils
import com.github.tartaricacid.nemc.util.LevelDataUtils
import com.github.tartaricacid.nemc.util.PackUtils
import com.github.tartaricacid.nemc.util.PackUtils.PackInfo
import com.github.tartaricacid.nemc.util.PackUtils.PackType
import com.github.tartaricacid.nemc.util.PathUtils
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.intellij.execution.ExecutionException
import com.intellij.openapi.project.Project
import org.cloudburstmc.nbt.NbtMap
import org.cloudburstmc.nbt.NbtMapBuilder
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

class BeforeRun {
    companion object {
        @Throws(ExecutionException::class)
        fun check(project: Project, config: MCRunConfigurationOptions) {
            // 检查启动器路径
            val gamePath = Paths.get(config.gameExecutablePath ?: "")
            if (!Files.isRegularFile(gamePath) || !gamePath.fileName.toString().endsWith(".exe")) {
                throw ExecutionException("Invalid game executable path: ${config.gameExecutablePath}")
            }

            // 清空符号链接
            FileUtils.removeSymlinks(PathUtils.behaviorPacksDir())
            FileUtils.removeSymlinks(PathUtils.resourcePacksDir())

            // 先把当前项目加入
            val packMaps: EnumMap<PackType, MutableList<PackInfo>> = Maps.newEnumMap(PackType::class.java)
            val projectPath = project.basePath
            if (projectPath == null) {
                throw ExecutionException("Project path is null.")
            }
            PackUtils.parsePack(Paths.get(projectPath), packMaps)

            // 再把额外添加的包加入
            for (extraPackPath in config.includedModDirs) {
                val extraPath = Paths.get(extraPackPath)
                PackUtils.parsePack(extraPath, packMaps)
            }

            // 创建符号链接
            for ((type, packList) in packMaps) {
                val targetDir = when (type) {
                    PackType.BEHAVIOR -> PathUtils.behaviorPacksDir()
                    PackType.RESOURCE -> PathUtils.resourcePacksDir()
                }
                if (targetDir == null) {
                    continue
                }
                for (pack in packList) {
                    FileUtils.createSymlink(pack.path, targetDir.resolve(pack.uuid))
                }
            }

            // 读取或新建存档
            val worldDir = PathUtils.worldsDir()
            if (worldDir == null) {
                throw ExecutionException("Worlds directory not found.")
            }
            val worldFolderPath = worldDir.resolve(config.worldFolderName ?: UUID.randomUUID().toString())
            if (!Files.isDirectory(worldFolderPath)) {
                Files.createDirectories(worldFolderPath)
            }

            // 检查 level.dat 是否存在，不存在则创建一个默认的
            val levelDatPath = worldFolderPath.resolve("level.dat")
            if (!Files.isRegularFile(levelDatPath)) {
                val success = LevelDataUtils.createDefaultLevelData(worldFolderPath)
                if (!success) {
                    throw ExecutionException("Failed to create default level.dat in $worldFolderPath")
                }
            }

            // 读取 level.dat 内容
            val tagInfo = try {
                Files.newInputStream(levelDatPath).use { input ->
                    LevelDataUtils.readNbt(input)
                }
            } catch (e: Exception) {
                throw ExecutionException("Failed to read level.dat in $levelDatPath", e)
            }

            // 检查
            val tag = tagInfo.tag
            if (tag !is NbtMap) {
                throw ExecutionException("Tag is not a nbt")
            }
            if (tag.isEmpty()) {
                throw ExecutionException("Tag is empty")
            }
            val builder = NbtMapBuilder.from(tag)

            // 写入自己设定的配置
            // 玩家在最后一次退出游戏时的 Unix 时间戳（秒）。
            builder.putLong("LastPlayed", System.currentTimeMillis() / 1000L)
            // 存档名，修改为项目文件夹名
            builder.putString("LevelName", project.name)
            // 世界种子
            builder.putLong("RandomSeed", config.worldSeed)
            // 游戏模式，0 生存 1 创造
            builder.putInt("GameType", config.gameMode.code)
            // 世界类型，1 默认 2 超平坦
            builder.putInt("Generator", config.levelType.code)
            // 是否允许作弊
            builder.putBoolean("cheatsEnabled", config.enableCheats)
            // 保留物品栏
            builder.putBoolean("keepInventory", config.keepInventory)
            // 是否进行昼夜循环
            builder.putBoolean("dodaylightcycle", config.doDaylightCycle)
            // 是否进行天气循环
            builder.putBoolean("doweathercycle", config.doWeatherCycle)

            // 写回 level.dat
            try {
                Files.newOutputStream(levelDatPath).use { output ->
                    LevelDataUtils.writerNbt(output, builder.build(), tagInfo.version)
                }
            } catch (e: Exception) {
                throw ExecutionException("Failed to write level.dat in $levelDatPath", e)
            }

            // 写清单文件
            val behPacksManifest = Lists.newArrayList<PackManifest>()
            val resPacksManifest = Lists.newArrayList<PackManifest>()
            for ((type, packList) in packMaps) {
                for (pack in packList) {
                    // 基岩版要求版本号格式为 x.y.z，存储为 "version": [x, y, z]
                    val versionParts = pack.version.split(".").mapNotNull { it.toIntOrNull() }
                    val manifest = PackManifest(
                        packId = pack.uuid,
                        version = versionParts
                    )
                    when (type) {
                        PackType.BEHAVIOR -> behPacksManifest.add(manifest)
                        PackType.RESOURCE -> resPacksManifest.add(manifest)
                    }
                }
            }

            // 分别存入 world_behavior_packs.json 和 world_resource_packs.json
            val gson = Gson()
            val behPacksPath = worldFolderPath.resolve("world_behavior_packs.json")
            val resPacksPath = worldFolderPath.resolve("world_resource_packs.json")
            Files.newBufferedWriter(behPacksPath).use { writer ->
                gson.toJson(behPacksManifest, writer)
            }
            Files.newBufferedWriter(resPacksPath).use { writer ->
                gson.toJson(resPacksManifest, writer)
            }

            // 开始写启动参数文件
            val launchConfigPath = worldFolderPath.resolve("launch_config.cppconfig")
            val launchConfig = Maps.newHashMap<String, Any>(
                mapOf(
                    "world_info" to mapOf(
                        "level_id" to (config.worldFolderName ?: UUID.randomUUID().toString()),
                    ),
                    "room_info" to Maps.newHashMap<String, Any>(),
                    "player_info" to mapOf(
                        "urs" to "",
                        "user_id" to 0,
                        "user_name" to (config.userName ?: "DevOps")
                    ),
                    "skin_info" to mapOf(
                        "slim" to false,
                        "skin" to gamePath.parent.resolve("data/skin_packs/vanilla/steve.png").toString()
                    )
                )
            )
            Files.newBufferedWriter(launchConfigPath).use { writer ->
                gson.toJson(launchConfig, writer)
            }
        }
    }

    data class PackManifest(
        @SerializedName("pack_id") val packId: String,
        @SerializedName("version") val version: List<Int>
    )
}
