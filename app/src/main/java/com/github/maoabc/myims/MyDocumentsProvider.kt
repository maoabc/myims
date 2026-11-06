package com.github.maoabc.myims

import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException


private const val MIME_TYPE_ANY = "*/*"
private const val MIME_TYPE_TEXT = "text/plain"
private const val MIME_TYPE_OCTET = "application/octet-stream"

private val TEXT_EXTENSIONS = arrayOf(
    "lua",
    "json",
    "yml",
    "yaml"
)


private val DEFAULT_ROOT_PROJECTION = arrayOf(
    Root.COLUMN_ROOT_ID,
    Root.COLUMN_FLAGS,
    Root.COLUMN_ICON,
    Root.COLUMN_TITLE,
    Root.COLUMN_DOCUMENT_ID,
    Root.COLUMN_MIME_TYPES,
)

private val DEFAULT_DOCUMENT_PROJECTION = arrayOf(
    Document.COLUMN_DOCUMENT_ID,
    Document.COLUMN_MIME_TYPE,
    Document.COLUMN_DISPLAY_NAME,
    Document.COLUMN_LAST_MODIFIED,
    Document.COLUMN_FLAGS,
    Document.COLUMN_SIZE
)

class MyDocumentsProvider : DocumentsProvider() {

    private lateinit var rootDir: File

    private val File.docId: String
        get() {
            if (this == rootDir) return rootDir.name
            val parentFile = rootDir.parentFile ?: return rootDir.name

            return toRelativeString(parentFile)
        }

    private val File.mimeType: String
        get() = when {
            isDirectory -> Document.MIME_TYPE_DIR
            TEXT_EXTENSIONS.contains(extension) -> MIME_TYPE_TEXT
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                ?: MIME_TYPE_OCTET
        }

    private fun isChildFile(parent: File, child: File): Boolean {
        var current: File? = child

        while (current != null) {
            if (current == parent) {
                return true
            }
            current = current.parentFile
        }
        return false
    }

    @Throws(IOException::class)
    private fun String.idToFile(): File {
        val file = File(rootDir.parentFile, this).canonicalFile
        if (isChildFile(rootDir, file)) {
            return file
        }
        // 使用 ../xx 尝试进入父目录, 限制它在rootDir之下
        return rootDir
    }


    override fun onCreate(): Boolean {
        rootDir = context!!.filesDir.canonicalFile ?: return false
        println("rime:provide $rootDir  ${rootDir.docId}")


        return true
    }

    private fun createNewFile(parentDocumentId: String, displayName: String): File {
        val parent = parentDocumentId.idToFile()
        var newFile = File(parent, displayName)
        var idx = 1
        while (newFile.exists()) {
            newFile = File(parent, "$displayName($idx)")
            idx += 1
        }
        return newFile
    }


    override fun queryRoots(projection: Array<String>?) =
        MatrixCursor(projection ?: DEFAULT_ROOT_PROJECTION).apply {
            newRow().apply {
                add(Root.COLUMN_ROOT_ID, rootDir.docId)
                add(
                    Root.COLUMN_FLAGS,
                    Root.FLAG_SUPPORTS_CREATE or Root.FLAG_LOCAL_ONLY or Root.FLAG_SUPPORTS_SEARCH or Root.FLAG_SUPPORTS_IS_CHILD
                )
                add(Root.COLUMN_ICON, R.mipmap.ic_launcher)
                add(Root.COLUMN_TITLE, context!!.getString(R.string.app_name))
                add(Root.COLUMN_DOCUMENT_ID, rootDir.docId)
                add(Root.COLUMN_MIME_TYPES, MIME_TYPE_ANY)
            }
        }


    @Throws(FileNotFoundException::class)
    private fun MatrixCursor.newRow(file: File) {
        if (!file.exists()) {
            throw FileNotFoundException("${file.absolutePath} not found")
        }

        val mimeType = file.mimeType
        var flags =
            Document.FLAG_SUPPORTS_COPY
        if (file.canWrite()) {
            flags = flags or
                    Document.FLAG_SUPPORTS_DELETE or
                    Document.FLAG_SUPPORTS_RENAME or
                    Document.FLAG_SUPPORTS_MOVE or
                    if (file.isDirectory) {
                        Document.FLAG_DIR_SUPPORTS_CREATE
                    } else {
                        Document.FLAG_SUPPORTS_WRITE
                    }
        }

        this.newRow().apply {
            add(Document.COLUMN_DOCUMENT_ID, file.docId)
            add(Document.COLUMN_MIME_TYPE, mimeType)
            add(Document.COLUMN_DISPLAY_NAME, file.name)
            add(Document.COLUMN_LAST_MODIFIED, file.lastModified())
            add(Document.COLUMN_FLAGS, flags)
            add(Document.COLUMN_SIZE, file.length())
        }
    }

    override fun queryDocument(documentId: String, projection: Array<out String>?) =
        MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION).apply {
            newRow(documentId.idToFile())
        }

    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<String>?,
        sortOrder: String?
    ) = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION).apply {
        parentDocumentId.idToFile().listFiles()?.forEach {
            newRow(it)
        }
    }

    override fun openDocument(
        documentId: String,
        mode: String,
        signal: CancellationSignal?
    ): ParcelFileDescriptor {
        return ParcelFileDescriptor.open(
            documentId.idToFile(),
            ParcelFileDescriptor.parseMode(mode)
        )
    }


    @Throws(FileNotFoundException::class)
    override fun createDocument(
        parentDocumentId: String,
        mimeType: String,
        displayName: String
    ): String {
        val newFile = createNewFile(parentDocumentId, displayName)
        try {
            val ok = if (mimeType == Document.MIME_TYPE_DIR) {
                newFile.mkdir()
            } else {
                newFile.createNewFile()
            }
            if (!ok) {
                throw FileNotFoundException("createDocument ${newFile.path} failed")
            }
        } catch (e: IOException) {
            throw FileNotFoundException("createDocument ${newFile.path} failed: ${e.localizedMessage}")
        }
        return newFile.docId
    }

    @Throws(FileNotFoundException::class)
    override fun deleteDocument(documentId: String) {
        val file = documentId.idToFile()

        if (file == rootDir) {
            throw FileNotFoundException("Cannot delete root directory!")
        }

        file.apply {
            val ok = if (isDirectory) {
                deleteRecursively()
            } else {
                delete()
            }
            if (!ok) {
                throw FileNotFoundException("deleteDocument $documentId failed")
            }
        }
    }

    override fun getDocumentType(documentId: String): String {
        return documentId.idToFile().mimeType
    }

    override fun isChildDocument(parentDocumentId: String, documentId: String): Boolean {
        return documentId.startsWith(parentDocumentId)
    }

    @Throws(FileNotFoundException::class)
    override fun copyDocument(sourceDocumentId: String, targetParentDocumentId: String): String {
        val oldFile = sourceDocumentId.idToFile()
        val newFile = createNewFile(targetParentDocumentId, oldFile.name)
        oldFile.apply {
            try {
                val ok = if (isDirectory) {
                    copyRecursively(newFile)
                } else {
                    copyTo(newFile).exists()
                }
                if (!ok) {
                    throw FileNotFoundException("copyDocument $sourceDocumentId to ${newFile.docId} failed")
                }
            } catch (e: Exception) {
                throw FileNotFoundException("copyDocument $sourceDocumentId to ${newFile.docId} failed: ${e.message}")
            }
        }
        return newFile.docId
    }

    @Throws(FileNotFoundException::class)
    override fun renameDocument(documentId: String, displayName: String): String {
        val oldFile = documentId.idToFile()
        val newFile = oldFile.resolveSibling(displayName)
        if (newFile.exists()) {
            throw FileNotFoundException("renameDocument $displayName target exists")
        }
        oldFile.renameTo(newFile)
        return newFile.docId
    }


    @Throws(FileNotFoundException::class)
    override fun moveDocument(
        sourceDocumentId: String,
        sourceParentDocumentId: String,
        targetParentDocumentId: String
    ): String {
        val oldFile = sourceDocumentId.idToFile()
        val newFile = createNewFile(targetParentDocumentId, oldFile.name)

        if (!oldFile.renameTo(newFile)) {
            throw FileNotFoundException("renameDocument $targetParentDocumentId failed")
        }
        return newFile.docId
    }

    @Throws(FileNotFoundException::class)
    override fun querySearchDocuments(
        rootId: String,
        query: String,
        projection: Array<String>?
    ) = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION).apply {
        val q = query.lowercase()
        rootId.idToFile().walk()
            .maxDepth(10)
            .filter { it.name.lowercase().contains(q) }
            .take(50)
            .forEach { newRow(it) }
    }


}
