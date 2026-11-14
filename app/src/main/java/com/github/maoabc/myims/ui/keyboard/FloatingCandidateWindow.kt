package com.github.maoabc.myims.ui.keyboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
import com.github.maoabc.librime.RimeContext
import com.github.maoabc.myims.ui.theme.GboardTheme
import com.github.maoabc.myims.ui.theme.LocalKeyboardColors

/**
 * 悬浮候选词 Popup 窗口 (PC 风格悬浮窗)
 *
 * 用于在光标处/指定坐标展示 composition (编码) 和 candidates (候选词)。
 *
 * @param rimeContext RIME 上下文数据源 (包含 composition 和 menu)
 * @param onCandidateClick 点击候选词时的回调
 * @param modifier 修饰符
 * @param customPositionProvider 自定义的 PopupPositionProvider，外部提供位置计算
 * @param maxDisplayedCandidates 悬浮窗单页最多显示的候选词数量 (默认 5)
 */
@Composable
fun FloatingCandidateWindow(
    rimeContext: RimeContext,
    onCandidateClick: (RimeCandidate) -> Unit,
    modifier: Modifier = Modifier,
    customPositionProvider: PopupPositionProvider? = null,
    maxDisplayedCandidates: Int = 5
) {
    val defaultPositionProvider = remember {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                return IntOffset.Zero
            }
        }
    }
    val positionProvider = customPositionProvider ?: defaultPositionProvider

    Popup(
        popupPositionProvider = positionProvider,
        properties = PopupProperties(
            focusable = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            clippingEnabled = false
        ),
    ) {
        FloatingCandidateContent(
            rimeContext = rimeContext,
            onCandidateClick = onCandidateClick,
            modifier = modifier,
            maxDisplayedCandidates = maxDisplayedCandidates,
        )
    }
}

/**
 * 悬浮窗的具体 UI 内容组件 (纯视图，便于在 Compose Preview 或嵌入视图中直接使用)
 */
@Composable
fun FloatingCandidateContent(
    rimeContext: RimeContext,
    onCandidateClick: (RimeCandidate) -> Unit,
    modifier: Modifier = Modifier,
    maxDisplayedCandidates: Int = 5
) {
    val keyboardColors = LocalKeyboardColors.current
    val shape = RoundedCornerShape(8.dp)
    val preedit = rimeContext.composition.preedit
    val candidates = rimeContext.menu.candidates

    Surface(
        shape = shape,
        color = keyboardColors.keyboardBackground,
        contentColor = keyboardColors.keyText,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, keyboardColors.candidateBackground),
        modifier = modifier
            .shadow(8.dp, shape = shape)
            .widthIn(min = 120.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp)
        ) {
            // 1. Composing 编码区 (如: shuo'ming)
            if (preedit.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = preedit,
                        color = keyboardColors.candidateText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
                }

                if (candidates.isNotEmpty()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        thickness = 0.8.dp,
                        color = keyboardColors.candidateBackground
                    )
                }
            }

            // 2. 候选词行 (例如: 1.说明  2.说明书  3.明确 ...)
            if (candidates.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val displayCount = candidates.size.coerceAtMost(maxDisplayedCandidates)

                    for (i in 0 until displayCount) {
                        val candidate = candidates[i]
                        val isSelected = (i == rimeContext.menu.highlighted_candidate_index)

                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isSelected) {
                                        keyboardColors.candidateBackground
                                    } else {
                                        Color.Transparent
                                    },
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .clickable { onCandidateClick(candidate) }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // 序号 1. 2. 3.
                                Text(
                                    text = "${i + 1}.",
                                    fontSize = 13.sp,
                                    color = if (isSelected) {
                                        keyboardColors.candidateText
                                    } else {
                                        keyboardColors.keySecondaryText
                                    },
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )

                                Spacer(modifier = Modifier.width(3.dp))

                                // 候选词文本
                                Text(
                                    text = candidate.text,
                                    fontSize = 16.sp,
                                    color = if (isSelected) {
                                        keyboardColors.candidateText
                                    } else {
                                        keyboardColors.keyText
                                    },
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }

                        if (i < displayCount - 1) {
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }
                }
            }
        }
    }
}

// --- Compose Previews ---

@Preview(name = "Light Mode - Composing & Candidates", showBackground = true)
@Composable
fun FloatingCandidateContentPreviewLight() {
    GboardTheme(darkTheme = false) {
        Box(modifier = Modifier.padding(16.dp)) {
            FloatingCandidateContent(
                rimeContext = RimeContext.forTest(
                    "shuo'ming",
                    "说明",
                    "说明书",
                    "明确",
                    "说明文档"
                ),
                onCandidateClick = {}
            )
        }
    }
}

@Preview(name = "Dark Mode - Composing & Candidates", showBackground = true)
@Composable
fun FloatingCandidateContentPreviewDark() {
    GboardTheme(darkTheme = true) {
        Box(modifier = Modifier.padding(16.dp)) {
            FloatingCandidateContent(
                rimeContext = RimeContext.forTest(
                    "shuo'ming",
                    "说明",
                    "说明书",
                    "明确",
                    "说明文档"
                ),
                onCandidateClick = {}
            )
        }
    }
}

@Preview(name = "Composing Only", showBackground = true)
@Composable
fun FloatingCandidateContentComposingOnlyPreview() {
    GboardTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            FloatingCandidateContent(
                rimeContext = RimeContext.forTest("ce'shi"),
                onCandidateClick = {}
            )
        }
    }
}
