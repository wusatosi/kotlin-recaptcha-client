package com.wusatosi.recaptcha.v3

import com.wusatosi.recaptcha.InvalidSiteKeyException
import com.wusatosi.recaptcha.RecaptchaClient
import com.wusatosi.recaptcha.RecaptchaError
import com.wusatosi.recaptcha.internal.RecaptchaV3ClientImpl
import com.wusatosi.recaptcha.internal.likelyValidRecaptchaParameter
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import kotlin.jvm.Throws

interface RecaptchaV3Client : RecaptchaClient {

    @Throws(RecaptchaError::class)
    suspend fun getVerifyScore(
        token: String,
        invalidateTokenScore: Double = -1.0,
        timeoutOrDuplicateScore: Double = -2.0
    ): Double

    companion object {
        fun create(
            secretKey: String,
            defaultScoreThreshold: Double = 0.5,
            useRecaptchaDotNetEndPoint: Boolean = false,
            engine: HttpClientEngine = CIO.create()
        ): RecaptchaV3Client {
            if (!likelyValidRecaptchaParameter(secretKey))
                throw InvalidSiteKeyException
            return RecaptchaV3ClientImpl(
                secretKey,
                defaultScoreThreshold,
                useRecaptchaDotNetEndPoint,
                engine
            )
        }
    }
}
