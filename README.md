# kotlin-recaptcha-client

kotlin-recaptcha-client is an kotlin library for server side reCAPTCHA verification, supports both recaptcha v2 and recaptcha v3

This client aims to be as simple as possible

"reCAPTCHA is a free CAPTCHA service that protects websites from spam and abuse."

- reCAPTCHA: https://www.google.com/recaptcha
- Version: 1.0.1
- License: MIT, see [LICENSE](LICENSE)

## Installation

Via package manager: 

Still in the phase of uploading to the maven central, not ready yet

## Usage

For v2 client:
```kotlin
import com.wusatosi.recaptcha.v2.RecaptchaV2Client

val v2Client = RecaptchaV2Client("your secret key...")

runBlocking {
  // RecaptchaV2Client.verify is an suspend function
  val result: Boolean = v2Client.verify(token)
  
  ...
}
```

For v3 client
```kotlin
import com.wusatosi.recaptcha.v3.RecaptchaV3Client

val v3Client = RecaptchaV3Client("your secret key...")
runBlocking {

  // RecaptchaV3Client.getVerifyScore is an suspend function
  val result: Double = v3Client.getVerifyScore(token)
  
  ...
}
```

Supports using "www.recaptcha.net" instead of "www.google.com" as the API endpoint, 
[for google side more documentation](https://developers.google.com/recaptcha/docs/faq), 
this can be enable via:
```kotlin
// V2
RecaptchaV2Client("your secret key...", true)
// RecaptchaV2Client("your secret key...", useRecaptchaDotNetEndPoint = true)

// V3
RecaptchaV3Client("your secret key...", true)
// RecaptchaV3Client("your secret key...", useRecaptchaDotNetEndPoint = true)
```

## Contributing
[Issues](https://github.com/wusatosi/kotlin-recaptcha-client/issues/new), pull requests, all always welcome

## Uses:
* Kotlin-coroutines, version 1.1.0
* [Fuel](https://github.com/kittinunf/Fuel) (http client), version 1.16.0
* Gson, version 2.8.5

## License
[MIT](https://choosealicense.com/licenses/mit/)