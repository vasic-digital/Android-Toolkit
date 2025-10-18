package digital.vasic.security.access.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import digital.vasic.security.access.databinding.FragmentWelcomeBinding
import digital.vasic.security.access.ui.SecurityAccessSetupViewModel

class WelcomeFragment : Fragment() {

    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SecurityAccessSetupViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
    }

    private fun setupUI() {
        binding.welcomeTitle.text = "Welcome to Secure Access"
        binding.welcomeDescription.text = "Secure your app with PIN, password, or biometric authentication. " +
                "This will protect your data and ensure only you can access your information."

        binding.welcomeIcon.setImageResource(android.R.drawable.ic_lock_lock)

        // Add some nice animations or effects here
        binding.root.alpha = 0f
        binding.root.animate()
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(100)
            .start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}