package com.github.maoabc.myims.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.LastPage
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.maoabc.myims.ui.theme.GboardTheme
import com.github.maoabc.myims.ui.theme.LocalKeyboardColors

@Composable
fun TextMovePanel(
    onAction: (TextMoveCommand) -> Unit,
    onDelete: () -> Unit,
    isSelecting: Boolean,
    modifier: Modifier = Modifier
) {
    val keyboardColors = LocalKeyboardColors.current
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .background(keyboardColors.keyboardBackground)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Area: D-Pad (Left tall vertical btn | Center stack [Up/Select/Down] | Right tall vertical btn) + Home/End at bottom
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(end = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // D-Pad Row: [ Left tall button ] [ Center stack (Up / Select / Down) ] [ Right tall button ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Tall Vertical Button (spanning height of Up + Select + Down)
                TextEditRepeatButton(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(10.dp),
                    elevation = 2.dp,
                    onClick = { onAction(TextMoveCommand.LEFT) }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Left",
                        tint = keyboardColors.keyActionText,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Center Column (Up, Select, Down) - same width as each other
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Up
                    TextEditRepeatButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        elevation = 1.dp,
                        onClick = { onAction(TextMoveCommand.UP) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Up",
                            tint = keyboardColors.keyActionText,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Select (Toggle select doesn't need repeat, regular click is fine)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .shadow(2.dp, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onAction(TextMoveCommand.TOGGLE_SELECT)
                            },
                        color = if (isSelecting) keyboardColors.keyPrimaryActionBackground else keyboardColors.keyActionBackground
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (isSelecting) "取消" else "选择",
                                color = if (isSelecting) keyboardColors.keyPrimaryActionText else keyboardColors.keyActionText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Down
                    TextEditRepeatButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        elevation = 1.dp,
                        onClick = { onAction(TextMoveCommand.DOWN) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Down",
                            tint = keyboardColors.keyActionText,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Right Tall Vertical Button (spanning height of Up + Select + Down)
                TextEditRepeatButton(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(10.dp),
                    elevation = 2.dp,
                    onClick = { onAction(TextMoveCommand.RIGHT) }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Right",
                        tint = keyboardColors.keyActionText,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Bottom Home & End buttons (开头 / 结尾)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextEditBottomNavButton(icon = Icons.Default.FirstPage, modifier = Modifier.weight(1f)) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onAction(TextMoveCommand.HOME)
                }
                TextEditBottomNavButton(icon = Icons.AutoMirrored.Filled.LastPage, modifier = Modifier.weight(1f)) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onAction(TextMoveCommand.END)
                }
            }
        }

        // Right Column: 全选, 复制, 粘贴, 删除 (filling height evenly)
        Column(
            modifier = Modifier
                .width(100.dp)
                .fillMaxHeight()
                .padding(start = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextEditSideButton(text = "全选", modifier = Modifier.weight(1f)) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onAction(TextMoveCommand.SELECT_ALL)
            }
            TextEditSideButton(text = "复制", modifier = Modifier.weight(1f)) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onAction(TextMoveCommand.COPY)
            }
            TextEditSideButton(text = "粘贴", modifier = Modifier.weight(1f)) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onAction(TextMoveCommand.PASTE)
            }
            // Delete with repeat action
            TextEditRepeatButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                elevation = 1.dp,
                onClick = onDelete
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    tint = keyboardColors.keyActionText,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun TextEditRepeatButton(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp),
    elevation: Dp = 1.dp,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val keyboardColors = LocalKeyboardColors.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    Surface(
        modifier = modifier
            .shadow(elevation, shape)
            .clip(shape)
            .pointerInput(onClick) {
                detectTapGestures(
                    onPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val repeatJob = coroutineScope.launchRepeatAction {
                            onClick()
                        }
                        try {
                            tryAwaitRelease()
                        } finally {
                            repeatJob.cancel()
                        }
                    }
                )
            },
        color = keyboardColors.keyActionBackground
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
private fun TextEditSideButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val keyboardColors = LocalKeyboardColors.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = keyboardColors.keyActionBackground
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = text, color = keyboardColors.keyActionText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun TextEditBottomNavButton(icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val keyboardColors = LocalKeyboardColors.current
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .shadow(1.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = keyboardColors.keyActionBackground
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = null, tint = keyboardColors.keyActionText, modifier = Modifier.size(24.dp))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFECEFF1)
@Composable
fun TextMovePanelPreview() {
    GboardTheme {
        Box(modifier = Modifier.size(width = 360.dp, height = 240.dp)) {
            TextMovePanel(
                onAction = {},
                onDelete = {},
                isSelecting = false,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
