package com.wusatosi.recaptcha.v2

import com.wusatosi.recaptcha.InvalidSiteKeyException
import com.wusatosi.recaptcha.RecaptchaClient
import com.wusatosi.recaptcha.RecaptchaV2Config
import com.wusatosi.recaptcha.internal.RecaptchaV2ClientImpl
import com.wusatosi.recaptcha.internal.likelyValidRecaptchaParameter

interface RecaptchaV2Client : RecaptchaClient {

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