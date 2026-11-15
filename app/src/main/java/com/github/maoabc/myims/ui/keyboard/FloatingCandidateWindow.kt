package com.github.maoabc.myims.ui.keyboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
 * 悬浮候选词 Popup 窗口 (PC 风格纯单行悬浮窗，无动画立即显示)
 *
 * @param rimeContext RIME 上下文数据源 (包含 composition 和 menu)
 * @param modifier 修饰符
 * @param onCandidateClick 点击候选词时的回调
 * @param onPageChange 点击换页三角箭头时的回调 (backward: true 为上一页, false 为下一页)
 * @param customPositionProvider 自定义的 PopupPositionProvider，外部提供位置计算
 * @param maxDisplayedCandidates 悬浮窗单页最多显示的候选词数量 (默认 5)
 */
@Composable
fun FloatingCandidateWindow(
    rimeContext: RimeContext,
    modifier: Modifier = Modifier,
    onCandidateClick: (RimeCandidate) -> Unit,
    onPageChange: (Boolean) -> Unit = {},
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
            onPageChange = onPageChange,
            modifier = modifier,
            maxDisplayedCandidates = maxDisplayedCandidates,
        )
    }
}

/**
 * 悬浮窗的具体 UI 内容组件 (纯单行模式，自适应内容大小，方向按键 ◀ ▶ 绝对优先保留在右侧)
 */
@Composable
fun FloatingCandidateContent(
    rimeContext: RimeContext,
    modifier: Modifier = Modifier,
    onCandidateClick: (RimeCandidate) -> Unit,
    onPageChange: (Boolean) -> Unit = {},
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
            .wrapContentSize()
    ) {
        Column(
            modifier = Modifier
                .wrapContentSize()
                .padding(vertical = 4.dp, horizontal = 6.dp)
        ) {
            // 1. Composing 编码区 (如: shuo'ming)
            if (preedit.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = preedit,
                        color = keyboardColors.candidateText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (candidates.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 2.dp)
                            .height(0.8.dp)
                            .background(keyboardColors.candidateBackground)
                    )
                }
            }

            // 2. 候选词纯单行展示 (1.说明  2.说明书  3.明确 ... ◀ ▶)
            if (candidates.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.wrapContentSize()
                ) {
                    val displayCount = candidates.size.coerceAtMost(maxDisplayedCandidates)

                    // 候选词列表使用 weight(1f, fill = false)
                    // 当内容短时不撑大全屏；当内容超出屏宽时优先压缩候选词，绝对不挤占右侧 ◀ ▶
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .wrapContentSize()
                    ) {
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
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // 序号 1. 2. 3.
                                    Text(
                                        text = "${i + 1}.",
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        color = if (isSelected) {
                                            keyboardColors.candidateText
                                        } else {
                                            keyboardColors.keySecondaryText
                                        },
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )

                                    Spacer(modifier = Modifier.width(2.dp))

                                    // 候选词文本
                                    Text(
                                        text = candidate.text,
                                        fontSize = 16.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
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

                    // 结尾的左右小三角翻页按键 ◀ ▶ (无 weight，最高优先级保留在右侧，绝对不会被顶出)
                    val menu = rimeContext.menu
                    val hasPrev = menu.page_no > 0
                    val hasNext = !menu.is_last_page

                    Spacer(modifier = Modifier.width(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(start = 2.dp)
                    ) {
                        // 左三角 ◀ (上一页)
                        Box(
                            modifier = Modifier
                                .background(
                                    color = Color.Transparent,
                                    shape = RoundedCornerShape(3.dp)
                                )
                                .then(
                                    if (hasPrev) {
                                        Modifier.clickable { onPageChange(true) }
                                    } else Modifier
                                )
                                .padding(horizontal = 4.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "◀",
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false,
                                color = if (hasPrev) {
                                    keyboardColors.candidateText
                                } else {
                                    keyboardColors.keySecondaryText.copy(alpha = 0.3f)
                                }
                            )
                        }

                        // 右三角 ▶ (下一页)
                        Box(
                            modifier = Modifier
                                .background(
                                    color = Color.Transparent,
                                    shape = RoundedCornerShape(3.dp)
                                )
                                .then(
                                    if (hasNext) {
                                        Modifier.clickable { onPageChange(false) }
                                    } else Modifier
                                )
                                .padding(horizontal = 4.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "▶",
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false,
                                color = if (hasNext) {
                                    keyboardColors.candidateText
                                } else {
                                    keyboardColors.keySecondaryText.copy(alpha = 0.3f)
                                }
                            )
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
