package com.wusatosi.recaptcha

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import java.io.Closeable

interface RecaptchaClient : Closeable {

    /**
     * Verify the user supplied token.
     *
     * @param token The token user has provided
     * @param remoteIp The remote IP of the user, this field is optional,
     *  if an empty string is provided, it will not be included in the request.
     * @return The result of the verification
     * @throws RecaptchaError This function will throw RecaptchaError when it either suffers IO problems,
     *  or if the response it receives is malformed.
     */
    @Throws(RecaptchaError::class)
    suspend fun verify(token: String, remoteIp: String = ""): Boolean

    /**
     * Close the underlying Http Client
     */
    override fun close()

}

enum class ErrorCode {
    InvalidToken,
    TimeOrDuplicatedToken
}
