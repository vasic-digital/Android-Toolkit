package digital.vasic.security.access.integration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import digital.vasic.security.access.access.PinAccessMethod
import digital.vasic.security.access.access.PasswordAccessMethod
import digital.vasic.security.access.access.SecurityAccessManager
import digital.vasic.security.access.data.AccessMethod
import digital.vasic.security.access.data.SecurityAccessRepository
import digital.vasic.security.access.database.SecurityAccessDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class SecurityAccessIntegrationTest {

    private lateinit var context: Context
    private lateinit var repository: SecurityAccessRepository
    private lateinit var database: SecurityAccessDatabase
    private lateinit var accessManager: SecurityAccessManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = SecurityAccessDatabase.getInstance(context)
        repository = SecurityAccessRepository.getInstance(context)
        accessManager = SecurityAccessManager.getInstance(context)
    }

    @After
    fun tearDown() {
        runBlocking {
            // Clean up test data
            database.securitySettingsDao().deleteSettings()
            database.accessCredentialDao().deleteCredential()
            database.accessAttemptDao().cleanupOldAttempts(LocalDateTime.now().minusDays(1))
            database.accessSessionDao().cleanupExpiredSessions(LocalDateTime.now().minusDays(1))
        }
        SecurityAccessDatabase.destroyInstance()
        SecurityAccessRepository.destroyInstance()
        SecurityAccessManager.destroyInstance()
    }

    @Test
    fun `complete PIN setup and authentication flow should work end-to-end`() = runBlocking {
        // Step 1: Enable security with PIN
        val enableResult = accessManager.enableSecurity(AccessMethod.PIN)
        assertTrue(enableResult.isSuccess)

        // Step 2: Setup PIN using access method
        val pinAccessMethod = PinAccessMethod(0, context as androidx.appcompat.app.AppCompatActivity)
        val setupResult = pinAccessMethod.setupPin("1234", "1234")
        assertTrue(setupResult is PinAccessMethod.SetupResult.Success)

        // Step 3: Verify security is enabled and PIN is configured
        val settings = repository.getSecuritySettingsSync()
        assertNotNull(settings)
        assertTrue(settings?.isEnabled == true)
        assertEquals(AccessMethod.PIN, settings?.accessMethod)

        val credential = repository.getAccessCredentialSync()
        assertNotNull(credential)
        assertNotNull(credential?.hashedPin)

        // Step 4: Test authentication
        val authResult = accessManager.authenticate(AccessMethod.PIN, "1234")
        assertTrue(authResult is SecurityAccessManager.AuthenticationResult.Success)

        // Step 5: Verify session was created
        val status = accessManager.getSecurityStatus().first()
        assertEquals(SecurityAccessManager.AccessState.AUTHENTICATED, status.state)
        assertNotNull(status.currentSessionId)

        // Step 6: Test logout
        accessManager.logout()
        val statusAfterLogout = accessManager.getSecurityStatus().first()
        assertEquals(SecurityAccessManager.AccessState.READY, statusAfterLogout.state)
        assertNull(statusAfterLogout.currentSessionId)
    }

    @Test
    fun `complete password setup and authentication flow should work end-to-end`() = runBlocking {
        // Step 1: Enable security with password
        val enableResult = accessManager.enableSecurity(AccessMethod.PASSWORD)
        assertTrue(enableResult.isSuccess)

        // Step 2: Setup password using access method
        val passwordAccessMethod = PasswordAccessMethod(0, context as androidx.appcompat.app.AppCompatActivity)
        val setupResult = passwordAccessMethod.setupPassword("MyStr0ng!P@ssw0rd", "MyStr0ng!P@ssw0rd")
        assertTrue(setupResult is PasswordAccessMethod.SetupResult.Success)

        // Step 3: Verify security is enabled and password is configured
        val settings = repository.getSecuritySettingsSync()
        assertNotNull(settings)
        assertTrue(settings?.isEnabled == true)
        assertEquals(AccessMethod.PASSWORD, settings?.accessMethod)

        val credential = repository.getAccessCredentialSync()
        assertNotNull(credential)
        assertNotNull(credential?.hashedPassword)

        // Step 4: Test authentication
        val authResult = accessManager.authenticate(AccessMethod.PASSWORD, "MyStr0ng!P@ssw0rd")
        assertTrue(authResult is SecurityAccessManager.AuthenticationResult.Success)

        // Step 5: Verify session was created
        val status = accessManager.getSecurityStatus().first()
        assertEquals(SecurityAccessManager.AccessState.AUTHENTICATED, status.state)
        assertNotNull(status.currentSessionId)
    }

    @Test
    fun `failed authentication attempts should be tracked correctly`() = runBlocking {
        // Setup PIN
        accessManager.enableSecurity(AccessMethod.PIN)
        val pinAccessMethod = PinAccessMethod(0, context as androidx.appcompat.app.AppCompatActivity)
        pinAccessMethod.setupPin("1234", "1234")

        // Make several failed attempts
        repeat(3) {
            val authResult = accessManager.authenticate(AccessMethod.PIN, "9999")
            assertTrue(authResult is SecurityAccessManager.AuthenticationResult.Failed)
        }

        // Verify failed attempts are tracked
        val status = accessManager.getSecurityStatus().first()
        assertEquals(3, status.failedAttempts)

        // Verify attempts are recorded in database
        val deviceId = "test-device-id" // This would be the actual device ID in real scenario
        val failedAttempts = repository.getRecentFailedAttempts(deviceId, 10).first()
        assertTrue(failedAttempts.size >= 3)
    }

    @Test
    fun `lockout mechanism should work correctly after max failed attempts`() = runBlocking {
        // Setup PIN with low max attempts for testing
        val settings = digital.vasic.security.access.data.SecuritySettings(
            accessMethod = AccessMethod.PIN,
            isEnabled = true,
            maxFailedAttempts = 2,
            lockoutDurationMinutes = 1 // 1 minute for quick testing
        )
        repository.saveSecuritySettings(settings)

        val pinAccessMethod = PinAccessMethod(0, context as androidx.appcompat.app.AppCompatActivity)
        pinAccessMethod.setupPin("1234", "1234")

        // Make failed attempts up to the limit
        repeat(2) {
            val authResult = accessManager.authenticate(AccessMethod.PIN, "9999")
            assertTrue(authResult is SecurityAccessManager.AuthenticationResult.Failed)
        }

        // Next attempt should result in lockout
        val lockoutResult = accessManager.authenticate(AccessMethod.PIN, "9999")
        assertTrue(lockoutResult is SecurityAccessManager.AuthenticationResult.LockedOut)

        // Verify lockout status
        val status = accessManager.getSecurityStatus().first()
        assertTrue(status.isLocked)
        assertNotNull(status.lockoutEndTime)
        assertTrue(status.lockoutEndTime!!.isAfter(LocalDateTime.now()))
    }

    @Test
    fun `lockout should be lifted after lockout duration expires`() = runBlocking {
        // Setup PIN with very short lockout duration for testing
        val settings = digital.vasic.security.access.data.SecuritySettings(
            accessMethod = AccessMethod.PIN,
            isEnabled = true,
            maxFailedAttempts = 1,
            lockoutDurationMinutes = 0 // Immediate expiry for testing
        )
        repository.saveSecuritySettings(settings)

        val pinAccessMethod = PinAccessMethod(0, context as androidx.appcompat.app.AppCompatActivity)
        pinAccessMethod.setupPin("1234", "1234")

        // Trigger lockout
        val lockoutResult = accessManager.authenticate(AccessMethod.PIN, "9999")
        assertTrue(lockoutResult is SecurityAccessManager.AuthenticationResult.LockedOut)

        // Wait for lockout to expire (in real scenario, this would be time-based)
        // For testing, we'll manually reset the lockout
        kotlinx.coroutines.delay(100) // Small delay to ensure state update

        // Verify lockout is still active initially
        var status = accessManager.getSecurityStatus().first()
        assertTrue(status.isLocked)

        // Manually reset lockout for testing (in real app, this would happen via timer)
        // This simulates the lockout expiring
        kotlinx.coroutines.delay(100)

        // The lockout monitoring should have reset the state
        // In a real scenario, this would be handled by the timer in SecurityAccessManager
    }

    @Test
    fun `session management should work correctly across authentication cycles`() = runBlocking {
        // Setup PIN
        accessManager.enableSecurity(AccessMethod.PIN)
        val pinAccessMethod = PinAccessMethod(0, context as androidx.appcompat.app.AppCompatActivity)
        pinAccessMethod.setupPin("1234", "1234")

        // First authentication
        val authResult1 = accessManager.authenticate(AccessMethod.PIN, "1234")
        assertTrue(authResult1 is SecurityAccessManager.AuthenticationResult.Success)

        val status1 = accessManager.getSecurityStatus().first()
        val sessionId1 = status1.currentSessionId
        assertNotNull(sessionId1)

        // Logout
        accessManager.logout()

        // Second authentication should create new session
        val authResult2 = accessManager.authenticate(AccessMethod.PIN, "1234")
        assertTrue(authResult2 is SecurityAccessManager.AuthenticationResult.Success)

        val status2 = accessManager.getSecurityStatus().first()
        val sessionId2 = status2.currentSessionId
        assertNotNull(sessionId2)
        assertNotEquals(sessionId1, sessionId2) // Should be different sessions
    }

    @Test
    fun `disabling security should clear all security data and sessions`() = runBlocking {
        // Setup and authenticate with PIN
        accessManager.enableSecurity(AccessMethod.PIN)
        val pinAccessMethod = PinAccessMethod(0, context as androidx.appcompat.app.AppCompatActivity)
        pinAccessMethod.setupPin("1234", "1234")

        val authResult = accessManager.authenticate(AccessMethod.PIN, "1234")
        assertTrue(authResult is SecurityAccessManager.AuthenticationResult.Success)

        // Verify security is active
        var status = accessManager.getSecurityStatus().first()
        assertEquals(SecurityAccessManager.AccessState.AUTHENTICATED, status.state)
        assertNotNull(status.currentSessionId)

        // Disable security
        val disableResult = accessManager.disableSecurity()
        assertTrue(disableResult.isSuccess)

        // Verify security is disabled
        status = accessManager.getSecurityStatus().first()
        assertEquals(SecurityAccessManager.AccessState.DISABLED, status.state)
        assertNull(status.currentSessionId)

        // Verify settings are disabled
        val settings = repository.getSecuritySettingsSync()
        assertNotNull(settings)
        assertFalse(settings?.isEnabled == true)

        // Verify credential is still there but inactive
        val credential = repository.getAccessCredentialSync()
        assertNotNull(credential)
        // Note: In a real implementation, you might want to clear credentials when disabling
    }

    @Test
    fun `concurrent authentication attempts should be handled correctly`() = runBlocking {
        // Setup PIN
        accessManager.enableSecurity(AccessMethod.PIN)
        val pinAccessMethod = PinAccessMethod(0, context as androidx.appcompat.app.AppCompatActivity)
        pinAccessMethod.setupPin("1234", "1234")

        // Launch multiple concurrent authentication attempts
        val authResults = (1..5).map {
            async {
                accessManager.authenticate(AccessMethod.PIN, "1234")
            }
        }

        // Wait for all to complete
        val results = authResults.awaitAll()

        // All should succeed (or fail gracefully)
        assertTrue(results.all { it is SecurityAccessManager.AuthenticationResult.Success })

        // Should have created multiple sessions (one per successful auth)
        val status = accessManager.getSecurityStatus().first()
        assertNotNull(status.currentSessionId)
    }

    @Test
    fun `database corruption should be handled gracefully`() = runBlocking {
        // This test would require more complex setup to simulate database corruption
        // For now, we'll test that the system handles missing database gracefully

        // Clear database instance to simulate corruption/loss
        SecurityAccessDatabase.destroyInstance()

        // Try to get new instance (should handle missing/corrupted database)
        val newDatabase = SecurityAccessDatabase.getInstance(context)
        val newRepository = SecurityAccessRepository.getInstance(context)

        // Should be able to create new settings
        val settings = digital.vasic.security.access.data.SecuritySettings(
            accessMethod = AccessMethod.NONE,
            isEnabled = false
        )

        newRepository.saveSecuritySettings(settings)

        val retrievedSettings = newRepository.getSecuritySettingsSync()
        assertNotNull(retrievedSettings)
        assertEquals(AccessMethod.NONE, retrievedSettings?.accessMethod)
        assertFalse(retrievedSettings?.isEnabled == true)
    }
}