package com.github.qczone.switch2cursor.actions

import com.github.qczone.switch2cursor.settings.AppConfig
import com.github.qczone.switch2cursor.settings.AppSettingsState
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.util.IconLoader
import javax.swing.KeyStroke

@Service(Service.Level.APP)
class DynamicActionManager {
    
    private val logger = Logger.getInstance(this::class.java)
    private val registeredActions = mutableMapOf<String, Pair<AnAction, AnAction>>() // appName -> (fileAction, projectAction)
    
    init {
        refreshActions()
    }
    
    fun refreshActions() {
        ApplicationManager.getApplication().invokeLater {
            try {
                val settings = AppSettingsState.getInstance()
                val enabledApps = settings.getEnabledApps()
                
                logger.info("=== Switch2Cursor Action Refresh Started ===")
                logger.info("Total apps in settings: ${settings.appConfigs.size}")
                logger.info("Enabled apps count: ${enabledApps.size}")
                
                enabledApps.forEachIndexed { index, app ->
                    logger.info("Enabled app #${index + 1}: name='${app.name}', displayName='${app.displayName}', enabled=${app.isEnabled}")
                }
                
                // 移除不再启用的应用
                val currentAppNames = enabledApps.map { it.name }.toSet()
                val toRemove = registeredActions.keys.filter { it !in currentAppNames }
                logger.info("Apps to remove: ${toRemove.joinToString()}")
                toRemove.forEach { unregisterApp(it) }
                
                // 注册新启用的应用或更新现有应用
                enabledApps.forEach { appConfig ->
                    if (appConfig.name !in registeredActions) {
                        logger.info("Registering new app: ${appConfig.displayName}")
                        registerApp(appConfig)
                    } else {
                        logger.info("Updating existing app: ${appConfig.displayName}")
                        // 更新现有应用的配置（包括快捷键、显示名称等）
                        updateAppConfig(appConfig)
                    }
                }
                
                // 强制刷新菜单
                refreshMenus()
                
                logger.info("Actions refresh completed successfully")
                logger.info("Currently registered apps: ${registeredActions.keys.joinToString()}")
                logger.info("=== Switch2Cursor Action Refresh Ended ===")
            } catch (e: Exception) {
                logger.error("Failed to refresh actions", e)
            }
        }
    }
    
    private fun registerApp(appConfig: AppConfig) {
        try {
            val actionManager = ActionManager.getInstance()
            
            // 创建打开文件的 action
            val fileActionId = "OpenFileIn${appConfig.name.capitalize()}"
            val fileAction = DynamicOpenFileAction(appConfig.name).apply {
                templatePresentation.text = "Open File In ${appConfig.displayName}"
                templatePresentation.description = "Open current file in ${appConfig.displayName}"
            }
            
            // 创建打开项目的 action
            val projectActionId = "OpenProjectIn${appConfig.name.capitalize()}"
            val projectAction = DynamicOpenProjectAction(appConfig.name).apply {
                templatePresentation.text = "Open Project In ${appConfig.displayName}"
                templatePresentation.description = "Open current project in ${appConfig.displayName}"
            }
            
            // 注册 actions
            actionManager.registerAction(fileActionId, fileAction)
            actionManager.registerAction(projectActionId, projectAction)
            
            // 添加到菜单组
            addToMenuGroups(fileActionId, projectActionId, appConfig)
            
            // 设置快捷键
            setShortcuts(fileActionId, appConfig.openFileShortcut)
            setShortcuts(projectActionId, appConfig.openProjectShortcut)
            
            registeredActions[appConfig.name] = Pair(fileAction, projectAction)
            
            logger.info("Successfully registered actions for ${appConfig.displayName}")
            
        } catch (e: Exception) {
            logger.error("Failed to register actions for ${appConfig.displayName}", e)
        }
    }
    
    private fun unregisterApp(appName: String) {
        try {
            val actionManager = ActionManager.getInstance()
            val fileActionId = "OpenFileIn${appName.capitalize()}"
            val projectActionId = "OpenProjectIn${appName.capitalize()}"
            
            // 从菜单组中移除
            removeFromMenuGroups(fileActionId, projectActionId)
            
            // 注销 actions
            actionManager.unregisterAction(fileActionId)
            actionManager.unregisterAction(projectActionId)
            
            registeredActions.remove(appName)
            
            logger.info("Successfully unregistered actions for $appName")
            
        } catch (e: Exception) {
            logger.error("Failed to unregister actions for $appName", e)
        }
    }
    
    private fun updateAppConfig(appConfig: AppConfig) {
        val fileActionId = "OpenFileIn${appConfig.name.capitalize()}"
        val projectActionId = "OpenProjectIn${appConfig.name.capitalize()}"
        
        val actionManager = ActionManager.getInstance()
        
        // 更新action的显示文本
        actionManager.getAction(fileActionId)?.let { action ->
            action.templatePresentation.text = "Open File In ${appConfig.displayName}"
            action.templatePresentation.description = "Open current file in ${appConfig.displayName}"
        }
        
        actionManager.getAction(projectActionId)?.let { action ->
            action.templatePresentation.text = "Open Project In ${appConfig.displayName}"
            action.templatePresentation.description = "Open current project in ${appConfig.displayName}"
        }
        
        // 更新快捷键
        setShortcuts(fileActionId, appConfig.openFileShortcut)
        setShortcuts(projectActionId, appConfig.openProjectShortcut)
        
        logger.info("Updated configuration for ${appConfig.displayName}")
    }
    
    private fun refreshMenus() {
        // 强制刷新所有菜单
        val actionManager = ActionManager.getInstance()
        
        listOf("ToolsMenu", "EditorPopupMenu", "ProjectViewPopupMenu").forEach { menuId ->
            val group = actionManager.getAction(menuId) as? DefaultActionGroup
            group?.getChildActionsOrStubs()
        }
        
        // 检查菜单分组状态
        checkMenuGroupStatus(actionManager)
        
        logger.info("Menu refresh triggered")
    }
    
    private fun checkMenuGroupStatus(actionManager: ActionManager) {
        logger.info("=== Menu Group Status Check ===")
        
        // 检查主菜单分组
        val mainGroup = actionManager.getAction("Switch2CursorMenuGroup") as? DefaultActionGroup
        logger.info("Main Switch2Cursor group found: ${mainGroup != null}")
        
        // 检查子分组
        val openFileGroup = actionManager.getAction("Switch2CursorOpenFileGroup") as? DefaultActionGroup
        val openProjectGroup = actionManager.getAction("Switch2CursorOpenProjectGroup") as? DefaultActionGroup
        
        logger.info("Open File group found: ${openFileGroup != null}")
        logger.info("Open Project group found: ${openProjectGroup != null}")
        
        // 检查分组中的动作数量
        openFileGroup?.let { group ->
            val children = group.childActionsOrStubs
            logger.info("Open File group has ${children.size} actions:")
            children.forEachIndexed { index, action ->
                logger.info("  #${index + 1}: ${action.templatePresentation.text}")
            }
        }
        
        openProjectGroup?.let { group ->
            val children = group.childActionsOrStubs
            logger.info("Open Project group has ${children.size} actions:")
            children.forEachIndexed { index, action ->
                logger.info("  #${index + 1}: ${action.templatePresentation.text}")
            }
        }
        
        // 检查工具菜单中的分组层次
        val toolsMenu = actionManager.getAction("ToolsMenu") as? DefaultActionGroup
        toolsMenu?.let { menu ->
            val children = menu.childActionsOrStubs
            val switch2cursorGroup = children.find { 
                it is DefaultActionGroup && it.templateText == "Switch2Cursor" 
            } as? DefaultActionGroup
            
            if (switch2cursorGroup != null) {
                logger.info("Found Switch2Cursor group in Tools menu")
                val subChildren = switch2cursorGroup.childActionsOrStubs
                logger.info("Switch2Cursor group has ${subChildren.size} sub-items:")
                subChildren.forEachIndexed { index, child ->
                    if (child is DefaultActionGroup) {
                        logger.info("  #${index + 1}: Group '${child.templateText}' with ${child.childActionsOrStubs.size} actions")
                    } else {
                        logger.info("  #${index + 1}: Action '${child.templatePresentation.text}'")
                    }
                }
            } else {
                logger.warn("Switch2Cursor group NOT found in Tools menu")
            }
        }
        
        logger.info("=== Menu Group Status Check End ===")
    }
    
    private fun addToMenuGroups(fileActionId: String, projectActionId: String, appConfig: AppConfig) {
        val actionManager = ActionManager.getInstance()
        
        // 添加到工具菜单 - 使用新的分组结构
        addToToolsMenu(fileActionId, projectActionId, actionManager)
        
        // 添加到编辑器右键菜单
        addToEditorPopupMenu(fileActionId, projectActionId, actionManager)
        
        // 添加到项目视图右键菜单 - 保持原有逻辑
        addToProjectPopupMenu(fileActionId, projectActionId, actionManager)
    }
    
    private fun addToToolsMenu(fileActionId: String, projectActionId: String, actionManager: ActionManager) {
        // 使用plugin.xml中定义的分组ID
        val openFileGroup = actionManager.getAction("Switch2CursorOpenFileGroup") as? DefaultActionGroup
        val openProjectGroup = actionManager.getAction("Switch2CursorOpenProjectGroup") as? DefaultActionGroup
        
        logger.info("Checking predefined groups - OpenFileGroup: ${openFileGroup != null}, OpenProjectGroup: ${openProjectGroup != null}")
        
        if (openFileGroup != null && openProjectGroup != null) {
            // 如果找到了预定义的分组，直接添加到对应分组
            val fileAction = actionManager.getAction(fileActionId)
            val projectAction = actionManager.getAction(projectActionId)
            
            logger.info("Adding actions - FileAction: ${fileAction != null}, ProjectAction: ${projectAction != null}")
            logger.info("FileActionId: $fileActionId, ProjectActionId: $projectActionId")
            
            if (fileAction != null) {
                openFileGroup.add(fileAction)
                logger.info("Added file action '${fileAction.templatePresentation.text}' to Open File group")
            }
            
            if (projectAction != null) {
                openProjectGroup.add(projectAction)
                logger.info("Added project action '${projectAction.templatePresentation.text}' to Open Project group")
            }
            
            logger.info("Successfully added actions to predefined groups for ${fileAction?.templatePresentation?.text}")
        } else {
            // 如果没有找到预定义分组，创建完整的分组结构
            logger.warn("Predefined groups not found - OpenFileGroup: $openFileGroup, OpenProjectGroup: $openProjectGroup")
            logger.warn("Creating fallback menu structure with sub-groups")
            addToToolsMenuWithSubGroups(fileActionId, projectActionId, actionManager)
        }
    }
    
    private fun addToToolsMenuWithSubGroups(fileActionId: String, projectActionId: String, actionManager: ActionManager) {
        val toolsGroup = actionManager.getAction("ToolsMenu") as? DefaultActionGroup
        toolsGroup?.let { group ->
            val switch2cursorGroup = findOrCreateSwitch2CursorGroup(group)
            val openFileGroup = findOrCreateOpenFileGroup(switch2cursorGroup)
            val openProjectGroup = findOrCreateOpenProjectGroup(switch2cursorGroup)
            
            val fileAction = actionManager.getAction(fileActionId)
            val projectAction = actionManager.getAction(projectActionId)
            
            if (fileAction != null) {
                openFileGroup.add(fileAction)
                logger.info("Added file action '${fileAction.templatePresentation.text}' to fallback Open File group")
            }
            
            if (projectAction != null) {
                openProjectGroup.add(projectAction)
                logger.info("Added project action '${projectAction.templatePresentation.text}' to fallback Open Project group")
            }
        }
    }
    
    private fun addToEditorPopupMenu(fileActionId: String, projectActionId: String, actionManager: ActionManager) {
        val editorPopupGroup = actionManager.getAction("EditorPopupMenu") as? DefaultActionGroup
        editorPopupGroup?.let { group ->
            val switch2cursorGroup = findOrCreateSwitch2CursorGroup(group)
            val openFileGroup = findOrCreateOpenFileGroup(switch2cursorGroup)
            val openProjectGroup = findOrCreateOpenProjectGroup(switch2cursorGroup)
            
            openFileGroup.add(actionManager.getAction(fileActionId))
            openProjectGroup.add(actionManager.getAction(projectActionId))
        }
    }
    
    private fun addToProjectPopupMenu(fileActionId: String, projectActionId: String, actionManager: ActionManager) {
        val projectPopupGroup = actionManager.getAction("ProjectViewPopupMenu") as? DefaultActionGroup
        projectPopupGroup?.let { group ->
            val switch2cursorGroup = findOrCreateSwitch2CursorGroup(group)
            val openFileGroup = findOrCreateOpenFileGroup(switch2cursorGroup)
            val openProjectGroup = findOrCreateOpenProjectGroup(switch2cursorGroup)
            
            openFileGroup.add(actionManager.getAction(fileActionId))
            openProjectGroup.add(actionManager.getAction(projectActionId))
        }
    }
    
    private fun findOrCreateSwitch2CursorGroup(parent: DefaultActionGroup): DefaultActionGroup {
        return findSwitch2CursorGroup(parent) ?: run {
            val newGroup = DefaultActionGroup("Switch2Cursor", true)
            parent.addSeparator()
            parent.add(newGroup)
            newGroup
        }
    }
    
    private fun findOrCreateOpenFileGroup(switch2cursorGroup: DefaultActionGroup): DefaultActionGroup {
        return switch2cursorGroup.childActionsOrStubs.find { 
            it is DefaultActionGroup && it.templateText == "Open File" 
        } as? DefaultActionGroup ?: run {
            val openFileGroup = DefaultActionGroup("Open File", true)
            switch2cursorGroup.add(openFileGroup)
            openFileGroup
        }
    }
    
    private fun findOrCreateOpenProjectGroup(switch2cursorGroup: DefaultActionGroup): DefaultActionGroup {
        return switch2cursorGroup.childActionsOrStubs.find { 
            it is DefaultActionGroup && it.templateText == "Open Project" 
        } as? DefaultActionGroup ?: run {
            val openProjectGroup = DefaultActionGroup("Open Project", true)
            switch2cursorGroup.add(openProjectGroup)
            openProjectGroup
        }
    }
    
    private fun removeFromMenuGroups(fileActionId: String, projectActionId: String) {
        val actionManager = ActionManager.getInstance()
        
        listOf("ToolsMenu", "EditorPopupMenu", "ProjectViewPopupMenu").forEach { menuId ->
            val group = actionManager.getAction(menuId) as? DefaultActionGroup
            group?.let { 
                removeActionFromGroup(it, fileActionId)
                removeActionFromGroup(it, projectActionId)
            }
        }
    }
    
    private fun removeActionFromGroup(group: DefaultActionGroup, actionId: String) {
        val action = ActionManager.getInstance().getAction(actionId)
        if (action != null) {
            group.remove(action)
            
            // 从嵌套的 Switch2Cursor 组中移除
            group.childActionsOrStubs.forEach { child ->
                if (child is DefaultActionGroup && child.templateText == "Switch2Cursor") {
                    child.remove(action)
                    
                    // 也从新的子分组中移除
                    child.childActionsOrStubs.forEach { subChild ->
                        if (subChild is DefaultActionGroup && 
                            (subChild.templateText == "Open File" || subChild.templateText == "Open Project")) {
                            subChild.remove(action)
                        }
                    }
                }
            }
            
            // 直接从预定义的分组中移除（如果存在）
            val actionManager = ActionManager.getInstance()
            val openFileGroup = actionManager.getAction("Switch2CursorOpenFileGroup") as? DefaultActionGroup
            val openProjectGroup = actionManager.getAction("Switch2CursorOpenProjectGroup") as? DefaultActionGroup
            
            openFileGroup?.remove(action)
            openProjectGroup?.remove(action)
        }
    }
    
    private fun findSwitch2CursorGroup(parent: DefaultActionGroup): DefaultActionGroup? {
        return parent.childActionsOrStubs.find { 
            it is DefaultActionGroup && it.templateText == "Switch2Cursor" 
        } as? DefaultActionGroup
    }
    
    private fun setShortcuts(actionId: String, shortcutText: String) {
        try {
            val keymapManager = KeymapManager.getInstance()
            val keymap = keymapManager.activeKeymap
            
            // 移除现有的快捷键
            keymap.removeAllActionShortcuts(actionId)
            
            // 如果快捷键文本为空，则只移除，不添加新的
            if (shortcutText.isBlank()) {
                logger.info("Removed shortcuts for action $actionId")
                return
            }
            
            val keyStroke = parseKeyStroke(shortcutText)
            if (keyStroke != null) {
                // 检查快捷键是否已被其他action占用
                val conflictingActions = keymap.getActionIds(keyStroke)
                if (conflictingActions.isNotEmpty() && !conflictingActions.contains(actionId)) {
                    logger.warn("Shortcut '$shortcutText' is already used by: ${conflictingActions.joinToString()}")
                    // 移除冲突的快捷键
                    conflictingActions.forEach { conflictingActionId ->
                        keymap.removeShortcut(conflictingActionId, KeyboardShortcut(keyStroke, null))
                    }
                }
                
                // 添加新的快捷键
                keymap.addShortcut(actionId, KeyboardShortcut(keyStroke, null))
                logger.info("Set shortcut '$shortcutText' for action $actionId")
            } else {
                logger.warn("Failed to parse shortcut '$shortcutText' for action $actionId")
            }
        } catch (e: Exception) {
            logger.warn("Failed to set shortcut '$shortcutText' for action $actionId", e)
        }
    }
    
    private fun parseKeyStroke(shortcutText: String): KeyStroke? {
        try {
            // 简单的快捷键解析，支持如 "alt shift F1" 格式
            val parts = shortcutText.lowercase().split(" ").map { it.trim() }.filter { it.isNotEmpty() }
            var modifiers = 0
            var keyCode = 0
            
            parts.forEach { part ->
                when (part) {
                    "ctrl", "control" -> modifiers = modifiers or java.awt.event.InputEvent.CTRL_DOWN_MASK
                    "alt" -> modifiers = modifiers or java.awt.event.InputEvent.ALT_DOWN_MASK
                    "shift" -> modifiers = modifiers or java.awt.event.InputEvent.SHIFT_DOWN_MASK
                    "meta", "cmd" -> modifiers = modifiers or java.awt.event.InputEvent.META_DOWN_MASK
                    else -> {
                        // 如果还没有设置主键，尝试解析当前部分作为主键
                        if (keyCode == 0) {
                            keyCode = when {
                                part.startsWith("f") && part.length > 1 -> {
                                    val fNumber = part.substring(1).toIntOrNull()
                                    if (fNumber != null && fNumber in 1..24) {
                                        java.awt.event.KeyEvent.VK_F1 + (fNumber - 1)
                                    } else 0
                                }
                                part.length == 1 && part[0].isLetterOrDigit() -> {
                                    part.uppercase().toCharArray()[0].code
                                }
                                else -> {
                                    // 尝试通过KeyEvent常量获取
                                    try {
                                        java.awt.event.KeyEvent::class.java.getField("VK_${part.uppercase()}").getInt(null)
                                    } catch (e: Exception) {
                                        0
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            return if (keyCode != 0) KeyStroke.getKeyStroke(keyCode, modifiers) else null
        } catch (e: Exception) {
            logger.warn("Failed to parse keystroke: $shortcutText", e)
            return null
        }
    }
    
    companion object {
        fun getInstance(): DynamicActionManager = ApplicationManager.getApplication().getService(DynamicActionManager::class.java)
        
        fun forceRefreshAndDiagnose() {
            try {
                val instance = getInstance()
                instance.logger.info("=== Manual Force Refresh and Diagnosis ===")
                instance.refreshActions()
                
                // 使用定时器延迟检查菜单状态，避免阻塞UI线程
                val timer = javax.swing.Timer(1000) { 
                    instance.checkMenuGroupStatus(ActionManager.getInstance())
                }
                timer.isRepeats = false
                timer.start()
            } catch (e: Exception) {
                println("Failed to force refresh: ${e.message}")
            }
        }
    }
} 