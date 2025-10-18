# Security Access Module

A comprehensive security access module for Android applications that provides secure authentication using PIN, password, and biometric methods (fingerprint, face recognition, iris scanning). The module uses SQLCipher for encrypted database storage and follows modern Android development practices.

## Features

- **Multiple Authentication Methods**: PIN, Password, Fingerprint, Face Recognition, Iris Scanning
- **Secure Storage**: SQLCipher encrypted database for all sensitive data
- **Modern UI/UX**: Material Design 3 with smooth animations and transitions
- **Session Management**: Automatic session handling with configurable timeouts
- **Lockout Protection**: Configurable failed attempt limits with lockout periods
- **Comprehensive Testing**: Unit, integration, automation, and E2E test coverage
- **Flow Integration**: Kotlin Flow and LiveData support for reactive programming

## Architecture

### Core Components

1. **SecurityAccessManager**: Main entry point for access control logic
2. **SecurityAccessRepository**: Data access layer with encrypted storage
3. **Access Methods**: Individual authentication method implementations
4. **UI Components**: Setup wizard and authentication screens
5. **Database Layer**: Room with SQLCipher for secure data persistence

### Package Structure

```
digital.vasic.security.access
├── access/           # Access method implementations
├── data/            # Data models and repository
├── database/        # Room database and DAOs
├── ui/              # UI components and activities
├── utils/           # Security utilities and helpers
└── installation/    # Installation and capability checking
```

## Setup and Configuration

### 1. Module Integration

Add the SecurityAccess module to your `settings.gradle`:

```kotlin
include ':Toolkit:SecurityAccess'
```

Add dependency in your app's `build.gradle`:

```kotlin
dependencies {
    implementation project(':Toolkit:SecurityAccess')
}
```

### 2. Database Initialization

The module automatically initializes the encrypted database. No additional setup required.

### 3. Basic Usage

```kotlin
// Get the security access manager
val accessManager = SecurityAccessManager.getInstance(context)

// Check if access is required
val isAccessRequired = accessManager.isAccessRequired()

// Enable security with PIN
val enableResult = accessManager.enableSecurity(AccessMethod.PIN)

// Authenticate user
val authResult = accessManager.authenticate(AccessMethod.PIN, "1234")

// Disable security
val disableResult = accessManager.disableSecurity()
```

## Authentication Methods

### PIN Authentication

```kotlin
val pinAccessMethod = PinAccessMethod(0, activity)

// Setup PIN
val setupResult = pinAccessMethod.setupPin("1234", "1234")

// Verify PIN
val verifyResult = pinAccessMethod.verifyPin("1234")

// Change PIN
val changeResult = pinAccessMethod.changePin("1234", "5678", "5678")
```

### Password Authentication

```kotlin
val passwordAccessMethod = PasswordAccessMethod(0, activity)

// Setup password
val setupResult = passwordAccessMethod.setupPassword("password", "password")

// Verify password
val verifyResult = passwordAccessMethod.verifyPassword("password")

// Change password
val changeResult = passwordAccessMethod.changePassword("old", "new", "new")
```

### Biometric Authentication

```kotlin
val fingerprintAccessMethod = FingerprintAccessMethod(0, activity)

// Setup fingerprint
val setupResult = fingerprintAccessMethod.setupFingerprint()

// The biometric prompt is handled automatically by the access method
```

## Setup Wizard

The module includes a complete setup wizard for initial configuration:

```kotlin
// Launch setup wizard
val intent = Intent(context, SecurityAccessSetupActivity::class.java)
startActivityForResult(intent, SecurityAccessSetupActivity.REQUEST_CODE)

// Handle result
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when (requestCode) {
        SecurityAccessSetupActivity.REQUEST_CODE -> {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    val setupSuccess = data?.getBooleanExtra(
                        SecurityAccessSetupActivity.EXTRA_SETUP_SUCCESS, false
                    ) ?: false

                    if (setupSuccess) {
                        // Security setup completed successfully
                    } else {
                        // Setup was skipped
                    }
                }
            }
        }
    }
}
```

## Security Features

### Encrypted Storage

All sensitive data is stored in an SQLCipher encrypted database:

- PIN hashes (PBKDF2 with HMAC-SHA256)
- Password hashes (PBKDF2 with HMAC-SHA256)
- Biometric templates (encrypted)
- Session data (encrypted)

### Session Management

- Configurable session timeouts (default: 5 minutes)
- Automatic session cleanup
- Device-specific session tracking
- Session invalidation on security changes

### Lockout Protection

- Configurable maximum failed attempts (default: 5)
- Automatic lockout after threshold reached
- Configurable lockout duration (default: 30 minutes)
- Failed attempt tracking and reporting

### Security Monitoring

- Comprehensive attempt logging
- Suspicious activity detection
- Location tracking for attempts (when available)
- Device fingerprinting for security

## Configuration Options

### Security Settings

```kotlin
val settings = SecuritySettings(
    accessMethod = AccessMethod.PIN,
    isEnabled = true,
    pinLength = 6,
    passwordMinLength = 12,
    requireSpecialChars = true,
    requireNumbers = true,
    requireUppercase = true,
    sessionTimeoutMinutes = 10,
    maxFailedAttempts = 3,
    lockoutDurationMinutes = 15,
    allowFingerprint = true,
    allowFaceRecognition = true,
    allowIris = true,
    requireStrongBiometric = false,
    showBiometricPrompt = true,
    vibrateOnError = true,
    playSoundOnError = true
)
```

## Testing

The module includes comprehensive test coverage:

### Unit Tests

```bash
./gradlew :Toolkit:SecurityAccess:testDebugUnitTest
```

### Integration Tests

```bash
./gradlew :Toolkit:SecurityAccess:connectedDebugAndroidTest
```

### Automation Tests

```bash
./gradlew :Toolkit:SecurityAccess:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=digital.vasic.security.access.automation.SecurityAccessSetupAutomationTest
```

### E2E Tests

```bash
./gradlew :Toolkit:SecurityAccess:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=digital.vasic.security.access.e2e.SecurityAccessE2ETest
```

## API Reference

### SecurityAccessManager

Main class for managing security access:

```kotlin
class SecurityAccessManager {
    // State management
    val accessState: StateFlow<AccessState>
    val isLocked: StateFlow<Boolean>
    val failedAttempts: StateFlow<Int>

    // Core operations
    suspend fun enableSecurity(method: AccessMethod): Result<Unit>
    suspend fun disableSecurity(): Result<Unit>
    suspend fun isAccessRequired(): Boolean
    suspend fun authenticate(method: AccessMethod, credential: String): AuthenticationResult
    suspend fun logout()

    // Status monitoring
    fun getSecurityStatus(): Flow<SecurityStatus>
}
```

### SecurityAccessRepository

Data access layer:

```kotlin
class SecurityAccessRepository {
    // Settings operations
    fun getSecuritySettings(): Flow<SecuritySettings?>
    suspend fun saveSecuritySettings(settings: SecuritySettings)

    // Credential operations
    fun getAccessCredential(): Flow<AccessCredential?>
    suspend fun updatePin(pin: String)
    suspend fun updatePassword(password: String)
    suspend fun verifyPin(pin: String): Boolean
    suspend fun verifyPassword(password: String): Boolean

    // Session management
    suspend fun createSession(deviceId: String, accessMethod: AccessMethod): AccessSession
    suspend fun updateSessionActivity(sessionId: String, duration: Long)

    // Attempt tracking
    suspend fun recordAccessAttempt(...)
    fun getRecentFailedAttempts(deviceId: String, limit: Int): Flow<List<AccessAttempt>>
}
```

### Access Methods

Individual authentication method implementations:

```kotlin
abstract class BaseAccessMethod(
    priority: Int,
    context: AppCompatActivity
) : Installation, Cancellation, CommonExecution, CapabilityCheck

class PinAccessMethod(...) : BaseAccessMethod(...)
class PasswordAccessMethod(...) : BaseAccessMethod(...)
class FingerprintAccessMethod(...) : BaseAccessMethod(...)
class FaceRecognitionAccessMethod(...) : BaseAccessMethod(...)
class IrisAccessMethod(...) : BaseAccessMethod(...)
```

## Error Handling

The module provides comprehensive error handling:

```kotlin
sealed class AuthenticationResult {
    object Success : AuthenticationResult()
    object Disabled : AuthenticationResult()
    object BiometricRequired : AuthenticationResult()
    data class Failed(val message: String) : AuthenticationResult()
    data class LockedOut(val endTime: LocalDateTime?) : AuthenticationResult()
    data class Error(val message: String) : AuthenticationResult()
}
```

## Performance Considerations

- **Database Encryption**: SQLCipher provides hardware-accelerated encryption
- **Memory Management**: Proper cleanup of sensitive data from memory
- **Background Processing**: All heavy operations run on background threads
- **Caching**: Efficient caching of frequently accessed data
- **Lazy Initialization**: Components are initialized only when needed

## Security Best Practices

1. **Key Management**: Uses Android Keystore for key storage
2. **Memory Protection**: Sensitive data is cleared from memory after use
3. **Timing Attacks**: Constant-time comparison for credential verification
4. **Brute Force Protection**: Exponential backoff and lockout mechanisms
5. **Audit Logging**: Comprehensive logging of all security events

## Troubleshooting

### Common Issues

1. **Database Corruption**: The module handles database corruption gracefully
2. **Biometric Unavailable**: Falls back to alternative authentication methods
3. **Session Timeout**: Automatic re-authentication when sessions expire
4. **Lockout Recovery**: Manual unlock or wait for lockout period to expire

### Debug Information

Enable debug logging:

```kotlin
// In your app's Application class
SecurityAccessManager.getInstance(context).apply {
    // Debug information is logged automatically
}
```

## Contributing

When contributing to the Security Access module:

1. Follow Kotlin coding standards
2. Add comprehensive tests for new features
3. Update documentation for API changes
4. Ensure security best practices are maintained
5. Test on multiple Android versions (10-16)

## License

This module is part of the ShareConnect project and follows the same licensing terms.