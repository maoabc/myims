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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
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
 * @param positionProvider 自定义的 PopupPositionProvider，外部提供位置计算
 */
@Composable
fun FloatingCandidateWindow(
    rimeContext: RimeContext,
    modifier: Modifier = Modifier,
    onCandidateClick: (RimeCandidate) -> Unit,
    onPageChange: (Boolean) -> Unit = {},
    positionProvider: PopupPositionProvider
) {
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
        )

    }
}

/**
 * 悬浮窗的具体 UI 内容组件 (支持手势左右滑动，且自动根据高亮索引平滑滚动到对应候选词)
 */
@Composable
fun FloatingCandidateContent(
    rimeContext: RimeContext,
    modifier: Modifier = Modifier,
    onCandidateClick: (RimeCandidate) -> Unit,
    onPageChange: (Boolean) -> Unit = {}
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

            // 2. 候选词单行展示，支持左右滑动 & 根据高亮索引自动跟随滚动 (1.说明  2.说明书  3.明确 ... ◀ ▶)
            if (candidates.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.wrapContentSize()
                ) {
                    val pageSize = rimeContext.menu.page_size
                    val displayCount =
                        if (pageSize > 0) candidates.size.coerceAtMost(pageSize) else candidates.size
                    val highlightedIndex = rimeContext.menu.highlighted_candidate_index.coerceIn(
                        0,
                        (displayCount - 1).coerceAtLeast(0)
                    )

                    val lazyListState = rememberLazyListState()

                    // 高亮索引或候选词列表变化时，自动平滑滚动保证高亮候选词 100% 可见
                    LaunchedEffect(highlightedIndex, candidates.firstOrNull()?.text) {
                        if (displayCount > 0 && highlightedIndex in 0 until displayCount) {
                            lazyListState.animateScrollToItem(highlightedIndex)
                        }
                    }

                    // 候选词列表：使用 LazyRow + weight(1f, fill = false)，既可左右划动，又绝对不挤占右侧 ◀ ▶
                    LazyRow(
                        state = lazyListState,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .wrapContentSize()
                    ) {
                        items(
                            count = displayCount,
                            key = { index -> candidates[index].text }
                        ) { i ->
                            val candidate = candidates[i]
                            val isSelected = (i == highlightedIndex)

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
                                    // 序号依据 select_keys，若为空则默认使用序号 1. 2. 3.
                                    val selectKeys = rimeContext.menu.select_keys
                                    val label =
                                        if (selectKeys.isNotEmpty() && i < selectKeys.size) {
                                            "${selectKeys[i]}."
                                        } else {
                                            "${i + 1}."
                                        }

                                    Text(
                                        text = label,
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
                                        color = if (isSelected) {
                                            keyboardColors.candidateText
                                        } else {
                                            keyboardColors.keyText
                                        },
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    // 结尾的左右小三角翻页按键 ◀ ▶ (无 weight，最高优先级保留在右侧)
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
