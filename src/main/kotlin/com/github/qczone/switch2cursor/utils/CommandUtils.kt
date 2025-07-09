package com.github.qczone.switch2cursor.utils

import com.github.qczone.switch2cursor.settings.AppConfig
import com.intellij.openapi.diagnostic.Logger

object CommandUtils {
    private val logger = Logger.getInstance(CommandUtils::class.java)
    
    fun buildOpenFileCommand(appConfig: AppConfig, filePath: String, line: Int, column: Int): Array<String> {
        return when {
            System.getProperty("os.name").lowercase().contains("mac") -> {
                if (appConfig.useCustomProtocol && appConfig.protocolPrefix.isNotEmpty()) {
                    // 对于 macOS 上的自定义协议，如 cursor://file 格式
                    arrayOf("open", "-a", appConfig.executablePath, "${appConfig.protocolPrefix}file$filePath:$line:$column")
                } else {
                    // 对于 macOS 上的标准命令行参数，需要分割参数
                    val args = appConfig.openFileArgs
                        .replace("{file}", filePath)
                        .replace("{line}", line.toString())
                        .replace("{column}", column.toString())
                    val argsList = parseArguments(args)
                    arrayOf("open", "-a", appConfig.executablePath) + argsList
                }
            }
            System.getProperty("os.name").lowercase().contains("windows") -> {
                // Windows 系统使用标准命令行参数
                val args = appConfig.openFileArgs
                    .replace("{file}", filePath)
                    .replace("{line}", line.toString())
                    .replace("{column}", column.toString())
                val argsList = parseArguments(args)
                arrayOf("cmd", "/c", appConfig.executablePath) + argsList
            }
            else -> {
                // Linux 系统使用标准命令行参数
                val args = appConfig.openFileArgs
                    .replace("{file}", filePath)
                    .replace("{line}", line.toString())
                    .replace("{column}", column.toString())
                val argsList = parseArguments(args)
                arrayOf(appConfig.executablePath) + argsList
            }
        }
    }
    
    private fun parseArguments(args: String): Array<String> {
        // 简单的参数解析，支持带引号的参数
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        var i = 0
        
        while (i < args.length) {
            val char = args[i]
            when {
                char == '"' || char == '\'' -> {
                    inQuotes = !inQuotes
                }
                char == ' ' && !inQuotes -> {
                    if (current.isNotEmpty()) {
                        result.add(current.toString())
                        current = StringBuilder()
                    }
                }
                else -> {
                    current.append(char)
                }
            }
            i++
        }
        
        if (current.isNotEmpty()) {
            result.add(current.toString())
        }
        
        return result.toTypedArray()
    }
    
    fun buildOpenProjectCommand(appConfig: AppConfig, projectPath: String): Array<String> {
        val args = appConfig.openProjectArgs.replace("{project}", projectPath)
        
        return when {
            System.getProperty("os.name").lowercase().contains("mac") -> {
                if (args.trim() == projectPath) {
                    // 如果参数就是项目路径，直接使用
                    arrayOf("open", "-a", appConfig.executablePath, projectPath)
                } else {
                    // 否则需要解析参数
                    val argsList = parseArguments(args)
                    arrayOf("open", "-a", appConfig.executablePath) + argsList
                }
            }
            System.getProperty("os.name").lowercase().contains("windows") -> {
                if (args.trim() == projectPath) {
                    // 如果参数就是项目路径，直接使用
                    arrayOf("cmd", "/c", appConfig.executablePath, projectPath)
                } else {
                    // 否则需要解析参数
                    val argsList = parseArguments(args)
                    arrayOf("cmd", "/c", appConfig.executablePath) + argsList
                }
            }
            else -> {
                if (args.trim() == projectPath) {
                    // 如果参数就是项目路径，直接使用
                    arrayOf(appConfig.executablePath, projectPath)
                } else {
                    // 否则需要解析参数
                    val argsList = parseArguments(args)
                    arrayOf(appConfig.executablePath) + argsList
                }
            }
        }
    }
    
    fun executeCommand(command: Array<String>, appDisplayName: String): Boolean {
        return try {
            logger.info("Executing command for $appDisplayName: ${command.joinToString(" ")}")
            ProcessBuilder(*command).start()
            true
        } catch (ex: Exception) {
            logger.error("Failed to execute $appDisplayName command: ${ex.message}", ex)
            false
        }
    }
} 