package com.wusatosi.recaptcha.internal

import com.wusatosi.recaptcha.RecaptchaClient
import io.ktor.client.engine.*

internal class UniversalRecaptchaClientImpl(
    secretKey: String,
    private val defaultScoreThreshold: Double,
    useRecaptchaDotNetEndPoint: Boolean,
    engine: HttpClientEngine
) : RecaptchaClientBase(secretKey, useRecaptchaDotNetEndPoint, engine), RecaptchaClient {

    override suspend fun verify(token: String): Boolean {
        if (!checkURLCompatibility(token))
            return false

        val obj = transact(token)

        val isSuccess = obj["success"]
            .expectBoolean("success")

        if (!isSuccess) {
            obj["error-codes"]?.let {
                checkErrorCodes(it.expectArray("error-codes"))
            }
            return false
        }

        val scoreIndicate = obj["score"] ?: return isSuccess

        val score = scoreIndicate
            .expectNumber("score")
            .asDouble

        return score > defaultScoreThreshold
    }

}