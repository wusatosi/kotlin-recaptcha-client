package com.wusatosi.recaptcha.v3

import com.wusatosi.recaptcha.InvalidSiteKeyException
import com.wusatosi.recaptcha.RecaptchaClient
import com.wusatosi.recaptcha.internal.RecaptchaV3ClientImpl
import com.wusatosi.recaptcha.internal.checkURLCompatibility

interface RecaptchaV3Client : RecaptchaClient {

    suspend fun getVerifyScore(
        token: String,
        invalidate_token_score: Double = -1.0,
        timeout_or_duplicate_score: Double = -2.0
    ): Double

    companion object {
        fun create(
            secretKey: String,
            defaultScoreThreshold: Double = 0.5,
            useRecaptchaDotNetEndPoint: Boolean = false
        ): RecaptchaV3Client {
            if (!checkURLCompatibility(secretKey))
                throw InvalidSiteKeyException
            return RecaptchaV3ClientImpl(
                secretKey,
                defaultScoreThreshold,
                useRecaptchaDotNetEndPoint
            )
        }
    }
}
