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

    companion object {
        fun create(
            secretKey: String,
            block: RecaptchaV3Config.()  -> Unit = {}
        ): RecaptchaV3Client {
            if (!likelyValidRecaptchaParameter(secretKey))
                throw InvalidSiteKeyException

            val config = RecaptchaV3Config()
            block(config)

            return RecaptchaV3ClientImpl(
                secretKey,
                config
            )
        }
    }
}
