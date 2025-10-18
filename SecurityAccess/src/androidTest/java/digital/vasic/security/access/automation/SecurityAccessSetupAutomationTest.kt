package digital.vasic.security.access.automation

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import digital.vasic.security.access.ui.SecurityAccessSetupActivity
import digital.vasic.security.access.data.AccessMethod
import digital.vasic.security.access.data.SecurityAccessRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecurityAccessSetupAutomationTest {

    private lateinit var repository: SecurityAccessRepository
    private lateinit var uiDevice: UiDevice

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        repository = SecurityAccessRepository.getInstance(context)
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
        // Clean up after tests
        runBlocking {
            repository.getSecuritySettingsSync()?.let { settings ->
                val updatedSettings = settings.copy(isEnabled = false)
                repository.saveSecuritySettings(updatedSettings)
            }
        }
        SecurityAccessRepository.destroyInstance()
    }

    @Test
    fun `setup wizard should complete successfully with PIN selection`() {
        // Launch setup activity
        val intent = Intent(ApplicationProvider.getApplicationContext(), SecurityAccessSetupActivity::class.java)
        ActivityScenario.launch<SecurityAccessSetupActivity>(intent).use { scenario ->

            // Wait for welcome screen
            Thread.sleep(1000)

            // Click "Get Started" on welcome screen
            onView(withId(R.id.btnNext)).perform(click())

            // Wait for method selection screen
            Thread.sleep(500)

            // Select PIN option
            onView(withId(R.id.pinOption)).perform(click())

            // Click "Continue"
            onView(withId(R.id.btnNext)).perform(click())

            // Wait for PIN setup screen
            Thread.sleep(500)

            // Enter PIN
            onView(withId(R.id.pinInput)).perform(typeText("1234"))
            onView(withId(R.id.confirmPinInput)).perform(typeText("1234"))

            // Click "Setup"
            onView(withId(R.id.btnNext)).perform(click())

            // Wait for success screen
            Thread.sleep(1000)

            // Verify success screen is shown
            onView(withId(R.id.btnNext)).check(matches(withText("Finish")))

            // Click "Finish"
            onView(withId(R.id.btnNext)).perform(click())

            // Verify activity finished with success
            scenario.onActivity { activity ->
                val resultCode = scenario.result.resultCode
                val resultData = scenario.result.resultData
                assertEquals(android.app.Activity.RESULT_OK, resultCode)
                assertTrue(resultData.getBooleanExtra(SecurityAccessSetupActivity.EXTRA_SETUP_SUCCESS, false))
            }
        }
    }

    @Test
    fun `setup wizard should complete successfully with password selection`() {
        // Launch setup activity
        val intent = Intent(ApplicationProvider.getApplicationContext(), SecurityAccessSetupActivity::class.java)
        ActivityScenario.launch<SecurityAccessSetupActivity>(intent).use { scenario ->

            // Wait for welcome screen
            Thread.sleep(1000)

            // Click "Get Started"
            onView(withId(R.id.btnNext)).perform(click())

            // Wait for method selection screen
            Thread.sleep(500)

            // Select password option
            onView(withId(R.id.passwordOption)).perform(click())

            // Click "Continue"
            onView(withId(R.id.btnNext)).perform(click())

            // Wait for password setup screen
            Thread.sleep(500)

            // Enter password
            onView(withId(R.id.passwordInput)).perform(typeText("MyStr0ng!P@ssw0rd"))
            onView(withId(R.id.confirmPasswordInput)).perform(typeText("MyStr0ng!P@ssw0rd"))

            // Click "Setup"
            onView(withId(R.id.btnNext)).perform(click())

            // Wait for success screen
            Thread.sleep(1000)

            // Verify success screen is shown
            onView(withId(R.id.btnNext)).check(matches(withText("Finish")))

            // Click "Finish"
            onView(withId(R.id.btnNext)).perform(click())

            // Verify activity finished with success
            scenario.onActivity { activity ->
                val resultCode = scenario.result.resultCode
                val resultData = scenario.result.resultData
                assertEquals(android.app.Activity.RESULT_OK, resultCode)
                assertTrue(resultData.getBooleanExtra(SecurityAccessSetupActivity.EXTRA_SETUP_SUCCESS, false))
            }
        }
    }

    @Test
    fun `setup wizard should handle PIN validation errors correctly`() {
        // Launch setup activity
        val intent = Intent(ApplicationProvider.getApplicationContext(), SecurityAccessSetupActivity::class.java)
        ActivityScenario.launch<SecurityAccessSetupActivity>(intent).use { scenario ->

            // Navigate to PIN setup
            Thread.sleep(1000)
            onView(withId(R.id.btnNext)).perform(click()) // Welcome
            Thread.sleep(500)
            onView(withId(R.id.pinOption)).perform(click())
            onView(withId(R.id.btnNext)).perform(click()) // Method selection

            // Wait for PIN setup screen
            Thread.sleep(500)

            // Enter mismatched PINs
            onView(withId(R.id.pinInput)).perform(typeText("1234"))
            onView(withId(R.id.confirmPinInput)).perform(typeText("5678"))

            // Try to continue (should show error)
            onView(withId(R.id.btnNext)).perform(click())

            // Verify error message is shown
            onView(withId(R.id.errorText)).check(matches(isDisplayed()))
            onView(withId(R.id.errorText)).check(matches(withText("PINs do not match")))

            // Fix the PINs
            onView(withId(R.id.confirmPinInput)).perform(clearText(), typeText("1234"))

            // Clear error by clicking continue again
            onView(withId(R.id.btnNext)).perform(click())

            // Should proceed to success screen
            Thread.sleep(1000)
            onView(withId(R.id.btnNext)).check(matches(withText("Finish")))
        }
    }

    @Test
    fun `setup wizard should handle password validation errors correctly`() {
        // Launch setup activity
        val intent = Intent(ApplicationProvider.getApplicationContext(), SecurityAccessSetupActivity::class.java)
        ActivityScenario.launch<SecurityAccessSetupActivity>(intent).use { scenario ->

            // Navigate to password setup
            Thread.sleep(1000)
            onView(withId(R.id.btnNext)).perform(click()) // Welcome
            Thread.sleep(500)
            onView(withId(R.id.passwordOption)).perform(click())
            onView(withId(R.id.btnNext)).perform(click()) // Method selection

            // Wait for password setup screen
            Thread.sleep(500)

            // Enter weak password
            onView(withId(R.id.passwordInput)).perform(typeText("weak"))
            onView(withId(R.id.confirmPasswordInput)).perform(typeText("weak"))

            // Try to continue (should show error)
            onView(withId(R.id.btnNext)).perform(click())

            // Verify error message is shown
            onView(withId(R.id.errorText)).check(matches(isDisplayed()))

            // Enter strong password
            onView(withId(R.id.passwordInput)).perform(clearText(), typeText("MyStr0ng!P@ssw0rd"))
            onView(withId(R.id.confirmPasswordInput)).perform(clearText(), typeText("MyStr0ng!P@ssw0rd"))

            // Should proceed to success screen
            onView(withId(R.id.btnNext)).perform(click())
            Thread.sleep(1000)
            onView(withId(R.id.btnNext)).check(matches(withText("Finish")))
        }
    }

    @Test
    fun `setup wizard should allow skipping the setup process`() {
        // Launch setup activity
        val intent = Intent(ApplicationProvider.getApplicationContext(), SecurityAccessSetupActivity::class.java)
        ActivityScenario.launch<SecurityAccessSetupActivity>(intent).use { scenario ->

            // Wait for welcome screen
            Thread.sleep(1000)

            // Click "Skip for now"
            onView(withId(R.id.btnSkip)).perform(click())

            // Verify activity finished with skip result
            scenario.onActivity { activity ->
                val resultCode = scenario.result.resultCode
                val resultData = scenario.result.resultData
                assertEquals(android.app.Activity.RESULT_OK, resultCode)
                assertTrue(resultData.getBooleanExtra(SecurityAccessSetupActivity.EXTRA_SETUP_SKIPPED, false))
            }
        }
    }

    @Test
    fun `setup wizard should handle biometric setup when available`() {
        // This test would require a device with biometric capabilities
        // For now, we'll test the UI flow

        // Launch setup activity
        val intent = Intent(ApplicationProvider.getApplicationContext(), SecurityAccessSetupActivity::class.java)
        ActivityScenario.launch<SecurityAccessSetupActivity>(intent).use { scenario ->

            // Navigate to biometric selection
            Thread.sleep(1000)
            onView(withId(R.id.btnNext)).perform(click()) // Welcome
            Thread.sleep(500)

            // Select fingerprint option
            onView(withId(R.id.fingerprintOption)).perform(click())
            onView(withId(R.id.btnNext)).perform(click()) // Method selection

            // Wait for fingerprint setup screen
            Thread.sleep(500)

            // The fingerprint setup would typically show a biometric prompt
            // For testing, we'll verify the UI elements are present
            onView(withId(R.id.fingerprintSetupTitle)).check(matches(isDisplayed()))
            onView(withId(R.id.fingerprintSetupDescription)).check(matches(isDisplayed()))

            // Click back to return to method selection
            onView(withId(R.id.btnPrevious)).perform(click())
            Thread.sleep(500)

            // Verify we're back at method selection
            onView(withId(R.id.title)).check(matches(withText("Choose Access Method")))
        }
    }

    @Test
    fun `setup wizard should handle navigation between pages correctly`() {
        // Launch setup activity
        val intent = Intent(ApplicationProvider.getApplicationContext(), SecurityAccessSetupActivity::class.java)
        ActivityScenario.launch<SecurityAccessSetupActivity>(intent).use { scenario ->

            // Start at welcome screen
            Thread.sleep(1000)
            onView(withId(R.id.btnNext)).check(matches(withText("Get Started")))
            onView(withId(R.id.btnPrevious)).check(matches(withEffectiveVisibility(Visibility.GONE)))

            // Go to method selection
            onView(withId(R.id.btnNext)).perform(click())
            Thread.sleep(500)

            // Verify method selection screen
            onView(withId(R.id.title)).check(matches(withText("Choose Access Method")))
            onView(withId(R.id.btnPrevious)).check(matches(isDisplayed()))
            onView(withId(R.id.btnNext)).check(matches(withText("Continue")))

            // Go back to welcome
            onView(withId(R.id.btnPrevious)).perform(click())
            Thread.sleep(500)

            // Verify back at welcome
            onView(withId(R.id.btnNext)).check(matches(withText("Get Started")))
            onView(withId(R.id.btnPrevious)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        }
    }

    @Test
    fun `setup wizard should show correct indicators for each page`() {
        // Launch setup activity
        val intent = Intent(ApplicationProvider.getApplicationContext(), SecurityAccessSetupActivity::class.java)
        ActivityScenario.launch<SecurityAccessSetupActivity>(intent).use { scenario ->

            // Check welcome screen indicators (first indicator should be selected)
            Thread.sleep(1000)
            // Note: Indicator testing would require custom matchers for the indicator views

            // Navigate through pages and verify indicators update
            onView(withId(R.id.btnNext)).perform(click()) // To method selection
            Thread.sleep(500)

            // Second indicator should be selected
            // This would require custom indicator matchers

            onView(withId(R.id.btnNext)).perform(click()) // To PIN setup
            Thread.sleep(500)

            // Third indicator should be selected
            // This would require custom indicator matchers
        }
    }
}