package com.wusatosi.recaptcha.internal

import com.wusatosi.recaptcha.*
import com.wusatosi.recaptcha.v2.RecaptchaV2Client
import com.wusatosi.recaptcha.v2.RecaptchaV2Client.V2Decision
import com.wusatosi.recaptcha.v2.RecaptchaV2Client.V2ResponseDetail

internal class RecaptchaV2ClientImpl(
    secretKey: String,
    config: RecaptchaV2Config
) : RecaptchaClientBase(secretKey, config), RecaptchaV2Client {

    override suspend fun getDetailedResponse(
        token: String,
        remoteIp: String
    ): Either<ErrorCode, Pair<V2ResponseDetail, V2Decision>> {
        if (!likelyValidRecaptchaParameter(token))
            return Either.left(ErrorCode.InvalidToken)

        val response = transact(token, remoteIp)
        return when (val interpretation = interpretResponseBody(response)) {
            is Left -> {
                Either.left(interpretation.left)
            }

            is Right -> {
                val (success, matchedHost, host) = interpretation.right
                val decision = V2Decision(success && matchedHost, matchedHost)
                Either.right(
                    V2ResponseDetail(
                        success,
                        host
                    ) to decision
                )
            }
        }

    }

    override suspend fun verify(token: String, remoteIp: String): Boolean {
        return when (val either = getDetailedResponse(token, remoteIp)) {
            is Left -> false
            is Right -> either.right.second.decision
        }
    }

}