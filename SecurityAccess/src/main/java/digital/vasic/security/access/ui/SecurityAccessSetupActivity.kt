package digital.vasic.security.access.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import digital.vasic.security.access.data.AccessMethod
import digital.vasic.security.access.databinding.ActivitySecurityAccessSetupBinding
import kotlinx.coroutines.launch

class SecurityAccessSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySecurityAccessSetupBinding
    private val viewModel: SecurityAccessSetupViewModel by viewModels()

    private lateinit var setupPagerAdapter: SecurityAccessSetupPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySecurityAccessSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
        setupIndicators()
        setupButtons()
        observeViewModel()
    }

    private fun setupViewPager() {
        setupPagerAdapter = SecurityAccessSetupPagerAdapter(this)
        binding.viewPager.adapter = setupPagerAdapter
        binding.viewPager.isUserInputEnabled = false // Disable swiping, use buttons instead

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateIndicators(position)
                updateButtons(position)
            }
        })
    }

    private fun setupIndicators() {
        // Create indicators dynamically based on number of pages
        val indicators = mutableListOf<View>()
        for (i in 0 until setupPagerAdapter.itemCount) {
            val indicator = View(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    16.dpToPx(),
                    16.dpToPx()
                ).apply {
                    marginStart = if (i == 0) 0 else 8.dpToPx()
                }
                background = androidx.core.content.res.ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.indicator_unselected,
                    theme
                )
            }
            indicators.add(indicator)
            binding.indicatorsContainer.addView(indicator)
        }
    }

    private fun updateIndicators(position: Int) {
        for (i in 0 until binding.indicatorsContainer.childCount) {
            val indicator = binding.indicatorsContainer.getChildAt(i)
            val drawable = if (i == position) {
                androidx.core.content.res.ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.indicator_selected,
                    theme
                )
            } else {
                androidx.core.content.res.ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.indicator_unselected,
                    theme
                )
            }
            indicator.background = drawable
        }
    }

    private fun setupButtons() {
        binding.btnPrevious.setOnClickListener {
            val currentPage = binding.viewPager.currentItem
            if (currentPage > 0) {
                binding.viewPager.currentItem = currentPage - 1
            }
        }

        binding.btnNext.setOnClickListener {
            val currentPage = binding.viewPager.currentItem
            when (currentPage) {
                0 -> { // Welcome page
                    binding.viewPager.currentItem = 1
                }
                1 -> { // Method selection page
                    val selectedMethod = viewModel.selectedAccessMethod.value
                    if (selectedMethod != null) {
                        when (selectedMethod) {
                            AccessMethod.PIN -> binding.viewPager.currentItem = 2
                            AccessMethod.PASSWORD -> binding.viewPager.currentItem = 3
                            AccessMethod.FINGERPRINT -> binding.viewPager.currentItem = 4
                            AccessMethod.FACE_RECOGNITION -> binding.viewPager.currentItem = 5
                            AccessMethod.IRIS -> binding.viewPager.currentItem = 6
                            AccessMethod.NONE -> {
                                // Show error or go back
                                showError("Please select an access method")
                            }
                        }
                    } else {
                        showError("Please select an access method")
                    }
                }
                2 -> { // PIN setup page
                    lifecycleScope.launch {
                        val pinAccessMethod = digital.vasic.security.access.access.PinAccessMethod(0, this@SecurityAccessSetupActivity)
                        val result = pinAccessMethod.setupPin(viewModel.pinValue.value, viewModel.confirmPinValue.value)
                        when (result) {
                            is digital.vasic.security.access.access.PinAccessMethod.SetupResult.Success -> {
                                binding.viewPager.currentItem = 7 // Success page
                            }
                            is digital.vasic.security.access.access.PinAccessMethod.SetupResult.Error -> {
                                showError(result.message)
                            }
                        }
                    }
                }
                3 -> { // Password setup page
                    lifecycleScope.launch {
                        val passwordAccessMethod = digital.vasic.security.access.access.PasswordAccessMethod(0, this@SecurityAccessSetupActivity)
                        val result = passwordAccessMethod.setupPassword(viewModel.passwordValue.value, viewModel.confirmPasswordValue.value)
                        when (result) {
                            is digital.vasic.security.access.access.PasswordAccessMethod.SetupResult.Success -> {
                                binding.viewPager.currentItem = 7 // Success page
                            }
                            is digital.vasic.security.access.access.PasswordAccessMethod.SetupResult.Error -> {
                                showError(result.message)
                            }
                        }
                    }
                }
                4 -> { // Fingerprint setup page
                    lifecycleScope.launch {
                        val fingerprintAccessMethod = digital.vasic.security.access.access.FingerprintAccessMethod(0, this@SecurityAccessSetupActivity)
                        val result = fingerprintAccessMethod.setupFingerprint()
                        when (result) {
                            is digital.vasic.security.access.access.FingerprintAccessMethod.SetupResult.Success -> {
                                binding.viewPager.currentItem = 7 // Success page
                            }
                            is digital.vasic.security.access.access.FingerprintAccessMethod.SetupResult.Error -> {
                                showError(result.message)
                            }
                        }
                    }
                }
                5 -> { // Face recognition setup page
                    lifecycleScope.launch {
                        val faceAccessMethod = digital.vasic.security.access.access.FaceRecognitionAccessMethod(0, this@SecurityAccessSetupActivity)
                        val result = faceAccessMethod.setupFaceRecognition()
                        when (result) {
                            is digital.vasic.security.access.access.FaceRecognitionAccessMethod.SetupResult.Success -> {
                                binding.viewPager.currentItem = 7 // Success page
                            }
                            is digital.vasic.security.access.access.FaceRecognitionAccessMethod.SetupResult.Error -> {
                                showError(result.message)
                            }
                        }
                    }
                }
                6 -> { // Iris setup page
                    lifecycleScope.launch {
                        val irisAccessMethod = digital.vasic.security.access.access.IrisAccessMethod(0, this@SecurityAccessSetupActivity)
                        val result = irisAccessMethod.setupIris()
                        when (result) {
                            is digital.vasic.security.access.access.IrisAccessMethod.SetupResult.Success -> {
                                binding.viewPager.currentItem = 7 // Success page
                            }
                            is digital.vasic.security.access.access.IrisAccessMethod.SetupResult.Error -> {
                                showError(result.message)
                            }
                        }
                    }
                }
                7 -> { // Success page
                    finishWithSuccess()
                }
            }
        }

        binding.btnSkip.setOnClickListener {
            finishWithSkip()
        }
    }

    private fun updateButtons(position: Int) {
        binding.btnPrevious.isVisible = position > 0
        binding.btnSkip.isVisible = position < 7 // Hide skip on success page

        when (position) {
            0 -> { // Welcome page
                binding.btnNext.text = "Get Started"
            }
            1 -> { // Method selection page
                binding.btnNext.text = "Continue"
            }
            2, 3, 4, 5, 6 -> { // Setup pages
                binding.btnNext.text = "Setup"
            }
            7 -> { // Success page
                binding.btnNext.text = "Finish"
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.selectedAccessMethod.collect { method ->
                // Update UI based on selected method
                updateMethodSpecificUI(method)
            }
        }
    }

    private fun updateMethodSpecificUI(method: AccessMethod?) {
        // Update UI elements based on selected access method
        // This would typically update descriptions, icons, etc.
    }

    private fun showError(message: String) {
        binding.errorText.text = message
        binding.errorText.isVisible = true

        // Hide error after 3 seconds
        binding.errorText.postDelayed({
            binding.errorText.isVisible = false
        }, 3000)
    }

    private fun finishWithSuccess() {
        val resultIntent = Intent().apply {
            putExtra(EXTRA_SETUP_SUCCESS, true)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun finishWithSkip() {
        val resultIntent = Intent().apply {
            putExtra(EXTRA_SETUP_SKIPPED, true)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    companion object {
        const val EXTRA_SETUP_SUCCESS = "extra_setup_success"
        const val EXTRA_SETUP_SKIPPED = "extra_setup_skipped"
        const val REQUEST_CODE = 1000
    }
}