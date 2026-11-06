package com.github.maoabc.myims.ui.keyboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.KeyboardCapslock
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.github.maoabc.myims.R
import com.github.maoabc.myims.ime.KeyData
import com.github.maoabc.myims.ime.KeyType
import com.github.maoabc.myims.ime.ShiftState
import com.github.maoabc.myims.ui.theme.GboardTheme
import com.github.maoabc.myims.ui.theme.LocalKeyboardColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun KeyButton(
    keyData: KeyData,
    shiftState: ShiftState,
    langLabel: String,
    asciiPunct: Boolean,
    modifier: Modifier = Modifier,
    onKeyClick: (KeyData) -> Unit,
    onKeyLongPress: (KeyData) -> Unit = {}
) {
    val keyboardColors = LocalKeyboardColors.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }

    val displayText = remember(keyData, shiftState) {
        if (keyData.type == KeyType.CHARACTER && shiftState.isCapitalized()) {
            keyData.primaryText.uppercase()
        } else {
            keyData.primaryText
        }
    }

    val targetColors = when (keyData.type) {
        KeyType.CHARACTER, KeyType.SPACE -> Pair(
            if (isPressed) keyboardColors.keyPressedBackground else keyboardColors.keyBackground,
            keyboardColors.keyText
        )

        KeyType.ENTER -> Pair(
            if (isPressed) keyboardColors.keyPrimaryActionBackground.copy(alpha = 0.8f) else keyboardColors.keyPrimaryActionBackground,
            keyboardColors.keyPrimaryActionText
        )

        KeyType.SHIFT -> Pair(
            if (shiftState != ShiftState.LOWERCASE) keyboardColors.keyPressedBackground else keyboardColors.keyActionBackground,
            if (shiftState != ShiftState.LOWERCASE) keyboardColors.keyPrimaryActionBackground else keyboardColors.keyActionText
        )

        else -> Pair(
            if (isPressed) keyboardColors.keyPressedBackground else keyboardColors.keyActionBackground,
            keyboardColors.keyActionText
        )
    }

    val animatedBgColor by animateColorAsState(
        targetValue = targetColors.first,
        animationSpec = tween(durationMillis = 40),
        label = "KeyBgAnimation"
    )
    val textColor = targetColors.second

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 3.dp, vertical = 4.dp)
            .shadow(1.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(animatedBgColor)
            .pointerInput(keyData, shiftState) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
//                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        var repeatJob: Job? = null
                        if (keyData.type == KeyType.BACKSPACE) {
                            repeatJob = coroutineScope.launchRepeatAction {
                                onKeyClick(keyData)
                            }
                        }

                        try {
                            tryAwaitRelease()
                        } finally {
                            repeatJob?.cancel()
                            isPressed = false
                        }
                    },
                    onTap = {
                        if (keyData.type != KeyType.BACKSPACE) {
                            onKeyClick(
                                if (keyData.type == KeyType.CHARACTER && shiftState.isCapitalized()) {
                                    keyData.copy(primaryText = keyData.primaryText.uppercase())
                                } else keyData
                            )
                        }
                    },
                    onLongPress = {
                        if (keyData.type != KeyType.BACKSPACE) {
//                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onKeyLongPress(keyData)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Main key content
        when (keyData.type) {
            KeyType.SHIFT -> {
                val icon = if (shiftState == ShiftState.CAPS_LOCK) {
                    Icons.Default.KeyboardCapslock
                } else {
                    Icons.Default.ArrowUpward
                }
                Icon(
                    imageVector = icon,
                    contentDescription = "Shift",
                    tint = textColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            KeyType.BACKSPACE -> {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    tint = textColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            KeyType.ENTER -> {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardReturn,
                    contentDescription = "Enter",
                    tint = textColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            KeyType.LANGUAGE -> {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = "Language",
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            KeyType.SPACE -> {
                Text(
                    text = stringResource(if (langLabel == "en") R.string.space_english else R.string.space_pinyin),
                    color = keyboardColors.keySecondaryText,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }

            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (displayText == "." && !asciiPunct) "◦" else displayText,
                        color = textColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )

                    // Secondary text hint (small number/symbol on top right)
                    if (!keyData.secondaryText.isNullOrEmpty()) {
                        Text(
                            text = keyData.secondaryText,
                            color = keyboardColors.keySecondaryText,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 2.dp, end = 5.dp)
                        )
                    }
                }
            }
        }

    }
}

@Preview(showBackground = true)
@Composable
fun KeyButtonPreview() {
    GboardTheme {
        KeyButton(
            keyData = KeyData(KeyType.CHARACTER, "a", "1"),
            shiftState = ShiftState.LOWERCASE,
            langLabel = "en",
            asciiPunct = false,
            onKeyClick = {}
        )
    }
}
