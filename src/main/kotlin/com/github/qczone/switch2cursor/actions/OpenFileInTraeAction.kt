package com.github.qczone.switch2cursor.actions

import com.intellij.openapi.actionSystem.AnActionEvent

class OpenFileInTraeAction : BaseOpenAction("trae") {
    override fun actionPerformed(e: AnActionEvent) {
        openFile(e)
    }

    override fun update(e: AnActionEvent) {
        updateFileAction(e)
    }
} 