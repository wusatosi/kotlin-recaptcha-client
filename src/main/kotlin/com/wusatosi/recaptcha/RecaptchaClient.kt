package com.wusatosi.recaptcha

import com.wusatosi.recaptcha.internal.UniversalRecaptchaClientImpl
import java.io.IOException

interface RecaptchaClient {

    /**
     * get the score from recaptcha remote, if your site key is correct, expect only IOException
     *
     * @param token the client's response token
     * @return Validation result
     * @throws IOException self explanatory
     * @throws RecaptchaError if the server responded an invalid json structure
     * @throws InvalidSiteKeyException if server responded with error code: invalid-input-secret
     */
    @Throws(IOException::class)
    suspend fun verify(token: String): Boolean

    companion object {

        /**
         * Create an universal Recaptcha Client which works for both v2 and v3 version of recaptcha
         * @param secretKey: the Secret key for the recaptcha service
         * @param defaultScoreThreshold: score threshold if it is an v3 client
        </a>
         */
        fun createUniversal(
            secretKey: String,
            defaultScoreThreshold: Double = 0.5,
            useRecaptchaDotNetEndPoint: Boolean = false
        ): RecaptchaClient = UniversalRecaptchaClientImpl(
            secretKey,
            defaultScoreThreshold,
            useRecaptchaDotNetEndPoint
        )

    }

}