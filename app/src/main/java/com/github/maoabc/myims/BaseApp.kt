package com.github.maoabc.myims

import android.app.Application
import com.github.maoabc.myims.ime.RimeManager
import java.io.File

class BaseApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Thread {
            RimeManager.ensureInitialized(this)
        }.start()
    }

    public fun getRimeDir(): String {
        return File(filesDir, "rime").absolutePath
    }
}
