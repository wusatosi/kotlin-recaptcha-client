package com.wusatosi.recaptcha.internal

import com.wusatosi.recaptcha.Left
import com.wusatosi.recaptcha.RecaptchaClient
import com.wusatosi.recaptcha.RecaptchaV3Config
import com.wusatosi.recaptcha.Right

internal class UniversalRecaptchaClientImpl(
    secretKey: String,
    config: RecaptchaV3Config
) : RecaptchaClientBase(secretKey, config), RecaptchaClient {

    private val defaultScoreThreshold: Double = config.scoreThreshold

    override suspend fun verify(token: String, remoteIp: String): Boolean {
        if (!likelyValidRecaptchaParameter(token))
            return false

        val response = transact(token, remoteIp)
        return when (val interpretation = interpretResponseBody(response)) {
            is Left -> {
                false
            }

            is Right -> {
                val (success, hostMatch, _) = interpretation.right

                (response["score"]
                    ?.expectNumber("score")
                    ?.asDouble
                    ?.let { it > defaultScoreThreshold }
                        ?: success && hostMatch)
            }
        }
    }

}