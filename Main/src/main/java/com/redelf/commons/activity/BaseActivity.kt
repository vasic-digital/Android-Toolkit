package com.redelf.commons.activity

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.text.TextUtils
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.redelf.commons.Broadcast
import com.redelf.commons.R
import com.redelf.commons.dialog.AttachFileDialog
import com.redelf.commons.dialog.OnPickFromCameraCallback
import com.redelf.commons.execution.Executor
import com.redelf.commons.execution.TaskExecutor
import com.redelf.commons.initRegistrationWithGoogle
import com.redelf.commons.isServiceRunning
import com.redelf.commons.lifecycle.LifecycleCallback
import com.redelf.commons.obtain.OnObtain
import com.redelf.commons.transmission.TransmissionManager
import com.redelf.commons.transmission.TransmissionService
import com.redelf.commons.util.UriUtil
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import net.yslibrary.android.keyboardvisibilityevent.Unregistrar
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

abstract class BaseActivity : AppCompatActivity() {

    protected var googleSignInRequestCode = AtomicInteger()
    protected var transmissionService: TransmissionService? = null
    protected var attachmentObtainedUris: MutableList<Uri> = mutableListOf()
    protected var attachmentObtainedFiles: MutableList<File> = mutableListOf()

    protected val executor = Executor.MAIN
    protected val dismissDialogsRunnable = Runnable { dismissDialogs() }

    protected val dismissDialogsAndTerminateRunnable = Runnable {

        dismissDialogs()
        closeActivity()
    }

    protected open val canSendOnTransmissionServiceConnected = true

    private var created = false
    private val paused = AtomicBoolean()
    private var unregistrar: Unregistrar? = null
    private val dialogs = mutableListOf<AlertDialog>()
    private var attachmentsDialog: AttachFileDialog? = null
    private lateinit var backPressedCallback: OnBackPressedCallback

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filter = IntentFilter()

        filter.addAction(Broadcast.ACTION_FINISH)
        filter.addAction(Broadcast.ACTION_FINISH_ALL)

        LocalBroadcastManager.getInstance(applicationContext)
            .registerReceiver(finishReceiver, filter)

        Timber.v("Transmission management supported: ${isTransmissionServiceSupported()}")

        if (isTransmissionServiceSupported()) {

            initializeTransmissionManager(transmissionManagerInitCallback)
        }

        backPressedCallback = object : OnBackPressedCallback(true) {

            override fun handleOnBackPressed() {

                onBack()
            }
        }

        onBackPressedDispatcher.addCallback(this, backPressedCallback)
        created = true
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {

        super.onPostCreate(savedInstanceState)

        unregistrar = KeyboardVisibilityEvent.registerEventListener(

            this
        ) {

            onKeyboardVisibilityEvent(it)
        }
    }

    override fun onPause() {

        paused.set(true)

        super.onPause()
    }

    override fun onResume() {

        paused.set(false)

        super.onResume()
    }

    protected open fun onKeyboardVisibilityEvent(isOpen: Boolean) {

        Timber.v("Keyboard :: Is open: $isOpen")
    }

    private val finishReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {

            intent?.let {

                if (Broadcast.ACTION_FINISH == intent.action) {

                    handleFinishBroadcast(intent)
                }

                if (Broadcast.ACTION_FINISH_ALL == intent.action) {

                    handleFinishAllBroadcast()
                }
            }
        }
    }

    private val serviceConnection = object : ServiceConnection {

        override fun onServiceDisconnected(name: ComponentName?) {

            transmissionService = null
            Timber.v("Transmission service disconnected: %s", name)
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {

            binder?.let {

                transmissionService =
                    (it as TransmissionService.TransmissionServiceBinder).getService()

                if (canSendOnTransmissionServiceConnected) {

                    val intent = Intent(TransmissionManager.BROADCAST_ACTION_SEND)
                    sendBroadcast(intent)

                    Timber.v("BROADCAST_ACTION_SEND on transmission service connected")

                } else {

                    Timber.w(

                        "BROADCAST_ACTION_SEND on transmission service connected, SKIPPED"
                    )
                }

                onTransmissionServiceConnected()
                onTransmissionManagementReady()
            }
        }
    }

    private val onPickFromCameraCallback = object : OnPickFromCameraCallback {

        override fun onDataAccessPrepared(file: File, uri: Uri) {

            Timber.v("Camera output uri: $uri")
            Timber.v("Camera output file: ${file.absolutePath}")

            val from = "onDataAccessPrepared"

            clearAttachmentUris(from)
            clearAttachmentFiles(from)

            attachmentObtainedUris.add(uri)
            attachmentObtainedFiles.add(file)
        }
    }

    private val transmissionManagerInitCallback = object : OnObtain<Boolean> {

        override fun onCompleted(data: Boolean) {

            Timber.v("Transmission manager :: INIT :: onCompleted: $data")

            try {

                val clazz = TransmissionService::class.java

                if (isServiceRunning(clazz)) {

                    Timber.v("Transmission service is already running")

                } else {

                    Timber.v("Transmission service is going to be started")

                    val serviceIntent = Intent(this@BaseActivity, clazz)
                    startService(serviceIntent)
                }

                val intent = Intent(this@BaseActivity, clazz)
                bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

            } catch (e: IllegalStateException) {

                onTransmissionManagementFailed(e)

            } catch (e: SecurityException) {

                onTransmissionManagementFailed(e)
            }
        }

        override fun onFailure(error: Throwable) {

            onTransmissionManagementFailed(error)
        }
    }

    protected open fun onTransmissionManagementReady() {

        Timber.v("Transmission management is ready")
    }

    protected open fun onTransmissionManagementFailed(error: Throwable) {

        Timber.e(error)
    }

    open fun onBack() {

        Timber.v("onBack()")

        if (isFinishing) {

            return
        }

        finish()
    }

    fun isNotFinishing() = !isFinishing

    override fun onDestroy() {

        dismissDialogs()
        LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(finishReceiver)

        unregistrar?.unregister()

        if (isTransmissionServiceSupported()) {

            transmissionService?.let {

                try {

                    unbindService(serviceConnection)

                } catch (e: IllegalArgumentException) {

                    Timber.w(e.message)
                }
            }
        }

        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    override fun finish() {
        super.finish()

        overridePendingTransition(

            0, 0
        )
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val tag = "Google registration :: On act. result ::"

        Timber.v("$tag requestCode: $requestCode")

        if (requestCode == googleSignInRequestCode.get()) {

            Timber.v("$tag Req. code ok")

            val task = GoogleSignIn.getSignedInAccountFromIntent(data)

            try {

                val account = task.getResult(ApiException::class.java)

                account.idToken?.let {

                    Timber.v("$tag We have token: $it")

                    firebaseAuthWithGoogle(it)
                }

                if (account.idToken == null) {

                    Timber.e("$tag We have no token")

                    val e = IllegalStateException("Obtained null Google token ID")
                    onRegistrationWithGoogleFailed(e)
                }

            } catch (e: ApiException) {

                Timber.e(

                    "$tag Status code: ${e.statusCode}, " +
                            "Message: ${e.message ?: "no message"}"
                )

                onRegistrationWithGoogleFailed(e)
            }

            return
        }

        if (resultCode == RESULT_CANCELED) {

            return
        }

        if (resultCode != RESULT_OK) {

            showError(R.string.error_attaching_file)
            return
        }

        when (requestCode) {

            AttachFileDialog.REQUEST_DOCUMENT,
            AttachFileDialog.REQUEST_GALLERY_PHOTO -> {

                if (data == null) {

                    showError(R.string.error_attaching_file)
                }

                data?.let {

                    val from = "onActivityResult, DOC or GALLERY"

                    clearAttachmentUris(from)
                    clearAttachmentFiles(from)

                    if (it.clipData != null) {

                        val clipData = it.clipData
                        val count = clipData?.itemCount ?: 0

                        if (count == 0) {

                            showError(R.string.error_attaching_file)

                        } else {

                            for (i in 0 until count) {

                                val uri = clipData?.getItemAt(i)?.uri

                                uri?.let { u ->

                                    attachmentObtainedUris.add(u)
                                }
                            }
                        }

                    } else {

                        it.data?.let { uri ->

                            attachmentObtainedUris.add(uri)
                        }

                        if (it.data == null) {

                            Timber.e("Gallery obtained uri is null")

                            showError(R.string.error_attaching_file)
                            return
                        }
                    }

                    handleObtainedAttachmentUris()
                }
            }

            AttachFileDialog.REQUEST_CAMERA_PHOTO -> {

                if (attachmentObtainedFiles.isEmpty()) {

                    showError(R.string.error_attaching_file)
                    return
                }

                attachmentObtainedFiles.forEach {

                    if (it.exists()) {

                        onAttachmentReady(it)

                    } else {

                        Timber.e("File does not exist: %s", it.absolutePath)
                        showError(R.string.error_attaching_file)
                    }
                }

                val from = "onActivityResult, CAM"

                clearAttachmentFiles(from)
            }

            else -> {

                Timber.w("Unknown request code: $requestCode")
            }
        }
    }

    fun broadcastFinish() {

        if (!isFinishing) {

            finish()
        }

        val intent = Intent(Broadcast.ACTION_FINISH)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
    }

    fun broadcastFinishAll() {

        if (!isFinishing) {

            finish()
        }

        val intent = Intent(Broadcast.ACTION_FINISH_ALL)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
    }

    protected open fun onRegistrationWithGoogleCompleted(tokenId: String) {

        Timber.v("Registration with Google completed: $tokenId")
    }

    protected open fun onRegistrationWithGoogleFailed(error: Throwable) {

        Timber.e("Registration with Google failed")

        Timber.e(error)
    }

    protected open fun isTransmissionServiceSupported(): Boolean {

        return resources.getBoolean(R.bool.transmission_service_supported)
    }

    protected open fun onTransmissionServiceConnected() {

        Timber.v("Transmission service connected: %s", transmissionService)
    }

    protected open fun onAttachmentReady(attachment: File) {

        Timber.v("Attachment is ready: ${attachment.absolutePath}")
    }

    protected open fun disposeAttachment(attachment: File) {

        executor.execute {

            if (attachment.exists()) {

                if (attachment.delete()) {

                    Timber.v("Attachment has been disposed: ${attachment.absolutePath}")

                } else {

                    Timber.w("Attachment has NOT been disposed: ${attachment.absolutePath}")
                }
            }
        }
    }

    protected open fun showError(error: Int) {

        alert(

            title = android.R.string.dialog_alert_title,
            message = error,
            action = {

                dismissDialogs()
            },
            dismissAction = {

                dismissDialogs()
            },
            actionLabel = android.R.string.ok,
            dismissible = false,
            cancellable = true
        )
    }

    protected open fun getTransmissionManager(callback: OnObtain<TransmissionManager<*>>) {

        val e = IllegalArgumentException("No transmission manager available")
        callback.onFailure(e)
    }

    protected open fun initializeTransmissionManager(successCallback: OnObtain<Boolean>) {

        Timber.v("Transmission manager :: INIT :: START")

        val callback = object : OnObtain<TransmissionManager<*>> {

            override fun onCompleted(data: TransmissionManager<*>) {

                if (data.isInitialized()) {

                    Timber.v("Sending manager :: Ready: $data")

                    successCallback.onCompleted(true)

                } else {

                    val sendingManagerInitCallback = object : LifecycleCallback<Unit> {

                        override fun onInitialization(success: Boolean, vararg args: Unit) {

                            successCallback.onCompleted(success)
                        }

                        override fun onShutdown(success: Boolean, vararg args: Unit) {

                            val e = IllegalStateException("Shut down unexpectedly")
                            successCallback.onFailure(e)
                        }
                    }

                    try {

                        data.initialize(sendingManagerInitCallback)

                    } catch (e: IllegalStateException) {

                        successCallback.onFailure(e)
                    }
                }
            }

            override fun onFailure(error: Throwable) {

                successCallback.onFailure(error)
            }
        }

        getTransmissionManager(callback)
    }

    fun alert(

        title: Int = android.R.string.dialog_alert_title,
        message: Int = 0,
        action: Runnable,
        dismissAction: Runnable? = null,
        icon: Int = android.R.drawable.ic_dialog_alert,
        cancellable: Boolean = false,
        dismissible: Boolean = true,
        actionLabel: Int = android.R.string.ok,
        dismissActionLabel: Int = android.R.string.cancel,
        style: Int = 0,
        messageString: String = getString(message)

    ): AlertDialog? {

        var thisDialog: AlertDialog? = null

        if (!isFinishing) {

            val ctx = if (style > 0) {

                ContextThemeWrapper(this, style)

            } else {

                this
            }

            val builder = AlertDialog.Builder(ctx, style)
                .setIcon(icon)
                .setCancelable(cancellable)
                .setTitle(title)
                .setMessage(messageString)
                .setPositiveButton(actionLabel) { dialog, _ ->

                    action.run()
                    dialog.dismiss()
                }

            if (dismissible) {

                builder.setNegativeButton(dismissActionLabel) { dialog, _ ->

                    dismissAction?.run()
                    dialog.dismiss()
                }
            }

            runOnUiThread {

                if (!isFinishing) {

                    thisDialog = builder.create()
                    thisDialog?.let {

                        dialogs.add(it)
                        it.show()
                    }

                } else {

                    Timber.w("Dialog will not be shown, the activity is finishing")
                }
            }

        } else {

            Timber.w("We will not present alert, activity is finishing")
        }

        return thisDialog
    }

    private fun closeActivity() {

        runOnUiThread {
            if (!isFinishing) {
                finish()
            }
        }
    }

    protected fun addAttachment() {

        Timber.v("Add attachment")

        attachmentsDialog?.dismiss()
        attachmentsDialog = AttachFileDialog(this, onPickFromCameraCallback, multiple = true)
        attachmentsDialog?.show(style = getAddAttachmentDialogStyle())
    }

    protected fun isPaused(): Boolean = paused.get()

    protected open fun getAddAttachmentDialogStyle(): Int = 0

    protected open fun dismissDialogs() {

        runOnUiThread {

            attachmentsDialog?.dismiss()
            attachmentsDialog = null

            dialogs.forEach {

                it.dismiss()
            }
        }
    }

    protected open fun handleFinishBroadcast(intent: Intent? = null) {

        val tag = "Finish broadcast ::"

        Timber.v("$tag START")

        val hash = this.hashCode()

        if (isFinishing) {

            Timber.v("$tag ALREADY FINISHING, THIS_HASH=$hash")

        } else {

            Timber.v("$tag FINISHING, THIS_HASH=$hash")

            finish()
        }
    }

    protected fun openLink(url: Int) {

        openLink(getString(url))
    }

    protected fun openLink(url: String) {

        val uri = Uri.parse(url)
        openUri(uri)
    }

    protected fun openUri(uri: Uri): Boolean {

        Timber.v("openUri(): $uri")

        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        try {

            startActivity(intent)

            return true

        } catch (e: ActivityNotFoundException) {

            Timber.e("openUri(): Activity has not been found")
        }

        return false
    }

    protected open fun registerWithGoogle(clientId: Int) {

        val code = initRegistrationWithGoogle(defaultWebClientId = clientId)
        googleSignInRequestCode.set(code)
    }

    protected open fun isCreated() = created

    @SuppressLint("Range")
    private fun handleObtainedAttachmentUris() {

        attachmentObtainedUris.forEach {

            val external = getExternalFilesDir(null)

            if (external == null) {

                Timber.e("External files dir is null")
                showError(R.string.error_attaching_file)
                return
            }

            val action = Runnable {

                val dir = external.absolutePath +
                        File.separator +
                        getString(R.string.app_name).replace(" ", "_") +
                        File.separator

                val newDir = File(dir)

                if (!newDir.exists() && !newDir.mkdirs()) {

                    Timber.e(

                        "Could not make directory: %s",
                        newDir.absolutePath
                    )

                    showError(R.string.error_attaching_file)
                    return@Runnable
                }

                var ins: InputStream? = null
                var fos: FileOutputStream? = null
                var bis: BufferedInputStream? = null
                var bos: BufferedOutputStream? = null

                fun closeAll() {

                    listOf(

                        bis,
                        ins,
                        fos,
                        bos

                    ).forEach {

                        it?.let { closable ->

                            try {

                                closable.close()

                            } catch (e: IOException) {

                                // Ignore, not spam
                            }
                        }
                    }
                }

                try {

                    Timber.v("Attachment uri: $it")

                    var extension = ""
                    val mimeType = contentResolver.getType(it)

                    if (mimeType != null && !TextUtils.isEmpty(mimeType)) {

                        extension = "." + mimeType.split(

                            File.separator.toRegex()

                        ).toTypedArray()[1]
                    }

                    var fileName = UriUtil().getFileName(it, applicationContext)

                    if (TextUtils.isEmpty(fileName)) {

                        fileName = System.currentTimeMillis().toString() + extension
                    }

                    val file = dir + fileName
                    val outputFile = File(file)

                    if (outputFile.exists()) {

                        if (outputFile.delete()) {

                            Timber.w("File already exists, deleting it: ${outputFile.absolutePath}")
                        }
                    }

                    if (!outputFile.createNewFile()) {

                        closeAll()

                        Timber.e("Could not create file: ${outputFile.absolutePath}")
                        showError(R.string.error_attaching_file)
                        return@Runnable
                    }

                    ins = contentResolver.openInputStream(it)

                    if (ins != null) {

                        val available = ins.available()

                        bis = BufferedInputStream(ins)
                        fos = FileOutputStream(file)
                        bos = BufferedOutputStream(fos)

                        var sent: Long
                        bos.use { fileOut ->
                            sent = bis.copyTo(fileOut)
                        }

                        Timber.v(

                            "Attachment is ready, size: " +
                                    "${outputFile.length()} :: ${sent.toInt() == available}"
                        )

                        onAttachmentReady(outputFile)

                    } else {

                        Timber.e("Input stream is null")
                        showError(R.string.error_attaching_file)
                    }

                } catch (e: IOException) {

                    Timber.e(e)
                    showError(R.string.error_attaching_file)

                } finally {

                    closeAll()
                }
            }

            executor.execute(action)
        }
    }

    private fun clearAttachmentUris(from: String) {

        Timber.v("Clearing attachment URIs from '$from'")

        attachmentObtainedUris.clear()
    }

    private fun clearAttachmentFiles(from: String) {

        Timber.v("Clearing attachment files from '$from'")

        attachmentObtainedFiles.clear()
    }

    private fun firebaseAuthWithGoogle(tokenId: String) {

        val mAuth = FirebaseAuth.getInstance()
        val credential = GoogleAuthProvider.getCredential(tokenId, null)

        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {

                    val user = mAuth.currentUser

                    user?.let {

                        onRegistrationWithGoogleCompleted(tokenId)
                    }

                    if (user == null) {

                        val e = IllegalStateException("Obtained null Google user")
                        onRegistrationWithGoogleFailed(e)
                    }

                } else {

                    val e = task.exception ?: IllegalStateException("Unknown exception")
                    onRegistrationWithGoogleFailed(e)
                }
            }
    }

    private fun handleFinishAllBroadcast() {

        val tag = "Finish broadcast :: All ::"

        Timber.v("$tag START")

        val hash = this.hashCode()

        if (isFinishing) {

            Timber.v("$tag ALREADY FINISHING, THIS_HASH=$hash")

        } else {

            Timber.v("$tag FINISHING, THIS_HASH=$hash")

            finish()
        }
    }
}