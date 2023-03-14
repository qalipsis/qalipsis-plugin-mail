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

import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Requires
import io.micronaut.core.bind.annotation.Bindable
import io.micronaut.core.util.StringUtils
import jakarta.annotation.Nullable
import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull

/**
 * Configuration for [MailNotificationPublisher].
 *
 * @author Francisca Eze
 */
@Requires(property = "report.export.mail.enabled", value = StringUtils.TRUE, defaultValue = StringUtils.FALSE)
@ConfigurationProperties("report.export.mail")
interface MailNotificationConfiguration {

    @get:Bindable(defaultValue = "false")
    @get:NotNull
    val enabled: Boolean

    @get:Bindable(defaultValue = "ALL")
    @get:NotEmpty
    val status: Set<ReportExecutionStatus>

    @get:Nullable
    val username: String?

    @get:Nullable
    val password: String?

    @get:Bindable(defaultValue = "localhost")
    @get:NotBlank
    val host: String

    @get:Bindable(defaultValue = "25")
    @get:NotNull
    val port: Int

    @get:Bindable(defaultValue = "PLAIN")
    @get:NotNull
    val authenticationMode: AuthenticationMode

    @get:Bindable(defaultValue = "no-reply@qalipsis.io")
    @get:NotBlank
    val from: String

    @get:NotEmpty
    val to: Set<@Email String>

    @get:Nullable
    val cc: Set<@Email String>?

    @get:Bindable(defaultValue = "false")
    @get:NotNull
    val junit: Boolean

    @get:Bindable(defaultValue = "false")
    @get:NotNull
    val ssl: Boolean

    @get:Bindable(defaultValue = "false")
    @get:NotNull
    val starttls: Boolean

}