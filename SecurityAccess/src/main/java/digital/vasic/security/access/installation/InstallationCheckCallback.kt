package digital.vasic.security.access.installation

interface InstallationCheckCallback {
    fun onInstallationChecked(installed: Boolean)
}