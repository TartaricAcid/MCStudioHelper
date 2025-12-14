package com.github.tartaricacid.nemc.run

import com.github.tartaricacid.nemc.gui.MCSettingsEditor
import com.github.tartaricacid.nemc.setting.MCRunConfigurationOptions
import com.github.tartaricacid.nemc.util.PathUtils
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessHandlerFactory
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project

class MCRunConfiguration(project: Project, factory: ConfigurationFactory?, name: String?) :
    RunConfigurationBase<MCRunConfigurationOptions?>(project, factory, name) {
    public override fun getOptions(): MCRunConfigurationOptions {
        return super.getOptions() as MCRunConfigurationOptions
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration?> {
        return MCSettingsEditor()
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
        return object : CommandLineState(environment) {
            @Throws(ExecutionException::class)
            override fun startProcess(): ProcessHandler {
                val gameExecutablePath = options.gameExecutablePath
                if (gameExecutablePath.isNullOrEmpty()) {
                    throw ExecutionException("Game executable path is not set.")
                }

                BeforeRun.check(project, options)

                val worldDir = PathUtils.worldsDir() ?: throw ExecutionException("Worlds directory not found.")
                val worldFolder = options.worldFolderName ?: throw ExecutionException("Worlds directory not found.")
                val launchConfigPath = worldDir.resolve(worldFolder).resolve("launch_config.cppconfig")
                if (!launchConfigPath.toFile().exists()) {
                    throw ExecutionException("Launch configuration file not found: $launchConfigPath")
                }

                val commandLine = GeneralCommandLine()
                    .withExePath(gameExecutablePath)
                    .withParameters("config=${launchConfigPath.toAbsolutePath()}")

                val processHandler = ProcessHandlerFactory.getInstance().createColoredProcessHandler(commandLine)
                ProcessTerminatedListener.attach(processHandler)
                return processHandler
            }
        }
    }
}