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

import io.qalipsis.api.report.ExecutionStatus
import io.qalipsis.plugins.mail.notification.ReportExecutionStatus.ABORTED
import io.qalipsis.plugins.mail.notification.ReportExecutionStatus.ALL
import io.qalipsis.plugins.mail.notification.ReportExecutionStatus.FAILED
import io.qalipsis.plugins.mail.notification.ReportExecutionStatus.SUCCESSFUL
import io.qalipsis.plugins.mail.notification.ReportExecutionStatus.WARNING

/**
 * Report Status for when to trigger notifications.
 *
 * @property ALL triggers notification for successful, failed, warning and aborted campaign statuses
 * @property SUCCESSFUL triggers notification for successful campaign executions only
 * @property WARNING triggers notification for campaign executions with warning status only
 * @property FAILED triggers notification for failed campaign executions only
 * @property ABORTED triggers notification for campaign executions with aborted status only
 *
 * @author Francisca Eze
 */
enum class ReportExecutionStatus(val supportedCampaignStatus: Set<ExecutionStatus>) {
    ALL(setOf(ExecutionStatus.FAILED, ExecutionStatus.WARNING, ExecutionStatus.SUCCESSFUL, ExecutionStatus.ABORTED)),
    FAILED(setOf(ExecutionStatus.FAILED)),
    SUCCESSFUL(setOf(ExecutionStatus.SUCCESSFUL)),
    WARNING(setOf(ExecutionStatus.WARNING)),
    ABORTED(setOf(ExecutionStatus.ABORTED))
}