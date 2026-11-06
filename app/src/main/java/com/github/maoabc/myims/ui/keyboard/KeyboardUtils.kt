package com.github.maoabc.myims.ui.keyboard

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Helper to launch a repeating action (e.g. for long-press backspace delete)
 * with an initial delay and subsequent repeat interval.
 */
fun CoroutineScope.launchRepeatAction(
    initialDelay: Duration = 350.milliseconds,
    repeatInterval: Duration = 50.milliseconds,
    action: () -> Unit
): Job {
    action() // immediate first action
    return launch {
        delay(initialDelay)
        while (isActive) {
            action()
            delay(repeatInterval)
        }
    }
}
