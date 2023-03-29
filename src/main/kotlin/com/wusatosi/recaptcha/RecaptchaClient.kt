package com.wusatosi.recaptcha

import com.wusatosi.recaptcha.internal.UniversalRecaptchaClientImpl
import com.wusatosi.recaptcha.internal.likelyValidRecaptchaParameter
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import java.io.Closeable

interface RecaptchaClient : Closeable {

    @Throws(RecaptchaError::class)
    suspend fun verify(token: String, remoteIp: String = ""): Boolean

    companion object {

        fun createUniversal(
            secretKey: String,
            defaultScoreThreshold: Double = 0.5,
            useRecaptchaDotNetEndPoint: Boolean = false,
            engine: HttpClientEngine = CIO.create()
        ): RecaptchaClient {
            if (!likelyValidRecaptchaParameter(secretKey))
                throw InvalidSiteKeyException

            val config = RecaptchaV3Config()
            config.scoreThreshold = defaultScoreThreshold
            config.useAlternativeDomain = useRecaptchaDotNetEndPoint
            config.engine = engine

            return UniversalRecaptchaClientImpl(
                secretKey,
                config
            )
        }

    }

}