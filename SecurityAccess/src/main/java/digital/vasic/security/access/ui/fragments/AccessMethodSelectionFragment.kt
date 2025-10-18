package digital.vasic.security.access.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import digital.vasic.security.access.data.AccessMethod
import digital.vasic.security.access.databinding.FragmentAccessMethodSelectionBinding
import digital.vasic.security.access.ui.SecurityAccessSetupViewModel

class AccessMethodSelectionFragment : Fragment() {

    private var _binding: FragmentAccessMethodSelectionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SecurityAccessSetupViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccessMethodSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupClickListeners()
    }

    private fun setupUI() {
        binding.title.text = "Choose Access Method"
        binding.subtitle.text = "Select how you want to secure your app"

        // Set up option cards
        setupOptionCard(
            binding.pinOption,
            "PIN",
            "Use a 4-6 digit PIN for quick access",
            android.R.drawable.ic_lock_idle_lock
        )

        setupOptionCard(
            binding.passwordOption,
            "Password",
            "Use a strong password for maximum security",
            android.R.drawable.ic_lock_lock
        )

        setupOptionCard(
            binding.fingerprintOption,
            "Fingerprint",
            "Use your fingerprint for fast, secure access",
            android.R.drawable.ic_menu_camera
        )

        setupOptionCard(
            binding.faceOption,
            "Face Recognition",
            "Use face recognition for convenient access",
            android.R.drawable.ic_menu_camera
        )

        setupOptionCard(
            binding.irisOption,
            "Iris Scan",
            "Use iris scanning for high-security access",
            android.R.drawable.ic_menu_camera
        )
    }

    private fun setupOptionCard(card: com.google.android.material.card.MaterialCardView, title: String, description: String, iconRes: Int) {
        card.findViewById<android.widget.TextView>(R.id.optionTitle).text = title
        card.findViewById<android.widget.TextView>(R.id.optionDescription).text = description
        card.findViewById<android.widget.ImageView>(R.id.optionIcon).setImageResource(iconRes)
    }

    private fun setupClickListeners() {
        binding.pinOption.setOnClickListener {
            selectMethod(AccessMethod.PIN)
        }

        binding.passwordOption.setOnClickListener {
            selectMethod(AccessMethod.PASSWORD)
        }

        binding.fingerprintOption.setOnClickListener {
            selectMethod(AccessMethod.FINGERPRINT)
        }

        binding.faceOption.setOnClickListener {
            selectMethod(AccessMethod.FACE_RECOGNITION)
        }

        binding.irisOption.setOnClickListener {
            selectMethod(AccessMethod.IRIS)
        }
    }

    private fun selectMethod(method: AccessMethod) {
        viewModel.selectAccessMethod(method)

        // Update visual selection
        updateSelectionUI(method)
    }

    private fun updateSelectionUI(selectedMethod: AccessMethod) {
        // Reset all selections
        binding.pinOption.strokeColor = androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.transparent)
        binding.passwordOption.strokeColor = androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.transparent)
        binding.fingerprintOption.strokeColor = androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.transparent)
        binding.faceOption.strokeColor = androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.transparent)
        binding.irisOption.strokeColor = androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.transparent)

        // Set selected stroke
        val selectedColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.colorPrimary)
        when (selectedMethod) {
            AccessMethod.PIN -> binding.pinOption.strokeColor = selectedColor
            AccessMethod.PASSWORD -> binding.passwordOption.strokeColor = selectedColor
            AccessMethod.FINGERPRINT -> binding.fingerprintOption.strokeColor = selectedColor
            AccessMethod.FACE_RECOGNITION -> binding.faceOption.strokeColor = selectedColor
            AccessMethod.IRIS -> binding.irisOption.strokeColor = selectedColor
            AccessMethod.NONE -> { /* No selection */ }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}