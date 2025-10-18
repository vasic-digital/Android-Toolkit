package digital.vasic.security.access.installation

interface Installation {
    fun install()
    fun checkInstalled(callback: InstallationCheckCallback)
}