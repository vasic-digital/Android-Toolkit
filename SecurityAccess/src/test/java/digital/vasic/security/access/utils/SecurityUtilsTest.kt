package digital.vasic.security.access.utils

import org.junit.Assert.*
import org.junit.Test
import java.security.MessageDigest

class SecurityUtilsTest {

    @Test
    fun `generateSalt should return correct length salt`() {
        val length = 32
        val salt = SecurityUtils.generateSalt(length)

        assertEquals(length, salt.size)
        assertNotEquals(ByteArray(length), salt) // Should not be all zeros
    }

    @Test
    fun `hashPassword should produce consistent results for same input`() {
        val password = "testPassword123"
        val salt = SecurityUtils.generateSalt()

        val hash1 = SecurityUtils.hashPassword(password, salt)
        val hash2 = SecurityUtils.hashPassword(password, salt)

        assertArrayEquals(hash1, hash2)
    }

    @Test
    fun `hashPassword should produce different results for different passwords`() {
        val password1 = "password1"
        val password2 = "password2"
        val salt = SecurityUtils.generateSalt()

        val hash1 = SecurityUtils.hashPassword(password1, salt)
        val hash2 = SecurityUtils.hashPassword(password2, salt)

        assertFalse(hash1.contentEquals(hash2))
    }

    @Test
    fun `hashPassword should produce different results for different salts`() {
        val password = "testPassword"
        val salt1 = SecurityUtils.generateSalt()
        val salt2 = SecurityUtils.generateSalt()

        val hash1 = SecurityUtils.hashPassword(password, salt1)
        val hash2 = SecurityUtils.hashPassword(password, salt2)

        assertFalse(hash1.contentEquals(hash2))
    }

    @Test
    fun `verifyPassword should return true for correct password and hash`() {
        val password = "correctPassword"
        val salt = SecurityUtils.generateSalt()
        val hash = SecurityUtils.hashPassword(password, salt)

        val isValid = SecurityUtils.verifyPassword(password, salt, hash)

        assertTrue(isValid)
    }

    @Test
    fun `verifyPassword should return false for incorrect password`() {
        val correctPassword = "correctPassword"
        val wrongPassword = "wrongPassword"
        val salt = SecurityUtils.generateSalt()
        val hash = SecurityUtils.hashPassword(correctPassword, salt)

        val isValid = SecurityUtils.verifyPassword(wrongPassword, salt, hash)

        assertFalse(isValid)
    }

    @Test
    fun `hashPin should produce consistent results for same input`() {
        val pin = "1234"
        val salt = SecurityUtils.generateSalt()

        val hash1 = SecurityUtils.hashPin(pin, salt)
        val hash2 = SecurityUtils.hashPin(pin, salt)

        assertArrayEquals(hash1, hash2)
    }

    @Test
    fun `verifyPin should return true for correct PIN and hash`() {
        val pin = "5678"
        val salt = SecurityUtils.generateSalt()
        val hash = SecurityUtils.hashPin(pin, salt)

        val isValid = SecurityUtils.verifyPin(pin, salt, hash)

        assertTrue(isValid)
    }

    @Test
    fun `verifyPin should return false for incorrect PIN`() {
        val correctPin = "9999"
        val wrongPin = "0000"
        val salt = SecurityUtils.generateSalt()
        val hash = SecurityUtils.hashPin(correctPin, salt)

        val isValid = SecurityUtils.verifyPin(wrongPin, salt, hash)

        assertFalse(isValid)
    }

    @Test
    fun `generateSecurePin should return PIN of correct length`() {
        val length = 6
        val pin = SecurityUtils.generateSecurePin(length)

        assertEquals(length, pin.length)
        assertTrue(pin.all { it.isDigit() })
    }

    @Test
    fun `generateSecurePin should generate different PINs`() {
        val pin1 = SecurityUtils.generateSecurePin(4)
        val pin2 = SecurityUtils.generateSecurePin(4)

        assertNotEquals(pin1, pin2)
    }

    @Test
    fun `validatePasswordStrength should validate minimum length`() {
        val shortPassword = "123"
        val result = SecurityUtils.validatePasswordStrength(shortPassword, minLength = 8)

        assertFalse(result.isValid)
        assertTrue(result.issues.any { it.contains("at least 8 characters") })
    }

    @Test
    fun `validatePasswordStrength should validate uppercase requirement`() {
        val noUpperPassword = "password123"
        val result = SecurityUtils.validatePasswordStrength(
            noUpperPassword,
            requireUppercase = true
        )

        assertFalse(result.isValid)
        assertTrue(result.issues.any { it.contains("uppercase") })
    }

    @Test
    fun `validatePasswordStrength should validate lowercase requirement`() {
        val noLowerPassword = "PASSWORD123"
        val result = SecurityUtils.validatePasswordStrength(
            noLowerPassword,
            requireLowercase = true
        )

        assertFalse(result.isValid)
        assertTrue(result.issues.any { it.contains("lowercase") })
    }

    @Test
    fun `validatePasswordStrength should validate numbers requirement`() {
        val noNumbersPassword = "PasswordOnly"
        val result = SecurityUtils.validatePasswordStrength(
            noNumbersPassword,
            requireNumbers = true
        )

        assertFalse(result.isValid)
        assertTrue(result.issues.any { it.contains("number") })
    }

    @Test
    fun `validatePasswordStrength should validate special characters requirement`() {
        val noSpecialPassword = "Password123"
        val result = SecurityUtils.validatePasswordStrength(
            noSpecialPassword,
            requireSpecialChars = true
        )

        assertFalse(result.isValid)
        assertTrue(result.issues.any { it.contains("special character") })
    }

    @Test
    fun `validatePasswordStrength should detect weak patterns`() {
        val weakPassword = "password123"
        val result = SecurityUtils.validatePasswordStrength(weakPassword)

        assertFalse(result.isValid)
        assertTrue(result.issues.any { it.contains("weak patterns") })
    }

    @Test
    fun `validatePasswordStrength should return valid for strong password`() {
        val strongPassword = "MyStr0ng!P@ssw0rd"
        val result = SecurityUtils.validatePasswordStrength(
            strongPassword,
            minLength = 8,
            requireUppercase = true,
            requireLowercase = true,
            requireNumbers = true,
            requireSpecialChars = true
        )

        assertTrue(result.isValid)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun `calculatePasswordScore should return higher score for longer passwords`() {
        val shortPassword = "Pass1!"
        val longPassword = "MyVeryLongAndComplexPassword123!"

        val shortScore = SecurityUtils.validatePasswordStrength(shortPassword).score
        val longScore = SecurityUtils.validatePasswordStrength(longPassword).score

        assertTrue(longScore > shortScore)
    }

    @Test
    fun `calculatePasswordScore should return higher score for passwords with more character types`() {
        val simplePassword = "password123"
        val complexPassword = "MyStr0ng!P@ssw0rd"

        val simpleScore = SecurityUtils.validatePasswordStrength(simplePassword).score
        val complexScore = SecurityUtils.validatePasswordStrength(complexPassword).score

        assertTrue(complexScore > simpleScore)
    }

    @Test
    fun `generateSessionId should return unique IDs`() {
        val id1 = SecurityUtils.generateSessionId()
        val id2 = SecurityUtils.generateSessionId()

        assertNotEquals(id1, id2)
        assertEquals(64, id1.length) // 32 bytes * 2 hex chars per byte
        assertEquals(64, id2.length)
    }

    @Test
    fun `generateSessionId should return valid hex strings`() {
        val sessionId = SecurityUtils.generateSessionId()

        assertTrue(sessionId.all { it.isLetterOrDigit() })
        assertTrue(sessionId.all { it in '0'..'9' || it in 'a'..'f' })
    }
}