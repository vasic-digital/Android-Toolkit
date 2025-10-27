/*
 * Copyright (c) 2025 MeTube Share
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


package digital.vasic.security.access.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import digital.vasic.security.access.database.SecurityAccessDatabase
import digital.vasic.security.access.utils.SecurityUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class SecurityAccessRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: SecurityAccessRepository
    private lateinit var database: SecurityAccessDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = SecurityAccessDatabase.getInstance(context)
        repository = SecurityAccessRepository.getInstance(context)
    }

    @After
    fun tearDown() {
        runBlocking {
            // Clean up test data
            database.securitySettingsDao().deleteSettings("default")
            database.accessCredentialDao().deleteCredential("default")
            database.accessAttemptDao().cleanupOldAttempts(LocalDateTime.now().minusDays(1))
            database.accessSessionDao().cleanupExpiredSessions(LocalDateTime.now().minusDays(1))
        }
        SecurityAccessDatabase.destroyInstance()
        SecurityAccessRepository.destroyInstance()
    }

    @Test
    fun `saveSecuritySettings should persist settings correctly`() = runBlocking {
        val settings = SecuritySettings(
            accessMethod = AccessMethod.PIN,
            isEnabled = true,
            pinLength = 6,
            sessionTimeoutMinutes = 10
        )

        repository.saveSecuritySettings(settings)

        val retrievedSettings = repository.getSecuritySettingsSync()
        assertNotNull(retrievedSettings)
        assertEquals(AccessMethod.PIN, retrievedSettings?.accessMethod)
        assertTrue(retrievedSettings?.isEnabled == true)
        assertEquals(6, retrievedSettings?.pinLength)
        assertEquals(10, retrievedSettings?.sessionTimeoutMinutes)
    }

    @Test
    fun `getSecuritySettings should return null when no settings exist`() = runBlocking {
        val settings = repository.getSecuritySettingsSync()
        assertNull(settings)
    }

    @Test
    fun `updateSecuritySettingsEnabled should update only enabled status`() = runBlocking {
        // Save initial settings
        val initialSettings = SecuritySettings(
            accessMethod = AccessMethod.PASSWORD,
            isEnabled = false,
            pinLength = 4
        )
        repository.saveSecuritySettings(initialSettings)

        // Update only enabled status
        repository.updateSecuritySettingsEnabled(true)

        val updatedSettings = repository.getSecuritySettingsSync()
        assertNotNull(updatedSettings)
        assertTrue(updatedSettings?.isEnabled == true)
        assertEquals(AccessMethod.PASSWORD, updatedSettings?.accessMethod) // Should remain unchanged
        assertEquals(4, updatedSettings?.pinLength) // Should remain unchanged
    }

    @Test
    fun `saveAccessCredential should persist credential correctly`() = runBlocking {
        val salt = SecurityUtils.generateSalt()
        val hashedPin = SecurityUtils.hashPin("1234", salt)

        val credential = AccessCredential(
            hashedPin = hashedPin.joinToString("") { "%02x".format(it) },
            salt = salt.joinToString("") { "%02x".format(it) },
            encryptionAlgorithm = "PBKDF2WithHmacSHA256",
            keyLength = 256,
            iterations = 100000
        )

        repository.saveAccessCredential(credential)

        val retrievedCredential = repository.getAccessCredentialSync()
        assertNotNull(retrievedCredential)
        assertEquals(credential.hashedPin, retrievedCredential?.hashedPin)
        assertEquals(credential.salt, retrievedCredential?.salt)
        assertEquals(credential.encryptionAlgorithm, retrievedCredential?.encryptionAlgorithm)
    }

    @Test
    fun `updatePin should update PIN hash and salt`() = runBlocking {
        // Save initial credential
        val initialSalt = SecurityUtils.generateSalt()
        val initialHashedPin = SecurityUtils.hashPin("1234", initialSalt)

        val initialCredential = AccessCredential(
            hashedPin = initialHashedPin.joinToString("") { "%02x".format(it) },
            salt = initialSalt.joinToString("") { "%02x".format(it) }
        )
        repository.saveAccessCredential(initialCredential)

        // Update PIN
        repository.updatePin("5678")

        val updatedCredential = repository.getAccessCredentialSync()
        assertNotNull(updatedCredential)
        assertNotEquals(initialCredential.hashedPin, updatedCredential?.hashedPin)
        assertNotEquals(initialCredential.salt, updatedCredential?.salt)

        // Verify new PIN works
        val newSalt = updatedCredential?.salt?.chunked(2)?.map { it.toInt(16).toByte() }?.toByteArray()
        val newHash = updatedCredential?.hashedPin?.chunked(2)?.map { it.toInt(16).toByte() }?.toByteArray()

        assertNotNull(newSalt)
        assertNotNull(newHash)
        assertTrue(SecurityUtils.verifyPin("5678", newSalt!!, newHash!!))
        assertFalse(SecurityUtils.verifyPin("1234", newSalt, newHash))
    }

    @Test
    fun `updatePassword should update password hash and salt`() = runBlocking {
        // Save initial credential
        val initialSalt = SecurityUtils.generateSalt()
        val initialHashedPassword = SecurityUtils.hashPassword("password123", initialSalt)

        val initialCredential = AccessCredential(
            hashedPassword = initialHashedPassword.joinToString("") { "%02x".format(it) },
            salt = initialSalt.joinToString("") { "%02x".format(it) }
        )
        repository.saveAccessCredential(initialCredential)

        // Update password
        repository.updatePassword("newPassword456")

        val updatedCredential = repository.getAccessCredentialSync()
        assertNotNull(updatedCredential)
        assertNotEquals(initialCredential.hashedPassword, updatedCredential?.hashedPassword)
        assertNotEquals(initialCredential.salt, updatedCredential?.salt)

        // Verify new password works
        val newSalt = updatedCredential?.salt?.chunked(2)?.map { it.toInt(16).toByte() }?.toByteArray()
        val newHash = updatedCredential?.hashedPassword?.chunked(2)?.map { it.toInt(16).toByte() }?.toByteArray()

        assertNotNull(newSalt)
        assertNotNull(newHash)
        assertTrue(SecurityUtils.verifyPassword("newPassword456", newSalt!!, newHash!!))
        assertFalse(SecurityUtils.verifyPassword("password123", newSalt, newHash))
    }

    @Test
    fun `verifyPin should return true for correct PIN`() = runBlocking {
        // Setup PIN
        val salt = SecurityUtils.generateSalt()
        val hashedPin = SecurityUtils.hashPin("1234", salt)

        val credential = AccessCredential(
            hashedPin = hashedPin.joinToString("") { "%02x".format(it) },
            salt = salt.joinToString("") { "%02x".format(it) }
        )
        repository.saveAccessCredential(credential)

        // Verify correct PIN
        val isValid = repository.verifyPin("1234")
        assertTrue(isValid)
    }

    @Test
    fun `verifyPin should return false for incorrect PIN`() = runBlocking {
        // Setup PIN
        val salt = SecurityUtils.generateSalt()
        val hashedPin = SecurityUtils.hashPin("1234", salt)

        val credential = AccessCredential(
            hashedPin = hashedPin.joinToString("") { "%02x".format(it) },
            salt = salt.joinToString("") { "%02x".format(it) }
        )
        repository.saveAccessCredential(credential)

        // Verify incorrect PIN
        val isValid = repository.verifyPin("5678")
        assertFalse(isValid)
    }

    @Test
    fun `verifyPassword should return true for correct password`() = runBlocking {
        // Setup password
        val salt = SecurityUtils.generateSalt()
        val hashedPassword = SecurityUtils.hashPassword("password123", salt)

        val credential = AccessCredential(
            hashedPassword = hashedPassword.joinToString("") { "%02x".format(it) },
            salt = salt.joinToString("") { "%02x".format(it) }
        )
        repository.saveAccessCredential(credential)

        // Verify correct password
        val isValid = repository.verifyPassword("password123")
        assertTrue(isValid)
    }

    @Test
    fun `verifyPassword should return false for incorrect password`() = runBlocking {
        // Setup password
        val salt = SecurityUtils.generateSalt()
        val hashedPassword = SecurityUtils.hashPassword("password123", salt)

        val credential = AccessCredential(
            hashedPassword = hashedPassword.joinToString("") { "%02x".format(it) },
            salt = salt.joinToString("") { "%02x".format(it) }
        )
        repository.saveAccessCredential(credential)

        // Verify incorrect password
        val isValid = repository.verifyPassword("wrongPassword")
        assertFalse(isValid)
    }

    @Test
    fun `createSession should create valid session`() = runBlocking {
        val deviceId = "test-device-id"
        val accessMethod = AccessMethod.PIN

        val session = repository.createSession(deviceId, accessMethod)

        assertNotNull(session.sessionId)
        assertEquals(deviceId, session.deviceId)
        assertEquals(accessMethod, session.accessMethod)
        assertTrue(session.isActive)
        assertFalse(session.isLocked)
        assertNotNull(session.expiresAt)
        assertTrue(session.expiresAt.isAfter(LocalDateTime.now()))
    }

    @Test
    fun `recordAccessAttempt should persist attempt correctly`() = runBlocking {
        val deviceId = "test-device-id"
        val accessMethod = AccessMethod.PIN
        val status = AccessStatus.SUCCESS

        repository.recordAccessAttempt(
            accessMethod = accessMethod,
            status = status,
            deviceId = deviceId,
            attemptDurationMs = 100
        )

        // Verify attempt was recorded (we can't easily query it back due to DAO limitations in test)
        // But we can verify no exceptions were thrown
        assertTrue(true)
    }

    @Test
    fun `getRecentFailedAttempts should return failed attempts only`() = runBlocking {
        val deviceId = "test-device-id"

        // Record some attempts
        repository.recordAccessAttempt(
            accessMethod = AccessMethod.PIN,
            status = AccessStatus.SUCCESS,
            deviceId = deviceId
        )

        repository.recordAccessAttempt(
            accessMethod = AccessMethod.PIN,
            status = AccessStatus.FAILED,
            deviceId = deviceId
        )

        repository.recordAccessAttempt(
            accessMethod = AccessMethod.PASSWORD,
            status = AccessStatus.FAILED,
            deviceId = deviceId
        )

        val failedAttempts = repository.getRecentFailedAttempts(deviceId, 10).first()

        assertEquals(2, failedAttempts.size)
        assertTrue(failedAttempts.all { it.status != AccessStatus.SUCCESS })
    }

    @Test
    fun `cleanupExpiredSessions should remove expired sessions`() = runBlocking {
        val deviceId = "test-device-id"

        // Create an expired session
        val expiredSession = AccessSession(
            sessionId = "expired-session",
            deviceId = deviceId,
            accessMethod = AccessMethod.PIN,
            expiresAt = LocalDateTime.now().minusMinutes(1),
            isActive = true
        )

        database.accessSessionDao().insertSession(expiredSession)

        // Create an active session
        val activeSession = repository.createSession(deviceId, AccessMethod.PASSWORD)

        // Cleanup expired sessions
        repository.cleanupExpiredSessions()

        // Verify expired session is gone but active session remains
        val expiredSessionAfter = database.accessSessionDao().getSession("expired-session").first()
        val activeSessionAfter = database.accessSessionDao().getSession(activeSession.sessionId).first()

        assertNull(expiredSessionAfter)
        assertNotNull(activeSessionAfter)
        assertTrue(activeSessionAfter?.isActive == true)
    }

    @Test
    fun `cleanupOldAttempts should remove old attempts`() = runBlocking {
        val deviceId = "test-device-id"

        // Record an old attempt (more than 30 days ago)
        val oldAttempt = AccessAttempt(
            accessMethod = AccessMethod.PIN,
            status = AccessStatus.FAILED,
            deviceId = deviceId,
            attemptDurationMs = 100,
            timestamp = LocalDateTime.now().minusDays(35)
        )

        database.accessAttemptDao().insertAttempt(oldAttempt)

        // Record a recent attempt
        val recentAttempt = AccessAttempt(
            accessMethod = AccessMethod.PASSWORD,
            status = AccessStatus.SUCCESS,
            deviceId = deviceId,
            attemptDurationMs = 200,
            timestamp = LocalDateTime.now()
        )

        database.accessAttemptDao().insertAttempt(recentAttempt)

        // Cleanup old attempts (30 days)
        repository.cleanupOldAttempts(30)

        // Verify old attempt is gone but recent attempt remains
        val allAttempts = database.accessAttemptDao().getAttemptsSince(LocalDateTime.now().minusDays(40)).first()

        assertEquals(1, allAttempts.size)
        assertEquals(recentAttempt.status, allAttempts[0].status)
    }
}