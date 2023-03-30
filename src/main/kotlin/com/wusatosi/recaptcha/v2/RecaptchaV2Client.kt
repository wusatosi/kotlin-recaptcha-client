package com.wusatosi.recaptcha.v2

import com.wusatosi.recaptcha.*
import com.wusatosi.recaptcha.internal.RecaptchaV2ClientImpl
import com.wusatosi.recaptcha.internal.likelyValidRecaptchaParameter

interface RecaptchaV2Client : RecaptchaClient {

    data class V2ResponseDetail(
        val success: Boolean,
        val host: String
    )

    data class V2Decision(val decision: Boolean, val hostMatch: Boolean)

    @Throws(RecaptchaError::class)
    suspend fun getDetailedResponse(
        token: String,
        remoteIp: String = ""
    ): Either<ErrorCode, Pair<V2ResponseDetail, V2Decision>>

    companion object {
        fun create(
            siteKey: String,
            block: RecaptchaV2Config.() -> Unit = {}
        ): RecaptchaV2Client {
            if (!likelyValidRecaptchaParameter(siteKey))
                throw InvalidSiteKeyException

            val config = RecaptchaV2Config().apply(block)
            return RecaptchaV2ClientImpl(
                siteKey,
                config
            )
        }
    }

}