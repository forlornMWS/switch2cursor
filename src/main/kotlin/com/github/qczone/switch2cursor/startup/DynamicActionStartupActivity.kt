package com.github.qczone.switch2cursor.startup

import com.github.qczone.switch2cursor.actions.DynamicActionManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class DynamicActionStartupActivity : ProjectActivity {
    
    override suspend fun execute(project: Project) {
        // 初始化动态 action 管理器
        DynamicActionManager.getInstance()
    }
} 