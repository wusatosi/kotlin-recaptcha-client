package com.wusatosi.recaptcha.v2

import com.wusatosi.recaptcha.InvalidSiteKeyException
import com.wusatosi.recaptcha.RecaptchaError
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
) {

    init {
        if (!checkURLCompatibility(secretKey)) throw InvalidSiteKeyException()
    }

    private val validateURL = "https://" +
            (if (useRecaptchaDotNetEndPoint) "www.recaptcha.net" else "www.google.com") +
            "/recaptcha/api/siteverify?secret=$secretKey&response="

    /**
     * get the score from recaptcha remote, if your site key is correct, expect only IOException
     *
     * @param token the client's response token
     * @return The score
     * (if timeout-or-duplicate, return -2.0,
     * if invalid-input-response, return -1.0)
     * @throws IOException self explanatory
     * @throws RecaptchaError if the server responded an invalid json structure
     * @throws InvalidSiteKeyException if server responded with error code: invalid-input-secret
     */
    @Throws(IOException::class)
    suspend fun verify(token: String): Boolean {
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
                processErrorCodes(it.expectArray("error-codes"))
            }

        return isSuccess
    }

}