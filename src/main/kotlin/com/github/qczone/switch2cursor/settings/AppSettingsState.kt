package com.github.qczone.switch2cursor.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "com.github.qczone.switch2cursor.settings.AppSettingsState",
    storages = [Storage("Switch2CursorSettings.xml")]
)
class AppSettingsState : PersistentStateComponent<AppSettingsState> {
    // 向后兼容的旧设置
    var cursorPath: String = "cursor"
    
    // 新的多应用配置
    var appConfigs: MutableList<AppConfig> = mutableListOf()
    
    // 初始化默认配置
    init {
        if (appConfigs.isEmpty()) {
            appConfigs.addAll(AppConfig.getDefaultApps())
        }
    }

    override fun getState(): AppSettingsState = this

    override fun loadState(state: AppSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
        
        // 向后兼容：如果没有新配置但有旧的cursorPath，则迁移
        if (appConfigs.isEmpty() && cursorPath.isNotEmpty()) {
            appConfigs.add(AppConfig(
                name = "cursor",
                displayName = "Cursor",
                executablePath = cursorPath,
                openFileShortcut = "alt shift F1",
                openProjectShortcut = "alt shift ctrl F1"
            ))
        }
        
        // 向后兼容：迁移旧的shortcutSuffix到新的快捷键格式
        appConfigs.forEach { config ->
            // 如果使用了反射或者直接访问不存在的字段，这里可能需要特殊处理
            // 但由于我们直接修改了字段，这应该不需要
            if (config.openFileShortcut.isEmpty() && config.openProjectShortcut.isEmpty()) {
                // 基于应用名称设置默认快捷键
                val defaultApp = AppConfig.getDefaultApps().find { it.name == config.name }
                if (defaultApp != null) {
                    config.openFileShortcut = defaultApp.openFileShortcut
                    config.openProjectShortcut = defaultApp.openProjectShortcut
                }
            }
        }
        
        // 确保至少有默认配置
        if (appConfigs.isEmpty()) {
            appConfigs.addAll(AppConfig.getDefaultApps())
        }
        
        // 确保至少有一个应用是启用的（用于调试）
        if (appConfigs.none { it.isEnabled }) {
            // 如果所有应用都被禁用，至少启用第一个默认应用
            val firstApp = appConfigs.firstOrNull { it.name == "cursor" }
                ?: appConfigs.firstOrNull()
            firstApp?.let {
                it.isEnabled = true
                println("Switch2Cursor: Auto-enabled '${it.displayName}' as no apps were enabled")
            }
        }
    }
    
    fun getEnabledApps(): List<AppConfig> {
        return appConfigs.filter { it.isEnabled }
    }
    
    fun getAppByName(name: String): AppConfig? {
        return appConfigs.find { it.name == name }
    }
    
    fun addCustomApp(appConfig: AppConfig) {
        appConfigs.add(appConfig)
    }
    
    fun removeApp(name: String) {
        appConfigs.removeAll { it.name == name }
    }

    companion object {
        fun getInstance(): AppSettingsState = ApplicationManager.getApplication().getService(AppSettingsState::class.java)
    }
} 