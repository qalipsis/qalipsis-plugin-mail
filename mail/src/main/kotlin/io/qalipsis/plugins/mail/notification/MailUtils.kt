/*
 * QALIPSIS
 * Copyright (C) 2025 AERIS IT Solutions GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package io.qalipsis.plugins.mail.notification

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Contains utility methods to handle file compression to zip.
 *
 * @author Francisca Eze
 */
internal object MailUtils {

    fun compressDirectory(reportDirectory: File, attachmentFile: File) {
        val collectedFiles = mutableListOf<File>()
        collectFiles(reportDirectory, collectedFiles)
        FileOutputStream(attachmentFile).use { fos ->
            ZipOutputStream(fos).use { zos ->
                collectedFiles.forEach { fileToCompress ->
                    val name = fileToCompress.relativeTo(reportDirectory).path.replace(File.separatorChar, '/')
                    zos.putNextEntry(ZipEntry(name))
                    FileInputStream(fileToCompress).use { fis ->
                        val buffer = ByteArray(1024)
                        var length: Int
                        while (fis.read(buffer).also { length = it } > 0) {
                            zos.write(buffer, 0, length)
                        }
                        zos.closeEntry()
                    }
                }
            }
        }
    }

    private fun collectFiles(directory: File, fileList: MutableCollection<File>) {
        directory.listFiles()?.forEach { file ->
            if (file.isFile) {
                fileList.add(file)
            } else if (file.isDirectory) {
                collectFiles(file, fileList)
            }
        }
    }
}