package com.github.qczone.switch2cursor.actions

import com.intellij.openapi.actionSystem.AnActionEvent

class OpenProjectInVSCodeAction : BaseOpenAction("vscode") {
    override fun actionPerformed(e: AnActionEvent) {
        openProject(e)
    }

    override fun update(e: AnActionEvent) {
        updateProjectAction(e)
    }
} 