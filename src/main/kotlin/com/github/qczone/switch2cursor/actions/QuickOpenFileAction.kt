package com.github.qczone.switch2cursor.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.ActionUpdateThread

class QuickOpenFileAction : AnAction() {
    
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
    
    override fun actionPerformed(e: AnActionEvent) {
        QuickOpenFileDialog.show(e)
    }
    
    override fun update(e: AnActionEvent) {
        val project = e.project
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        
        e.presentation.isEnabledAndVisible = project != null && 
                                           virtualFile != null && 
                                           !virtualFile.isDirectory
    }
} 