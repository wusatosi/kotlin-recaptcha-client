package com.wusatosi.recaptcha

import java.io.IOException

sealed class RecaptchaError(
    message: String,
    cause: Throwable?
) : Error(message, cause)

object InvalidSiteKeyException : RecaptchaError("site key is invalid", null)

class UnexpectedError internal constructor(
    message: String,
    cause: Throwable?
) : RecaptchaError(message, cause)

class UnexpectedJsonStructure internal constructor(
    message: String,
    override val cause: Throwable? = null
) : RecaptchaError(
    message,
    null
)

class RecaptchaIOError internal constructor(cause: IOException) :
    RecaptchaError("IOException during request", cause)