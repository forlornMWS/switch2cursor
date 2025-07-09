package com.github.qczone.switch2cursor.actions

import com.github.qczone.switch2cursor.settings.AppConfig
import com.github.qczone.switch2cursor.settings.AppSettingsState
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import javax.swing.Icon

class QuickOpenProjectDialog {
    
    companion object {
        fun show(e: AnActionEvent) {
            val project: Project = e.project ?: return
            
            val settings = AppSettingsState.getInstance()
            val enabledApps = settings.getEnabledApps()
            
            if (enabledApps.isEmpty()) {
                com.intellij.openapi.ui.Messages.showInfoMessage(
                    project,
                    "没有启用的应用。\n请在 Settings > Tools > Switch2Cursor 中配置应用。",
                    "Switch2Cursor"
                )
                return
            }
            
            val popupStep = AppSelectionPopupStep(
                "选择编辑器打开项目: ${project.name}",
                enabledApps,
                project
            ) { appConfig, popup ->
                popup.closeOk(null)
                executeProjectOpen(e, appConfig, project)
            }
            
            val popup = JBPopupFactory.getInstance().createListPopup(popupStep)
            popupStep.setPopup(popup)
            popup.showCenteredInCurrentWindow(project)
        }
        
        private fun executeProjectOpen(e: AnActionEvent, appConfig: AppConfig, project: Project) {
            ProgressManager.getInstance().run(object : Task.Backgroundable(project, "打开 ${appConfig.displayName}...", false) {
                override fun run(indicator: ProgressIndicator) {
                    ApplicationManager.getApplication().invokeLater {
                        try {
                            val projectAction = DynamicOpenProjectAction(appConfig.name)
                            projectAction.setQuickMode(true)  // 启用快速模式
                            projectAction.actionPerformed(e)
                        } catch (ex: Exception) {
                            com.intellij.openapi.ui.Messages.showErrorDialog(
                                project,
                                "打开 ${appConfig.displayName} 时出错: ${ex.message}",
                                "错误"
                            )
                        }
                    }
                }
            })
        }
    }
    
    private class AppSelectionPopupStep(
        title: String,
        private val apps: List<AppConfig>,
        private val project: Project,
        private val onSelected: (AppConfig, ListPopup) -> Unit
    ) : BaseListPopupStep<AppConfig>(title, apps) {
        
        override fun getTextFor(value: AppConfig): String {
            val parts = mutableListOf<String>()
            parts.add(value.displayName)
            
            if (value.openProjectShortcut.isNotEmpty()) {
                parts.add("(${value.openProjectShortcut})")
            }
            
            if (value.executablePath.isNotEmpty()) {
                parts.add("• ${value.executablePath}")
            }
            
            return parts.joinToString("  ")
        }
        
        override fun getIconFor(value: AppConfig): Icon? {
            // 可以根据应用类型返回不同的图标
            return null
        }
        
        override fun onChosen(selectedValue: AppConfig, finalChoice: Boolean): PopupStep<*>? {
            if (finalChoice) {
                return doFinalStep {
                    popup?.let { 
                        onSelected(selectedValue, it)
                    }
                }
            }
            return PopupStep.FINAL_CHOICE
        }
        
        override fun isSpeedSearchEnabled(): Boolean = true
        
        override fun getSpeedSearchFilter(): com.intellij.openapi.ui.popup.SpeedSearchFilter<AppConfig>? {
            return com.intellij.openapi.ui.popup.SpeedSearchFilter { item -> 
                "${item.displayName} ${item.openProjectShortcut}"
            }
        }
        
        override fun hasSubstep(selectedValue: AppConfig?): Boolean = false
        
        override fun isSelectable(value: AppConfig?): Boolean = true
        
        private var popup: ListPopup? = null
        
        fun setPopup(popup: ListPopup) {
            this.popup = popup
        }
    }
} 