package com.github.maoabc.myims.ui.keyboard

import androidx.collection.mutableObjectListOf
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.maoabc.librime.RimeCandidate
import com.github.maoabc.myims.ime.ImeActionHandler
import com.github.maoabc.myims.ime.ImeUiState
import com.github.maoabc.myims.ime.KeyData
import com.github.maoabc.myims.ime.KeyType
import com.github.maoabc.myims.ime.KeyboardLayouts
import com.github.maoabc.myims.ime.KeyboardMode
import com.github.maoabc.myims.ime.KeyboardPanelType
import com.github.maoabc.myims.ui.theme.GboardTheme
import com.github.maoabc.myims.ui.theme.LocalKeyboardColors

@Composable
fun GboardKeyboard(
    uiState: ImeUiState,
    actionHandler: ImeActionHandler,
    modifier: Modifier = Modifier
) {
    GboardTheme {
        val keyboardColors = LocalKeyboardColors.current

        Surface(
            modifier = modifier
                .fillMaxWidth()
                .focusProperties { canFocus = false },
            color = keyboardColors.keyboardBackground
        ) {
            var visibleCandidateCount by remember { mutableIntStateOf(0) }
            val density = LocalDensity.current
            val navInsetsBottomDp =
                with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
            val effectiveBottomPadding = maxOf(uiState.bottomInsetDp.dp, navInsetsBottomDp, 48.dp)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp)
                    .padding(top = 4.dp, bottom = effectiveBottomPadding)
            ) {
                // Candidate / Tool bar at the top
                CandidateBar(
                    composingText = uiState.composingText,
                    candidates = uiState.candidates,
                    isExpanded = uiState.isCandidatesExpanded,
                    onCandidateClick = { candidate ->
                        actionHandler.onCandidateSelect(candidate)
                    },
                    onToggleExpand = {
                        actionHandler.onToggleCandidatesExpand()
                    },
                    onToggleEmojiPanel = {
                        actionHandler.onToggleEmojiPanel()
                    },
                    onToggleTextEditPanel = {
                        actionHandler.onToggleTextEditPanel()
                    },
                    onToggleMenuPanel = {
                        actionHandler.onToggleMenuPanel()
                    },
                    onOpenAdvancedSettings = {
                        actionHandler.onOpenAdvancedSettings()
                    },
                    isTextEditOpen = uiState.isTextEditPanelOpen,
                    isEmojiOpen = uiState.isEmojiPanelOpen,
                    isMenuOpen = uiState.isMenuPanelOpen,
                    onVisibleCountChanged = { count ->
                        visibleCandidateCount = count
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Standard Keyboard Keys Layout / Dynamic Height Calculation
                val rows = when (uiState.keyboardMode) {
                    KeyboardMode.QWERTY -> KeyboardLayouts.qwertyRows
                    KeyboardMode.NUMBERS -> KeyboardLayouts.numbersRows
                    KeyboardMode.SYMBOLS -> KeyboardLayouts.symbolsRows
                }
                val keyRowHeight = 60.dp
                val totalKeysHeight = keyRowHeight * rows.size

                AnimatedContent(
                    targetState = when {
                        uiState.isTextEditPanelOpen -> KeyboardPanelType.TEXT_EDIT
                        uiState.isEmojiPanelOpen -> KeyboardPanelType.EMOJI
                        uiState.isMenuPanelOpen -> KeyboardPanelType.MENU
                        uiState.isCandidatesExpanded && uiState.candidates.isNotEmpty() -> KeyboardPanelType.EXPANDED_CANDIDATES
                        else -> KeyboardPanelType.STANDARD
                    },
                    transitionSpec = {
                        val aniDurationMillis = 150
                        (slideInVertically(animationSpec = tween(aniDurationMillis)) { it / 2 } +
                                fadeIn(animationSpec = tween(aniDurationMillis))) togetherWith
                                (slideOutVertically(animationSpec = tween(aniDurationMillis)) { -it } +
                                        fadeOut(animationSpec = tween(aniDurationMillis)))
                    },
                    label = "PanelAnimation",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(totalKeysHeight)
                ) { panelType ->
                    when (panelType) {
                        KeyboardPanelType.MENU -> {
                            MenuPanel(
                                asciiPunct = uiState.asciiPunct,
                                fullShape = uiState.fullShape,
                                simplified = uiState.tradition,
                                emoji = uiState.emoji,
                                onToggleOption = { option -> actionHandler.onToggleRimeOption(option) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        KeyboardPanelType.TEXT_EDIT -> {
                            TextMovePanel(
                                onAction = { command -> actionHandler.onTextEditCommand(command) },
                                onDelete = {
                                    actionHandler.onKeyClick(
                                        KeyData(
                                            KeyType.BACKSPACE,
                                            "Del"
                                        )
                                    )
                                },
                                isSelecting = uiState.isTextSelecting,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        KeyboardPanelType.EMOJI -> {
                            EmojiPanel(
                                onEmojiSelect = { emoji -> actionHandler.onEmojiSelect(emoji) },
                                onDelete = {
                                    actionHandler.onKeyClick(
                                        KeyData(
                                            KeyType.BACKSPACE,
                                            "Del"
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        KeyboardPanelType.EXPANDED_CANDIDATES -> {
                            // Expanded Candidates Grid Panel View
                            val fromIndex =
                                visibleCandidateCount.coerceIn(0, uiState.candidates.size)
                            ExpandedCandidatesPanel(
                                candidates = uiState.candidates,
                                fromIndex = fromIndex,
                                onCandidateClick = { candidate ->
                                    actionHandler.onCandidateSelect(candidate)
                                },
                                onKeyClick = { keyData ->
                                    actionHandler.onKeyClick(keyData)
                                },
                                onLoadMore = {
                                    actionHandler.onLoadMoreCandidates()
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        KeyboardPanelType.STANDARD -> {
                            // Standard Keyboard Keys Layout
                            Column(modifier = Modifier.fillMaxSize()) {
                                rows.forEach { rowKeys ->
                                    val isNineLetterRow =
                                        rowKeys.size == 9 && rowKeys.all { it.type == KeyType.CHARACTER }
                                    val rowPadding = if (isNineLetterRow) 16.dp else 0.dp

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(keyRowHeight)
                                            .padding(horizontal = rowPadding)
                                    ) {
                                        rowKeys.forEach { keyData ->
                                            KeyButton(
                                                keyData = keyData,
                                                shiftState = uiState.shiftState,
                                                langLabel = uiState.languageLabel,
                                                asciiPunct = uiState.asciiPunct,
                                                modifier = Modifier.weight(keyData.weight),
                                                onKeyClick = { clickedKey ->
                                                    actionHandler.onKeyClick(clickedKey)
                                                },
                                                onKeyLongPress = { longPressedKey ->
                                                    actionHandler.onKeyLongPress(longPressedKey)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GboardKeyboardPreview() {
    GboardTheme {
        GboardKeyboard(
            uiState = ImeUiState(
                composingText = "hello",
                languageLabel = "en",
                candidates = mutableObjectListOf(
                    RimeCandidate("hello"), RimeCandidate("help"), RimeCandidate("hell"),
                    RimeCandidate("world"), RimeCandidate("work"), RimeCandidate("word"),
                    RimeCandidate("code"), RimeCandidate("compose"), RimeCandidate("core")
                ),
                isCandidatesExpanded = false
            ),
            actionHandler = object : ImeActionHandler {
                override fun onKeyClick(keyData: KeyData) {}
                override fun onKeyLongPress(keyData: KeyData) {}
                override fun onCandidateSelect(candidate: RimeCandidate) {}
                override fun onToggleCandidatesExpand() {}
                override fun onLanguageSwitch() {}
                override fun onClearComposing() {}
            }
        )
    }
}
