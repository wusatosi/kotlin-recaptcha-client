package com.wusatosi.recaptcha.internal

import com.wusatosi.recaptcha.v2.RecaptchaV2Client
import io.ktor.client.engine.*

internal class RecaptchaV2ClientImpl(
    secretKey: String,
    useRecaptchaDotNetEndPoint: Boolean,
    engine: HttpClientEngine
) : RecaptchaClientBase(secretKey, useRecaptchaDotNetEndPoint, engine), RecaptchaV2Client {

    override suspend fun verify(token: String, remoteIp: String): Boolean {
        if (!likelyValidRecaptchaParameter(token))
            return false

        val response = transact(token, remoteIp)
        val (isSuccess, _) = interpretResponseBody(response)
        return isSuccess
    }

}