package com.wusatosi.recaptcha

import com.wusatosi.recaptcha.internal.UniversalRecaptchaClientImpl
import com.wusatosi.recaptcha.internal.checkURLCompatibility
import com.wusatosi.recaptcha.v2.RecaptchaV2Client
import com.wusatosi.recaptcha.v3.RecaptchaV3Client

interface RecaptchaClient {

    @Throws(RecaptchaError::class)
    suspend fun verify(token: String): Boolean

    companion object {

        fun createUniversal(
            secretKey: String,
            defaultScoreThreshold: Double = 0.5,
            useRecaptchaDotNetEndPoint: Boolean = false
        ): RecaptchaClient {
            if (!checkURLCompatibility(secretKey))
                throw InvalidSiteKeyException

            return UniversalRecaptchaClientImpl(
                secretKey,
                defaultScoreThreshold,
                useRecaptchaDotNetEndPoint
            )
        }

        fun createV2(siteKey: String, useRecaptchaDotNetEndpoint: Boolean = false) =
            RecaptchaV2Client.create(siteKey, useRecaptchaDotNetEndpoint)

        fun createV3(
            siteKey: String,
            defaultScoreThreshold: Double = 0.5,
            useRecaptchaDotNetEndpoint: Boolean = false
        ) = RecaptchaV3Client.create(siteKey, defaultScoreThreshold, useRecaptchaDotNetEndpoint)

    }

}