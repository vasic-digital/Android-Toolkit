@file:Suppress("DEPRECATION")

package com.redelf.commons.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.text.TextUtils
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.redelf.commons.R
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.execution.Executor
import com.redelf.commons.extensions.exec
import com.redelf.commons.extensions.fitInsideSystemBoundaries
import com.redelf.commons.extensions.initRegistrationWithGoogle
import com.redelf.commons.extensions.isServiceRunning
import com.redelf.commons.extensions.randomInteger
import com.redelf.commons.logging.Console
import com.redelf.commons.messaging.broadcast.Broadcast
import com.redelf.commons.obtain.OnObtain
import com.redelf.commons.transmission.TransmissionManagement
import com.redelf.commons.transmission.TransmissionManager
import com.redelf.commons.transmission.TransmissionService
import com.redelf.commons.ui.dialog.AttachFileDialog
import com.redelf.commons.ui.dialog.OnPickFromCameraCallback
import com.redelf.commons.util.UriUtil
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import net.yslibrary.android.keyboardvisibilityevent.Unregistrar
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass

abstract class BaseActivity :

    ProgressActivity,
    StatefulActivity()

{

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
    protected open val detectAudioStreamed = BaseApplication.takeContext().detectAudioStreamed

    protected open val detectPhoneCallReceived =
        BaseApplication.takeContext().detectPhoneCallReceived

    private var created = false
    private var unregistrar: Unregistrar? = null
    private val requestPhoneState = randomInteger()
    private val dialogs = mutableListOf<AlertDialog>()
    private val PRIVATE_REQUEST_WRITE_EXTERNAL_STORAGE = 111
    private var attachmentsDialog: AttachFileDialog? = null
    private lateinit var backPressedCallback: OnBackPressedCallback

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {

        BaseApplication.takeContext().initTerminationListener()

        super.onCreate(savedInstanceState)

        val recordLogs = BaseApplication.takeContext().canRecordApplicationLogs()

        if (recordLogs && !Console.filesystemGranted()) {

            val permissions = arrayOf(

                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )

            ActivityCompat.requestPermissions(

                this,
                permissions,
                PRIVATE_REQUEST_WRITE_EXTERNAL_STORAGE
            )
        }

        val filter = IntentFilter()

        filter.addAction(Broadcast.ACTION_FINISH)
        filter.addAction(Broadcast.ACTION_FINISH_ALL)

        LocalBroadcastManager.getInstance(applicationContext)
            .registerReceiver(finishReceiver, filter)

        Console.log("Transmission management supported: ${isTransmissionServiceSupported()}")

        if (isTransmissionServiceSupported()) {

            initializeTransmissionManager(transmissionManagerInitCallback)
        }

        backPressedCallback = object : OnBackPressedCallback(true) {

            override fun handleOnBackPressed() {

                onBack()
            }
        }

        onBackPressedDispatcher.addCallback(this, backPressedCallback)

        fitInsideSystemBoundaries()

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

    override fun onPostResume() {
        super.onPostResume()

        if (detectPhoneCallReceived) {

            val permissionResult = ContextCompat
                .checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)

            val granted = permissionResult != PackageManager.PERMISSION_GRANTED

            if (granted) {

                ActivityCompat.requestPermissions(

                    this,
                    arrayOf(Manifest.permission.READ_PHONE_STATE),
                    requestPhoneState
                )

            } else {

                BaseApplication.takeContext().registerPhoneStateListener()
            }
        }

        if (detectAudioStreamed) {

            BaseApplication.takeContext().registerAudioFocusChangeListener()
        }
    }

    override fun onRequestPermissionsResult(

        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray

    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {

            requestPhoneState -> {

                val granted = grantResults.isNotEmpty() &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED

                if (granted) {

                    BaseApplication.takeContext().registerPhoneStateListener()

                } else {

                    Console.error("Permission denied for phone state listener")
                }

                return
            }

            PRIVATE_REQUEST_WRITE_EXTERNAL_STORAGE -> {

                if (

                    (grantResults.isNotEmpty() && grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED)

                ) {

                    BaseApplication.takeContext().enableLogsRecording()
                }
            }
        }
    }

    override fun showProgress(from: String) {

        Console.log("Progress :: SHOW, from: $from")
    }

    override fun hideProgress(from: String) {

        Console.log("Progress :: HIDE, from: $from")
    }

    override fun toggleProgress(show: Boolean, from: String) {

        val f = "Toggle progress <- from: $from"

        if (show) {

            showProgress(f)

        } else {

            hideProgress(f)
        }
    }

    protected open fun onKeyboardVisibilityEvent(isOpen: Boolean) {

        Console.log("Keyboard :: Is open: $isOpen")
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
            Console.log("Transmission service disconnected: %s", name)
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {

            binder?.let {

                transmissionService =
                    (it as TransmissionService.TransmissionServiceBinder).getService()

                if (canSendOnTransmissionServiceConnected) {

                    val intent = Intent(TransmissionManager.BROADCAST_ACTION_SEND)
                    sendBroadcast(intent)

                    Console.log("BROADCAST_ACTION_SEND on transmission service connected")

                } else {

                    Console.warning(

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

            Console.log("Camera output uri: $uri")
            Console.log("Camera output file: ${file.absolutePath}")

            val from = "onDataAccessPrepared"

            clearAttachmentUris(from)
            clearAttachmentFiles(from)

            attachmentObtainedUris.add(uri)
            attachmentObtainedFiles.add(file)
        }
    }

    private val transmissionManagerInitCallback = object : OnObtain<Boolean> {

        override fun onCompleted(data: Boolean) {

            Console.log("Transmission manager :: INIT :: onCompleted: $data")

            try {

                val clazz = TransmissionService::class.java

                if (isServiceRunning(clazz)) {

                    Console.log("Transmission service is already running")

                } else {

                    Console.log("Transmission service is going to be started")

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

        Console.log("Transmission management is ready")
    }

    protected open fun onTransmissionManagementFailed(error: Throwable) {

        Console.error(error)
    }

    open fun onBack() {

        Console.log("onBack()")

        if (isFinishing) {

            return
        }

        finish()
    }

    open fun showError(

        error: Int,

        positiveAction: Runnable? = null,
        dismissAction: Runnable? = null,

        style: Int? = null

    ): AlertDialog? {

        return showError(error, null, positiveAction, dismissAction, style)
    }

    open fun showError(

        error: String,

        positiveAction: Runnable? = null,
        dismissAction: Runnable? = null,

        style: Int? = null

    ): AlertDialog? {

        return showError(error, null, positiveAction, dismissAction, style)
    }

    open fun showError(

        error: Int,
        title: Int? = null,

        positiveAction: Runnable? = null,
        dismissAction: Runnable? = null,

        style: Int? = null

    ): AlertDialog? {

        return alert(

            title = title ?: android.R.string.dialog_alert_title,
            message = error,
            action = {

                dismissDialogs()
                positiveAction?.run()

            },
            dismissAction = {

                dismissDialogs()
                dismissAction?.run()
            },
            actionLabel = android.R.string.ok,
            dismissible = false,
            cancellable = true,
            style = style ?: 0
        )
    }

    open fun showError(

        error: String,
        title: Int? = null,

        positiveAction: Runnable? = null,
        dismissAction: Runnable? = null,

        style: Int? = null

    ): AlertDialog? {

        return alert(

            title = title ?: android.R.string.dialog_alert_title,
            messageString = error,
            action = {

                dismissDialogs()
                positiveAction?.run()

            },
            dismissAction = {

                dismissDialogs()
                dismissAction?.run()
            },
            actionLabel = android.R.string.ok,
            dismissible = false,
            cancellable = true,
            style = style ?: 0
        )
    }

    open fun showConfirmation(

        message: Int,

        positiveAction: Runnable? = null,
        dismissAction: Runnable? = null,

        style: Int? = null

    ): AlertDialog? {

        return showConfirmation(message, null, positiveAction, dismissAction)
    }

    open fun dismissDialogs() {

        runOnUiThread {

            attachmentsDialog?.dismiss()
            attachmentsDialog = null

            dialogs.forEach {

                it.dismiss()
            }
        }
    }

    open fun showConfirmation(

        message: Int,
        positiveLabel: Int?,

        positiveAction: Runnable? = null,
        dismissAction: Runnable? = null,

        style: Int? = null

    ): AlertDialog? {

        return alert(

            title = android.R.string.dialog_alert_title,
            message = message,
            action = {

                dismissDialogs()
                positiveAction?.run()
            },
            dismissAction = {

                dismissDialogs()
                dismissAction?.run()
            },
            actionLabel = positiveLabel ?: android.R.string.ok,
            dismissible = false,
            cancellable = true,
            style = style ?: 0
        )
    }

    open fun showConfirmation(

        message: String,
        positiveLabel: Int?,

        positiveAction: Runnable? = null,
        dismissAction: Runnable? = null,

        style: Int? = null

    ): AlertDialog? {

        return alert(

            title = android.R.string.dialog_alert_title,
            messageString = message,
            action = {

                dismissDialogs()
                positiveAction?.run()
            },
            dismissAction = {

                dismissDialogs()
                dismissAction?.run()
            },
            actionLabel = positiveLabel ?: android.R.string.ok,
            dismissible = false,
            cancellable = true,
            style = style ?: 0
        )
    }

    fun isNotFinishing() = !isFinishing

    override fun onDestroy() {

        val tag = "On destroy ::"

        Console.log("$tag START")

        dismissDialogs()
        LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(finishReceiver)

        unregistrar?.unregister()

        if (isTransmissionServiceSupported()) {

            transmissionService?.let {

                try {

                    unbindService(serviceConnection)

                } catch (e: IllegalArgumentException) {

                    Console.warning(e.message)
                }
            }
        }

        super.onDestroy()

        Console.log("$tag END")

        removeActivityFromHistory()
    }

    fun finishFrom(from: String) {

        val tag = "ACTIVITY Activity = '${this.javaClass.simpleName}' :: " +
                "TERMINATE :: FINISH FROM :: From: '$from'"

        Console.log("$tag START")

        finish()

        Console.log("$tag END")
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val tag = "Google account :: Registration :: On act. result ::"

        Console.log("$tag requestCode: $requestCode")

        if (requestCode == googleSignInRequestCode.get()) {

            Console.log("$tag Req. code ok")

            val task = GoogleSignIn.getSignedInAccountFromIntent(data)

            try {

                val account = task.getResult(ApiException::class.java)

                account.idToken?.let {

                    Console.log("$tag We have token: $it")

                    firebaseAuthWithGoogle(it)
                }

                if (account.idToken == null) {

                    Console.error("$tag We have no token")

                    onRegistrationWithGoogleFailed()
                }

            } catch (e: ApiException) {

                Console.error(

                    "$tag Status code: ${e.statusCode}, " +
                            "Message: ${e.message ?: "no message"}"
                )

                onRegistrationWithGoogleFailed()
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

                            Console.error("Gallery obtained uri is null")

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

                        Console.error("File does not exist: %s", it.absolutePath)
                        showError(R.string.error_attaching_file)
                    }
                }

                val from = "onActivityResult, CAM"

                clearAttachmentFiles(from)
            }

            else -> {

                Console.warning("Unknown request code: $requestCode")
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

        Console.log("Registration with Google completed: $tokenId")
    }

    
    protected open fun onRegistrationWithGoogleFailed() {

        Console.error("Registration with Google failed")
    }

    protected open fun isTransmissionServiceSupported(): Boolean {

        return false
    }

    protected open fun onTransmissionServiceConnected() {

        Console.log("Transmission service connected: %s", transmissionService)
    }

    protected open fun onAttachmentReady(attachment: File) {

        Console.log("Attachment is ready: ${attachment.absolutePath}")
    }

    protected open fun disposeAttachment(attachment: File) {

        executor.execute {

            if (attachment.exists()) {

                if (attachment.delete()) {

                    Console.log("Attachment has been disposed: ${attachment.absolutePath}")

                } else {

                    Console.warning("Attachment has NOT been disposed: ${attachment.absolutePath}")
                }
            }
        }
    }

    protected open fun getTransmissionManager(callback: OnObtain<TransmissionManagement>) {

        val e = IllegalArgumentException("No transmission manager available")
        callback.onFailure(e)
    }

    protected open fun initializeTransmissionManager(successCallback: OnObtain<Boolean>) {

        Console.log("Transmission manager :: INIT :: START")

        val callback = object : OnObtain<TransmissionManagement> {

            override fun onCompleted(data: TransmissionManagement) {

                Console.log("Sending manager :: Ready: $data")

                successCallback.onCompleted(true)
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

    ) = alert(

        title, message, action, dismissAction, icon, cancellable, dismissible, actionLabel,
        dismissActionLabel, style, messageString, false
    )

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
        messageString: String = getString(message),
        disableButtons: Boolean = false

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


            if (!disableButtons) {

                builder.setPositiveButton(actionLabel) { dialog, _ ->

                    action.run()
                    dialog.dismiss()
                }
            }

            if (dismissible && !disableButtons) {

                builder.setNegativeButton(dismissActionLabel) { dialog, _ ->

                    dismissAction?.run()
                    dialog.dismiss()
                }
            }

            runOnUiThread {

                if (!isFinishing) {

                    thisDialog = builder.create()

                    thisDialog.let {

                        dialogs.add(it)
                        it.show()
                    }

                } else {

                    Console.warning("Dialog will not be shown, the activity is finishing")
                }
            }

        } else {

            Console.warning("We will not present alert, activity is finishing")
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

        Console.log("Add attachment")

        attachmentsDialog?.dismiss()

        attachmentsDialog = AttachFileDialog(

            this,
            getAddAttachmentDialogStyle(),
            multiple = true,
            onPickFromCameraCallback = onPickFromCameraCallback
        )

        attachmentsDialog?.show()
    }

    protected fun execute(what: Runnable): Boolean {

        try {

            exec(what)

        } catch (e: RejectedExecutionException) {

            Console.error(e)

            return false
        }

        return true
    }

    protected open fun getAddAttachmentDialogStyle(): Int = 0

    protected open fun handleFinishBroadcast(intent: Intent? = null) {

        val tag = "Finish broadcast ::"

        Console.log("$tag START")

        val hash = this.hashCode()

        if (isFinishing) {

            Console.log("$tag ALREADY FINISHING, THIS_HASH=$hash")

        } else {

            Console.log("$tag FINISHING, THIS_HASH=$hash")

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

        Console.log("openUri(): $uri")

        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        try {

            startActivity(intent)

            return true

        } catch (e: ActivityNotFoundException) {

            Console.error("openUri(): Activity has not been found")
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

                Console.error("External files dir is null")
                showError(R.string.error_attaching_file)
                return
            }

            val action = Runnable {

                val dir = external.absolutePath +
                        File.separator +
                        BaseApplication.getName().replace(" ", "_") +
                        File.separator

                val newDir = File(dir)

                if (!newDir.exists() && !newDir.mkdirs()) {

                    Console.error(

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

                    Console.log("Attachment uri: $it")

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

                            Console.warning("File already exists, deleting it: ${outputFile.absolutePath}")
                        }
                    }

                    if (!outputFile.createNewFile()) {

                        closeAll()

                        Console.error("Could not create file: ${outputFile.absolutePath}")
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

                        Console.log(

                            "Attachment is ready, size: " +
                                    "${outputFile.length()} :: ${sent.toInt() == available}"
                        )

                        onAttachmentReady(outputFile)

                    } else {

                        Console.error("Input stream is null")
                        showError(R.string.error_attaching_file)
                    }

                } catch (e: IOException) {

                    Console.error(e)
                    showError(R.string.error_attaching_file)

                } finally {

                    closeAll()
                }
            }

            executor.execute(action)
        }
    }

    private fun clearAttachmentUris(from: String) {

        Console.log("Clearing attachment URIs from '$from'")

        attachmentObtainedUris.clear()
    }

    private fun clearAttachmentFiles(from: String) {

        Console.log("Clearing attachment files from '$from'")

        attachmentObtainedFiles.clear()
    }

    private fun firebaseAuthWithGoogle(tokenId: String) {

        val start = System.currentTimeMillis()
        val tag = "Google account :: Authenticate ::"

        Console.log("$tag START")

        val mAuth = FirebaseAuth.getInstance()
        val credential = GoogleAuthProvider.getCredential(tokenId, null)

        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {

                    val user = mAuth.currentUser

                    user?.let {

                        onRegistrationWithGoogleCompleted(tokenId)

                        val end = System.currentTimeMillis()

                        Console.log("$tag END, Time = ${(end - start) / 1000.0} seconds")
                    }

                    if (user == null) {

                        onRegistrationWithGoogleFailed()
                    }

                } else {

                    onRegistrationWithGoogleFailed()
                }
            }
    }

    private fun handleFinishAllBroadcast() {

        val tag = "Finish broadcast :: All ::"

        Console.log("$tag START")

        val hash = this.hashCode()

        if (isFinishing) {

            Console.log("$tag ALREADY FINISHING, THIS_HASH=$hash")

        } else {

            Console.log("$tag FINISHING, THIS_HASH=$hash")

            finish()
        }
    }

    fun sendBroadcast(action: String) {

        val intent = Intent(action)

        sendBroadcast(intent)
    }

    override fun sendBroadcast(intent: Intent?) {

        intent?.let {

            LocalBroadcastManager.getInstance(getActivityContext()).sendBroadcast(intent);
        }
    }

    fun sendBroadcastIPC(

        action: String,
        receiver: String,
        receiverClass: KClass<*>,
        function: String,
        content: String? = null,
        tag: String = "IPC :: Send ::"

    ): Boolean {

        return BaseApplication.takeContext().sendBroadcastIPC(

            content = content,
            function = function,
            receiver = receiver,
            action = action,
            tag = tag,
            receiverClass = receiverClass
        )
    }

    fun sendBroadcastIPC(

        action: String,
        receiver: String,
        component: String,
        function: String,
        content: String? = null,
        tag: String = "IPC :: Send ::"

    ): Boolean {

        return BaseApplication.takeContext().sendBroadcastIPC(

            content = content,
            function = function,
            receiver = receiver,
            action = action,
            tag = tag,
            cName = component
        )
    }

    override fun registerReceiver(receiver: BroadcastReceiver?, filter: IntentFilter?): Intent? {

        return doRegisterReceiver(receiver, filter)
    }

    override fun unregisterReceiver(receiver: BroadcastReceiver?) {

        receiver?.let { r ->

            LocalBroadcastManager.getInstance(getActivityContext()).unregisterReceiver(r)
        }
    }

    protected fun getActivityContext(): Context = this

    protected fun doRegisterReceiver(receiver: BroadcastReceiver?, filter: IntentFilter?): Intent? {

        receiver?.let { r ->
            filter?.let { f ->

                LocalBroadcastManager.getInstance(getActivityContext()).registerReceiver(r, f)
            }
        }

        return null
    }
}