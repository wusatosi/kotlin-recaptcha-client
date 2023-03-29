package com.wusatosi.recaptcha.internal

import com.wusatosi.recaptcha.UnexpectedError
import com.wusatosi.recaptcha.v3.RecaptchaV3Client
import io.ktor.client.engine.*

private const val SCORE_ATTRIBUTE = "score"

internal class RecaptchaV3ClientImpl(
    secretKey: String,
    private val defaultScoreThreshold: Double,
    useRecaptchaDotNetEndPoint: Boolean,
    engine: HttpClientEngine
) : RecaptchaClientBase(secretKey, useRecaptchaDotNetEndPoint, engine), RecaptchaV3Client {

    override suspend fun getVerifyScore(
        token: String,
        invalidateTokenScore: Double,
        timeoutOrDuplicateScore: Double
    ): Double {
        if (!likelyValidRecaptchaParameter(token)) return invalidateTokenScore

        val response = transact(token)
        val (isSuccess, errorCodes) = interpretResponseBody(response)
        return if (isSuccess) {
            response[SCORE_ATTRIBUTE]
                .expectNumber(SCORE_ATTRIBUTE)
                .asDouble
        } else {
            mapErrorCodes(errorCodes, invalidateTokenScore, timeoutOrDuplicateScore)
        }
    }

    private fun mapErrorCodes(
        errorCodes: List<String>,
        invalidTokenScore: Double,
        timeoutOrDuplicateTokenScore: Double
    ): Double {
        if (INVALID_TOKEN_KEY in errorCodes)
            return invalidTokenScore
        if (TIMEOUT_OR_DUPLICATE_KEY in errorCodes)
            return timeoutOrDuplicateTokenScore
        throw UnexpectedError("unexpected error codes: $errorCodes")
    }

    override suspend fun verify(token: String): Boolean = getVerifyScore(token) > defaultScoreThreshold

}
