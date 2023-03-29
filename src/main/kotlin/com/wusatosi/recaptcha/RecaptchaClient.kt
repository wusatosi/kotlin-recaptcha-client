package com.wusatosi.recaptcha

import com.wusatosi.recaptcha.internal.UniversalRecaptchaClientImpl
import com.wusatosi.recaptcha.internal.likelyValidRecaptchaParameter
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import java.io.Closeable

interface RecaptchaClient : Closeable {

    @Throws(RecaptchaError::class)
    suspend fun verify(token: String): Boolean

    companion object {

        fun createUniversal(
            secretKey: String,
            defaultScoreThreshold: Double = 0.5,
            useRecaptchaDotNetEndPoint: Boolean = false,
            engine: HttpClientEngine = CIO.create()
        ): RecaptchaClient {
            if (!likelyValidRecaptchaParameter(secretKey))
                throw InvalidSiteKeyException

            return UniversalRecaptchaClientImpl(
                secretKey,
                defaultScoreThreshold,
                useRecaptchaDotNetEndPoint,
                engine
            )
        }

    }

}