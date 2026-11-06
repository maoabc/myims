package com.github.maoabc.myims.ui.keyboard

import android.view.KeyEvent
import android.view.inputmethod.InputConnection

enum class TextMoveCommand {
    UP, DOWN, LEFT, RIGHT, HOME, END, SELECT_ALL, CUT, COPY, PASTE, TOGGLE_SELECT
}

object TextMoveActions {
    fun execute(ic: InputConnection?, command: TextMoveCommand, isSelecting: Boolean) {
        if (ic == null) return
        val metaState = if (isSelecting) KeyEvent.META_SHIFT_ON else 0
        when (command) {
            TextMoveCommand.UP -> {
            }

            TextMoveCommand.DOWN -> {
            }

            TextMoveCommand.LEFT -> {
            }

            TextMoveCommand.RIGHT -> {
            }

            TextMoveCommand.HOME -> {
            }

            TextMoveCommand.END -> {
            }

            TextMoveCommand.SELECT_ALL -> {
                ic.performContextMenuAction(android.R.id.selectAll)
            }

            TextMoveCommand.CUT -> {
                ic.performContextMenuAction(android.R.id.cut)
            }

            TextMoveCommand.COPY -> {
                ic.performContextMenuAction(android.R.id.copy)
            }

            TextMoveCommand.PASTE -> {
                ic.performContextMenuAction(android.R.id.paste)
            }

            TextMoveCommand.TOGGLE_SELECT -> {
                // Handled via uiState in service
            }
        }
    }
}
