package com.github.maoabc.myims.ime

import android.view.inputmethod.EditorInfo
import androidx.collection.ObjectList
import androidx.collection.mutableObjectListOf
import com.github.maoabc.librime.RimeCandidate

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
    val candidates: ObjectList<RimeCandidate> = mutableObjectListOf(),
    val isCandidatesExpanded: Boolean = false,
    val isEmojiPanelOpen: Boolean = false,
    val isTextEditPanelOpen: Boolean = false,
    val isTextSelecting: Boolean = false,
    val isMenuPanelOpen: Boolean = false,
    val options: KeyboardOptions = KeyboardOptions(),
    val bottomInsetDp: Int = 0,
    val imeOptions: Int = EditorInfo.IME_ACTION_UNSPECIFIED,
    val languageLabel: String = "en"
)
