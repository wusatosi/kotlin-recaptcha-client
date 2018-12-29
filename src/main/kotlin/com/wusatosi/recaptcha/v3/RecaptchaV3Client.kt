package com.wusatosi.recaptcha.v3

import com.wusatosi.recaptcha.InvalidSiteKeyException
import com.wusatosi.recaptcha.RecaptchaConfig.DEFAULT_INVALIDATE_TOKEN_SCORE
import com.wusatosi.recaptcha.RecaptchaConfig.DEFAULT_TIMEOUT_OR_DUPLICATE_SCORE
import com.wusatosi.recaptcha.RecaptchaError
import com.wusatosi.recaptcha.internal.*
import java.io.IOException
import java.util.regex.Pattern

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
 *  @param invalidate_token_score default score for invalidate-token in this instance
 *  @param timeout_or_duplicate_score default score for timeout_or_duplicate_score in this instance
 */
constructor(
    secretKey: String,
    useRecaptchaDotNetEndPoint: Boolean = false,

    @set:Synchronized
    var invalidate_token_score: Double = DEFAULT_INVALIDATE_TOKEN_SCORE,

    @set:Synchronized
    var timeout_or_duplicate_score: Double = DEFAULT_TIMEOUT_OR_DUPLICATE_SCORE
) {

    private val pattern = Pattern.compile("^[-a-zA-Z0-9+&@#/%?=~_!:,.;]*[-a-zA-Z0-9+&@#/%=~_]")

    init {
        if (!pattern.matcher(secretKey).matches()) throw InvalidSiteKeyException()
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
    suspend fun getVerifyScore(token: String): Double {
//        There is no way to validate it here,
//        So check if it only contains characters
//        that is valid for a URL string
        if (!pattern.matcher(token).matches()) return DEFAULT_INVALIDATE_TOKEN_SCORE

        val obj = getJsonObj(validateURL, token)

        val isSuccess = obj["success"]
            .expectPrimitive("success")
            .expectBoolean("success")

        if (!isSuccess) {
            val errorCodes = obj["error-codes"].expectArray("error-codes")
            val result = processErrorCodes(errorCodes)
            return when (result) {
                INVALID_INPUT_RESPONSE -> invalidate_token_score
                TIMEOUT_OR_DUPLICATE -> timeout_or_duplicate_score
                else -> throw AssertionError("Not gonna happen")
            }
        } else {
            return obj["score"]
                .expectPrimitive("score")
                .expectNumber("score")
                .asDouble
        }
    }

}
