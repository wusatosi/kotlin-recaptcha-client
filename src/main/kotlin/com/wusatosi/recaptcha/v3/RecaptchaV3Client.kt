package com.wusatosi.recaptcha.v3

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.awaitResponse
import com.github.kittinunf.result.Result
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.wusatosi.recaptcha.*
import com.wusatosi.recaptcha.internal.*
import java.io.IOException
import java.util.regex.Pattern

class RecaptchaV3Client
/**
 * @throws InvalidSiteKeyException
 * if the site key can't pass the initial site key check (that it is made up of validate character for an url)
 */
constructor(
    secretKey: String
) {

    private val pattern = Pattern.compile("^[-a-zA-Z0-9+&@#/%?=~_!:,.;]*[-a-zA-Z0-9+&@#/%=~_]")

    init {
        if (!pattern.matcher(secretKey).matches())
            throw InvalidSiteKeyException()
    }

    private val validateURL = "https://" +
            (if (useRecaptchaDotNetEndPoint) "www.recaptcha.net" else "www.google.com") +
            "/recaptcha/api/siteverify?secret=$secretKey&response="

    /**
     * get the json response object from recaptcha remote, if your site key is correct, expect only IOException
     *
     * @param token the client's response token
     * @throws IOException self explanatory
     * @throws RecaptchaError if the server responded an invalid json structure
     * @throws InvalidSiteKeyException if server responded with error code: invalid-input-secret
     */
    @Throws(IOException::class)
    suspend fun getJsonObj(token: String): JsonObject {
        val (_, _, result) = Fuel
            .post(validateURL + token)
            .awaitResponse(JsonResponseDeserializer)
        if (result is Result.Failure) {
            val cause = result.error.cause
            when (cause) {
                is JsonParseException -> throw UnableToDeserializeError(cause, result.error.response)
                is IOException -> throw cause
                else ->
                    throw UnexpectedError(
                        "Unexpected error occurred when requesting verification ${cause ?: ""}",
                        cause
                    )
            }
        }

        val res = result.get()
        if (!res.isJsonObject)
            throw UnexpectedJsonStructure("respond json isn't an object")

        return res.asJsonObject
    }

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
        if (!pattern.matcher(token).matches()) return INVALIDATE_TOKEN_SCORE

        val obj = getJsonObj(token)

        val isSuccess = obj["success"]
            .expectPrimitive("success")
            .expectBoolean("success")

        if (!isSuccess) {
            val errorCodes = obj["error-codes"].expectArray("error-codes")
            for (errorCode in errorCodes) {
                val string = errorCode.expectString("error-codes")
//                Error code	            Description
//                missing-input-secret	    The secret parameter is missing.
//                invalid-input-secret	    The secret parameter is invalid or malformed.
//                missing-input-response	The response parameter is missing.
//                invalid-input-response	The response parameter is invalid or malformed.
//                bad-request	            The request is invalid or malformed.
//                timeout-or-duplicate      Timeout... (didn't include in the v3 documentation)
                when {
                    string.startsWith("missing-") || string == "bad-request" ->
                        throw UnexpectedError("assertion failed, should not report $string", null)

                    string == "invalid-input-secret" ->
                        throw InvalidSiteKeyException()

                    string == "invalid-input-response" ->
                        return INVALIDATE_TOKEN_SCORE

                    string == "timeout-or-duplicate" ->
                        return TIMEOUT_OR_DUPLICATE_SCORE

                    else ->
                        UnexpectedJsonStructure("unexpected error code: $string")
                }
            }
            throw UnexpectedJsonStructure("empty error code")
        } else {
            return obj["score"]
                .expectPrimitive("score")
                .expectNumber("score")
                .asDouble
        }
    }

    companion object {
        /**
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
         *     https://developers.google.com/recaptcha/docs/faq
         *     </a>
         */
        @set:Synchronized
        var useRecaptchaDotNetEndPoint = false

        /**
         * Default value when remote returns "invalid-input-response", or token contains characters that
         * is not suitable in an URL string
         */
        @set:Synchronized
        var INVALIDATE_TOKEN_SCORE = -1.0

        /**
         * Default value when remote returns "timeout-or-duplicate"
         */
        @set:Synchronized
        var TIMEOUT_OR_DUPLICATE_SCORE = -2.0
    }

}
