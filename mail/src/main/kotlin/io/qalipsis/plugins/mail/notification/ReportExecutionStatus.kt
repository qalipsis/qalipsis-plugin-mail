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

import io.qalipsis.api.report.ExecutionStatus

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