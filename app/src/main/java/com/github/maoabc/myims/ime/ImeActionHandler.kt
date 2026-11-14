package com.github.maoabc.myims.ime

import com.github.maoabc.myims.ui.keyboard.TextMoveCommand

interface ImeActionHandler {
    fun onKeyClick(keyData: KeyData)
    fun onKeyLongPress(keyData: KeyData)
    fun onCandidateSelect(canIdx: Int, pageIdx: Int = -1)
    fun onToggleCandidatesExpand()
    fun onLanguageSwitch()
    fun onClearComposing()
    fun onLoadMoreCandidates() {}
    fun onToggleEmojiPanel() {}
    fun onEmojiSelect(emoji: String) {}
    fun onToggleTextEditPanel() {}
    fun onTextEditCommand(command: TextMoveCommand) {}
    fun onToggleMenuPanel() {}
    fun onToggleRimeOption(option: String) {}
    fun onOpenAdvancedSettings() {}
}
