package com.github.qczone.switch2cursor.actions

import com.intellij.openapi.actionSystem.AnActionEvent

class OpenFileInTraeCNAction : BaseOpenAction("trae_cn") {
    override fun actionPerformed(e: AnActionEvent) {
        openFile(e)
    }

    override fun update(e: AnActionEvent) {
        updateFileAction(e)
    }
} 