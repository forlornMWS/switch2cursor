package com.github.qczone.switch2cursor.settings

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*

class AppConfigDialog(private val existingConfig: AppConfig? = null) : DialogWrapper(true) {
    
    private val nameField = JBTextField()
    private val displayNameField = JBTextField()
    private val executablePathField = JBTextField()
    private val openFileShortcutField = KeyStrokeRecorderPanel()
    private val openProjectShortcutField = KeyStrokeRecorderPanel()
    private val openFileArgsField = JBTextField()
    private val openProjectArgsField = JBTextField()
    private val isEnabledCheckBox = JBCheckBox("启用此应用")
    private val useCustomProtocolCheckBox = JBCheckBox("使用自定义协议")
    private val protocolPrefixField = JBTextField()
    
    init {
        title = if (existingConfig != null) "编辑应用配置" else "添加应用配置"
        init()
        loadConfig()
    }
    
    private fun loadConfig() {
        existingConfig?.let { config ->
            nameField.text = config.name
            displayNameField.text = config.displayName
            executablePathField.text = config.executablePath
            openFileShortcutField.setKeyStroke(config.openFileShortcut)
            openProjectShortcutField.setKeyStroke(config.openProjectShortcut)
            openFileArgsField.text = config.openFileArgs
            openProjectArgsField.text = config.openProjectArgs
            isEnabledCheckBox.isSelected = config.isEnabled
            useCustomProtocolCheckBox.isSelected = config.useCustomProtocol
            protocolPrefixField.text = config.protocolPrefix
        } ?: run {
            // 设置默认值
            openFileArgsField.text = "--goto {file}:{line}:{column}"
            openProjectArgsField.text = "{project}"
            isEnabledCheckBox.isSelected = true
        }
        
        updateProtocolFieldsVisibility()
        useCustomProtocolCheckBox.addActionListener { updateProtocolFieldsVisibility() }
    }
    
    private fun updateProtocolFieldsVisibility() {
        protocolPrefixField.isEnabled = useCustomProtocolCheckBox.isSelected
    }
    
    override fun createCenterPanel(): JComponent {
        // 创建快捷键输入面板（包含清除按钮）
        val createShortcutPanel = { recorder: KeyStrokeRecorderPanel ->
            val panel = JPanel(GridBagLayout())
            val gbc = GridBagConstraints()
            
            gbc.gridx = 0
            gbc.gridy = 0
            gbc.weightx = 1.0
            gbc.fill = GridBagConstraints.HORIZONTAL
            panel.add(recorder, gbc)
            
            gbc.gridx = 1
            gbc.weightx = 0.0
            gbc.fill = GridBagConstraints.NONE
            gbc.insets = java.awt.Insets(0, 5, 0, 0)
            val clearButton = JButton("清除")
            clearButton.addActionListener { recorder.clearKeyStroke() }
            panel.add(clearButton, gbc)
            
            panel
        }
        
        val fileShortcutPanel = createShortcutPanel(openFileShortcutField)
        val projectShortcutPanel = createShortcutPanel(openProjectShortcutField)
        
        val panel = FormBuilder.createFormBuilder()
            .addLabeledComponent("应用名称（唯一标识）:", nameField)
            .addLabeledComponent("显示名称:", displayNameField)
            .addLabeledComponent("可执行文件路径:", executablePathField)
            .addSeparator()
            .addLabeledComponent("打开文件快捷键:", fileShortcutPanel)
            .addLabeledComponent("打开项目快捷键:", projectShortcutPanel)
            .addSeparator()
            .addLabeledComponent("打开文件参数模板:", openFileArgsField)
            .addLabeledComponent("打开项目参数模板:", openProjectArgsField)
            .addSeparator()
            .addComponent(useCustomProtocolCheckBox)
            .addLabeledComponent("协议前缀:", protocolPrefixField)
            .addSeparator()
            .addComponent(isEnabledCheckBox)
            .addComponentFillVertically(JPanel(), 0)
            .panel
            
        // 添加提示信息
        val hintLabel = JBLabel("<html><small>" +
            "提示：<br>" +
            "• 应用名称必须唯一，建议使用小写英文<br>" +
            "• 参数模板中可使用 {file}, {line}, {column}, {project} 占位符<br>" +
            "• 自定义协议如：cursor:// 或 vscode://<br>" +
            "• 快捷键录制：点击快捷键输入框，然后按下想要设置的快捷键组合<br>" +
            "• 可使用清除按钮移除快捷键设置，通过菜单访问" +
            "</small></html>")
        
        val mainPanel = JPanel()
        mainPanel.layout = java.awt.BorderLayout()
        mainPanel.add(panel, java.awt.BorderLayout.CENTER)
        mainPanel.add(hintLabel, java.awt.BorderLayout.SOUTH)
        
        return mainPanel
    }
    
    override fun doValidate(): ValidationInfo? {
        if (nameField.text.isBlank()) {
            return ValidationInfo("应用名称不能为空", nameField)
        }
        
        if (displayNameField.text.isBlank()) {
            return ValidationInfo("显示名称不能为空", displayNameField)
        }
        
        if (executablePathField.text.isBlank()) {
            return ValidationInfo("可执行文件路径不能为空", executablePathField)
        }
        
        if (openFileArgsField.text.isBlank()) {
            return ValidationInfo("打开文件参数模板不能为空", openFileArgsField)
        }
        
        if (openProjectArgsField.text.isBlank()) {
            return ValidationInfo("打开项目参数模板不能为空", openProjectArgsField)
        }
        
        return null
    }
    
    fun getAppConfig(): AppConfig {
        return AppConfig(
            name = nameField.text.trim(),
            displayName = displayNameField.text.trim(),
            executablePath = executablePathField.text.trim(),
            openFileShortcut = openFileShortcutField.getKeyStrokeText(),
            openProjectShortcut = openProjectShortcutField.getKeyStrokeText(),
            openFileArgs = openFileArgsField.text.trim(),
            openProjectArgs = openProjectArgsField.text.trim(),
            isEnabled = isEnabledCheckBox.isSelected,
            useCustomProtocol = useCustomProtocolCheckBox.isSelected,
            protocolPrefix = protocolPrefixField.text.trim()
        )
    }
} 