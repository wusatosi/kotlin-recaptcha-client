package com.wusatosi.recaptcha.internal

import com.wusatosi.recaptcha.v2.RecaptchaV2Client
import io.ktor.client.engine.*

internal class RecaptchaV2ClientImpl(
    secretKey: String,
    useRecaptchaDotNetEndPoint: Boolean,
    engine: HttpClientEngine
) : RecaptchaClientBase(secretKey, useRecaptchaDotNetEndPoint, engine), RecaptchaV2Client {

    override suspend fun verify(token: String): Boolean {
//        There is no way to validate it here,
//        So check if it only contains characters
//        that is valid for a URL string
        if (!checkURLCompatibility(token))
            return false

        val obj = transact(token)
        val isSuccess = obj["success"]
            .expectBoolean("success")

        if (!isSuccess)
            obj["error-codes"]?.let {
//                 Check if we need to throw InvalidSiteKeyException,
//                 we don't care if the token is invalid.
                checkSiteSecretError(it.expectStringArray("error-codes"))
            }

        return isSuccess
    }

}