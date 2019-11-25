package com.wusatosi.recaptcha.v2

import com.wusatosi.recaptcha.InvalidSiteKeyException
import com.wusatosi.recaptcha.RecaptchaClient
import com.wusatosi.recaptcha.internal.RecaptchaV2ClientImpl
import com.wusatosi.recaptcha.internal.checkURLCompatibility

interface RecaptchaV2Client : RecaptchaClient {

    companion object {
        fun create(siteKey: String, useRecaptchaDotNetEndPoint: Boolean = false): RecaptchaV2Client {
            if (!checkURLCompatibility(siteKey))
                throw InvalidSiteKeyException
            return RecaptchaV2ClientImpl(
                siteKey,
                useRecaptchaDotNetEndPoint
            )
        }
    }

}