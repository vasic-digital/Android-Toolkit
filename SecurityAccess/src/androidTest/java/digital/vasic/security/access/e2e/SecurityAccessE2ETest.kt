package digital.vasic.security.access.e2e

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import digital.vasic.security.access.access.SecurityAccessManager
import digital.vasic.security.access.data.AccessMethod
import digital.vasic.security.access.data.SecurityAccessRepository
import digital.vasic.security.access.ui.SecurityAccessActivity
import digital.vasic.security.access.ui.SecurityAccessSetupActivity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecurityAccessE2ETest {

    private lateinit var context: Context
    private lateinit var repository: SecurityAccessRepository
    private lateinit var accessManager: SecurityAccessManager
    private lateinit var uiDevice: UiDevice

    @Before
    fun setup() {
        Intents.init()
        context = ApplicationProvider.getApplicationContext()
        repository = SecurityAccessRepository.getInstance(context)
        accessManager = SecurityAccessManager.getInstance(context)
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // Clear any existing security settings
        runBlocking {
            repository.getSecuritySettingsSync()?.let { settings ->
                val updatedSettings = settings.copy(isEnabled = false)
                repository.saveSecuritySettings(updatedSettings)
            }
        }
    }

    @After
    fun tearDown() {
        Intents.release()
        runBlocking {
            repository.getSecuritySettingsSync()?.let { settings ->
                val updatedSettings = settings.copy(isEnabled = false)
                repository.saveSecuritySettings(updatedSettings)
            }
        }
        SecurityAccessRepository.destroyInstance()
        SecurityAccessManager.destroyInstance()
    }

    @Test
    fun `complete PIN setup and authentication E2E flow should work correctly`() = runBlocking {
        // Step 1: Launch setup wizard
        val setupIntent = Intent(context, SecurityAccessSetupActivity::class.java)
        ActivityScenario.launch<SecurityAccessSetupActivity>(setupIntent).use { setupScenario ->

            // Step 2: Complete setup wizard with PIN
            completeSetupWizardWithPIN()

            // Verify setup completed successfully
            setupScenario.onActivity { activity ->
                val resultCode = setupScenario.result.resultCode
                val resultData = setupScenario.result.resultData
                assertEquals(android.app.Activity.RESULT_OK, resultCode)
                assertTrue(resultData.getBooleanExtra(SecurityAccessSetupActivity.EXTRA_SETUP_SUCCESS, false))
            }
        }

        // Step 3: Verify security is enabled in repository
        val settings = repository.getSecuritySettingsSync()
        assertNotNull(settings)
        assertTrue(settings?.isEnabled == true)
        assertEquals(AccessMethod.PIN, settings?.accessMethod)

        // Step 4: Test authentication flow
        val authIntent = Intent(context, SecurityAccessActivity::class.java)
        ActivityScenario.launch<SecurityAccessActivity>(authIntent).use { authScenario ->

            // Wait for authentication UI to appear
            Thread.sleep(2000)

            // Enter correct PIN
            onView(withId(R.id.pinInput)).perform(typeText("1234"))

            // Submit authentication
            onView(withId(R.id.btnAuthenticate)).perform(click())

            // Wait for authentication to complete
            Thread.sleep(1000)

            // Verify authentication succeeded (activity should finish or show success)
            authScenario.onActivity { activity ->
                // In a real implementation, this would check if the activity finished successfully
                // For now, we'll verify the authentication state
            }
        }

        // Step 5: Verify session was created
        val status = accessManager.getSecurityStatus().first()
        assertEquals(SecurityAccessManager.AccessState.AUTHENTICATED, status.state)
        assertNotNull(status.currentSessionId)

        // Step 6: Test logout functionality
        accessManager.logout()

        val statusAfterLogout = accessManager.getSecurityStatus().first()
        assertEquals(SecurityAccessManager.AccessState.READY, statusAfterLogout.state)
        assertNull(statusAfterLogout.currentSessionId)
    }

    @Test
    fun `complete password setup and authentication E2E flow should work correctly`() = runBlocking {
        // Step 1: Launch setup wizard
        val setupIntent = Intent(context, SecurityAccessSetupActivity::class.java)
        ActivityScenario.launch<SecurityAccessSetupActivity>(setupIntent).use { setupScenario ->

            // Step 2: Complete setup wizard with password
            completeSetupWizardWithPassword()

            // Verify setup completed successfully
            setupScenario.onActivity { activity ->
                val resultCode = setupScenario.result.resultCode
                val resultData = setupScenario.result.resultData
                assertEquals(android.app.Activity.RESULT_OK, resultCode)
                assertTrue(resultData.getBooleanExtra(SecurityAccessSetupActivity.EXTRA_SETUP_SUCCESS, false))
            }
        }

        // Step 3: Verify security is enabled in repository
        val settings = repository.getSecuritySettingsSync()
        assertNotNull(settings)
        assertTrue(settings?.isEnabled == true)
        assertEquals(AccessMethod.PASSWORD, settings?.accessMethod)

        // Step 4: Test authentication flow
        val authIntent = Intent(context, SecurityAccessActivity::class.java)
        ActivityScenario.launch<SecurityAccessActivity>(authIntent).use { authScenario ->

            // Wait for authentication UI to appear
            Thread.sleep(2000)

            // Enter correct password
            onView(withId(R.id.passwordInput)).perform(typeText("MyStr0ng!P@ssw0rd"))

            // Submit authentication
            onView(withId(R.id.btnAuthenticate)).perform(click())

            // Wait for authentication to complete
            Thread.sleep(1000)

            // Verify authentication succeeded
            authScenario.onActivity { activity ->
                // Verify authentication state
            }
        }

        // Step 5: Verify session was created
        val status = accessManager.getSecurityStatus().first()
        assertEquals(SecurityAccessManager.AccessState.AUTHENTICATED, status.state)
        assertNotNull(status.currentSessionId)
    }

    @Test
    fun `failed authentication attempts should trigger lockout E2E flow`() = runBlocking {
        // Setup PIN with low failure threshold for testing
        val testSettings = digital.vasic.security.access.data.SecuritySettings(
            accessMethod = AccessMethod.PIN,
            isEnabled = true,
            maxFailedAttempts = 2,
            lockoutDurationMinutes = 1
        )
        repository.saveSecuritySettings(testSettings)

        // Setup PIN credential
        val pinAccessMethod = digital.vasic.security.access.access.PinAccessMethod(0, context as androidx.appcompat.app.AppCompatActivity)
        pinAccessMethod.setupPin("1234", "1234")

        // Step 1: Launch authentication activity
        val authIntent = Intent(context, SecurityAccessActivity::class.java)
        ActivityScenario.launch<SecurityAccessActivity>(authIntent).use { authScenario ->

            // Step 2: Make failed authentication attempts
            repeat(2) {
                Thread.sleep(1000)

                // Enter wrong PIN
                onView(withId(R.id.pinInput)).perform(typeText("9999"))
                onView(withId(R.id.btnAuthenticate)).perform(click())

                // Wait for error feedback
                Thread.sleep(500)
            }

            // Step 3: Verify lockout is triggered
            Thread.sleep(1000)

            // The UI should show lockout message
            onView(withId(R.id.lockoutMessage)).check(matches(isDisplayed()))

            // Verify lockout status in manager
            val status = accessManager.getSecurityStatus().first()
            assertTrue(status.isLocked)
            assertNotNull(status.lockoutEndTime)
        }
    }

    @Test
    fun `security settings should persist across app restarts E2E flow`() = runBlocking {
        // Step 1: Setup security with PIN
        val setupIntent = Intent(context, SecurityAccessSetupActivity::class.java)
        ActivityScenario.launch<SecurityAccessSetupActivity>(setupIntent).use { setupScenario ->
            completeSetupWizardWithPIN()

            setupScenario.onActivity { activity ->
                val resultCode = setupScenario.result.resultCode
                assertEquals(android.app.Activity.RESULT_OK, resultCode)
            }
        }

        // Step 2: Verify settings are persisted
        var settings = repository.getSecuritySettingsSync()
        assertNotNull(settings)
        assertTrue(settings?.isEnabled == true)
        assertEquals(AccessMethod.PIN, settings?.accessMethod)

        // Step 3: Simulate app restart by recreating manager and repository
        SecurityAccessManager.destroyInstance()
        SecurityAccessRepository.destroyInstance()

        val newRepository = SecurityAccessRepository.getInstance(context)
        val newAccessManager = SecurityAccessManager.getInstance(context)

        // Step 4: Verify settings are still available after "restart"
        val restoredSettings = newRepository.getSecuritySettingsSync()
        assertNotNull(restoredSettings)
        assertTrue(restoredSettings?.isEnabled == true)
        assertEquals(AccessMethod.PIN, restoredSettings?.accessMethod)

        // Step 5: Verify authentication still works after "restart"
        val authResult = newAccessManager.authenticate(AccessMethod.PIN, "1234")
        assertTrue(authResult is SecurityAccessManager.AuthenticationResult.Success)

        val status = newAccessManager.getSecurityStatus().first()
        assertEquals(SecurityAccessManager.AccessState.AUTHENTICATED, status.state)
    }

    @Test
    fun `biometric authentication E2E flow should work when available`() = runBlocking {
        // This test would require a device with biometric capabilities
        // For now, we'll test the setup flow

        // Setup biometric authentication
        val settings = digital.vasic.security.access.data.SecuritySettings(
            accessMethod = AccessMethod.FINGERPRINT,
            isEnabled = true,
            allowFingerprint = true
        )
        repository.saveSecuritySettings(settings)

        // Launch authentication activity
        val authIntent = Intent(context, SecurityAccessActivity::class.java)
        ActivityScenario.launch<SecurityAccessActivity>(authIntent).use { authScenario ->

            // Wait for biometric prompt to appear
            Thread.sleep(2000)

            // The biometric prompt should be shown
            // In a real test with biometric hardware, we would interact with the fingerprint sensor
            // For now, we'll verify the UI shows the biometric prompt

            onView(withId(R.id.biometricPromptTitle)).check(matches(isDisplayed()))

            // Simulate biometric authentication success (this would normally come from hardware)
            // In a real implementation, this would be handled by the BiometricPrompt callback

            Thread.sleep(1000)
        }
    }

    @Test
    fun `multiple authentication methods should coexist correctly E2E flow`() = runBlocking {
        // Step 1: Setup PIN initially
        val setupIntent = Intent(context, SecurityAccessSetupActivity::class.java)
        ActivityScenario.launch<SecurityAccessSetupActivity>(setupIntent).use { setupScenario ->
            completeSetupWizardWithPIN()
        }

        // Step 2: Change to password authentication
        val passwordAccessMethod = digital.vasic.security.access.access.PasswordAccessMethod(0, context as androidx.appcompat.app.AppCompatActivity)
        val changeResult = passwordAccessMethod.setupPassword("NewPassword123!", "NewPassword123!")
        assertTrue(changeResult is digital.vasic.security.access.access.PasswordAccessMethod.SetupResult.Success)

        // Step 3: Verify settings updated
        val settings = repository.getSecuritySettingsSync()
        assertNotNull(settings)
        assertEquals(AccessMethod.PASSWORD, settings?.accessMethod)

        // Step 4: Test password authentication
        val authResult = accessManager.authenticate(AccessMethod.PASSWORD, "NewPassword123!")
        assertTrue(authResult is SecurityAccessManager.AuthenticationResult.Success)

        // Step 5: Verify old PIN no longer works
        val oldPinResult = accessManager.authenticate(AccessMethod.PIN, "1234")
        assertTrue(oldPinResult is SecurityAccessManager.AuthenticationResult.Failed)

        // Step 6: Test password authentication again
        val passwordResult = accessManager.authenticate(AccessMethod.PASSWORD, "NewPassword123!")
        assertTrue(passwordResult is SecurityAccessManager.AuthenticationResult.Success)
    }

    // Helper methods

    private fun completeSetupWizardWithPIN() {
        // Wait for welcome screen
        Thread.sleep(1000)

        // Click "Get Started"
        onView(withId(R.id.btnNext)).perform(click())

        // Wait for method selection
        Thread.sleep(500)

        // Select PIN
        onView(withId(R.id.pinOption)).perform(click())
        onView(withId(R.id.btnNext)).perform(click())

        // Wait for PIN setup
        Thread.sleep(500)

        // Enter PIN
        onView(withId(R.id.pinInput)).perform(typeText("1234"))
        onView(withId(R.id.confirmPinInput)).perform(typeText("1234"))

        // Complete setup
        onView(withId(R.id.btnNext)).perform(click())

        // Wait for success screen
        Thread.sleep(1000)

        // Click "Finish"
        onView(withId(R.id.btnNext)).perform(click())
    }

    private fun completeSetupWizardWithPassword() {
        // Wait for welcome screen
        Thread.sleep(1000)

        // Click "Get Started"
        onView(withId(R.id.btnNext)).perform(click())

        // Wait for method selection
        Thread.sleep(500)

        // Select password
        onView(withId(R.id.passwordOption)).perform(click())
        onView(withId(R.id.btnNext)).perform(click())

        // Wait for password setup
        Thread.sleep(500)

        // Enter password
        onView(withId(R.id.passwordInput)).perform(typeText("MyStr0ng!P@ssw0rd"))
        onView(withId(R.id.confirmPasswordInput)).perform(typeText("MyStr0ng!P@ssw0rd"))

        // Complete setup
        onView(withId(R.id.btnNext)).perform(click())

        // Wait for success screen
        Thread.sleep(1000)

        // Click "Finish"
        onView(withId(R.id.btnNext)).perform(click())
    }
}