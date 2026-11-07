package com.github.maoabc.myims.ui.keyboard

import androidx.collection.ObjectList
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.maoabc.librime.RimeCandidate
import com.github.maoabc.myims.ime.KeyData
import com.github.maoabc.myims.ime.KeyType
import com.github.maoabc.myims.ui.theme.LocalKeyboardColors
import kotlinx.coroutines.launch

@Composable
fun ExpandedCandidatesPanel(
    candidates: ObjectList<RimeCandidate>,
    fromIndex: Int,
    onCandidateClick: (RimeCandidate) -> Unit,
    onKeyClick: (KeyData) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardColors = LocalKeyboardColors.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    val deleteKeyData = remember { KeyData(KeyType.BACKSPACE, "Del") }

    val scrollAmount = {
        val layoutInfo = gridState.layoutInfo
        val viewportHeight = layoutInfo.viewportSize.height.toFloat()
        val rowHeight = layoutInfo.visibleItemsInfo.firstOrNull()?.size?.height?.toFloat()
            ?: with(density) { 44.dp.toPx() }
        val minFiveRows = rowHeight * 5
        if (viewportHeight > 0f) maxOf(viewportHeight, minFiveRows) else minFiveRows
    }

    val lastVisibleItemIndexState = remember {
        derivedStateOf {
            gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        }
    }
    LaunchedEffect(lastVisibleItemIndexState.value) {
        val totalItemsCount = gridState.layoutInfo.totalItemsCount
        val lastVisibleIndex = lastVisibleItemIndexState.value
        if (candidates.size > 0 && lastVisibleIndex >= totalItemsCount - 18) {
//            println("rime: loadMore $totalItemsCount $lastVisibleIndex ${candidates.size}")
            onLoadMore()
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = keyboardColors.keyboardBackground
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Candidate Grid (Flat layout, ending vertical divider line, no card/shadows)
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                state = gridState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                items(candidates.size - fromIndex) { candidateIdx ->
                    val candidate = candidates[candidateIdx + fromIndex]
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clickable {
                                onCandidateClick(candidate)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = candidate.text,
                            color = keyboardColors.keyText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )

                        // Ending divider line between candidate cells
                        VerticalDivider(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .height(22.dp)
                                .width(1.dp),
                            color = keyboardColors.keyActionBackground
                        )

                        HorizontalDivider(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth(),
                            color = keyboardColors.keyActionBackground.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            // Divider separating grid from right action keys
            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp),
                color = keyboardColors.keyActionBackground
            )

            // Right Action Column with distinct key containers & solid triangle arrows
            Column(
                modifier = Modifier
                    .width(58.dp)
                    .fillMaxHeight()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 1. Scroll Up Key (Scrolls by full page / at least 5 rows dynamically with long-press repeat)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .shadow(1.dp, RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                        .background(keyboardColors.keyActionBackground)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val repeatJob = coroutineScope.launchRepeatAction {
                                        coroutineScope.launch {
                                            gridState.animateScrollBy(-scrollAmount())
                                        }
                                    }
                                    try {
                                        tryAwaitRelease()
                                    } finally {
                                        repeatJob.cancel()
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropUp,
                        contentDescription = "Scroll Up",
                        tint = keyboardColors.keyActionText,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // 2. Scroll Down Key (Scrolls by full page / at least 5 rows dynamically with long-press repeat)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .shadow(1.dp, RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                        .background(keyboardColors.keyActionBackground)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val repeatJob = coroutineScope.launchRepeatAction {
                                        coroutineScope.launch {
                                            gridState.animateScrollBy(scrollAmount())
                                        }
                                    }
                                    try {
                                        tryAwaitRelease()
                                    } finally {
                                        repeatJob.cancel()
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Scroll Down",
                        tint = keyboardColors.keyActionText,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // 3. Delete / Backspace Key (with key outline & repeat delete on long press)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .shadow(1.dp, RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                        .background(keyboardColors.keyActionBackground)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val repeatJob = coroutineScope.launchRepeatAction {
                                        onKeyClick(deleteKeyData)
                                    }

                                    try {
                                        tryAwaitRelease()
                                    } finally {
                                        repeatJob.cancel()
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Backspace",
                        tint = keyboardColors.keyActionText,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 4. Enter / Confirm Key (with primary action key container)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .shadow(1.dp, RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                        .background(keyboardColors.keyPrimaryActionBackground)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onKeyClick(KeyData(KeyType.ENTER, "Enter"))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardReturn,
                        contentDescription = "Enter",
                        tint = keyboardColors.keyPrimaryActionText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
