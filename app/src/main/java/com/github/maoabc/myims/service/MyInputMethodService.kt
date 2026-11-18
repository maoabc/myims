package com.github.maoabc.myims.service

import android.content.Intent
import android.content.res.Configuration
import android.hardware.input.InputManager
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.SystemClock.uptimeMillis
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodSubtype
import androidx.collection.MutableObjectList
import androidx.collection.ObjectList
import androidx.collection.emptyObjectList
import androidx.collection.mutableObjectListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.github.maoabc.librime.RimeCandidate
import com.github.maoabc.librime.RimeSession
import com.github.maoabc.myims.MainActivity
import com.github.maoabc.myims.ime.ImeActionHandler
import com.github.maoabc.myims.ime.ImeUiState
import com.github.maoabc.myims.ime.KeyboardPreferencesManager
import com.github.maoabc.myims.ime.KeyData
import com.github.maoabc.myims.ime.KeyType
import com.github.maoabc.myims.ime.RimeOptions
import com.github.maoabc.myims.ime.ShiftState
import com.github.maoabc.myims.ui.keyboard.TextMoveActions
import com.github.maoabc.myims.ui.keyboard.TextMoveCommand
import androidx.lifecycle.lifecycleScope
import com.github.maoabc.librime.RimeAPI
import com.github.maoabc.librime.RimeKeyMap
import com.github.maoabc.librime.RimeKeyMap.NONE
import com.github.maoabc.myims.ime.KeyboardMode
import com.github.maoabc.myims.ime.RimeManager
import com.github.maoabc.myims.ui.keyboard.HardOrSoftKeyboard
import com.github.maoabc.myims.ui.theme.GboardTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

class MyInputMethodService : InputMethodService(),
    LifecycleOwner,
    SavedStateRegistryOwner,
    ViewModelStoreOwner,
    ImeActionHandler {

    private val TAG: String = "rime"
    private val DEBUG: Boolean = false

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    private var uiState by mutableStateOf(ImeUiState())
    private var cursorLocation by mutableStateOf<IntOffset?>(null)

    private val rimeDispatcher = Executors.newSingleThreadExecutor { r ->
        Thread(r, "RimeWorker")
    }.asCoroutineDispatcher()

    private inline fun launchWithMain(crossinline block: suspend CoroutineScope.() -> Unit) {
        lifecycleScope.launch(Dispatchers.Main) {
            block()
        }
    }

    private var rimeSessionInstance: RimeSession? = null

    private fun createSession(): RimeSession {
        RimeManager.ensureInitialized(applicationContext)
        return RimeAPI.createSession()
    }

    private val rimeMutex = Mutex()

    private suspend inline fun <T> withRimeContext(
        crossinline block: suspend CoroutineScope.(RimeSession) -> T
    ): T = rimeMutex.withLock {
        withContext(rimeDispatcher) {
            val session = rimeSessionInstance ?: createSession().apply {
                val options = KeyboardPreferencesManager.loadOptions(applicationContext)
                options?.let {
                    KeyboardPreferencesManager.applyToSession(this, options)
                }
                val langTag = getCurrentSubtype()
                this.setOption(RimeOptions.ASCII_MODE, langTag.contains("en"))
            }.also { rimeSessionInstance = it }
            block(session)
        }
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        RimeManager.onDeployed = {
            launchWithMain {
                val options = withRimeContext {
                    val options = KeyboardPreferencesManager.getOptionsFromSession(it)
                    it.close()
                    rimeSessionInstance = null
                    options
                }
                uiState = uiState.copy(
                    keyboardMode = KeyboardMode.QWERTY,
                    options = options,
                    composingText = "",
                    candidates = emptyObjectList(),
                    isCandidatesExpanded = false
                )
            }
        }
    }


    override fun onCreateInputView(): View {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        uiState = uiState.copy(keyboardMode = KeyboardMode.QWERTY)


        window?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val decorView = window.decorView
            decorView.setViewTreeLifecycleOwner(this@MyInputMethodService)
            decorView.setViewTreeSavedStateRegistryOwner(this@MyInputMethodService)
            decorView.setViewTreeViewModelStoreOwner(this@MyInputMethodService)

            ViewCompat.setOnApplyWindowInsetsListener(decorView) { _, insets ->
                val sysBars = insets.getInsets(
                    WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.systemBars()
                )
                val density = resources.displayMetrics.density
                val bottomDp = (sysBars.bottom / density).toInt()

                uiState = uiState.copy(bottomInsetDp = maxOf(bottomDp, 48))
                insets
            }
            val isDark =
                (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            WindowCompat.getInsetsController(window, decorView).isAppearanceLightNavigationBars =
                !isDark
        }
        val composeView = ComposeView(this)
        return composeView.apply {
            setContent {

                val positionProvider = object : PopupPositionProvider {
                    override fun calculatePosition(
                        anchorBounds: IntRect,
                        windowSize: IntSize,
                        layoutDirection: LayoutDirection,
                        popupContentSize: IntSize
                    ): IntOffset {
                        val location = cursorLocation
                        return if (location != null) {
                            val viewLocation = IntArray(2)
                            composeView.getLocationOnScreen(viewLocation)
                            val viewScreenX = viewLocation[0]
                            val viewScreenY = viewLocation[1]

                            var popupX = location.x - viewScreenX
                            var popupY = location.y - viewScreenY

                            if (popupX + popupContentSize.width > windowSize.width) {
                                popupX = windowSize.width - popupContentSize.width
                            }
                            popupX = popupX.coerceAtLeast(0)

                            // Y 轴下边界检查：如果悬浮窗底部会超出屏幕/窗口底部，则将其向上收拢贴紧屏幕底部
                            val maxAllowedY = -popupContentSize.height
                            if (popupY + popupContentSize.height > 0) {
                                popupY = popupY.coerceAtMost(maxAllowedY)
                            }

                            IntOffset(popupX, popupY)
                        } else {
                            // 文本编辑器不支持坐标获取时（cursorLocation == null），悬浮窗口刚好放在左下角（避开底部导航栏 insets）
                            val popupX = -anchorBounds.left
                            val popupY = -popupContentSize.height
                            IntOffset(popupX, popupY)
                        }
                    }
                }

                GboardTheme {
                    HardOrSoftKeyboard(
                        uiState = uiState,
                        actionHandler = this@MyInputMethodService,
                        positionProvider = positionProvider
                    )
                }
            }
        }
    }

    private fun updateSubtypeAndLanguage() {
        val langTag = getCurrentSubtype()
        launchWithMain {
            withRimeContext { session ->
                session.setOption(RimeOptions.ASCII_MODE, langTag.contains("en"))
            }
            uiState = uiState.copy(
                languageLabel = langTag
            )
        }
    }


    //override fun onEvaluateInputViewShown(): Boolean {
    //    return super.onEvaluateInputViewShown()
    //}
    //override fun onConfigurationChanged(newConfig: Configuration) {
    //    super.onConfigurationChanged(newConfig)
    //}

    override fun onStartInput(info: EditorInfo?, restarting: Boolean) {
        super.onStartInput(info, restarting)

        updateSubtypeAndLanguage()
    }


    override fun onWindowShown() {
        super.onWindowShown()
        updateSubtypeAndLanguage()
    }

    private fun getCurrentSubtype(): String {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val currentSubtype = imm.currentInputMethodSubtype
            ?: return uiState.languageLabel.ifEmpty { "en" }
        return convertModeToLangTag(currentSubtype)
    }

    private fun convertModeToLangTag(currentSubtype: InputMethodSubtype?): String {
        if (currentSubtype == null) {
            return uiState.languageLabel.ifEmpty { "en" }
        }
        val mode = currentSubtype.mode ?: ""

        @Suppress("DEPRECATION")
        val locale = currentSubtype.locale
        val langTag = currentSubtype.languageTag

        return if (mode.contains("pinyin", ignoreCase = true) ||
            langTag.startsWith("zh", ignoreCase = true) ||
            locale.startsWith("zh", ignoreCase = true)
        ) {
            "cn"
        } else {
            "en"
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        val langTag = getCurrentSubtype()


        val inputManager: InputManager = getSystemService(INPUT_SERVICE) as InputManager

        if (DEBUG) Log.d(TAG, "onStartInputView: ${inputManager.inputDeviceIds.contentToString()}")

        if (lifecycleRegistry.currentState != Lifecycle.State.RESUMED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
        launchWithMain {
            val options = withRimeContext { session ->
                session.clearComposition()
                session.setOption(RimeOptions.ASCII_MODE, langTag.contains("en"))

                KeyboardPreferencesManager.getOptionsFromSession(session)
            }
            uiState = uiState.copy(
                imeOptions = info?.imeOptions ?: EditorInfo.IME_ACTION_UNSPECIFIED,
                languageLabel = langTag,
                options = options,
            )
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        commitOrClearComposing()

        if (DEBUG) Log.d(TAG, "onFinishInputView: " + resources.configuration.hardKeyboardHidden)

        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        onClearComposing()

        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        RimeManager.onDeployed = null
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        launchWithMain {
            withRimeContext {
                it.close()
                rimeSessionInstance = null
            }
        }
    }

    private fun rimeKeyHandler(event: KeyEvent?): Boolean {
        if (event == null) return false
        if (event.isSystem) return false

        val xk = RimeKeyMap.toX11Keycode(event)

        if (xk == RimeKeyMap.XK_VoidSymbol) return false

        if (uiState.rimeContext == null) {// 切换到硬键盘时,请求获得光标位置
            currentInputConnection?.requestCursorUpdates(
                InputConnection.CURSOR_UPDATE_MONITOR or
                        InputConnection.CURSOR_UPDATE_IMMEDIATE
            )
        }

        val modifier = RimeKeyMap.toRimeModifier(event)

        launchWithMain {
            if (DEBUG) Log.d(TAG, "onKey: $xk $event $modifier")
            val (rimeContext, commitText, handled) = withRimeContext { rimeSession ->

                val handled = rimeSession.processKey(xk, modifier)

                Triple(rimeSession.context, rimeSession.commitText, handled)
            }
            if (handled) {
                if (DEBUG) Log.d(TAG, "onKeyHandled: $rimeContext")
                uiState = uiState.copy(
                    rimeContext = rimeContext,
                    composingText = "",
                    candidates = emptyObjectList()
                )
                if (commitText.isNotEmpty()) {
                    currentInputConnection?.commitText(commitText, 1);
                }
            } else {
                currentInputConnection?.sendKeyEvent(event)
            }
        }
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return rimeKeyHandler(event) || super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return rimeKeyHandler(event) || super.onKeyUp(keyCode, event)
    }

    //override fun onUpdateSelection(
    //    oldSelStart: Int,
    //    oldSelEnd: Int,
    //    newSelStart: Int,
    //    newSelEnd: Int,
    //    candidatesStart: Int,
    //    candidatesEnd: Int
    //) {
    //    super.onUpdateSelection(
    //        oldSelStart,
    //        oldSelEnd,
    //        newSelStart,
    //        newSelEnd,
    //        candidatesStart,
    //        candidatesEnd
    //    )
    //}

    override fun onUpdateCursorAnchorInfo(cursorAnchorInfo: CursorAnchorInfo?) {
        super.onUpdateCursorAnchorInfo(cursorAnchorInfo)
        cursorAnchorInfo ?: return

        val matrix = cursorAnchorInfo.matrix
        val pts = floatArrayOf(
            cursorAnchorInfo.insertionMarkerHorizontal,
            cursorAnchorInfo.insertionMarkerBottom
        )
        matrix.mapPoints(pts)
        val screenX = pts[0].toInt()
        val screenY = pts[1].toInt()

        cursorLocation = IntOffset(screenX, screenY)
    }


    // --- ImeActionHandler Implementation ---

    override fun onKeyClick(keyData: KeyData) {
        val ic = currentInputConnection ?: return

        when (keyData.type) {
            KeyType.CHARACTER -> {
                val textToCommit = keyData.primaryText
                launchWithMain {
                    val (composingText, commitText, candidates) = withRimeContext { session ->
                        if (session.processKey(textToCommit.codePointAt(0), NONE)) {
                            val candidates = rimeGetCandidates(session, 0, 10, 60)
                            RimeResult(
                                session.composingText,
                                session.commitText,
                                candidates,
                                true
                            )
                        } else {
                            RimeResult(
                                "",
                                "${session.commitText}$textToCommit",
                                mutableObjectListOf()
                            )
                        }
                    }
                    uiState = uiState.copy(
                        composingText = composingText,
                        candidates = candidates
                    )

                    if (commitText.isNotEmpty()) {
                        ic.commitText(commitText, 1)
                    }
                }

                if (uiState.shiftState == ShiftState.SHIFTED) {
                    uiState = uiState.copy(shiftState = ShiftState.LOWERCASE)
                }
            }

            KeyType.SHIFT -> {
                val nextShift = when (uiState.shiftState) {
                    ShiftState.LOWERCASE -> ShiftState.SHIFTED
                    ShiftState.SHIFTED -> ShiftState.CAPS_LOCK
                    ShiftState.CAPS_LOCK -> ShiftState.LOWERCASE
                }
                uiState = uiState.copy(shiftState = nextShift)
            }

            KeyType.BACKSPACE -> {
                launchWithMain {
                    val (composingText, _, candidates, consumeKey) = withRimeContext { session ->
                        if (session.processKey(RimeKeyMap.XK_BackSpace, NONE)) {
                            val candidates = rimeGetCandidates(session, 0, 10)
                            RimeResult(session.composingText, "", candidates, true)
                        } else {
                            RimeResult("", "", mutableObjectListOf())
                        }
                    }

                    uiState = uiState.copy(
                        composingText = composingText,
                        candidates = candidates,
                        isCandidatesExpanded = if (candidates.isEmpty()) false else uiState.isCandidatesExpanded
                    )

                    if (!consumeKey) {
                        sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
//                        ic.deleteSurroundingTextInCodePoints(1, 0)
//                        ic.deleteSurroundingText(1, 0)
                    }
                }
            }

            KeyType.MODE_CHANGE -> {
                keyData.targetMode?.let { targetMode ->
                    uiState = uiState.copy(keyboardMode = targetMode)
                }
            }

            KeyType.SPACE -> {
                launchWithMain {
                    val (composingText, commitText, candidates) = withRimeContext { session ->
                        if (session.processKey(0x20, NONE)) {
                            val candidates = rimeGetCandidates(session, 0, 10, 60)
                            RimeResult(session.composingText, session.commitText, candidates, true)
                        } else {
                            RimeResult(
                                "", "${session.commitText} ",
                                mutableObjectListOf(),
                                false
                            )
                        }
                    }
                    uiState = uiState.copy(
                        composingText = composingText,
                        candidates = candidates
                    )

                    if (commitText.isNotEmpty()) {
                        ic.commitText(commitText, 1)
                    }
                }
            }

            KeyType.ENTER -> {
                launchWithMain {
                    val commitText = withRimeContext { session ->
                        if (session.processKey(RimeKeyMap.XK_Return, NONE)) {
                            session.commitText
                        } else {
                            null
                        }
                    }
                    if (commitText != null) {
                        ic.commitText(commitText, 1)
                        onClearComposing()
                    } else {
                        val action = uiState.imeOptions and EditorInfo.IME_MASK_ACTION
                        if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
                            ic.performEditorAction(action)
                        } else {
                            ic.commitText("\n", 1)
                        }
                    }
                }
            }

            KeyType.LANGUAGE -> {
                onLanguageSwitch()
            }
        }
    }

    override fun onKeyLongPress(keyData: KeyData) {
        if (keyData.type == KeyType.SHIFT) {
            uiState = uiState.copy(shiftState = ShiftState.CAPS_LOCK)
        } else if (!keyData.secondaryText.isNullOrEmpty()) {
            launchWithMain {
                val (composingText, commitText, candidates) = withRimeContext {
                    val secondaryText = keyData.secondaryText
                    if (it.processKey(secondaryText.codePointAt(0), NONE)) {
                        val candidates = rimeGetCandidates(it, 0, 10, 60)
                        RimeResult(
                            it.composingText,
                            it.commitText,
                            candidates,
                            true
                        )
                    } else {
                        RimeResult(
                            "",
                            "${it.commitText}$secondaryText",
                            mutableObjectListOf()
                        )
                    }
                }

                uiState = uiState.copy(
                    composingText = composingText,
                    candidates = candidates
                )

                if (commitText.isNotEmpty()) {
                    currentInputConnection?.commitText(commitText, 1)
                }
            }
        }
    }

    override fun onToggleEmojiPanel() {
        uiState = uiState.copy(
            isEmojiOpen = !uiState.isEmojiOpen,
            isMenuOpen = false,
            isTextEditOpen = false,
            isCandidatesExpanded = false
        )
    }

    override fun onEmojiSelect(emoji: String) {
        currentInputConnection?.commitText(emoji, 1)
    }

    override fun onToggleTextEditPanel() {
        uiState = uiState.copy(
            isTextEditOpen = !uiState.isTextEditOpen,
            isEmojiOpen = false,
            isMenuOpen = false,
            isCandidatesExpanded = false
        )
    }

    override fun onToggleMenuPanel() {
        uiState = uiState.copy(
            isMenuOpen = !uiState.isMenuOpen,
            isEmojiOpen = false,
            isTextEditOpen = false,
            isCandidatesExpanded = false
        )
    }

    override fun onToggleRimeOption(option: String) {
        val currentValue = when (option) {
            RimeOptions.ASCII_PUNCT -> uiState.options.asciiPunct
            RimeOptions.TRADITIONALIZATION -> uiState.options.tradition
            RimeOptions.FULL_SHAPE -> uiState.options.fullShape
            RimeOptions.EMOJI -> uiState.options.emoji
            RimeOptions.VIBRATION -> uiState.options.vibrationEnabled
            else -> false
        }
        launchWithMain {
            val newValue = withRimeContext { session ->
                KeyboardPreferencesManager.toggleOption(
                    applicationContext,
                    session,
                    option,
                    currentValue
                )
            }
            val newOptions = when (option) {
                RimeOptions.ASCII_PUNCT -> uiState.options.copy(asciiPunct = newValue)
                RimeOptions.TRADITIONALIZATION -> uiState.options.copy(tradition = newValue)
                RimeOptions.FULL_SHAPE -> uiState.options.copy(fullShape = newValue)
                RimeOptions.EMOJI -> uiState.options.copy(emoji = newValue)
                RimeOptions.VIBRATION -> uiState.options.copy(vibrationEnabled = newValue)
                else -> uiState.options
            }
            uiState = uiState.copy(options = newOptions)
        }
    }

    override fun onOpenAdvancedSettings() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    override fun onTextEditCommand(command: TextMoveCommand) {
        if (command == TextMoveCommand.TOGGLE_SELECT) {
            uiState = uiState.copy(isTextSelecting = !uiState.isTextSelecting)
        } else {
            val keyCode = when (command) {
                TextMoveCommand.UP -> KeyEvent.KEYCODE_DPAD_UP
                TextMoveCommand.DOWN -> KeyEvent.KEYCODE_DPAD_DOWN
                TextMoveCommand.LEFT -> KeyEvent.KEYCODE_DPAD_LEFT
                TextMoveCommand.RIGHT -> KeyEvent.KEYCODE_DPAD_RIGHT
                TextMoveCommand.HOME -> KeyEvent.KEYCODE_MOVE_HOME
                TextMoveCommand.END -> KeyEvent.KEYCODE_MOVE_END
                else -> KeyEvent.KEYCODE_UNKNOWN
            }
            if (keyCode != KeyEvent.KEYCODE_UNKNOWN) {
                if (uiState.isTextSelecting) {
                    val ic = currentInputConnection
                    val eventTime = uptimeMillis()
                    ic?.sendKeyEvent(
                        KeyEvent(
                            eventTime,
                            eventTime,
                            KeyEvent.ACTION_DOWN,
                            KeyEvent.KEYCODE_SHIFT_LEFT,
                            0,
                            KeyEvent.META_SHIFT_ON
                        )
                    )
                    sendDownUpKeyEvents(keyCode)
                    ic?.sendKeyEvent(
                        KeyEvent(
                            eventTime,
                            eventTime,
                            KeyEvent.ACTION_UP,
                            KeyEvent.KEYCODE_SHIFT_LEFT,
                            0,
                            0
                        )
                    )
                } else {
                    sendDownUpKeyEvents(keyCode)
                }
            } else {
                TextMoveActions.execute(currentInputConnection, command, uiState.isTextSelecting)
            }

            if (command == TextMoveCommand.CUT || command == TextMoveCommand.COPY) {
                if (uiState.isTextSelecting) {
                    uiState = uiState.copy(isTextSelecting = false)
                }
            }
        }
    }

    override fun onPageChange(backward: Boolean) {
        launchWithMain {
            val rimeContext = withRimeContext { session ->
                session.changePage(backward)
                session.context
            }
            uiState = uiState.copy(
                rimeContext = rimeContext
            )
        }
    }

    override fun onCandidateSelect(canIdx: Int, pageIdx: Int) {
        if (pageIdx != -1) { // hardKeyboard
            launchWithMain {
                val (commitText, rimeContext) = withRimeContext { session ->
                    session.selectCandidateOnCurrentPage(canIdx)
                    Pair(session.commitText, session.context)
                }

                uiState = uiState.copy(
                    rimeContext = rimeContext
                )
                if (commitText.isNotEmpty()) {
                    currentInputConnection?.commitText(commitText, 1)
                }
            }
            return
        }
        launchWithMain {
            val (composingText, commitText, candidates) = withRimeContext { session ->

                session.selectCandidate(canIdx)
                val composingText = session.composingText
                if (composingText.isNotEmpty()) {
                    val candidates = rimeGetCandidates(
                        session, 0,
                        if (uiState.isCandidatesExpanded) 20 else 10
                    )
                    Triple(composingText, "", candidates)
                } else {
                    Triple("", session.commitText, emptyObjectList())
                }
            }

            if (composingText.isNotEmpty()) {
                uiState = uiState.copy(
                    composingText = composingText,
                    candidates = candidates
                )
            } else {
                if (commitText.isNotEmpty()) {
                    currentInputConnection?.let { ic ->
                        ic.commitText(commitText, 1)
                        onClearComposing()
                    }
                }
            }
        }
    }


    private fun rimeGetCandidates(
        rimeSession: RimeSession,
        index: Int,
        count: Int,
        maxBytes: Int = 0xffff
    ): ObjectList<RimeCandidate> {
        val candidates = MutableObjectList<RimeCandidate>(count).apply {
            rimeSession.getCandidates(index, count, maxBytes)?.let {
                plusAssign(it)
            }
        }
        return candidates
    }

    override fun onToggleCandidatesExpand() {
        uiState = uiState.copy(isCandidatesExpanded = !uiState.isCandidatesExpanded)
    }

    override fun onLanguageSwitch() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                switchToNextInputMethod(true)
            } else {
                val token = window?.window?.attributes?.token
                val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
                if (token != null && imm != null) {
                    imm.switchToNextInputMethod(token, true)
                }
            }
        } catch (_: Exception) {
            // Fallback if no next input method
        }
    }

    override fun onCurrentInputMethodSubtypeChanged(newSubtype: InputMethodSubtype?) {
        launchWithMain {
            val langTag = convertModeToLangTag(newSubtype)
            withRimeContext {
                it.setOption(RimeOptions.ASCII_MODE, langTag.contains("en"))
            }

            uiState = uiState.copy(
                languageLabel = langTag,
            )
        }
        super.onCurrentInputMethodSubtypeChanged(newSubtype)
    }

    override fun onClearComposing() {

        uiState = ImeUiState(
            imeOptions = uiState.imeOptions,
            languageLabel = uiState.languageLabel,
            options = uiState.options,
        )
        currentInputConnection?.finishComposingText()
    }

    override fun onLoadMoreCandidates() {
        launchWithMain {
            val fetchCount = 20
            val oldSize = uiState.candidates.size

            val candidates = withRimeContext { session ->
                MutableObjectList<RimeCandidate>(oldSize + fetchCount).apply {
                    addAll(uiState.candidates)
                    session.getCandidates(oldSize, fetchCount)?.let {
                        plusAssign(it)
                    }
                }
            }
            if (candidates.size > oldSize) {
                uiState = uiState.copy(candidates = candidates)
            }
        }
    }

    private fun commitOrClearComposing() {
        currentInputConnection?.commitText(uiState.composingText, 1)
        onClearComposing()
    }
}

data class RimeResult(
    val composingText: String,
    val commitText: String = "",
    val candidates: ObjectList<RimeCandidate>,
    val consumeKey: Boolean = false
)