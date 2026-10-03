package com.aa.duanju.feature.library

import android.net.Uri
import android.provider.DocumentsContract

/**
 * 把 SAF 目录 Uri 转成老人看得懂的文字。
 * 例：content://.../tree/primary:Movies -> 卷名"内部存储"，目录名 "Movies"，全路径 "内部存储 / Movies"。
 */
object SourceDirFormat {

    /** 卷名：primary -> 内部存储，其他（如 1234-5678）-> SD 卡 */
    fun volumeName(treeUri: String): String {
        val volumeId = treeId(treeUri)?.substringBefore(':') ?: return "本地存储"
        return if (volumeId.equals("primary", ignoreCase = true)) "内部存储" else "SD 卡"
    }

    /** 目录名：取路径最后一段，取不到时用 fallback */
    fun dirName(treeUri: String, fallback: String): String {
        val name = treeId(treeUri)?.substringAfter(':', "")?.trim('/')?.substringAfterLast('/')?.ifBlank { null }
        return name ?: fallback.takeIf { it.isNotBlank() } ?: "短剧目录"
    }

    /** 完整可读路径，如 "内部存储 / Movies" */
    fun fullPath(treeUri: String, fallback: String): String {
        val path = treeId(treeUri)?.substringAfter(':', "")?.trim('/') ?: ""
        return if (path.isBlank()) volumeName(treeUri) else "${volumeName(treeUri)} / $path"
    }

    private fun treeId(treeUri: String): String? = runCatching {
        DocumentsContract.getTreeDocumentId(Uri.parse(treeUri))
    }.getOrNull()
}
