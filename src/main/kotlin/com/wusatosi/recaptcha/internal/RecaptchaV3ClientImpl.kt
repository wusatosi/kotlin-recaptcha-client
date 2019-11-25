package com.wusatosi.recaptcha.internal

import com.wusatosi.recaptcha.v3.RecaptchaV3Client

internal class RecaptchaV3ClientImpl(
    secretKey: String,
    private val defaultScoreThreshold: Double = 0.5,
    useRecaptchaDotNetEndPoint: Boolean = false
) : RecaptchaV3Client {

    private val validateURL = "https://" +
            (if (useRecaptchaDotNetEndPoint) "www.recaptcha.net" else "www.google.com") +
            "/recaptcha/api/siteverify?secret=$secretKey&response="

    override suspend fun getVerifyScore(
        token: String,
        invalidate_token_score: Double,
        timeout_or_duplicate_score: Double
    ): Double {
//        There is no way to validate it here,
//        So check if it only contains characters
//        that is valid for a URL string
        if (!checkURLCompatibility(token)) return invalidate_token_score

        val obj = getJsonObj(validateURL, token)

        val isSuccess = obj["success"]
            .expectPrimitive("success")
            .expectBoolean("success")

        return if (!isSuccess) {
            val errorCodes = obj["error-codes"].expectArray("error-codes")
            checkErrorCodes(errorCodes, invalidate_token_score, timeout_or_duplicate_score)
        } else {
            obj["score"]
                .expectPrimitive("score")
                .expectNumber("score")
                .asDouble
        }
    }

    override suspend fun verify(token: String): Boolean = getVerifyScore(token) > defaultScoreThreshold

}
