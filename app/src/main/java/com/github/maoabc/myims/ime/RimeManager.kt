package com.github.maoabc.myims.ime

import android.content.Context
import android.util.Log
import com.github.maoabc.librime.RimeAPI
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

object RimeManager {
    private var isInitialized = false
    var onDeployed: (() -> Unit)? = null

    @Synchronized
    fun deploy(context: Context): Boolean {
        ensureInitialized(context.applicationContext)
        val success = RimeAPI.deploy()
        RimeAPI.syncUserData()
        if (success) {
            onDeployed?.invoke()
        }
        return success
    }


    @Synchronized
    fun ensureInitialized(appContext: Context) {
        if (isInitialized) return
        val rimeDir = File(appContext.filesDir, "rime")
        if (!rimeDir.exists()) {
            rimeDir.mkdirs()
            unzipAssetsRime(appContext, rimeDir)
        }

        // 实现回调相关事件处理
        RimeAPI.rimeInit(rimeDir.absolutePath, false) { _, type, value ->
            Log.d("rime", "type: $type, value: $value ")
        }
        isInitialized = true
    }

    private fun unzipAssetsRime(appContext: Context, rimeDir: File) {
        try {
            appContext.assets.open("rime.zip").use { inputStream ->
                ZipInputStream(inputStream).use { zis ->
                    var zipEntry = zis.nextEntry
                    while (zipEntry != null) {
                        val newFile = File(rimeDir.parentFile, zipEntry.name)
                        newFile.parentFile?.mkdirs()
                        if (!zipEntry.isDirectory) {
                            FileOutputStream(newFile).use { fos ->
                                val buffer = ByteArray(8192)
                                var len: Int
                                while (zis.read(buffer).also { len = it } != -1) {
                                    if (len > 0) {
                                        fos.write(buffer, 0, len)
                                    }
                                }
                            }
                        }
                        zipEntry = zis.nextEntry
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
