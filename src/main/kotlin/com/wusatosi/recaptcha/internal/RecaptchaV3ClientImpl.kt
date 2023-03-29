package com.wusatosi.recaptcha.internal

import com.wusatosi.recaptcha.Either
import com.wusatosi.recaptcha.RecaptchaV3Config
import com.wusatosi.recaptcha.UnexpectedError
import com.wusatosi.recaptcha.v3.RecaptchaV3Client

private const val SCORE_ATTRIBUTE = "score"

internal class RecaptchaV3ClientImpl(
    secretKey: String,
    config: RecaptchaV3Config
) : RecaptchaClientBase(secretKey, config), RecaptchaV3Client {

    private val actionToScoreThreshold = config.actionToScoreThreshold

    override suspend fun getVerifyScore(
        token: String,
        invalidateTokenScore: Double,
        timeoutOrDuplicateScore: Double,
        remoteIp: String
    ): Double {
        if (!likelyValidRecaptchaParameter(token)) return invalidateTokenScore

        val response = transact(token, remoteIp)
        val (isSuccess, matchedHost, errorCodes) = interpretResponseBody(response)
        return if (isSuccess) {
            response[SCORE_ATTRIBUTE]
                .expectNumber(SCORE_ATTRIBUTE)
                .asDouble
        } else {
            if (matchedHost)
                when (mapErrorCode(errorCodes)) {
                    V3ErrorCode.InvalidToken -> invalidateTokenScore
                    V3ErrorCode.TimeOrDuplicatedToken -> timeoutOrDuplicateScore
                    else -> throw UnexpectedError("Cannot interpret error code: $errorCodes")
                }
            else
                0.0
        }
    }

    data class V3ResponseDetail(
        val success: Boolean,
        val score: Double,
        val action: String,
        val error: V3ErrorCode?,
        val host: String
    )

    enum class V3ErrorCode {
        InvalidToken,
        TimeOrDuplicatedToken
    }

    object PreCheckFailed

    suspend fun getDetailedResponse(token: String, remoteIp: String = ""): Either<PreCheckFailed, V3ResponseDetail> {
        if (!likelyValidRecaptchaParameter(token))
            return Either.left(PreCheckFailed)

        val response = transact(token, remoteIp)
        val basicInterpretation = interpretResponseBody(response)
        val score = response[SCORE_ATTRIBUTE]
            .expectNumber(SCORE_ATTRIBUTE)
            .asDouble
        val action = response["action"]
            .expectString("action")
        val errorCode = mapErrorCode(basicInterpretation.errorCodes)
        return Either.right(
            V3ResponseDetail(
                basicInterpretation.success,
                score,
                action,
                errorCode,
                basicInterpretation.host
            )
        )
    }

    private fun mapErrorCode(errorCodes: List<String>) =
        errorCodes.firstNotNullOfOrNull {
            when (it) {
                INVALID_TOKEN_KEY -> V3ErrorCode.InvalidToken
                TIMEOUT_OR_DUPLICATE_KEY -> V3ErrorCode.TimeOrDuplicatedToken
                else -> null
            }
        }

    override suspend fun verify(token: String, remoteIp: String): Boolean =
        getVerifyScore(token) > actionToScoreThreshold("")

}
