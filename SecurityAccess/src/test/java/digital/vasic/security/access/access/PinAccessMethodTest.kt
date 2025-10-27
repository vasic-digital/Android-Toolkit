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


package digital.vasic.security.access.access

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import digital.vasic.security.access.data.AccessMethod
import digital.vasic.security.access.data.SecurityAccessRepository
import digital.vasic.security.access.database.SecurityAccessDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class PinAccessMethodTest {

    private lateinit var context: Context
    private lateinit var repository: SecurityAccessRepository
    private lateinit var database: SecurityAccessDatabase

    @Mock
    private lateinit var mockContext: Context

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
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
        }
        SecurityAccessDatabase.destroyInstance()
        SecurityAccessRepository.destroyInstance()
    }

    @Test
    fun `setupPin should succeed with valid matching PINs`() = runBlocking {
        val pinAccessMethod = PinAccessMethod(0, context as androidx.appcompat.app.AppCompatActivity)

        val result = pinAccessMethod.setupPin("1234", "1234")

        assertTrue(result is PinAccessMethod.SetupResult.Success)

        // Verify settings were updated
        val settings = repository.getSecuritySettingsSync()
        assertNotNull(settings)
        assertEquals(AccessMethod.PIN, settings?.accessMethod)
        assertTrue(settings?.isEnabled == true)

        // Verify credential was created
        val credential = repository.getAccessCredentialSync()
        assertNotNull(credential)
        assertNotNull(credential?.hashedPin)
        assertNotNull(credential?.salt)
    }

    @Test
    fun `setupPin should fail with non-matching PINs`() = runBlocking {
        val pinAccessMethod = PinAccessMethod(0, context as androidx.appcompat.app.AppCompatActivity)

        val result = pinAccessMethod.setupPin("1234", "5678")

        assertTrue(result is PinAccessMethod.SetupResult.Error)
        assertEquals("PINs do not match", (result as PinAccessMethod.SetupResult.Error).message)
    }

    @Test
    fun `setupPin should fail with too short PIN`() = runBlocking {
        val pinAccessMethod = PinAccessMethod(0, context as androidx.appcompat.app.AppCompatActivity)

        val result = pinAccessMethod.setupPin("123", "123")

        assertTrue(result is PinAccessMethod.SetupResult.Error)
        assertEquals("PIN must be at least 4 digits", (result as PinAccessMethod.SetupResult.Error).message)
    }

    @Test
    fun `setupPin should fail when PIN already exists`() = runBlocking {
        val pinAccessMethod = PinAccessMethod(0, context as androidx.appcompat.app.AppCompatActivity)

        // Setup PIN first time
        pinAccessMethod.setupPin("1234", "1234")

        // Try to setup again
        val result = pinAccessMethod.setupPin("5678", "5678")

        assertTrue(result is PinAccessMethod.SetupResult.Error)
        assertEquals("PIN already configured", (result as PinAccessMethod.SetupResult.Error).message)
    }

    @Test
    fun `verifyPin should succeed with correct PIN`() = runBlocking {
        val pinAccessMethod = PinAccessMethod(0, context as androidx.appcompat.app.AppCompatActivity)

        // Setup PIN first
        pinAccessMethod.setupPin("1234", "1234")

        // Verify correct PIN
        val result = pinAccessMethod.verifyPin("1234")

        assertTrue(result is PinAccessMethod.VerificationResult.Success)
    }

    @Test
    fun `verifyPin should fail with incorrect PIN`() = runBlocking {
        val pinAccessMethod = PinAccessMethod(0, context as androidx.appcompat.app.AppCompatActivity)

        // Setup PIN first
        pinAccessMethod.setupPin("1234", "1234")

        // Verify incorrect PIN
        val result = pinAccessMethod.verifyPin("5678")

        assertTrue(result is PinAccessMethod.VerificationResult.Failed)
        assertEquals("Invalid PIN", (result as PinAccessMethod.VerificationResult.Failed).message)
    }

    @Test
    fun `verifyPin should fail when no PIN is configured`() = runBlocking {
        val pinAccessMethod = PinAccessMethod(0, context as androidx.appcompat.app.AppCompatActivity)

        // Try to verify without setting up PIN
        val result = pinAccessMethod.verifyPin("1234")

        assertTrue(result is PinAccessMethod.VerificationResult.Failed)
        assertEquals("Invalid PIN", (result as PinAccessMethod.VerificationResult.Failed).message)
    }

    @Test
    fun `changePin should succeed with correct current PIN and matching new PINs`() = runBlocking {
        val pinAccessMethod = PinAccessMethod(0, context as androidx.appcompat.app.AppCompatActivity)

        // Setup initial PIN
        pinAccessMethod.setupPin("1234", "1234")

        // Change PIN
        val result = pinAccessMethod.changePin("1234", "5678", "5678")

        assertTrue(result is PinAccessMethod.ChangeResult.Success)

        // Verify new PIN works
        val verifyResult = pinAccessMethod.verifyPin("5678")
        assertTrue(verifyResult is PinAccessMethod.VerificationResult.Success)

        // Verify old PIN doesn't work
        val oldVerifyResult = pinAccessMethod.verifyPin("1234")
        assertTrue(oldVerifyResult is PinAccessMethod.VerificationResult.Failed)
    }

    @Test
    fun `changePin should fail with incorrect current PIN`() = runBlocking {
        val pinAccessMethod = PinAccessMethod(0, context as androidx.appcompat.app.AppCompatActivity)

        // Setup initial PIN
        pinAccessMethod.setupPin("1234", "1234")

        // Try to change with wrong current PIN
        val result = pinAccessMethod.changePin("9999", "5678", "5678")

        assertTrue(result is PinAccessMethod.ChangeResult.Error)
        assertEquals("Current PIN is incorrect", (result as PinAccessMethod.ChangeResult.Error).message)
    }

    @Test
    fun `changePin should fail with non-matching new PINs`() = runBlocking {
        val pinAccessMethod = PinAccessMethod(0, context as androidx.appcompat.app.AppCompatActivity)

        // Setup initial PIN
        pinAccessMethod.setupPin("1234", "1234")

        // Try to change with non-matching new PINs
        val result = pinAccessMethod.changePin("1234", "5678", "9999")

        assertTrue(result is PinAccessMethod.ChangeResult.Error)
        assertEquals("New PINs do not match", (result as PinAccessMethod.ChangeResult.Error).message)
    }

    @Test
    fun `changePin should fail when new PIN is same as current PIN`() = runBlocking {
        val pinAccessMethod = PinAccessMethod(0, context as androidx.appcompat.app.AppCompatActivity)

        // Setup initial PIN
        pinAccessMethod.setupPin("1234", "1234")

        // Try to change to same PIN
        val result = pinAccessMethod.changePin("1234", "1234", "1234")

        assertTrue(result is PinAccessMethod.ChangeResult.Error)
        assertEquals("New PIN must be different from current PIN", (result as PinAccessMethod.ChangeResult.Error).message)
    }

    @Test
    fun `checkCapability should return true when PIN is configured and enabled`() = runBlocking {
        val pinAccessMethod = PinAccessMethod(0, context as androidx.appcompat.app.AppCompatActivity)

        // Setup PIN first
        pinAccessMethod.setupPin("1234", "1234")

        var capabilityResult = false
        pinAccessMethod.checkCapability(object : digital.vasic.security.access.utils.CapabilityCheckCallback {
            override fun onCapabilityChecked(capable: Boolean) {
                capabilityResult = capable
            }
        })

        // Wait for async operation
        Thread.sleep(100)

        assertTrue(capabilityResult)
    }

    @Test
    fun `checkCapability should return false when PIN is not configured`() = runBlocking {
        val pinAccessMethod = PinAccessMethod(0, context as androidx.appcompat.app.AppCompatActivity)

        var capabilityResult = true
        pinAccessMethod.checkCapability(object : digital.vasic.security.access.utils.CapabilityCheckCallback {
            override fun onCapabilityChecked(capable: Boolean) {
                capabilityResult = capable
            }
        })

        // Wait for async operation
        Thread.sleep(100)

        assertFalse(capabilityResult)
    }

    @Test
    fun `checkInstalled should return true when PIN credential exists`() = runBlocking {
        val pinAccessMethod = PinAccessMethod(0, context as androidx.appcompat.app.AppCompatActivity)

        // Setup PIN first
        pinAccessMethod.setupPin("1234", "1234")

        var installedResult = false
        pinAccessMethod.checkInstalled(object : digital.vasic.security.access.installation.InstallationCheckCallback {
            override fun onInstallationChecked(installed: Boolean) {
                installedResult = installed
            }
        })

        // Wait for async operation
        Thread.sleep(100)

        assertTrue(installedResult)
    }

    @Test
    fun `checkInstalled should return false when no PIN credential exists`() = runBlocking {
        val pinAccessMethod = PinAccessMethod(0, context as androidx.appcompat.app.AppCompatActivity)

        var installedResult = true
        pinAccessMethod.checkInstalled(object : digital.vasic.security.access.installation.InstallationCheckCallback {
            override fun onInstallationChecked(installed: Boolean) {
                installedResult = installed
            }
        })

        // Wait for async operation
        Thread.sleep(100)

        assertFalse(installedResult)
    }
}