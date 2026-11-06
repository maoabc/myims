package com.github.maoabc.myims.ime

enum class KeyType {
    CHARACTER,
    SPACE,
    ENTER,
    SHIFT,
    BACKSPACE,
    LANGUAGE,
    MODE_CHANGE,
}

data class KeyData(
    val type: KeyType,
    val primaryText: String,
    val secondaryText: String? = null,
    val weight: Float = 1f,
    val targetMode: KeyboardMode? = null
)

object KeyboardLayouts {
    val qwertyRows = listOf(
        listOf(
            KeyData(KeyType.CHARACTER, "q", "1", 1f),
            KeyData(KeyType.CHARACTER, "w", "2", 1f),
            KeyData(KeyType.CHARACTER, "e", "3", 1f),
            KeyData(KeyType.CHARACTER, "r", "4", 1f),
            KeyData(KeyType.CHARACTER, "t", "5", 1f),
            KeyData(KeyType.CHARACTER, "y", "6", 1f),
            KeyData(KeyType.CHARACTER, "u", "7", 1f),
            KeyData(KeyType.CHARACTER, "i", "8", 1f),
            KeyData(KeyType.CHARACTER, "o", "9", 1f),
            KeyData(KeyType.CHARACTER, "p", "0", 1f)
        ),
        listOf(
            KeyData(KeyType.CHARACTER, "a", "@", 1f),
            KeyData(KeyType.CHARACTER, "s", "#", 1f),
            KeyData(KeyType.CHARACTER, "d", "$", 1f),
            KeyData(KeyType.CHARACTER, "f", "_", 1f),
            KeyData(KeyType.CHARACTER, "g", "&", 1f),
            KeyData(KeyType.CHARACTER, "h", "-", 1f),
            KeyData(KeyType.CHARACTER, "j", "+", 1f),
            KeyData(KeyType.CHARACTER, "k", "(", 1f),
            KeyData(KeyType.CHARACTER, "l", ")", 1f)
        ),
        listOf(
            KeyData(KeyType.SHIFT, "Shift", null, 1.5f),
            KeyData(KeyType.CHARACTER, "z", "\"", 1f),
            KeyData(KeyType.CHARACTER, "x", "'", 1f),
            KeyData(KeyType.CHARACTER, "c", ":", 1f),
            KeyData(KeyType.CHARACTER, "v", ";", 1f),
            KeyData(KeyType.CHARACTER, "b", "!", 1f),
            KeyData(KeyType.CHARACTER, "n", "?", 1f),
            KeyData(KeyType.CHARACTER, "m", ",", 1f),
            KeyData(KeyType.BACKSPACE, "Del", null, 1.5f)
        ),
        listOf(
            KeyData(KeyType.MODE_CHANGE, "?123", null, 1.5f, targetMode = KeyboardMode.NUMBERS),
            KeyData(KeyType.CHARACTER, ",", null, 1f),
            KeyData(KeyType.LANGUAGE, "Lang", null, 1f),
            KeyData(KeyType.SPACE, "Space", null, 4f),
            KeyData(KeyType.CHARACTER, ".", null, 1f),
            KeyData(KeyType.ENTER, "Enter", null, 1.5f)
        )
    )

    val numbersRows = listOf(
        listOf(
            KeyData(KeyType.CHARACTER, "1", null, 1f),
            KeyData(KeyType.CHARACTER, "2", null, 1f),
            KeyData(KeyType.CHARACTER, "3", null, 1f),
            KeyData(KeyType.CHARACTER, "4", null, 1f),
            KeyData(KeyType.CHARACTER, "5", null, 1f),
            KeyData(KeyType.CHARACTER, "6", null, 1f),
            KeyData(KeyType.CHARACTER, "7", null, 1f),
            KeyData(KeyType.CHARACTER, "8", null, 1f),
            KeyData(KeyType.CHARACTER, "9", null, 1f),
            KeyData(KeyType.CHARACTER, "0", null, 1f)
        ),
        listOf(
            KeyData(KeyType.CHARACTER, "@", null, 1f),
            KeyData(KeyType.CHARACTER, "#", null, 1f),
            KeyData(KeyType.CHARACTER, "$", null, 1f),
            KeyData(KeyType.CHARACTER, "_", null, 1f),
            KeyData(KeyType.CHARACTER, "&", null, 1f),
            KeyData(KeyType.CHARACTER, "-", null, 1f),
            KeyData(KeyType.CHARACTER, "+", null, 1f),
            KeyData(KeyType.CHARACTER, "(", null, 1f),
            KeyData(KeyType.CHARACTER, ")", null, 1f),
            KeyData(KeyType.CHARACTER, "/", null, 1f)
        ),
        listOf(
            KeyData(KeyType.MODE_CHANGE, "=\\+", null, 1.5f, targetMode = KeyboardMode.SYMBOLS),
            KeyData(KeyType.CHARACTER, "*", null, 1f),
            KeyData(KeyType.CHARACTER, "\"", null, 1f),
            KeyData(KeyType.CHARACTER, "'", null, 1f),
            KeyData(KeyType.CHARACTER, ":", null, 1f),
            KeyData(KeyType.CHARACTER, ";", null, 1f),
            KeyData(KeyType.CHARACTER, "!", null, 1f),
            KeyData(KeyType.CHARACTER, "?", null, 1f),
            KeyData(KeyType.BACKSPACE, "Del", null, 1.5f)
        ),
        listOf(
            KeyData(KeyType.MODE_CHANGE, "ABC", null, 1.5f, targetMode = KeyboardMode.QWERTY),
            KeyData(KeyType.CHARACTER, ",", null, 1f),
            KeyData(KeyType.LANGUAGE, "Lang", null, 1f),
            KeyData(KeyType.SPACE, "Space", null, 4f),
            KeyData(KeyType.CHARACTER, ".", null, 1f),
            KeyData(KeyType.ENTER, "Enter", null, 1.5f)
        )
    )

    val symbolsRows = listOf(
        listOf(
            KeyData(KeyType.CHARACTER, "~", null, 1f),
            KeyData(KeyType.CHARACTER, "`", null, 1f),
            KeyData(KeyType.CHARACTER, "|", null, 1f),
            KeyData(KeyType.CHARACTER, "•", null, 1f),
            KeyData(KeyType.CHARACTER, "√", null, 1f),
            KeyData(KeyType.CHARACTER, "π", null, 1f),
            KeyData(KeyType.CHARACTER, "÷", null, 1f),
            KeyData(KeyType.CHARACTER, "×", null, 1f),
            KeyData(KeyType.CHARACTER, "¶", null, 1f),
            KeyData(KeyType.CHARACTER, "∆", null, 1f)
        ),
        listOf(
            KeyData(KeyType.CHARACTER, "£", null, 1f),
            KeyData(KeyType.CHARACTER, "¢", null, 1f),
            KeyData(KeyType.CHARACTER, "€", null, 1f),
            KeyData(KeyType.CHARACTER, "¥", null, 1f),
            KeyData(KeyType.CHARACTER, "^", null, 1f),
            KeyData(KeyType.CHARACTER, "°", null, 1f),
            KeyData(KeyType.CHARACTER, "=", null, 1f),
            KeyData(KeyType.CHARACTER, "{", null, 1f),
            KeyData(KeyType.CHARACTER, "}", null, 1f),
            KeyData(KeyType.CHARACTER, "\\", null, 1f)
        ),
        listOf(
            KeyData(KeyType.MODE_CHANGE, "?123", null, 1.5f, targetMode = KeyboardMode.NUMBERS),
            KeyData(KeyType.CHARACTER, "%", null, 1f),
            KeyData(KeyType.CHARACTER, "_", null, 1f),
            KeyData(KeyType.CHARACTER, "<", null, 1f),
            KeyData(KeyType.CHARACTER, ">", null, 1f),
            KeyData(KeyType.CHARACTER, "[", null, 1f),
            KeyData(KeyType.CHARACTER, "]", null, 1f),
            KeyData(KeyType.CHARACTER, "™", null, 1f),
            KeyData(KeyType.BACKSPACE, "Del", null, 1.5f)
        ),
        listOf(
            KeyData(KeyType.MODE_CHANGE, "ABC", null, 1.5f, targetMode = KeyboardMode.QWERTY),
            KeyData(KeyType.CHARACTER, ",", null, 1f),
            KeyData(KeyType.LANGUAGE, "Lang", null, 1f),
            KeyData(KeyType.SPACE, "Space", null, 4f),
            KeyData(KeyType.CHARACTER, ".", null, 1f),
            KeyData(KeyType.ENTER, "Enter", null, 1.5f)
        )
    )
}
