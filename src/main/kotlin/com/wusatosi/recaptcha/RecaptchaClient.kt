package com.wusatosi.recaptcha

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import java.io.Closeable

interface RecaptchaClient : Closeable {

    @Throws(RecaptchaError::class)
    suspend fun verify(token: String, remoteIp: String = ""): Boolean

}

enum class ErrorCode {
    InvalidToken,
    TimeOrDuplicatedToken
}

