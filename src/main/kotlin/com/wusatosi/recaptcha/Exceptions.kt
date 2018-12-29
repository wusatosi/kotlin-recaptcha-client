package com.wusatosi.recaptcha

import com.github.kittinunf.fuel.core.Response
import com.google.gson.JsonParseException

class InvalidSiteKeyException: RecaptchaError("site key is invalid", null)

sealed class RecaptchaError(
    message: String,
    cause: Throwable?
) : Error(message, cause)

class UnableToDeserializeError internal constructor(
    cause: JsonParseException,
    val response: Response
) : RecaptchaError(
    "The server did not respond with a valid Json object",
    cause
) {
    override val cause: JsonParseException
        get() = super.cause as JsonParseException
}

class UnexpectedError internal constructor(
    message: String,
    cause: Throwable?
) : RecaptchaError(message, cause)

class UnexpectedJsonStructure internal constructor(
    message: String
) : RecaptchaError(
    "$message, " +
            "check if the latest version is imported, if so, please submit an issue to $issue_address",
    null
)

private const val issue_address = "https://github.com/wusatosi/kotlin-recaptcha-v3-client/issues/new"
