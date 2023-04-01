package com.wusatosi.recaptcha

import java.io.IOException

/**
 * The client has met major error that prevents the client to verify the given token.
 */
sealed class RecaptchaError(
    message: String,
    cause: Throwable?
) : Error(message, cause)

/**
 * The site key supplied is invalid.
 */
object InvalidSiteKeyException : RecaptchaError("site key is invalid", null)

/**
 * The client has met some unexpected internal error, mainly if Google's response is malformed.
 */
open class UnexpectedError internal constructor(
    message: String,
    cause: Throwable? = null
) : RecaptchaError(message, cause)

/**
 * The client has met a malformed json response from Google.
 */
class UnexpectedJsonStructure internal constructor(
    message: String,
    override val cause: Throwable? = null
) : UnexpectedError(
    message,
    null
)

/**
 * The client has suffered an [IOException] during request.
 */
class RecaptchaIOError internal constructor(cause: IOException) :
    RecaptchaError("IOException during request", cause)