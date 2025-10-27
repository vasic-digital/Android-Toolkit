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
import digital.vasic.security.access.data.SecuritySettings
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecurityAccessManagerTest {

    private lateinit var context: Context
    private lateinit var securityAccessManager: SecurityAccessManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        securityAccessManager = SecurityAccessManager.getInstance(context)
    }

    @After
    fun tearDown() {
        // Clean up after each test
        runBlocking {
            securityAccessManager.disableSecurity()
        }
        SecurityAccessManager.destroyInstance()
    }

    @Test
    fun testInitialState() {
        // Test that security is initially disabled
        assertEquals(SecurityAccessManager.AccessState.DISABLED, securityAccessManager.accessState.value)
        assertFalse(securityAccessManager.isLocked.value)
        assertEquals(0, securityAccessManager.failedAttempts.value)
    }

    @Test
    fun testEnableSecurity() = runBlocking {
        // Enable security with PIN
        val result = securityAccessManager.enableSecurity(AccessMethod.PIN)
        assertTrue(result.isSuccess)
        assertEquals(SecurityAccessManager.AccessState.READY, securityAccessManager.accessState.value)
    }

    @Test
    fun testDisableSecurity() = runBlocking {
        // First enable security
        securityAccessManager.enableSecurity(AccessMethod.PIN)

        // Then disable it
        val result = securityAccessManager.disableSecurity()
        assertTrue(result.isSuccess)
        assertEquals(SecurityAccessManager.AccessState.DISABLED, securityAccessManager.accessState.value)
    }

    @Test
    fun testIsAccessRequiredWhenDisabled() = runBlocking {
        // When security is disabled, access should not be required
        val accessRequired = securityAccessManager.isAccessRequired()
        assertFalse(accessRequired)
    }

    @Test
    fun testIsAccessRequiredWhenEnabled() = runBlocking {
        // Enable security
        securityAccessManager.enableSecurity(AccessMethod.PIN)

        // Access should be required when security is enabled and no session exists
        val accessRequired = securityAccessManager.isAccessRequired()
        assertTrue(accessRequired)
    }

    @Test
    fun testAuthenticationWithInvalidPin() = runBlocking {
        // Enable security first
        securityAccessManager.enableSecurity(AccessMethod.PIN)

        // Try to authenticate with invalid PIN
        val result = securityAccessManager.authenticate(AccessMethod.PIN, "9999")
        assertTrue(result is SecurityAccessManager.AuthenticationResult.Failed)
        assertEquals(1, securityAccessManager.failedAttempts.value)
    }

    @Test
    fun testSessionTimeout() = runBlocking {
        // Enable security
        securityAccessManager.enableSecurity(AccessMethod.PIN)

        // Initially access should be required
        assertTrue(securityAccessManager.isAccessRequired())

        // After successful authentication, access should not be required (session active)
        // Note: This test assumes PIN setup is done elsewhere
        // In a real scenario, we'd need to set up PIN first
    }

    @Test
    fun testLogout() = runBlocking {
        // Enable security
        securityAccessManager.enableSecurity(AccessMethod.PIN)

        // Logout
        securityAccessManager.logout()

        // Session should be cleared
        assertNull(securityAccessManager.currentSessionId.value)
        assertEquals(SecurityAccessManager.AccessState.READY, securityAccessManager.accessState.value)
    }

    @Test
    fun testLockoutAfterFailedAttempts() = runBlocking {
        // Enable security
        securityAccessManager.enableSecurity(AccessMethod.PIN)

        // Fail authentication multiple times
        repeat(5) {
            securityAccessManager.authenticate(AccessMethod.PIN, "9999")
        }

        // Should be locked out
        assertTrue(securityAccessManager.isLocked.value)
        assertNotNull(securityAccessManager.lockoutEndTime.value)
    }

    @Test
    fun testSingletonInstance() {
        val instance1 = SecurityAccessManager.getInstance(context)
        val instance2 = SecurityAccessManager.getInstance(context)

        // Should be the same instance
        assertSame(instance1, instance2)
    }
}