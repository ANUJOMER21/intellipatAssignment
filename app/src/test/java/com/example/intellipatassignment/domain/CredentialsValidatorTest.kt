package com.example.intellipatassignment.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CredentialsValidatorTest {
    @Test fun `blank email is rejected as empty`() {
        assertEquals(EmailError.Empty, CredentialsValidator.validateEmail(""))
        assertEquals(EmailError.Empty, CredentialsValidator.validateEmail("   "))
    }

    @Test fun `malformed emails are invalid`() {
        listOf("plain", "a@b", "a@b.", "@b.com", "a b@c.com", "a@@b.com").forEach {
            assertEquals(it, EmailError.Invalid, CredentialsValidator.validateEmail(it))
        }
    }

    @Test fun `well-formed emails pass, surrounding spaces are ignored`() {
        listOf("user@intellipat.com", "first.last+tag@sub.example.org", "  user@intellipat.com  ").forEach {
            assertNull(it, CredentialsValidator.validateEmail(it))
        }
    }

    @Test fun `password length boundary`() {
        assertEquals(PasswordError.Empty, CredentialsValidator.validatePassword(""))
        assertEquals(PasswordError.TooShort, CredentialsValidator.validatePassword("1234567"))
        assertNull(CredentialsValidator.validatePassword("12345678"))
    }
}
