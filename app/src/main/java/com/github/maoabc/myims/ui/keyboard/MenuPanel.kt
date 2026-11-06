package com.github.maoabc.myims.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Abc
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.maoabc.myims.ime.RimeOptions
import com.github.maoabc.myims.ui.theme.GboardTheme
import com.github.maoabc.myims.ui.theme.LocalKeyboardColors

data class MenuOptionItem(
    val key: String,
    val getTitle: (Boolean) -> String,
    val getIcon: (Boolean) -> ImageVector,
    val isActive: (Boolean) -> Boolean
)

@Composable
fun MenuPanel(
    asciiPunct: Boolean,
    fullShape: Boolean,
    simplified: Boolean,
    emoji: Boolean,
    onToggleOption: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardColors = LocalKeyboardColors.current
    val haptic = LocalHapticFeedback.current

    val items = listOf(
        MenuOptionItem(
            key = RimeOptions.FULL_SHAPE,
            getTitle = { active -> if (active) "半角" else "全角" },
            getIcon = { active -> if (active) Icons.Default.Bedtime else Icons.Default.Circle },
            isActive = { rawState -> !rawState } // half shape is active when full_shape is false
        ),
        MenuOptionItem(
            key = RimeOptions.ASCII_PUNCT,
            getTitle = { active -> if (active) "英文标点" else "中文标点" },
            getIcon = { _ -> Icons.Default.Title },
            isActive = { rawState -> rawState }
        ),
        MenuOptionItem(
            key = RimeOptions.TRADITIONALIZATION,
            getTitle = { active -> if (active) "繁体" else "简体" },
            getIcon = { _ -> Icons.Default.Translate },
            isActive = { rawState -> rawState }
        ),
        MenuOptionItem(
            key = RimeOptions.EMOJI,
            getTitle = { _ -> "Emoji" },
            getIcon = { _ -> Icons.Default.SentimentSatisfied },
            isActive = { rawState -> rawState }
        )
    )

    Column(
        modifier = modifier
            .background(keyboardColors.keyboardBackground)
            .padding(8.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items) { item ->
                val rawState = when (item.key) {
                    RimeOptions.FULL_SHAPE -> fullShape
                    RimeOptions.ASCII_PUNCT -> asciiPunct
                    RimeOptions.TRADITIONALIZATION -> simplified
                    RimeOptions.EMOJI -> emoji
                    else -> false
                }

                val isActive = item.isActive(rawState)
                val title = item.getTitle(isActive)
                val icon = item.getIcon(isActive)

                val interactionSource = remember { MutableInteractionSource() }
                Column(
                    modifier = Modifier
                        .aspectRatio(1.1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleOption(item.key)
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(if (isActive) keyboardColors.keyPrimaryActionBackground else keyboardColors.keyActionBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = if (isActive) keyboardColors.keyPrimaryActionText else keyboardColors.keyActionText,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = title,
                        color = keyboardColors.keyActionText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFECEFF1)
@Composable
fun MenuPanelPreview() {
    GboardTheme {
        Box(modifier = Modifier.width(360.dp).height(200.dp)) {
            MenuPanel(
                asciiPunct = false,
                fullShape = false,
                simplified = true,
                emoji = true,
                onToggleOption = {},
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
