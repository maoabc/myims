package com.github.maoabc.myims.ui.keyboard

import androidx.collection.mutableObjectListOf
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.github.maoabc.librime.RimeCandidate
import com.github.maoabc.myims.ime.ImeUiState
import com.github.maoabc.myims.ui.theme.GboardTheme
import com.github.maoabc.myims.ui.theme.LocalKeyboardColors

@Composable
fun TextEditIcon(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val strokeWidth = 2.dp.toPx()

        // Vertical I-beam line
        drawLine(
            color = tint,
            start = Offset(width / 2f, height * 0.2f),
            end = Offset(width / 2f, height * 0.8f),
            strokeWidth = strokeWidth
        )
        // Top and bottom horizontal caps
        drawLine(
            color = tint,
            start = Offset(width * 0.35f, height * 0.2f),
            end = Offset(width * 0.65f, height * 0.2f),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = tint,
            start = Offset(width * 0.35f, height * 0.8f),
            end = Offset(width * 0.65f, height * 0.8f),
            strokeWidth = strokeWidth
        )

        // Left arrow pointing outwards (left)
        val pathLeft = Path().apply {
            moveTo(width * 0.15f, height * 0.5f)
            lineTo(width * 0.3f, height * 0.32f)
            moveTo(width * 0.15f, height * 0.5f)
            lineTo(width * 0.3f, height * 0.68f)
        }
        drawPath(pathLeft, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))

        // Right arrow pointing outwards (right)
        val pathRight = Path().apply {
            moveTo(width * 0.85f, height * 0.5f)
            lineTo(width * 0.7f, height * 0.32f)
            moveTo(width * 0.85f, height * 0.5f)
            lineTo(width * 0.7f, height * 0.68f)
        }
        drawPath(
            pathRight,
            color = tint,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun CandidateBar(
    uiState: ImeUiState,
    onCandidateClick: (RimeCandidate) -> Unit,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier,
    onDeleteCandidate: (RimeCandidate) -> Unit = {},
    onToggleEmojiPanel: () -> Unit = {},
    onToggleTextEditPanel: () -> Unit = {},
    onToggleMenuPanel: () -> Unit = {},
    onOpenAdvancedSettings: () -> Unit = {},
    onVisibleCountChanged: (Int) -> Unit = {},
) {
    val composingText = uiState.composingText
    val candidates = uiState.candidates
    val isExpanded = uiState.isCandidatesExpanded
    val keyboardColors = LocalKeyboardColors.current
    var renderedCount by remember { mutableIntStateOf(candidates.size) }

    SideEffect {
        onVisibleCountChanged(renderedCount)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
    ) {
        if (uiState.isTextEditOpen) {
            // Text Edit Mode Header in CandidateBar: Left arrow + Title "文本编辑"
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleTextEditPanel,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = keyboardColors.keySecondaryText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else if (uiState.isEmojiOpen) {
            // Emoji Mode Header in CandidateBar: Left arrow + Title "表情"
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleEmojiPanel,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = keyboardColors.keySecondaryText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else if (uiState.isMenuOpen) {
            // Menu Mode Header in CandidateBar: Left arrow + Title "菜单" + Right Settings button
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onToggleMenuPanel,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = keyboardColors.keySecondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                }
                IconButton(
                    onClick = onOpenAdvancedSettings,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = keyboardColors.keySecondaryText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else if (candidates.isNotEmpty() || composingText.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Non-scrollable candidate chips fitting container
                SubcomposeLayout(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) { constraints ->
                    val maxWidth = constraints.maxWidth
                    val placeables = mutableListOf<Placeable>()
                    var currentWidth = 0

                    for (index in candidates.indices) {
                        val candidate = candidates[index]
                        val measurable = subcompose(index) {
                            var showMenu by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .clickable { onCandidateClick(candidate) }
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = candidate.text,
                                    color = keyboardColors.keyText,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Normal,
                                    maxLines = 1
                                )

                                CandidateContextMenu(
                                    candidate = candidate,
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false },
                                    onDeleteCandidate = onDeleteCandidate
                                )
                            }
                        }.first()

                        val placeable =
                            measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))

                        val fits = currentWidth + placeable.width <= maxWidth
                        val isLastRemaining = placeables.isEmpty() && index == candidates.lastIndex

                        if (fits || isLastRemaining) {
                            placeables.add(placeable)
                            currentWidth += placeable.width
                        } else {
                            break
                        }
                    }

                    renderedCount = placeables.size

                    layout(maxWidth, constraints.maxHeight) {
                        var x = 0
                        placeables.forEach { placeable ->
                            placeable.placeRelative(
                                x,
                                (constraints.maxHeight - placeable.height) / 2
                            )
                            x += placeable.width
                        }
                    }
                }

                // Expand / Collapse Arrow Toggle Button
                if (candidates.isNotEmpty()) {
                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = keyboardColors.keySecondaryText
                        )
                    }
                }
            }
        } else {
            // Quick Toolbar when idle
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onToggleMenuPanel,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "Menu",
                            tint = keyboardColors.keySecondaryText,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(
                        onClick = onToggleEmojiPanel,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SentimentSatisfied,
                            contentDescription = "Emoji",
                            tint = keyboardColors.keySecondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onToggleTextEditPanel,
                        modifier = Modifier.size(36.dp)
                    ) {
                        TextEditIcon(
                            tint = keyboardColors.keySecondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Real Android System Floating Window (PopupWindow)
        if (composingText.isNotEmpty()) {
            val density = LocalDensity.current
            val popupPositionProvider = remember(density) {
                object : PopupPositionProvider {
                    override fun calculatePosition(
                        anchorBounds: IntRect,
                        windowSize: IntSize,
                        layoutDirection: LayoutDirection,
                        popupContentSize: IntSize
                    ): IntOffset {
                        val marginPx = with(density) { 0.dp.roundToPx() }
                        val gapPx = with(density) { 4.dp.roundToPx() }
                        val x = anchorBounds.left + marginPx
                        val y = anchorBounds.top - popupContentSize.height - gapPx
                        return IntOffset(x, y)
                    }
                }
            }

            Popup(
                popupPositionProvider = popupPositionProvider,
                properties = PopupProperties(
                    focusable = false,
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                    clippingEnabled = false
                )
            ) {
                Surface(
                    color = keyboardColors.candidateBackground,
                ) {
                    Text(
                        text = composingText,
                        color = keyboardColors.candidateText,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFECEFF1)
@Composable
fun CandidateBarPreview() {
    GboardTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
        ) {
            CandidateBar(
                ImeUiState(
                    composingText = "hh",
                    candidates = mutableObjectListOf(
                        RimeCandidate.forTest("hello"),
                        RimeCandidate.forTest("world"),
                        RimeCandidate.forTest("compose")
                    ),
                    isCandidatesExpanded = false,
                ),
                onCandidateClick = {},
                onToggleExpand = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFECEFF1)
@Composable
fun CandidateBarToolbarPreview() {
    GboardTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
        ) {
            CandidateBar(
                ImeUiState(
                    composingText = "",
                    candidates = mutableObjectListOf(),
                ),
                onCandidateClick = {},
                onToggleExpand = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFECEFF1)
@Composable
fun CandidateBarTextEditPreview() {
    GboardTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
        ) {
            CandidateBar(
                ImeUiState(
                    composingText = "",
                    candidates = mutableObjectListOf(),
                    isTextEditOpen = true
                ),
                onCandidateClick = {},
                onToggleExpand = {},
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFECEFF1)
@Composable
fun CandidateBarEmojiPreview() {
    GboardTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
        ) {
            CandidateBar(
                ImeUiState(
                    composingText = "",
                    candidates = mutableObjectListOf(),
                    isEmojiOpen = true
                ),
                onCandidateClick = {},
                onToggleExpand = {},
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFECEFF1)
@Composable
fun CandidateBarMenuPreview() {
    GboardTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
        ) {
            CandidateBar(
                ImeUiState(
                    composingText = "",
                    candidates = mutableObjectListOf(),
                    isMenuOpen = true
                ),
                onCandidateClick = {},
                onToggleExpand = {},
            )
        }
    }
}
