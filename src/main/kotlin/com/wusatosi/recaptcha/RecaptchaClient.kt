package com.wusatosi.recaptcha

import com.wusatosi.recaptcha.internal.UniversalRecaptchaClientImpl
import com.wusatosi.recaptcha.internal.checkURLCompatibility
import com.wusatosi.recaptcha.v2.RecaptchaV2Client
import com.wusatosi.recaptcha.v3.RecaptchaV3Client

interface RecaptchaClient {

    /**
     * Verify a given recapthca token.
     * Note: this is a suspend function
     *
     * Example: {@sample
     *   val token = tokenFromUser()
     *   if (client.verify(token))
     *      return "Yes, you are welcome!"
     *   else
     *      return "No, beep boop!!!"
     * }
     *
     * @param token The given recapthca token
     * @return If the token is valid
     */
    @Throws(RecaptchaError::class)
    suspend fun verify(token: String): Boolean

    companion object {

        /**
         * Create a universal client for token verification.
         * @param secretKey Secret key for cpathca verification (aka site key),
         *      you should be able to find this in your recapthca console.
         * @param defaultScoreThreshold If the client is internally v3 where Google returns a
         */
        fun createUniversal(
            secretKey: String,
            defaultScoreThreshold: Double = 0.5,
            useRecaptchaDotNetEndPoint: Boolean = false
        ): RecaptchaClient {
            if (!checkURLCompatibility(secretKey))
                throw InvalidSiteKeyException

            return UniversalRecaptchaClientImpl(
                secretKey,
                defaultScoreThreshold,
                useRecaptchaDotNetEndPoint
            )
        }

        fun createV2(siteKey: String, useRecaptchaDotNetEndpoint: Boolean = false) =
            RecaptchaV2Client.create(siteKey, useRecaptchaDotNetEndpoint)

        fun createV3(
            siteKey: String,
            defaultScoreThreshold: Double = 0.5,
            useRecaptchaDotNetEndpoint: Boolean = false
        ) = RecaptchaV3Client.create(siteKey, defaultScoreThreshold, useRecaptchaDotNetEndpoint)

    }

}