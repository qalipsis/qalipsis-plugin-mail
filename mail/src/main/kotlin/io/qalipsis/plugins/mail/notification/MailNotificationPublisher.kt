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

package io.qalipsis.plugins.mail.notification

import io.micronaut.context.annotation.Requirements
import io.micronaut.context.annotation.Requires
import io.micronaut.context.annotation.Value
import io.qalipsis.api.context.CampaignKey
import io.qalipsis.api.logging.LoggerHelper.logger
import io.qalipsis.api.report.CampaignReport
import io.qalipsis.api.report.CampaignReportPublisher
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Duration
import java.util.Properties
import javax.activation.DataHandler
import javax.activation.FileDataSource
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.Multipart
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart


/**
 * Custom implementation of a [CampaignReportPublisher] that sends a mail notification
 * at the end of a campaign.
 *
 * @author Francisca Eze
 */
@Singleton
@Requirements(
    Requires(beans = [MailNotificationConfiguration::class])
)
internal class MailNotificationPublisher(
    val mailConfiguration: MailNotificationConfiguration,
    @Value("\${report.export.junit.folder}") private val reportFolder: String
) : CampaignReportPublisher {

    private val properties = Properties()

    override suspend fun publish(campaignKey: CampaignKey, report: CampaignReport) {
        val reportStatus = ReportExecutionStatus.values().firstOrNull { it.name === report.status.toString() }
        withContext(Dispatchers.IO) {
            async {
                if (reportStatus != null && ((mailConfiguration.status.contains(ReportExecutionStatus.ALL)) || mailConfiguration.status.contains(
                        ReportExecutionStatus.valueOf(reportStatus.toString())
                    ))
                ) sendNotification(report)
            }
        }
    }

    private fun sendNotification(report: CampaignReport) {
        setMailProperties()
        val session = getSession(properties)
        val message = MimeMessage(session)
        val attachmentFile = File.createTempFile(report.campaignKey, ".zip")
        attachmentFile.mkdirs()
        try {
            message.apply {
                subject = "${report.campaignKey} ${report.status}"
                setFrom(InternetAddress(mailConfiguration.from))
                setRecipients(Message.RecipientType.TO, mailConfiguration.to.map { InternetAddress(it) }.toTypedArray())
                if (!mailConfiguration.cc.isNullOrEmpty()) {
                    setRecipients(
                        Message.RecipientType.CC,
                        mailConfiguration.cc!!.map { InternetAddress(it) }.toTypedArray()
                    )
                }
            }
            val textPart = MimeBodyPart()
            val multipart: Multipart = MimeMultipart()
            textPart.setText(composeMessageBody(report), CHARSET, CONTENT_TYPE)
            multipart.addBodyPart(textPart)

            // Add the Junit report as attachments if it exists.
            if (mailConfiguration.junit && reportFolder.isNotEmpty() && File(reportFolder).exists() && File(reportFolder).isDirectory) {
                val reportDirectory = File("$reportFolder/${report.campaignKey}")
                if (reportDirectory.exists() && (reportDirectory.listFiles()?.size ?: 0) > 0) {
                    val attachmentPart = MimeBodyPart()
                    MailUtils.compressDirectory(reportDirectory, attachmentFile)
                    attachmentPart.dataHandler = DataHandler(FileDataSource(attachmentFile))
                    attachmentPart.fileName = attachmentFile.name
                    multipart.addBodyPart(attachmentPart)
                }
            }
            message.setContent(multipart)
            Transport.send(message)
            logger.info { "Mail sent successfully" }
        } catch (e: Exception) {
            logger.error(e) { "Was not able to send mail: ${e.message}" }
            throw e
        } finally {
            if (attachmentFile.exists()) {
                val deleteSuccess = attachmentFile.delete()
                logger.debug { "Deletion of the temporary ZIP report archive $attachmentFile: $deleteSuccess" }
            }
        }
    }

    private fun composeMessageBody(report: CampaignReport): String {
        val duration = report.end?.let { Duration.between(report.start, it).toSeconds() }
        return """
            <!DOCTYPE html>
            <html>
                <head>
                    <style>
                        table, th, td {
                          border:1px solid black;
                        }
                    </style>
                </head>
            <body>
                <table>
                    <tr>
                      <td>Campaign</td>
                      <td>${report.campaignKey}</td>
                    </tr>
                    <tr>
                      <td>Start</td>
                      <td>${report.start}</td>
                    </tr>
                    <tr>
                      <td>End</td>
                      <td>${report.end ?: RUNNING_INDICATOR}</td>
                    </tr>
                    <tr>
                      <td>Duration</td>
                      <td>${duration?.let { "$it seconds" } ?: RUNNING_INDICATOR}</td>
                    </tr>
                    <tr>
                      <td>Started minions</td>
                      <td>${report.startedMinions}</td> 
                    </tr>
                    <tr>
                      <td>Completed minions</td>
                      <td>${report.completedMinions}</td>
                    </tr>
                    <tr>
                      <td>Successful steps executions</td>
                      <td>${report.successfulExecutions}</td>
                    </tr>
                    <tr>
                      <td>Failed steps executions</td>
                      <td>${report.failedExecutions}</td>
                    </tr>
                    <tr>
                      <td>Status</td>
                      <td>${report.status}</td>
                    </tr>
                </table>
            </body></html>
        """.trimIndent()
    }

    /**
     * Returns key, value [Properties] pair to be used for mail session configuration.
     */
    private fun setMailProperties() {
        properties["mail.smtp.host"] = mailConfiguration.host
        properties["mail.smtp.port"] = mailConfiguration.port
        properties["mail.smtp.auth"] = mailConfiguration.authenticationMode == AuthenticationMode.USERNAME_PASSWORD
        properties["mail.smtp.ssl.enable"] = mailConfiguration.ssl
        properties["mail.smtp.starttls.enable"] = mailConfiguration.starttls
        properties["mail.transport.protocol"] = TRANSPORT_PROTOCOL
    }

    /**
     * Returns the default or a new mail session based on the configured properties and [AuthenticationMode].
     *
     * @param properties persistent set of properties to configure mail session
     */
    private fun getSession(properties: Properties): Session {
        return if (mailConfiguration.authenticationMode == AuthenticationMode.USERNAME_PASSWORD) {
            Session.getInstance(properties, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(mailConfiguration.username, mailConfiguration.password)
                }
            })
        } else {
            Session.getDefaultInstance(properties, null)
        }
    }

    /**
     * @property RUNNING_INDICATOR alternate value to replace missing duration or end values
     * @property TRANSPORT_PROTOCOL specifies the protocol type to be used in configuring mail properties
     * @property logger to log mail publishing events
     * @property CHARSET character set used in encoding mail properties
     * @property CONTENT_TYPE specifies the content type to be used by [MimeBodyPart]
     */
    companion object {
        private const val RUNNING_INDICATOR = "<Running>"
        private const val TRANSPORT_PROTOCOL = "smtp"
        private const val CONTENT_TYPE = "html"
        private const val CHARSET = "utf-8"
        private val logger = logger()
    }
}
