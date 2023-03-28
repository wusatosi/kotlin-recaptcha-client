package com.wusatosi.recaptcha.internal

import com.wusatosi.recaptcha.RecaptchaClient

internal class UniversalRecaptchaClientImpl(
    secretKey: String,
    private val defaultScoreThreshold: Double = 0.5,
    useRecaptchaDotNetEndPoint: Boolean = false
) : RecaptchaClient {

    private val validateURL = "https://" +
            (if (useRecaptchaDotNetEndPoint) "www.recaptcha.net" else "www.google.com") +
            "/recaptcha/api/siteverify?secret=$secretKey&response="

    override suspend fun verify(token: String): Boolean {
        if (!checkURLCompatibility(token))
            return false

        val obj = getJsonObj(validateURL, token)

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