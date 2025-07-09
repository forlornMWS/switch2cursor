package com.github.qczone.switch2cursor.settings

import com.intellij.ui.components.JBTextField
import java.awt.Color
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.KeyStroke

class KeyStrokeRecorderPanel : JBTextField(), KeyListener {
    
    private var isRecording = false
    private var recordedKeyStroke: KeyStroke? = null
    
    init {
        isEditable = false
        addKeyListener(this)
        addFocusListener(object : FocusAdapter() {
            override fun focusGained(e: FocusEvent) {
                startRecording()
            }
            
            override fun focusLost(e: FocusEvent) {
                stopRecording()
            }
        })
        updateDisplay()
    }
    
    private fun startRecording() {
        isRecording = true
        text = "按下想要设置的快捷键..."
        background = Color(255, 255, 200) // 淡黄色背景表示正在录制
        border = BorderFactory.createLineBorder(Color.ORANGE, 2)
        repaint()
    }
    
    private fun stopRecording() {
        isRecording = false
        updateDisplay()
        background = null
        border = null
        repaint()
    }
    
    private fun updateDisplay() {
        if (!isRecording) {
            text = recordedKeyStroke?.let { formatKeyStroke(it) } ?: "点击此处录制快捷键"
        }
    }
    
    fun setKeyStroke(keyStrokeText: String) {
        recordedKeyStroke = parseKeyStrokeFromText(keyStrokeText)
        updateDisplay()
    }
    
    fun getKeyStrokeText(): String {
        return recordedKeyStroke?.let { formatKeyStroke(it) } ?: ""
    }
    
    private fun formatKeyStroke(keyStroke: KeyStroke): String {
        val parts = mutableListOf<String>()
        
        val modifiers = keyStroke.modifiers
        if (modifiers and KeyEvent.CTRL_DOWN_MASK != 0) parts.add("ctrl")
        if (modifiers and KeyEvent.ALT_DOWN_MASK != 0) parts.add("alt")
        if (modifiers and KeyEvent.SHIFT_DOWN_MASK != 0) parts.add("shift")
        if (modifiers and KeyEvent.META_DOWN_MASK != 0) parts.add("meta")
        
        val keyCode = keyStroke.keyCode
        val keyText = when {
            keyCode >= KeyEvent.VK_F1 && keyCode <= KeyEvent.VK_F24 -> {
                "F${keyCode - KeyEvent.VK_F1 + 1}"
            }
            keyCode >= KeyEvent.VK_0 && keyCode <= KeyEvent.VK_9 -> {
                (keyCode - KeyEvent.VK_0).toString()
            }
            keyCode >= KeyEvent.VK_A && keyCode <= KeyEvent.VK_Z -> {
                KeyEvent.getKeyText(keyCode)
            }
            else -> KeyEvent.getKeyText(keyCode)
        }
        
        parts.add(keyText)
        return parts.joinToString(" ")
    }
    
    private fun parseKeyStrokeFromText(text: String): KeyStroke? {
        if (text.isBlank()) return null
        
        try {
            val parts = text.lowercase().split(" ").map { it.trim() }
            var modifiers = 0
            var keyCode = 0
            
            parts.forEach { part ->
                when (part) {
                    "ctrl", "control" -> modifiers = modifiers or KeyEvent.CTRL_DOWN_MASK
                    "alt" -> modifiers = modifiers or KeyEvent.ALT_DOWN_MASK
                    "shift" -> modifiers = modifiers or KeyEvent.SHIFT_DOWN_MASK
                    "meta", "cmd" -> modifiers = modifiers or KeyEvent.META_DOWN_MASK
                    else -> {
                        if (keyCode == 0) { // 只取第一个非修饰键
                            keyCode = when {
                                part.matches(Regex("f\\d+")) -> {
                                    val fNumber = part.substring(1).toIntOrNull()
                                    if (fNumber != null && fNumber in 1..24) {
                                        KeyEvent.VK_F1 + (fNumber - 1)
                                    } else 0
                                }
                                part.length == 1 && part[0].isLetterOrDigit() -> {
                                    part.uppercase().toCharArray()[0].code
                                }
                                else -> {
                                    // 尝试通过KeyEvent常量获取
                                    try {
                                        KeyEvent::class.java.getField("VK_${part.uppercase()}").getInt(null)
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
            return null
        }
    }
    
    override fun keyTyped(e: KeyEvent) {
        // 不处理
    }
    
    override fun keyPressed(e: KeyEvent) {
        if (!isRecording) return
        
        e.consume() // 阻止事件传播
        
        // 忽略单独的修饰键
        if (e.keyCode in listOf(KeyEvent.VK_CONTROL, KeyEvent.VK_ALT, KeyEvent.VK_SHIFT, KeyEvent.VK_META)) {
            return
        }
        
        // 记录按键组合
        recordedKeyStroke = KeyStroke.getKeyStroke(e.keyCode, e.modifiersEx)
        updateDisplay()
        
        // 延迟结束录制，让用户看到结果
        javax.swing.Timer(500) {
            transferFocus() // 移动焦点到下一个组件
        }.apply {
            isRepeats = false
            start()
        }
    }
    
    override fun keyReleased(e: KeyEvent) {
        if (isRecording) {
            e.consume()
        }
    }
    
    fun clearKeyStroke() {
        recordedKeyStroke = null
        updateDisplay()
    }
} 