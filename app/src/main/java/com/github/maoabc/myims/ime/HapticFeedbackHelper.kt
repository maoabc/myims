package com.github.maoabc.myims.ime

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

object HapticFeedbackHelper {
    fun perform(haptic: HapticFeedback, type: HapticFeedbackType = HapticFeedbackType.TextHandleMove) {
        if (KeyboardPreferencesManager.options.vibrationEnabled) {
            haptic.performHapticFeedback(type)
        }
    }

    fun performTap(haptic: HapticFeedback) {
        perform(haptic, HapticFeedbackType.TextHandleMove)
    }

    fun performLongPress(haptic: HapticFeedback) {
        perform(haptic, HapticFeedbackType.LongPress)
    }
}
