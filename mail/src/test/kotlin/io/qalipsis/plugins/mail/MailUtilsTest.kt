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

package io.qalipsis.plugins.mail

import assertk.assertThat
import assertk.assertions.isTrue
import io.qalipsis.plugins.mail.notification.MailUtils
import io.qalipsis.plugins.mail.utils.TestUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors

internal class MailUtilsTest {

    @TempDir
    private lateinit var tempDir: File

    @TempDir
    private lateinit var unzipDirectory: File

    private lateinit var zippedFile: File

    @BeforeEach
    fun setup() {
        zippedFile = File.createTempFile(CAMPAIGN_KEY, ".zip", tempDir)
    }

    @Test
    fun `should recursively compress a directory`() {
        // given
        val reportDirectory = copyJunitReportsToTemp()

        // when
        MailUtils.compressDirectory(reportDirectory.toFile(), zippedFile)

        // then
        assertThat(zippedFile).transform("exists") { it.exists() }.isTrue()

        TestUtil.unzip(zippedFile, unzipDirectory.absolutePath)
        val fileBeforeCompression =
            Files.walk(reportDirectory)
                .filter(Files::isRegularFile)
                .collect(Collectors.toList())
        val filesAfterCompression =
                Files.walk(unzipDirectory.toPath())
                .filter((Files::isRegularFile))
                .collect(Collectors.toList())
        assertEquals(filesAfterCompression.size, fileBeforeCompression.size)
        assertTrue(isEqualContent(fileBeforeCompression, filesAfterCompression))
    }

    private fun isEqualContent(fileBeforeCompression: List<Path>, filesAfterCompression: List<Path>): Boolean {
        var isEqualContent = true
        val k = filesAfterCompression.iterator()
        val t = fileBeforeCompression.iterator()
        while (k.hasNext() && t.hasNext()) {
            if (!isEqual(k.next(), t.next())) {
                isEqualContent = false
                break
            }
        }
        return isEqualContent
    }

    private fun isEqual(firstFile: Path, secondFile: Path): Boolean {
        if (Files.size(firstFile) != Files.size(secondFile)) {
            return false
        }
        val first = Files.readString(firstFile)
        val second = Files.readString(secondFile)
        return first.contentEquals(second)
    }

    private fun copyJunitReportsToTemp(): Path {
        val reportDirectory = Files.createTempDirectory(REPORT_FOLDER)
        val fileList = this.javaClass.getResourceAsStream("/$REPORT_FOLDER/index.txt")!!
            .bufferedReader().readLines()
        for (relativePath in fileList) {
            val inputStream = this.javaClass.getResourceAsStream("/$REPORT_FOLDER/$relativePath")
            val targetFile = File(reportDirectory.toFile(), relativePath)
            targetFile.parentFile.mkdirs()

            inputStream.use { input ->
                targetFile.outputStream().use { output ->
                    input!!.copyTo(output)
                }
            }
        }

        return reportDirectory
    }

    companion object {
        const val REPORT_FOLDER = "junit-reports"
        const val CAMPAIGN_KEY = "campaign-8"
    }
}