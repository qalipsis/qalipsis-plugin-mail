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

import io.micronaut.core.annotation.Introspected

/**
 * Custom mail response class to retrieve basic mail info from api response.
 *
 * @author Francisca Eze
 */
@Introspected
class MailResponse(
    val from: Array<MailContact>,
    val to: Array<MailContact>,
    val cc: Array<MailContact>?,
    val subject: String,
    val html: String,
    val attachments: Array<MailAttachment>?
)

/**
 * Response class to retrieve contact info values from api response.
 *
 * @author Francisca Eze
 */
@Introspected
class MailContact(val address: String, val name: String)

/**
 * Response class to retrieve mail attachment values from api response.
 *
 * @author Francisca Eze
 */
@Introspected
class MailAttachment(
    val contentType: String,
    val contentDisposition: String,
    val fileName: String,
    val generatedFileName: String
)
