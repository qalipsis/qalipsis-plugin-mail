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

package io.qalipsis.plugins.mail.utils

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipInputStream

/**
 * Contains utility methods to handle unzipping of files for test purposes.
 *
 * @author Francisca Eze
 */
internal object TestUtil {
    fun unzip(zipFile: File, destDir: String) {
        val dir = File(destDir)
        // create output directory if it doesn't exist
        if (!dir.exists()) dir.mkdirs()
        val fis: FileInputStream
        //buffer for read and write data to file
        val buffer = ByteArray(1024)
        try {
            fis = FileInputStream(zipFile)
            val zis = ZipInputStream(fis)
            var ze = zis.nextEntry
            while (ze != null) {
                val fileName = ze.name
                val newFile = File(destDir + File.separator + fileName)
                //create directories for sub directories in zip
                File(newFile.parent).mkdirs()
                val fos = FileOutputStream(newFile)
                var len: Int
                while (zis.read(buffer).also { len = it } > 0) {
                    fos.write(buffer, 0, len)
                }
                fos.close()
                //close this ZipEntry
                zis.closeEntry()
                ze = zis.nextEntry
            }
            //close last ZipEntry
            zis.closeEntry()
            zis.close()
            fis.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}