package com.wusatosi.recaptcha

import com.wusatosi.recaptcha.internal.UniversalRecaptchaClientImpl
import com.wusatosi.recaptcha.internal.checkURLCompatibility
import com.wusatosi.recaptcha.v2.RecaptchaV2Client
import com.wusatosi.recaptcha.v3.RecaptchaV3Client
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
            if (!checkURLCompatibility(secretKey))
                throw InvalidSiteKeyException

            return UniversalRecaptchaClientImpl(
                secretKey,
                defaultScoreThreshold,
                useRecaptchaDotNetEndPoint,
                engine
            )
        }

        fun createV2(
            siteKey: String,
            useRecaptchaDotNetEndpoint: Boolean = false,
            engine: HttpClientEngine = CIO.create()
        ) =
            RecaptchaV2Client.create(siteKey, useRecaptchaDotNetEndpoint, engine)

        fun createV3(
            siteKey: String,
            defaultScoreThreshold: Double = 0.5,
            useRecaptchaDotNetEndpoint: Boolean = false,
            engine: HttpClientEngine = CIO.create()
        ) = RecaptchaV3Client.create(siteKey, defaultScoreThreshold, useRecaptchaDotNetEndpoint, engine)

    }

}