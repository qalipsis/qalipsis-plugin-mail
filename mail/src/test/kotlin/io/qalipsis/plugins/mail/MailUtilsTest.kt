/*
 * Copyright 2022 AERIS IT Solutions GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package io.qalipsis.plugins.mail

import assertk.assertThat
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.qalipsis.plugins.mail.notification.MailUtils
import io.qalipsis.plugins.mail.utils.TestUtil
import io.qalipsis.test.coroutines.TestDispatcherProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors

@MicronautTest(startApplication = false)
internal class MailUtilsTest {

    private lateinit var file: File

    @JvmField
    @RegisterExtension
    val testDispatcherProvider = TestDispatcherProvider()

    @BeforeEach
    fun setup() {
        file = File.createTempFile(CAMPAIGN_KEY, ".zip")
        file.mkdirs()
    }

    @AfterEach
    fun cleanup() {
        val unzippedDirectory = File(DESTINATION_DIRECTORY)
        unzippedDirectory.deleteRecursively()
        file.delete()
    }

    @Test
    fun `should recursively compress a directory`() = testDispatcherProvider.run {
        val reportDirectory = File("$REPORT_FOLDER/${CAMPAIGN_KEY}")
        MailUtils.compressDirectory(reportDirectory, file)
        val zippedFile = File("$reportDirectory.zip")
        assertThat { zippedFile.exists() }
        TestUtil.unzip(file, "$DESTINATION_DIRECTORY/$CAMPAIGN_KEY")
        val filesAfterCompression =
            withContext(Dispatchers.IO) {
                Files.walk(Paths.get("$DESTINATION_DIRECTORY/$CAMPAIGN_KEY"))
            }
                .filter((Files::isRegularFile))
                .collect(Collectors.toList())
        val fileBeforeCompression =
            withContext(Dispatchers.IO) {
                Files.walk(Paths.get("$REPORT_FOLDER/$CAMPAIGN_KEY"))
            }
                .filter(Files::isRegularFile)
                .collect(Collectors.toList())
        assertEquals(filesAfterCompression.size, fileBeforeCompression.size)
        assertTrue(isEqualContent(fileBeforeCompression, filesAfterCompression))
    }

    private fun isEqualContent(fileBeforeCompression: List<Path>, filesAfterCompression: List<Path>): Boolean {
        var isEqualContent = true
        val k = filesAfterCompression.iterator()
        val t = fileBeforeCompression.iterator()
        while (k.hasNext() && t.hasNext()) {
            if(!isEqual(k.next(), t.next())) {
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
        return first!!.contentEquals(second)
    }

    companion object {
        const val REPORT_FOLDER = "src/test/resources/junit-reports"
        const val CAMPAIGN_KEY = "campaign-8"
        const val DESTINATION_DIRECTORY = "src/test/resources/junit-reports/unzip"
    }
}