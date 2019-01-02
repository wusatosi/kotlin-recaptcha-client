package com.wusatosi.recaptcha.v2

import com.wusatosi.recaptcha.InvalidSiteKeyException
import com.wusatosi.recaptcha.RecaptchaClient
import com.wusatosi.recaptcha.internal.*
import java.io.IOException

class RecaptchaV2Client
/**
 * @throws InvalidSiteKeyException
 * if the site key can't pass the initial site key check (that it is made up of validate character for an url)
 *
 * @param useRecaptchaDotNetEndPoint
 * If you don't know, recaptcha have an end point at www.recaptcha.net,
 * for countries which blocks www.google.com <br><br>
 *
 * Enable this feature will change the verification endpoint from<br>
 *  https://www.google.com/recaptcha/api/siteverify?...<br> to <br>
 *  https://www.recaptcha.net/recaptcha/api/siteverify?...<br>
 *
 * Notice:<br>
 *     Change this variable will not update all the instance, this only
 *     apply to instances created after the change
 *
 * @see <a href="https://developers.google.com/recaptcha/docs/faq">
 *     info about www.recaptcha.net
 *     </a>
 */
constructor(
    secretKey: String,
    useRecaptchaDotNetEndPoint: Boolean = false
): RecaptchaClient {

    init {
        if (!checkURLCompatibility(secretKey)) throw InvalidSiteKeyException()
    }

    private val validateURL = "https://" +
            (if (useRecaptchaDotNetEndPoint) "www.recaptcha.net" else "www.google.com") +
            "/recaptcha/api/siteverify?secret=$secretKey&response="

    @Throws(IOException::class)
    override suspend fun verify(token: String): Boolean {
//        There is no way to validate it here,
//        So check if it only contains characters
//        that is valid for a URL string
        if (!checkURLCompatibility(token)) return false

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