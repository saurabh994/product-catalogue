package com.saurabh.catalogue.domain.usecase

import com.saurabh.catalogue.domain.model.EnquiryRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EnquiryValidatorTest {
    private fun request(
        name: String = "Asha",
        email: String = "asha@example.com",
        phone: String? = null,
        message: String? = null,
    ) = EnquiryRequest("3", name, email, phone, message)

    @Test
    fun `a minimal valid request has no errors`() {
        assertTrue(EnquiryValidator.validate(request()).isEmpty())
    }

    @Test
    fun `name rules match the backend`() {
        assertEquals("Name is required", EnquiryValidator.validate(request(name = ""))[EnquiryValidator.NAME])
        assertTrue(EnquiryValidator.NAME in EnquiryValidator.validate(request(name = "x".repeat(101))))
        assertTrue("blocks header injection", EnquiryValidator.NAME in EnquiryValidator.validate(request(name = "A\r\nBcc: x@y.z")))
    }

    @Test
    fun `email must look like an address`() {
        listOf("", "plain", "a@b", "a b@c.de").forEach {
            assertTrue("'$it' should be invalid", EnquiryValidator.EMAIL in EnquiryValidator.validate(request(email = it)))
        }
    }

    @Test
    fun `phone allows digits, spaces and + ( ) -`() {
        assertTrue(EnquiryValidator.validate(request(phone = "+91 (987) 654-3210")).isEmpty())
        assertTrue(EnquiryValidator.PHONE in EnquiryValidator.validate(request(phone = "call me")))
    }

    @Test
    fun `message is limited to 1000 characters`() {
        assertTrue(EnquiryValidator.validate(request(message = "x".repeat(1000))).isEmpty())
        assertTrue(EnquiryValidator.MESSAGE in EnquiryValidator.validate(request(message = "x".repeat(1001))))
    }
}
