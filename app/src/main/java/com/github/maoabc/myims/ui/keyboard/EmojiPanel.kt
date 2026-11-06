package com.github.maoabc.myims.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.maoabc.myims.ime.EmojiCategories
import com.github.maoabc.myims.ui.theme.GboardTheme
import com.github.maoabc.myims.ui.theme.LocalKeyboardColors
import kotlinx.coroutines.launch

@Composable
fun EmojiPanel(
    onEmojiSelect: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardColors = LocalKeyboardColors.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val categories = EmojiCategories.list
    val gridState = rememberLazyGridState()

    // Determine current active category based on first visible item index
    val currentCategoryIndex by remember {
        derivedStateOf {
            val firstIndex = gridState.firstVisibleItemIndex
            var accumulated = 0
            var activeIdx = 0
            for (i in categories.indices) {
                // Header (1 item) + emojis
                val count = 1 + categories[i].emojis.size
                if (firstIndex < accumulated + count) {
                    activeIdx = i
                    break
                }
                accumulated += count
                activeIdx = i
            }
            activeIdx
        }
    }

    Column(
        modifier = modifier
            .background(keyboardColors.keyboardBackground)
            .padding(4.dp)
    ) {
        // Emoji Grid Area with Category Headers
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(8),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                categories.forEachIndexed { catIndex, category ->
                    // Section Header spanning all 8 columns
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = stringResource(category.titleResId),
                            color = keyboardColors.keySecondaryText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp, top = if (catIndex == 0) 2.dp else 10.dp, bottom = 4.dp)
                        )
                    }
                    items(category.emojis) { emoji ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onEmojiSelect(emoji)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = emoji,
                                fontSize = 24.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Category Tabs Bar & Bottom Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Tabs with clear active indicator
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                categories.forEachIndexed { index, category ->
                    val isSelected = currentCategoryIndex == index
                    Surface(
                        modifier = Modifier
                            .size(36.dp)
                            .shadow(if (isSelected) 1.dp else 0.dp, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                var targetIndex = 0
                                for (i in 0 until index) {
                                    targetIndex += 1 + categories[i].emojis.size
                                }
                                coroutineScope.launch {
                                    gridState.animateScrollToItem(targetIndex)
                                }
                            },
                        color = if (isSelected) keyboardColors.keyPressedBackground else Color.Transparent
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = category.icon,
                                fontSize = 20.sp
                            )
                        }
                    }
                }
            }

            // Delete / Backspace button on the right side
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .fillMaxHeight()
                    .padding(start = 4.dp)
                    .shadow(1.dp, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .background(keyboardColors.keyActionBackground)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val repeatJob = coroutineScope.launchRepeatAction {
                                    onDelete()
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
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFECEFF1)
@Composable
fun EmojiPanelPreview() {
    GboardTheme {
        Box(modifier = Modifier.size(width = 360.dp, height = 240.dp)) {
            EmojiPanel(
                onEmojiSelect = {},
                onDelete = {},
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
