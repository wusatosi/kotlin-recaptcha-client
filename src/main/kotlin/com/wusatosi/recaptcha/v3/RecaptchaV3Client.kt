package com.wusatosi.recaptcha.v3

import com.wusatosi.recaptcha.*
import com.wusatosi.recaptcha.internal.RecaptchaV3ClientImpl
import com.wusatosi.recaptcha.internal.likelyValidRecaptchaParameter

interface RecaptchaV3Client : RecaptchaClient {

    /**
     * A Representation of the response for verifying V3 token.
     * This is in line with [Recaptcha's official documentation](https://developers.google.com/recaptcha/docs/v3).
     *
     * This is similar to V2ResponseDetail. But with the additional score and action field.
     *
     * @property success whether this request was a valid reCAPTCHA token for your site
     * @property host The hostname of the site where the reCAPTCHA was solved
     * @property score The score for this request (0.0 - 1.0)
     * @property action The action name for this request
     *
     */
    data class V3ResponseDetail(
        val success: Boolean,
        val host: String,
        val score: Double,
        val action: String,
    )

    /**
     * The representation of the decision the V3 client made
     *
     * @property decision The final decision of this verification, this is equivalent to the result of calling verify
     * @property hostMatch Whether the verifier's domain is within the allowed domain configured
     *  through RecaptchaV2Config at client's creation
     * @property suggestedThreshold The suggested score threshold the score should be tested against. This is configured
     *  through RecaptchaV3Config at client's creation as either a function of the "action" of the verification, or a
     *  flat constant value.
     */
    data class V3Decision(
        val decision: Boolean,
        val hostMatch: Boolean,
        val suggestedThreshold: Double
    )

    /**
     * Get detailed response of the verification. This includes Google's response to the verification request,
     * and the client's decision acting on top (in the case of V3, whether the domain matches the requirement,
     * and whether the score provided by Google meets the score threshold).
     *
     * @param token The token user has provided
     * @param remoteIp The remote IP of the user, this field is optional,
     *  if an empty string is provided, it will not be included in the request.
     * @return Either the error code of the response, or the response from Google
     *  along with the client's suggested decision, see V3ResponseDetail and V3Decision
     * @throws RecaptchaError This function will throw RecaptchaError when it either suffers IO problems,
     *  or if the response it receives is malformed.
     */
    @Throws(RecaptchaError::class)
    suspend fun getDetailedResponse(
        token: String,
        remoteIp: String = ""
    ): Either<ErrorCode, Pair<V3ResponseDetail, V3Decision>>

    companion object {

        /**
         * Create a V3 client
         *
         * @param secretKey The secret key for verification. You should find them on your recaptcha admin panel.
         * @param block A DSL style configuration of the V3 client, see RecaptchaV3Config.
         */
        fun create(
            secretKey: String,
            block: RecaptchaV3Config.() -> Unit = {}
        ): RecaptchaV3Client {
            if (!likelyValidRecaptchaParameter(secretKey))
                throw InvalidSiteKeyException

            val config = RecaptchaV3Config()
            block(config)

            return RecaptchaV3ClientImpl(
                secretKey,
                config
            )
        }
    }
}
