package com.wusatosi.recaptcha.internal

import com.google.gson.JsonObject
import com.wusatosi.recaptcha.*
import com.wusatosi.recaptcha.ErrorCode.InvalidToken
import com.wusatosi.recaptcha.v3.RecaptchaV3Client
import com.wusatosi.recaptcha.v3.RecaptchaV3Client.V3Decision
import com.wusatosi.recaptcha.v3.RecaptchaV3Client.V3ResponseDetail

private const val ACTION_ATTRIBUTE = "action"
private const val SCORE_ATTRIBUTE = "score"

internal class RecaptchaV3ClientImpl(
    secretKey: String,
    config: RecaptchaV3Config
) : RecaptchaClientBase(secretKey, config), RecaptchaV3Client {

    private val actionToScoreThreshold = config.actionToScoreThreshold

    private val failDecision = V3Decision(
        decision = false, hostMatch = false, suggestedThreshold = 5.0
    )


    override suspend fun getDetailedResponse(
        token: String,
        remoteIp: String
    ): Either<ErrorCode, Pair<V3ResponseDetail, V3Decision>> {
        if (!likelyValidRecaptchaParameter(token))
            return Either.left(InvalidToken)

        val response = transact(token, remoteIp)
        return when (val either = interpretResponseBody(response)) {
            is Left -> Either.left(either.left)
            is Right -> {
                val (success, hostMatch, hostname) = either.right

                if (!success)
                    return Either.right(V3ResponseDetail(false, hostname, 0.0, "") to failDecision)

                // TODO: Test no score
                val score = extractScore(response)
                // TODO: Test no action
                val action = extractAction(response)

                val threshold = generateThreshold(action)
                val decision = V3Decision(hostMatch && (score > threshold), hostMatch, threshold)

                Either.right(
                    V3ResponseDetail(
                        success = true,
                        hostname,
                        score,
                        action
                    ) to decision
                )
            }
        }
    }

    private fun extractAction(response: JsonObject) = response[ACTION_ATTRIBUTE]
        .expectString(ACTION_ATTRIBUTE)

    private fun extractScore(body: JsonObject) = body[SCORE_ATTRIBUTE]
        .expectNumber(SCORE_ATTRIBUTE)

    private fun generateThreshold(action: String): Double = actionToScoreThreshold(action)

    override suspend fun verify(token: String, remoteIp: String): Boolean {
        return when (val interpretation = getDetailedResponse(token, remoteIp)) {
            is Left -> false
            is Right -> {
                val (_, decision) = interpretation.right
                decision.decision
            }
        }
    }

}
