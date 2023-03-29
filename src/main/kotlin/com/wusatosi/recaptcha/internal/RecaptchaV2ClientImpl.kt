package com.wusatosi.recaptcha.internal

import com.wusatosi.recaptcha.RecaptchaV2Config
import com.wusatosi.recaptcha.v2.RecaptchaV2Client

internal class RecaptchaV2ClientImpl(
    secretKey: String,
    config: RecaptchaV2Config
) : RecaptchaClientBase(secretKey, config), RecaptchaV2Client {

    override suspend fun verify(token: String, remoteIp: String): Boolean {
        if (!likelyValidRecaptchaParameter(token))
            return false

        val response = transact(token, remoteIp)
        val (success, hostMatch, _) = interpretResponseBody(response)
        return success && hostMatch
    }

}