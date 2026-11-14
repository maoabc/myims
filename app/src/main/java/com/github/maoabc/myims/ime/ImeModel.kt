package com.github.maoabc.myims.ime

import android.view.inputmethod.EditorInfo
import androidx.collection.ObjectList
import androidx.collection.emptyObjectList
import com.github.maoabc.librime.RimeCandidate
import com.github.maoabc.librime.RimeContext

enum class KeyboardMode {
    QWERTY,
    NUMBERS,
    SYMBOLS
}

enum class KeyboardPanelType {
    STANDARD,
    EXPANDED_CANDIDATES,
    EMOJI,
    TEXT_EDIT,
    MENU
}

enum class ShiftState {
    LOWERCASE,
    SHIFTED,
    CAPS_LOCK;

    fun isCapitalized(): Boolean = this == SHIFTED || this == CAPS_LOCK
}

data class ImeUiState(
    val keyboardMode: KeyboardMode = KeyboardMode.QWERTY,
    val shiftState: ShiftState = ShiftState.LOWERCASE,
    val composingText: String = "",
    val candidates: ObjectList<RimeCandidate> = emptyObjectList(),
    val isCandidatesExpanded: Boolean = false,
    val isEmojiOpen: Boolean = false,
    val isTextEditOpen: Boolean = false,
    val isMenuOpen: Boolean = false,
    val isTextSelecting: Boolean = false,
    val options: KeyboardOptions = KeyboardOptions(),
    val bottomInsetDp: Int = 0,
    val imeOptions: Int = EditorInfo.IME_ACTION_UNSPECIFIED,
    val rimeContext: RimeContext? = null,
    val languageLabel: String = "en"
)
