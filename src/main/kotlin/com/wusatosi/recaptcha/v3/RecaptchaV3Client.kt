package com.wusatosi.recaptcha.v3

import com.wusatosi.recaptcha.*
import com.wusatosi.recaptcha.internal.RecaptchaV3ClientImpl
import com.wusatosi.recaptcha.internal.likelyValidRecaptchaParameter
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import kotlin.jvm.Throws

interface RecaptchaV3Client : RecaptchaClient {

    class V3ResponseDetail(
        val success: Boolean,
        val host: String,
        val score: Double,
        val action: String,
    )

    class V3Decision(
        val decision: Boolean,
        val hostMatch: Boolean,
        val suggestedThreshold: Double
    )

    @Throws(RecaptchaError::class)
    suspend fun getDetailedResponse(
        token: String,
        remoteIp: String = ""
    ): Either<ErrorCode, Pair<V3ResponseDetail, V3Decision>>

    @Throws(RecaptchaError::class)
    suspend fun getVerifyScore(
        token: String,
        invalidateTokenScore: Double = -1.0,
        timeoutOrDuplicateScore: Double = -2.0,
        remoteIp: String = ""
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


            val config = RecaptchaV3Config()
            config.scoreThreshold = defaultScoreThreshold
            config.useAlternativeDomain = useRecaptchaDotNetEndPoint
            config.engine = engine

            return RecaptchaV3ClientImpl(
                secretKey,
                config
            )
        }
    }
}
