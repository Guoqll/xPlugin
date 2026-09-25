package com.frame.xplugins.getx.path

import com.frame.xplugins.getx.naming.DartNamingConverter
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Path

data class FlutterPathContext(
    val parentDirectory: VirtualFile,
    val libDirectory: VirtualFile,
)

object FlutterProjectPathResolver {

    fun resolve(selectedDirectory: VirtualFile): FlutterPathContext? {
        if (!selectedDirectory.isDirectory) return null
        var current: VirtualFile? = selectedDirectory
        while (current != null && current.name != "lib") current = current.parent
        val lib = current ?: return null
        return FlutterPathContext(selectedDirectory, lib)
    }

    fun businessDomain(context: FlutterPathContext, normalizedRouteName: String): String {
        val relative = VfsUtilCore.getRelativePath(context.parentDirectory, context.libDirectory, '/')
            ?.split('/')
            ?.filter { it.isNotBlank() }
            .orEmpty()
        return if (relative.firstOrNull() == "features") {
            relative.getOrNull(1)?.let { DartNamingConverter.routeName(it).snakeCase } ?: normalizedRouteName
        } else {
            normalizedRouteName
        }
    }

    fun relativeImport(fromDirectoryPath: String, targetFilePath: String): String {
        val relative = Path.of(fromDirectoryPath).relativize(Path.of(targetFilePath)).toString()
            .replace('\\', '/')
        return if (relative.startsWith(".")) relative else relative
    }
}
