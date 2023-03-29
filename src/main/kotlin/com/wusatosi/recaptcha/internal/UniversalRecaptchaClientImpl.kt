package com.wusatosi.recaptcha.internal

import com.wusatosi.recaptcha.RecaptchaClient
import io.ktor.client.engine.*

internal class UniversalRecaptchaClientImpl(
    secretKey: String,
    private val defaultScoreThreshold: Double,
    useRecaptchaDotNetEndPoint: Boolean,
    engine: HttpClientEngine
) : RecaptchaClientBase(secretKey, useRecaptchaDotNetEndPoint, engine), RecaptchaClient {

    override suspend fun verify(token: String, remoteIp: String): Boolean {
        if (!likelyValidRecaptchaParameter(token))
            return false

        val response = transact(token, remoteIp)
        val (isSuccess, _) = interpretResponseBody(response)

        val score = response["score"]
            ?.expectNumber("score")
            ?.asDouble
            ?: return isSuccess

        return score > defaultScoreThreshold
    }

}