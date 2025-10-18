package digital.vasic.security.access.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import digital.vasic.security.access.ui.fragments.*

class SecurityAccessSetupPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 8 // 8 pages total

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> WelcomeFragment()
            1 -> AccessMethodSelectionFragment()
            2 -> PinSetupFragment()
            3 -> PasswordSetupFragment()
            4 -> FingerprintSetupFragment()
            5 -> FaceRecognitionSetupFragment()
            6 -> IrisSetupFragment()
            7 -> SetupSuccessFragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}