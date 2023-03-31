package com.wusatosi.recaptcha

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class EitherTest {

    @Test
    fun correctHolder() {
        run {
            val either = Either.left<String, String>("hi")
            assertEquals("hi", either.left)
        }

        run {
            val either = Either.right<String, String>("hi")
            assertEquals("hi", either.right)
        }
    }

    @Test
    fun boom() {
        run {
            val either = Either.left<String, String>("hi")
            assertThrows<IllegalStateException> { either.right }
        }

        run {
            val either = Either.right<String, String>("hi")
            assertThrows<IllegalStateException> { either.left }
        }
    }


}