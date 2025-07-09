package com.github.qczone.switch2cursor.actions

import com.github.qczone.switch2cursor.settings.AppConfig
import com.github.qczone.switch2cursor.settings.AppSettingsState
import com.github.qczone.switch2cursor.utils.CommandUtils
import com.github.qczone.switch2cursor.utils.WindowUtils
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.actionSystem.ActionUpdateThread

abstract class BaseOpenAction(private val appName: String) : AnAction() {
    private val logger = Logger.getInstance(this::class.java)
    
    // 添加快速模式标志，用于快速对话框场景
    private var quickMode = false
    
    fun setQuickMode(quick: Boolean) {
        this.quickMode = quick
    }
    
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
    
    protected fun getAppConfig(): AppConfig? {
        val settings = AppSettingsState.getInstance()
        return settings.getAppByName(appName)
    }
    
    protected fun openFile(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val virtualFile: VirtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val editor: Editor? = e.getData(CommonDataKeys.EDITOR)
        
        val appConfig = getAppConfig()
        if (appConfig == null || !appConfig.isEnabled) {
            Messages.showErrorDialog(
                project,
                "应用 \"${appName}\" 未配置或已禁用。\n请在 Settings > Tools > Switch2Cursor 中进行配置。",
                "错误"
            )
            return
        }
        
        val line = editor?.caretModel?.logicalPosition?.line?.plus(1) ?: 1
        val column = editor?.caretModel?.logicalPosition?.column?.plus(1) ?: 1
        val filePath = virtualFile.path
        
        val command = CommandUtils.buildOpenFileCommand(appConfig, filePath, line, column)
        val success = CommandUtils.executeCommand(command, appConfig.displayName)
        
        if (!success) {
            Messages.showErrorDialog(
                project,
                """
                无法执行 ${appConfig.displayName} 命令。
                
                请检查：
                1. ${appConfig.displayName} 路径是否正确配置
                2. ${appConfig.displayName} 是否已正确安装
                3. 配置的路径是否指向有效的可执行文件
                """.trimIndent(),
                "错误"
            )
            return
        }
        
        // 在快速模式下使用轻量级窗口激活
        if (quickMode) {
            WindowUtils.activeWindowQuick()
        } else {
            WindowUtils.activeWindow()
        }
    }
    
    protected fun openProject(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val projectPath = project.basePath ?: return
        
        val appConfig = getAppConfig()
        if (appConfig == null || !appConfig.isEnabled) {
            Messages.showErrorDialog(
                project,
                "应用 \"${appName}\" 未配置或已禁用。\n请在 Settings > Tools > Switch2Cursor 中进行配置。",
                "错误"
            )
            return
        }
        
        val command = CommandUtils.buildOpenProjectCommand(appConfig, projectPath)
        val success = CommandUtils.executeCommand(command, appConfig.displayName)
        
        if (!success) {
            Messages.showErrorDialog(
                project,
                """
                无法执行 ${appConfig.displayName} 命令。
                
                请检查：
                1. ${appConfig.displayName} 路径是否正确配置
                2. ${appConfig.displayName} 是否已正确安装
                3. 配置的路径是否指向有效的可执行文件
                """.trimIndent(),
                "错误"
            )
            return
        }
        
        // 在快速模式下使用轻量级窗口激活
        if (quickMode) {
            WindowUtils.activeWindowQuick()
        } else {
            WindowUtils.activeWindow()
        }
    }
    
    protected fun updateFileAction(e: AnActionEvent) {
        val project = e.project
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val appConfig = getAppConfig()
        
        e.presentation.isEnabledAndVisible = project != null && 
                                           virtualFile != null && 
                                           !virtualFile.isDirectory &&
                                           appConfig != null &&
                                           appConfig.isEnabled
    }
    
    protected fun updateProjectAction(e: AnActionEvent) {
        val project = e.project
        val appConfig = getAppConfig()
        
        e.presentation.isEnabledAndVisible = project != null &&
                                           appConfig != null &&
                                           appConfig.isEnabled
    }
} 