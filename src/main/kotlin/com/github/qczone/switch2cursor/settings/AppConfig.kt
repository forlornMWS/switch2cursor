package com.github.qczone.switch2cursor.settings

data class AppConfig(
    var name: String = "",
    var displayName: String = "",
    var executablePath: String = "",
    var isEnabled: Boolean = true,
    var openFileShortcut: String = "",
    var openProjectShortcut: String = "",
    var openFileArgs: String = "--goto {file}:{line}:{column}",
    var openProjectArgs: String = "{project}",
    var useCustomProtocol: Boolean = false,
    var protocolPrefix: String = ""
) {
    companion object {
        fun getDefaultApps(): List<AppConfig> {
            return listOf(
                AppConfig(
                    name = "cursor",
                    displayName = "Cursor",
                    executablePath = "cursor",
                    openFileShortcut = "alt shift F1",
                    openProjectShortcut = "alt shift ctrl F1",
                    openFileArgs = "--goto {file}:{line}:{column}",
                    openProjectArgs = "{project}",
                    useCustomProtocol = false,
                    protocolPrefix = "cursor://"
                ),
                AppConfig(
                    name = "vscode",
                    displayName = "VSCode",
                    executablePath = "code",
                    openFileShortcut = "alt shift F2",
                    openProjectShortcut = "alt shift ctrl F2",
                    openFileArgs = "--goto {file}:{line}:{column}",
                    openProjectArgs = "{project}"
                ),
                AppConfig(
                    name = "trae",
                    displayName = "Trae",
                    executablePath = "trae",
                    openFileShortcut = "alt shift F3",
                    openProjectShortcut = "alt shift ctrl F3",
                    openFileArgs = "--goto {file}:{line}:{column}",
                    openProjectArgs = "{project}"
                ),
                AppConfig(
                    name = "trae_cn",
                    displayName = "Trae CN",
                    executablePath = "trae-cn",
                    openFileShortcut = "alt shift F4",
                    openProjectShortcut = "alt shift ctrl F4",
                    openFileArgs = "--goto {file}:{line}:{column}",
                    openProjectArgs = "{project}"
                )
            )
        }
    }
} 