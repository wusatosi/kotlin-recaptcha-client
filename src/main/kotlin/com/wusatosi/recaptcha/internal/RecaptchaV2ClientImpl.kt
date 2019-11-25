package com.wusatosi.recaptcha.internal

import com.wusatosi.recaptcha.v2.RecaptchaV2Client

internal class RecaptchaV2ClientImpl(
    secretKey: String,
    useRecaptchaDotNetEndPoint: Boolean = false
) : RecaptchaV2Client {

    private val validateURL = "https://" +
            (if (useRecaptchaDotNetEndPoint) "www.recaptcha.net" else "www.google.com") +
            "/recaptcha/api/siteverify?secret=$secretKey&response="

    override suspend fun verify(token: String): Boolean {
//        There is no way to validate it here,
//        So check if it only contains characters
//        that is valid for a URL string
        if (!checkURLCompatibility(token))
            return false

        val obj = getJsonObj(validateURL, token)

        val isSuccess = obj["success"]
            .expectPrimitive("success")
            .expectBoolean("success")

        if (!isSuccess)
            obj["error-codes"]?.let {
                checkErrorCodes(it.expectArray("error-codes"))
            }

        return isSuccess
    }

}