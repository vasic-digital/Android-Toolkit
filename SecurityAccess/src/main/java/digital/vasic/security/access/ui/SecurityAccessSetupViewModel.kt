package digital.vasic.security.access.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import digital.vasic.security.access.access.FaceRecognitionAccessMethod
import digital.vasic.security.access.access.FingerprintAccessMethod
import digital.vasic.security.access.access.IrisAccessMethod
import digital.vasic.security.access.access.PasswordAccessMethod
import digital.vasic.security.access.access.PinAccessMethod
import digital.vasic.security.access.data.AccessMethod
import digital.vasic.security.access.data.SecurityAccessRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SecurityAccessSetupViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SecurityAccessRepository.getInstance(application)

    private val _selectedAccessMethod = MutableStateFlow<AccessMethod?>(null)
    val selectedAccessMethod: StateFlow<AccessMethod?> = _selectedAccessMethod

    private val _pinValue = MutableStateFlow("")
    val pinValue: StateFlow<String> = _pinValue

    private val _confirmPinValue = MutableStateFlow("")
    val confirmPinValue: StateFlow<String> = _confirmPinValue

    private val _passwordValue = MutableStateFlow("")
    val passwordValue: StateFlow<String> = _passwordValue

    private val _confirmPasswordValue = MutableStateFlow("")
    val confirmPasswordValue: StateFlow<String> = _confirmPasswordValue

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun selectAccessMethod(method: AccessMethod) {
        _selectedAccessMethod.value = method
    }

    fun updatePin(pin: String) {
        _pinValue.value = pin
    }

    fun updateConfirmPin(confirmPin: String) {
        _confirmPinValue.value = confirmPin
    }

    fun updatePassword(password: String) {
        _passwordValue.value = password
    }

    fun updateConfirmPassword(confirmPassword: String) {
        _confirmPasswordValue.value = confirmPassword
    }

    suspend fun setupPin(): PinAccessMethod.SetupResult {
        _isLoading.value = true
        return try {
            // We'll need to pass the activity context from the activity
            // For now, this is a placeholder
            PinAccessMethod.SetupResult.Error("Activity context required")
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun setupPassword(): PasswordAccessMethod.SetupResult {
        _isLoading.value = true
        return try {
            // We'll need to pass the activity context from the activity
            // For now, this is a placeholder
            PasswordAccessMethod.SetupResult.Error("Activity context required")
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun setupFingerprint(): FingerprintAccessMethod.SetupResult {
        _isLoading.value = true
        return try {
            // We'll need to pass the activity context from the activity
            // For now, this is a placeholder
            FingerprintAccessMethod.SetupResult.Error("Activity context required")
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun setupFaceRecognition(): FaceRecognitionAccessMethod.SetupResult {
        _isLoading.value = true
        return try {
            // We'll need to pass the activity context from the activity
            // For now, this is a placeholder
            FaceRecognitionAccessMethod.SetupResult.Error("Activity context required")
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun setupIris(): IrisAccessMethod.SetupResult {
        _isLoading.value = true
        return try {
            // We'll need to pass the activity context from the activity
            // For now, this is a placeholder
            IrisAccessMethod.SetupResult.Error("Activity context required")
        } finally {
            _isLoading.value = false
        }
    }
}