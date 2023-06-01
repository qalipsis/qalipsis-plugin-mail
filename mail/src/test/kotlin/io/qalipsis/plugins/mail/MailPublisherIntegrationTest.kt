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

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import io.micronaut.context.annotation.Value
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpRequest.DELETE
import io.micronaut.http.HttpRequest.GET
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.micronaut.test.support.TestPropertyProvider
import io.mockk.every
import io.mockk.mockk
import io.qalipsis.api.report.CampaignReport
import io.qalipsis.api.report.ExecutionStatus
import io.qalipsis.plugins.mail.notification.AuthenticationMode
import io.qalipsis.plugins.mail.notification.MailNotificationConfiguration
import io.qalipsis.plugins.mail.notification.MailNotificationPublisher
import io.qalipsis.plugins.mail.notification.ReportExecutionStatus
import io.qalipsis.test.coroutines.TestDispatcherProvider
import io.qalipsis.test.mockk.WithMockk
import jakarta.inject.Inject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Duration
import java.time.Instant
import kotlin.math.pow


@Testcontainers
@WithMockk
@MicronautTest(startApplication = false, propertySources = ["classpath:application-mail-test.yml"])
internal class MailPublisherIntegrationTest : TestPropertyProvider {
    @JvmField
    @RegisterExtension
    val testDispatcherProvider = TestDispatcherProvider()

    private lateinit var mailNotificationConfiguration: MailNotificationConfiguration

    private lateinit var mailNotificationPublisher: MailNotificationPublisher

    private lateinit var campaignReportPrototype: CampaignReport

    @Value("\${report.export.mail.password}")
    private lateinit var mailServerPassword: String

    @Inject
    @field:Client("no-authentication-mail-api")
    private lateinit var httpClient: HttpClient

    override fun getProperties(): Map<String, String> {
        return mapOf(
            "micronaut.http.services.no-authentication-mail-api.url" to
                    "http://localhost:${noAuthenticationContainer.getMappedPort(API_PORT)}"
        )
    }

    @AfterEach
    internal fun cleanup() {
        val deleteRequest: HttpRequest<*> = DELETE<Any>("/email/all")
        httpClient.toBlocking().retrieve(deleteRequest)
    }

    @BeforeEach
    fun setupAll() {
        campaignReportPrototype = CampaignReport(
            campaignKey = "Campaign-1",
            startedMinions = 1000,
            completedMinions = 990,
            successfulExecutions = 990,
            failedExecutions = 10,
            status = ExecutionStatus.SUCCESSFUL,
            scheduledMinions = 4,
            start = Instant.parse("2022-10-29T00:00:00.00Z"),
            end = Instant.parse("2022-11-05T00:00:00.00Z")
        )
        mailNotificationConfiguration = mockk {
            every { enabled } returns true
            every { status } returns setOf(ReportExecutionStatus.ALL)
            every { username } returns USERNAME
            every { password } returns mailServerPassword
            every { host } returns "localhost"
            every { port } returns noAuthenticationContainer.getMappedPort(SMTP_PORT)
            every { from } returns FROM
            every { authenticationMode } returns AuthenticationMode.PLAIN
            every { junit } returns false
            every { to } returns setOf(TO)
            every { cc } returns null
            every { ssl } returns false
            every { starttls } returns false
        }
        mailNotificationPublisher = MailNotificationPublisher(mailNotificationConfiguration, REPORT_FOLDER)
    }

    @Test
    fun `should successfully send a mail with plain authentication and without attachment`() =
        testDispatcherProvider.run {
            // given
            val campaignReport = campaignReportPrototype.copy(failedExecutions = 0)
            val subject = "${campaignReport.campaignKey} ${campaignReport.status}"
            val htmlMessage = composeMessage(campaignReport)

            // when
            mailNotificationPublisher.publish(campaignReport.campaignKey, campaignReport)

            // then
            val response = retrieveBySubject("${campaignReport.campaignKey}+${campaignReport.status}")
            assertThat { response.isNotEmpty() }
            assertThat(response[0]).all {
                prop(MailResponse::subject).isEqualTo(subject)
                prop(MailResponse::html).isEqualTo(htmlMessage)
            }
            val from = response[0].from
            val to = response[0].to
            assertThat(from[0]).all {
                prop(MailContact::address).isEqualTo(FROM)
                prop(MailContact::name).isEqualTo("")
            }
            assertThat(to[0]).all {
                prop(MailContact::address).isEqualTo(TO)
                prop(MailContact::name).isEqualTo("")
            }
        }

    @Test
    fun `should successfully send a mail to cc recipients with plain authentication and without attachment`() =
        testDispatcherProvider.run {
            // given
            val campaignReport = campaignReportPrototype.copy(failedExecutions = 0, campaignKey = "Campaign-2")
            val subject = "${campaignReport.campaignKey} ${campaignReport.status}"
            every { mailNotificationConfiguration.cc } returns setOf(CC)
            val htmlMessage = composeMessage(campaignReport)

            // when
            mailNotificationPublisher.publish(campaignReport.campaignKey, campaignReport)

            // then
            val response = retrieveBySubject("${campaignReport.campaignKey}+${campaignReport.status}")
            assertThat { response.isNotEmpty() }
            assertThat(response[0]).all {
                prop(MailResponse::subject).isEqualTo(subject)
                prop(MailResponse::html).isEqualTo(htmlMessage)
            }
            val from = response[0].from
            val to = response[0].to
            val cc = response[0].cc
            assertThat(from[0]).all {
                prop(MailContact::address).isEqualTo(FROM)
                prop(MailContact::name).isEqualTo("")
            }
            assertThat(to[0]).all {
                prop(MailContact::address).isEqualTo(TO)
                prop(MailContact::name).isEqualTo("")
            }
            assertFalse { cc.isNullOrEmpty() }
            val first = cc?.get(0) as MailContact
            assertThat(first).all {
                prop(MailContact::address).isEqualTo(CC)
                prop(MailContact::name).isEqualTo("")
            }


        }

    @Test
    fun `should support both address and name of the sender`() =
        testDispatcherProvider.run {
            // given
            val campaignReport = campaignReportPrototype.copy(failedExecutions = 0, campaignKey = "Campaign-3")
            val subject = "${campaignReport.campaignKey} ${campaignReport.status}"
            every { mailNotificationConfiguration.from } returns "Qalipsis Sender <$FROM>"
            every { mailNotificationConfiguration.to } returns setOf("Qalipsis Recipient<$TO>")
            every { mailNotificationConfiguration.cc } returns setOf("Qalipsis Carbon Copy <$CC>")
            val htmlMessage = composeMessage(campaignReport)

            // when
            mailNotificationPublisher.publish(campaignReport.campaignKey, campaignReport)

            // then
            val response = retrieveBySubject("${campaignReport.campaignKey}+${campaignReport.status}")
            assertThat { response.isNotEmpty() }
            assertThat(response[0]).all {
                prop(MailResponse::subject).isEqualTo(subject)
                prop(MailResponse::html).isEqualTo(htmlMessage)
            }
            val from = response[0].from
            val to = response[0].to
            val cc = response[0].cc
            assertThat(from[0]).all {
                prop(MailContact::address).isEqualTo(FROM)
                prop(MailContact::name).isEqualTo("Qalipsis Sender")
            }
            assertThat(to[0]).all {
                prop(MailContact::address).isEqualTo(TO)
                prop(MailContact::name).isEqualTo("Qalipsis Recipient")
            }
            assertFalse { cc.isNullOrEmpty() }
            val first = cc?.get(0) as MailContact
            assertThat(first).all {
                prop(MailContact::address).isEqualTo(CC)
                prop(MailContact::name).isEqualTo("Qalipsis Carbon Copy")
            }
        }

    @Test
    fun `should successfully send a mail with username and password authentication and without attachment`() =
        testDispatcherProvider.run {
            // given
            val campaignReport = campaignReportPrototype.copy(failedExecutions = 0, campaignKey = "Campaign-4")
            every { mailNotificationConfiguration.authenticationMode } returns AuthenticationMode.USERNAME_PASSWORD
            val subject = "${campaignReport.campaignKey} ${campaignReport.status}"
            val htmlMessage = composeMessage(campaignReport)

            // when
            mailNotificationPublisher.publish(campaignReport.campaignKey, campaignReport)

            // then
            val response = retrieveBySubject("${campaignReport.campaignKey}+${campaignReport.status}")
            assertThat { response.isNotEmpty() }
            assertThat(response[0]).all {
                prop(MailResponse::subject).isEqualTo(subject)
                prop(MailResponse::html).isEqualTo(htmlMessage)
            }
            val from = response[0].from
            val to = response[0].to
            assertThat(from[0]).all {
                prop(MailContact::address).isEqualTo(FROM)
                prop(MailContact::name).isEqualTo("")
            }
            assertThat(to[0]).all {
                prop(MailContact::address).isEqualTo(TO)
                prop(MailContact::name).isEqualTo("")
            }
        }

    @Test
    fun `should successfully send mail with attachments`() =
        testDispatcherProvider.run {
            // given
            val campaignReport = campaignReportPrototype.copy(failedExecutions = 0, campaignKey = "campaign-7")
            val subject = "${campaignReport.campaignKey} ${campaignReport.status}"
            every { mailNotificationConfiguration.from } returns "Qalipsis Sender <$FROM>"
            every { mailNotificationConfiguration.to } returns setOf("Qalipsis Recipient<$TO>")
            every { mailNotificationConfiguration.cc } returns setOf("Qalipsis Carbon Copy <$CC>")
            every { mailNotificationConfiguration.junit } returns true
            val htmlMessage = composeMessage(campaignReport)

            // when
            mailNotificationPublisher.publish(campaignReport.campaignKey, campaignReport)

            // then
            val response = retrieveBySubject("${campaignReport.campaignKey}+${campaignReport.status}")
            assertTrue { response.isNotEmpty() }
            assertThat(response[0]).all {
                prop(MailResponse::subject).isEqualTo(subject)
                prop(MailResponse::html).isEqualTo(htmlMessage)
            }
            val attachments = response[0].attachments
            assertFalse { attachments.isNullOrEmpty() }
            val firstAttachment = attachments?.get(0) as MailAttachment
            assertThat(firstAttachment).all {
                prop(MailAttachment::fileName).contains(campaignReport.campaignKey)
                prop(MailAttachment::generatedFileName).contains(campaignReport.campaignKey)
                prop(MailAttachment::contentType).isEqualTo("application/zip")
                prop(MailAttachment::contentDisposition).isEqualTo("attachment")
            }
        }

    @Test
    fun `should not send notification when campaign report status is not in the list of subscribed statuses`() =
        testDispatcherProvider.run {
            //given
            every { mailNotificationConfiguration.status } returns setOf(
                ReportExecutionStatus.ABORTED,
                ReportExecutionStatus.FAILED
            )
            val campaignReport =
                campaignReportPrototype.copy(campaignKey = "Campaign-4", status = ExecutionStatus.SUCCESSFUL)

            // when
            mailNotificationPublisher.publish(campaignReport.campaignKey, campaignReport)

            //then
            val response = retrieveBySubject("${campaignReport.campaignKey}+${campaignReport.status}")
            assertThat { response.isNotEmpty() }
        }

    @Test
    fun `should not send notification when campaign report status is not in the list of known report statuses`() =
        testDispatcherProvider.run {
            //given
            every { mailNotificationConfiguration.status } returns setOf(
                ReportExecutionStatus.ABORTED,
                ReportExecutionStatus.FAILED
            )
            val campaignReport =
                campaignReportPrototype.copy(campaignKey = "Campaign-5", status = ExecutionStatus.QUEUED)

            // when
            mailNotificationPublisher.publish(campaignReport.campaignKey, campaignReport)

            //then
            val response = retrieveBySubject("${campaignReport.campaignKey}+${campaignReport.status}")
            assertThat { response.isNotEmpty() }
        }

    @Test
    fun `should send notification when campaign report status is in the list of subscribed statuses`() =
        testDispatcherProvider.run {
            //given
            every { mailNotificationConfiguration.status } returns setOf(
                ReportExecutionStatus.ABORTED,
                ReportExecutionStatus.FAILED
            )
            val campaignReport =
                campaignReportPrototype.copy(campaignKey = "Campaign-6", status = ExecutionStatus.ABORTED)
            val subject = "${campaignReport.campaignKey} ${campaignReport.status}"
            val htmlMessage = composeMessage(campaignReport)

            // when
            mailNotificationPublisher.publish(campaignReport.campaignKey, campaignReport)

            // then
            val response = retrieveBySubject("${campaignReport.campaignKey}+${campaignReport.status}")
            assertThat { response.isNotEmpty() }
            assertThat(response[0]).all {
                prop(MailResponse::subject).isEqualTo(subject)
                prop(MailResponse::html).isEqualTo(htmlMessage)
            }
            val from = response[0].from
            val to = response[0].to
            assertThat(from[0]).all {
                prop(MailContact::address).isEqualTo(FROM)
                prop(MailContact::name).isEqualTo("")
            }
            assertThat(to[0]).all {
                prop(MailContact::address).isEqualTo(TO)
                prop(MailContact::name).isEqualTo("")
            }
        }

    @Test
    fun `should not add attachment to mail notification when files for the campaigns do not exist`() =
        testDispatcherProvider.run {
            // given
            val campaignReport = campaignReportPrototype.copy(failedExecutions = 0, campaignKey = "campaign-19")
            val subject = "${campaignReport.campaignKey} ${campaignReport.status}"
            every { mailNotificationConfiguration.from } returns "Qalipsis Sender <$FROM>"
            every { mailNotificationConfiguration.to } returns setOf("Qalipsis Recipient<$TO>")
            every { mailNotificationConfiguration.cc } returns setOf("Qalipsis Carbon Copy <$CC>")
            every { mailNotificationConfiguration.junit } returns true
            val htmlMessage = composeMessage(campaignReport)

            // when
            mailNotificationPublisher.publish(campaignReport.campaignKey, campaignReport)

            // then
            val response = retrieveBySubject("${campaignReport.campaignKey}+${campaignReport.status}")
            assertThat { response.isNotEmpty() }
            assertThat(response[0]).all {
                prop(MailResponse::subject).isEqualTo(subject)
                prop(MailResponse::html).isEqualTo(htmlMessage)
            }
            val attachments = response[0].attachments
            assertTrue { attachments.isNullOrEmpty() }
        }

    private fun composeMessage(report: CampaignReport): String {
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
                      <td>${report.end ?: "<Running>"}</td>
                    </tr>
                    <tr>
                      <td>Duration</td>
                      <td>${duration?.let { "$it seconds" } ?: "<Running>"}</td>
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

    private fun retrieveBySubject(subject: String): MutableList<MailResponse> {
        val getSubjectRequest: HttpRequest<*> = GET<Any>("/email/?subject=$subject")
        return httpClient.toBlocking().retrieve(getSubjectRequest, Argument.listOf(MailResponse::class.java))
    }

    companion object {
        private const val CC = "qalipsisccuser@test.com"
        private const val TO = "qalipsisuser@test.com"
        private const val FROM = "qualipsistester@test.com"
        private const val USERNAME = "testqalipsis@test.com"
        private const val SMTP_PORT = 25
        private const val API_PORT = 80
        private const val REPORT_FOLDER = "src/test/resources/junit-reports"
        private const val DOCKER_IMAGE = "djfarrelly/maildev"


        @Container
        @JvmStatic
        private val noAuthenticationContainer = GenericContainer<Nothing>(DOCKER_IMAGE).apply {
            withCreateContainerCmdModifier {
                it.hostConfig!!.withMemory(256 * 1024.0.pow(2).toLong()).withCpuCount(2)
            }
            withExposedPorts(API_PORT, SMTP_PORT)
            waitingFor(Wait.forListeningPort())
        }
    }
}
