package com.github.qczone.switch2cursor.settings

import com.github.qczone.switch2cursor.actions.DynamicActionManager
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*
import javax.swing.table.AbstractTableModel

class AppSettingsConfigurable : Configurable {
    private var mySettingsComponent: AppSettingsComponent? = null

    override fun getDisplayName(): String = "Switch2Cursor"

    override fun createComponent(): JComponent {
        mySettingsComponent = AppSettingsComponent()
        return mySettingsComponent!!.panel
    }

    override fun isModified(): Boolean {
        val settings = AppSettingsState.getInstance()
        return mySettingsComponent!!.isModified(settings)
    }

    override fun apply() {
        val settings = AppSettingsState.getInstance()
        mySettingsComponent!!.apply(settings)
        
        // 刷新动态 actions
        DynamicActionManager.getInstance().refreshActions()
        
        // 显示应用成功的消息
        com.intellij.openapi.ui.Messages.showInfoMessage(
            "配置已更新，菜单和快捷键将在几秒内生效。",
            "Switch2Cursor"
        )
    }

    override fun reset() {
        val settings = AppSettingsState.getInstance()
        mySettingsComponent!!.reset(settings)
    }

    override fun disposeUIResources() {
        mySettingsComponent = null
    }
}

class AppSettingsComponent {
    val panel: JPanel = JPanel(BorderLayout())
    private val tableModel = AppConfigTableModel()
    private val table = JBTable(tableModel)
    
    init {
        setupUI()
    }
    
    private fun setupUI() {
        val topPanel = JPanel(BorderLayout())
        topPanel.add(JBLabel("配置应用程序:"), BorderLayout.NORTH)
        
        // 表格设置
        table.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        table.preferredScrollableViewportSize = Dimension(800, 300)
        
        val scrollPane = JBScrollPane(table)
        topPanel.add(scrollPane, BorderLayout.CENTER)
        
        // 按钮面板
        val buttonPanel = JPanel()
        val addButton = JButton("添加应用")
        val editButton = JButton("编辑")
        val deleteButton = JButton("删除")
        val resetButton = JButton("重置为默认")
        
        addButton.addActionListener { addApp() }
        editButton.addActionListener { editSelectedApp() }
        deleteButton.addActionListener { deleteSelectedApp() }
        resetButton.addActionListener { resetToDefaults() }
        
        buttonPanel.add(addButton)
        buttonPanel.add(editButton)
        buttonPanel.add(deleteButton)
        buttonPanel.add(resetButton)
        
        topPanel.add(buttonPanel, BorderLayout.SOUTH)
        
        // 说明信息
        val infoPanel = FormBuilder.createFormBuilder()
            .addComponent(JBLabel("<html><b>说明:</b><br>" +
                "• 快捷键录制：点击快捷键输入框，然后按下想要设置的快捷键组合<br>" +
                "• 参数模板：{file} = 文件路径, {line} = 行号, {column} = 列号, {project} = 项目路径<br>" +
                "• 自定义协议：某些应用支持如 cursor://file 这样的协议<br>" +
                "• 修改配置后会立即生效，菜单和快捷键会自动更新<br>" +
                "• 启用/禁用应用会实时更新菜单项的显示状态</html>"))
            .panel
            
        panel.add(topPanel, BorderLayout.CENTER)
        panel.add(infoPanel, BorderLayout.SOUTH)
    }
    
    private fun addApp() {
        val dialog = AppConfigDialog()
        if (dialog.showAndGet()) {
            val config = dialog.getAppConfig()
            // 检查应用名称是否已存在
            if (tableModel.appConfigs.any { it.name == config.name }) {
                Messages.showErrorDialog(panel, "应用名称 '${config.name}' 已存在，请使用不同的名称", "错误")
                return
            }
            tableModel.addApp(config)
        }
    }
    
    private fun editSelectedApp() {
        val selectedRow = table.selectedRow
        if (selectedRow >= 0) {
            val config = tableModel.getAppAt(selectedRow)
            val dialog = AppConfigDialog(config)
            if (dialog.showAndGet()) {
                val updatedConfig = dialog.getAppConfig()
                // 检查应用名称是否与其他应用冲突
                if (updatedConfig.name != config.name && 
                    tableModel.appConfigs.any { it.name == updatedConfig.name }) {
                    Messages.showErrorDialog(panel, "应用名称 '${updatedConfig.name}' 已存在，请使用不同的名称", "错误")
                    return
                }
                tableModel.updateApp(selectedRow, updatedConfig)
            }
        } else {
            Messages.showInfoMessage(panel, "请先选择要编辑的应用", "提示")
        }
    }
    
    private fun deleteSelectedApp() {
        val selectedRow = table.selectedRow
        if (selectedRow >= 0) {
            val config = tableModel.getAppAt(selectedRow)
            val result = Messages.showYesNoDialog(
                panel,
                "确定要删除应用 \"${config.displayName}\" 吗？",
                "确认删除",
                Messages.getQuestionIcon()
            )
            if (result == Messages.YES) {
                tableModel.removeApp(selectedRow)
            }
        } else {
            Messages.showInfoMessage(panel, "请先选择要删除的应用", "提示")
        }
    }
    
    private fun resetToDefaults() {
        val result = Messages.showYesNoDialog(
            "这将重置为默认的应用配置，确定继续吗？",
            "确认重置",
            Messages.getQuestionIcon()
        )
        if (result == Messages.YES) {
            tableModel.resetToDefaults()
        }
    }
    
    fun isModified(settings: AppSettingsState): Boolean {
        return tableModel.appConfigs != settings.appConfigs
    }
    
    fun apply(settings: AppSettingsState) {
        settings.appConfigs.clear()
        settings.appConfigs.addAll(tableModel.appConfigs)
    }
    
    fun reset(settings: AppSettingsState) {
        // 确保设置已经正确初始化
        if (settings.appConfigs.isEmpty()) {
            settings.appConfigs.addAll(AppConfig.getDefaultApps())
        }
        tableModel.setAppConfigs(settings.appConfigs.toMutableList())
    }
}

class AppConfigTableModel : AbstractTableModel() {
    var appConfigs: MutableList<AppConfig> = mutableListOf()
        private set
    
    private val columnNames = arrayOf("启用", "显示名称", "应用名称", "打开文件快捷键", "打开项目快捷键", "可执行文件路径")
    
    override fun getRowCount(): Int = appConfigs.size
    override fun getColumnCount(): Int = columnNames.size
    override fun getColumnName(column: Int): String = columnNames[column]
    
    override fun getColumnClass(columnIndex: Int): Class<*> = when (columnIndex) {
        0 -> Boolean::class.java
        else -> String::class.java
    }
    
    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = columnIndex == 0
    
    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        val config = appConfigs[rowIndex]
        return when (columnIndex) {
            0 -> config.isEnabled
            1 -> config.displayName
            2 -> config.name
            3 -> config.openFileShortcut
            4 -> config.openProjectShortcut
            5 -> config.executablePath
            else -> ""
        }
    }
    
    override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
        if (columnIndex == 0 && aValue is Boolean) {
            appConfigs[rowIndex].isEnabled = aValue
            fireTableCellUpdated(rowIndex, columnIndex)
            
            // 立即刷新动态菜单
            try {
                DynamicActionManager.getInstance().refreshActions()
            } catch (e: Exception) {
                // 忽略可能的异常，避免影响用户操作
            }
        }
    }
    
    fun addApp(config: AppConfig) {
        appConfigs.add(config)
        fireTableRowsInserted(appConfigs.size - 1, appConfigs.size - 1)
    }
    
    fun removeApp(index: Int) {
        appConfigs.removeAt(index)
        fireTableRowsDeleted(index, index)
    }
    
    fun updateApp(index: Int, config: AppConfig) {
        appConfigs[index] = config
        fireTableRowsUpdated(index, index)
    }
    
    fun getAppAt(index: Int): AppConfig = appConfigs[index]
    
    fun setAppConfigs(configs: MutableList<AppConfig>) {
        appConfigs = configs
        fireTableDataChanged()
    }
    
    fun resetToDefaults() {
        appConfigs.clear()
        appConfigs.addAll(AppConfig.getDefaultApps())
        fireTableDataChanged()
    }
} 