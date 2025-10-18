package digital.vasic.security.access.access

import androidx.appcompat.app.AppCompatActivity
import digital.vasic.security.access.data.AccessMethod
import digital.vasic.security.access.installation.Installation
import digital.vasic.security.access.installation.InstallationCheckCallback
import digital.vasic.security.access.utils.CapabilityCheck
import digital.vasic.security.access.utils.CapabilityCheckCallback
import digital.vasic.security.access.utils.CallbackOperation
import digital.vasic.security.access.utils.Callbacks
import digital.vasic.security.access.utils.Cancellation
import digital.vasic.security.access.utils.CommonExecution
import digital.vasic.security.access.utils.CommonExecutionCallback
import digital.vasic.security.access.utils.Executor
import digital.vasic.security.access.utils.Console
import java.util.concurrent.atomic.AtomicBoolean

abstract class BaseAccessMethod(
    private val priority: Int,
    protected val context: AppCompatActivity
) : Installation, Cancellation, CommonExecution, CapabilityCheck, Comparable<BaseAccessMethod> {

    protected val executor = Executor.MAIN
    protected val executing = AtomicBoolean()
    protected val executionCallbacks = Callbacks<CommonExecutionCallback>(identifier = "Execution")
    protected val installationCallbacks = Callbacks<InstallationCheckCallback>(identifier = "Installation")
    protected val capabilityCheckCallbacks = Callbacks<CapabilityCheckCallback>(identifier = "Capability check")

    abstract val accessMethod: AccessMethod

    override fun checkCapability(callback: CapabilityCheckCallback) {
        capabilityCheckCallbacks.register(callback)
    }

    override fun checkInstalled(callback: InstallationCheckCallback) {
        installationCallbacks.register(callback)
    }

    final override fun execute(callback: CommonExecutionCallback) {
        executionCallbacks.register(callback)
        if (executing.get()) {
            Console.warning("Already executing: $this")
            return
        }

        executing.set(true)

        val checkCallback = object : InstallationCheckCallback {
            override fun onInstallationChecked(installed: Boolean) {
                if (installed) {
                    execute()
                } else {
                    val msg = "onInstallationChecked, not installed"
                    executionCallback.onExecution(false, msg)
                }
            }
        }

        checkInstalled(checkCallback)
    }

    abstract fun execute()

    override fun cancel() {
        // Implementation specific cancellation logic
    }

    fun isExecuting() = executing.get()

    override fun compareTo(other: BaseAccessMethod) = priority.compareTo(other.priority)

    override fun toString(): String {
        return "BaseAccessMethod(name=${this::class.simpleName}, priority=$priority, method=$accessMethod)"
    }

    protected val executionCallback = object : CommonExecutionCallback {
        override fun onExecution(success: Boolean, calledFrom: String) {
            executing.set(false)

            executionCallbacks.doOnAll(object : CallbackOperation<CommonExecutionCallback> {
                override fun perform(callback: CommonExecutionCallback) {
                    callback.onExecution(success, "executionCallback :: $calledFrom")
                    executionCallbacks.unregister(callback)
                }
            }, operationName = "Execution operation")
        }
    }

    protected val installationCallback = object : InstallationCheckCallback {
        override fun onInstallationChecked(installed: Boolean) {
            installationCallbacks.doOnAll(object : CallbackOperation<InstallationCheckCallback> {
                override fun perform(callback: InstallationCheckCallback) {
                    callback.onInstallationChecked(installed)
                    installationCallbacks.unregister(callback)
                }
            }, operationName = "Installation operation")
        }
    }

    protected val capabilityCheckCallback = object : CapabilityCheckCallback {
        override fun onCapabilityChecked(capable: Boolean) {
            capabilityCheckCallbacks.doOnAll(object : CallbackOperation<CapabilityCheckCallback> {
                override fun perform(callback: CapabilityCheckCallback) {
                    callback.onCapabilityChecked(capable)
                    capabilityCheckCallbacks.unregister(callback)
                }
            }, operationName = "Capability check operation")
        }
    }
}