package digital.vasic.security.access.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import digital.vasic.security.access.databinding.FragmentPinSetupBinding
import digital.vasic.security.access.ui.SecurityAccessSetupViewModel

class PinSetupFragment : Fragment() {

    private var _binding: FragmentPinSetupBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SecurityAccessSetupViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPinSetupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupTextWatchers()
    }

    private fun setupUI() {
        binding.title.text = "Set up PIN"
        binding.subtitle.text = "Choose a 4-6 digit PIN to secure your app"

        // Set up PIN input field
        binding.pinInput.transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
        binding.confirmPinInput.transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()

        // Add visual feedback for PIN strength
        updatePinStrength()
    }

    private fun setupTextWatchers() {
        binding.pinInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.updatePin(s.toString())
                updatePinStrength()
                validatePins()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.confirmPinInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.updateConfirmPin(s.toString())
                validatePins()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun updatePinStrength() {
        val pin = binding.pinInput.text.toString()

        when {
            pin.length < 4 -> {
                binding.pinStrengthText.text = "Too short"
                binding.pinStrengthIndicator.setBackgroundColor(
                    androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
                )
            }
            pin.length == 4 -> {
                binding.pinStrengthText.text = "Good"
                binding.pinStrengthIndicator.setBackgroundColor(
                    androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark)
                )
            }
            pin.length >= 5 -> {
                binding.pinStrengthText.text = "Strong"
                binding.pinStrengthIndicator.setBackgroundColor(
                    androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
                )
            }
        }

        binding.pinStrengthContainer.isVisible = pin.isNotEmpty()
    }

    private fun validatePins() {
        val pin = binding.pinInput.text.toString()
        val confirmPin = binding.confirmPinInput.text.toString()

        val isValid = pin.length >= 4 && pin == confirmPin

        binding.errorText.isVisible = pin.isNotEmpty() && confirmPin.isNotEmpty() && pin != confirmPin
        if (pin != confirmPin) {
            binding.errorText.text = "PINs do not match"
        }

        // Update continue button state
        // This would be handled by the parent activity
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}