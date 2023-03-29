package com.wusatosi.recaptcha.internal

import com.wusatosi.recaptcha.RecaptchaClient
import com.wusatosi.recaptcha.RecaptchaV3Config

internal class UniversalRecaptchaClientImpl(
    secretKey: String,
    config: RecaptchaV3Config
) : RecaptchaClientBase(secretKey, config), RecaptchaClient {

    private val defaultScoreThreshold: Double = config.scoreThreshold

    override suspend fun verify(token: String, remoteIp: String): Boolean {
        if (!likelyValidRecaptchaParameter(token))
            return false

        val response = transact(token, remoteIp)
        val (success, hostMatch, _) = interpretResponseBody(response)

        val score = response["score"]
            ?.expectNumber("score")
            ?.asDouble
            ?: return success && hostMatch

        return score > defaultScoreThreshold
    }

}