package digital.vasic.security.access.utils

interface CapabilityCheck {
    fun checkCapability(callback: CapabilityCheckCallback)
}