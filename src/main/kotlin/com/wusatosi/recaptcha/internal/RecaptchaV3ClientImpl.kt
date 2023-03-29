package com.wusatosi.recaptcha.internal

import com.wusatosi.recaptcha.v3.RecaptchaV3Client
import io.ktor.client.engine.*

internal class RecaptchaV3ClientImpl(
    secretKey: String,
    private val defaultScoreThreshold: Double,
    useRecaptchaDotNetEndPoint: Boolean,
    engine: HttpClientEngine
) : RecaptchaClientBase(secretKey, useRecaptchaDotNetEndPoint, engine), RecaptchaV3Client {

    override suspend fun getVerifyScore(
        token: String,
        invalidate_token_score: Double,
        timeout_or_duplicate_score: Double
    ): Double {
//        There is no way to validate it here,
//        So check if it only contains characters
//        that is valid for a URL string
        if (!checkURLCompatibility(token)) return invalidate_token_score

        val obj = transact(token)

        val isSuccess = obj["success"]
            .expectBoolean("success")

        return if (!isSuccess) {
            val errorCodes = obj["error-codes"].expectStringArray("error-codes")
            mapErrorCodes(errorCodes, invalidate_token_score, timeout_or_duplicate_score)
        } else {
            obj["score"]
                .expectNumber("score")
                .asDouble
        }
    }

    override suspend fun verify(token: String): Boolean = getVerifyScore(token) > defaultScoreThreshold

}
