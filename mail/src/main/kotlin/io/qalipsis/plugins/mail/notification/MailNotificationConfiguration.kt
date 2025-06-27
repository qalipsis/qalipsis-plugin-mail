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