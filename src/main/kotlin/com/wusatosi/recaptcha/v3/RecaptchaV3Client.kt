package com.wusatosi.recaptcha.v3

import com.wusatosi.recaptcha.InvalidSiteKeyException
import com.wusatosi.recaptcha.RecaptchaClient
import com.wusatosi.recaptcha.RecaptchaError
import com.wusatosi.recaptcha.internal.*
import java.io.IOException

class RecaptchaV3Client
/**
 * @throws InvalidSiteKeyException
 * if the site key can't pass the initial site key check (that it is made up of validate character for an url)
 *
 * @param useRecaptchaDotNetEndPoint
 *  If you don't know, recaptcha have an end point at www.recaptcha.net,
 *  for countries which blocks www.google.com <br><br>
 *  Enable this feature will change the verification endpoint from<br>
 *  https://www.google.com/recaptcha/api/siteverify?...<br> to <br>
 *  https://www.recaptcha.net/recaptcha/api/siteverify?...<br>
 *  Notice:<br>
 *  Change this variable will not update all the instance, this only
 *  *apply to instances created after the change
 *  @see <a href="https://developers.google.com/recaptcha/docs/faq">
 *  info about www.recaptcha.net
 *  </a>
 *
 */
constructor(
    secretKey: String,
    private val defaultScoreThreshold: Double = 0.5,
    useRecaptchaDotNetEndPoint: Boolean = false
): RecaptchaClient {
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
     * @param invalidate_token_score default score for invalidate-token in this instance
     * @param timeout_or_duplicate_score default score for timeout_or_duplicate_score in this instance
     * @return The score
     * (if timeout-or-duplicate, return -2.0,
     * if invalid-input-response, return -1.0)
     * @throws IOException self explanatory
     * @throws RecaptchaError if the server responded an invalid json structure
     * @throws InvalidSiteKeyException if server responded with error code: invalid-input-secret
     */
    @Throws(IOException::class)
    suspend fun getVerifyScore(
        token: String,
        invalidate_token_score: Double = -1.0,
        timeout_or_duplicate_score: Double = -2.0
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
