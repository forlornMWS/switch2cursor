package com.github.qczone.switch2cursor.utils

import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.application.ApplicationManager
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

object WindowUtils {

    private val logger = Logger.getInstance(WindowUtils::class.java)

    fun activeWindow() {
        if (!SystemInfo.isWindows) {
            return
        }
        
        // 异步执行窗口激活，避免阻塞 EDT
        CompletableFuture.runAsync {
            try {
                activateWindowInternal()
            } catch (e: Exception) {
                logger.error("Failed to activate window asynchronously", e)
            }
        }.orTimeout(5, TimeUnit.SECONDS)  // 5秒超时
         .whenComplete { _, throwable ->
             if (throwable != null) {
                 logger.warn("Window activation timed out or failed: ${throwable.message}")
             }
         }
    }
    
    private fun activateWindowInternal() {
        try {
            val command = """Get-Process | Where-Object { ${'$'}_.ProcessName -eq 'Cursor' -and ${'$'}_.MainWindowTitle -match 'Cursor' } | Sort-Object { ${'$'}_.StartTime } -Descending | Select-Object -First 1 | ForEach-Object { (New-Object -ComObject WScript.Shell).AppActivate(${'$'}_.Id) }"""
            logger.debug("Executing PowerShell command for window activation")
            
            val processBuilder = ProcessBuilder("powershell", "-NoProfile", "-Command", command)
            processBuilder.redirectErrorStream(true)
            
            val process = processBuilder.start()
            
            // 使用超时等待，避免无限阻塞
            val finished = process.waitFor(3, TimeUnit.SECONDS)
            
            if (finished) {
                val exitCode = process.exitValue()
                if (exitCode == 0) {
                    logger.debug("Window activation command completed successfully")
                } else {
                    logger.warn("Window activation command failed with exit code: $exitCode")
                }
            } else {
                logger.warn("Window activation command timed out, destroying process")
                process.destroyForcibly()
            }
            
        } catch (e: Exception) {
            logger.warn("Failed to activate Cursor window: ${e.message}")
        }
    }
    
    /**
     * 简化版本的窗口激活，用于对性能要求较高的场景
     */
    fun activeWindowQuick() {
        if (!SystemInfo.isWindows) {
            return
        }
        
        CompletableFuture.runAsync {
            try {
                // 使用更简单的命令，减少执行时间
                val command = "Add-Type -TypeDefinition 'using System; using System.Runtime.InteropServices; public class Win32 { [DllImport(\"user32.dll\")] public static extern bool SetForegroundWindow(IntPtr hWnd); [DllImport(\"user32.dll\")] public static extern IntPtr FindWindow(string lpClassName, string lpWindowName); }'; [Win32]::SetForegroundWindow([Win32]::FindWindow(\$null, '*Cursor*'))"
                
                val processBuilder = ProcessBuilder("powershell", "-NoProfile", "-WindowStyle", "Hidden", "-Command", command)
                val process = processBuilder.start()
                
                process.waitFor(1, TimeUnit.SECONDS)
                if (process.isAlive) {
                    process.destroyForcibly()
                }
                
            } catch (e: Exception) {
                // 静默失败，不记录日志以避免性能影响
            }
        }
    }
}