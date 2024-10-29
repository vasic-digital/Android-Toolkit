package com.redelf.commons.ui.dialog

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import com.redelf.commons.context.ContextAvailability
import com.redelf.commons.logging.Console

abstract class BaseDialog(

    private val context: Activity,
    protected open var dialogStyle: Int = 0

) :

    ContextAvailability<Activity>,
    DialogInterface.OnDismissListener,
    com.redelf.commons.ui.dialog.Dialog

{

    protected abstract val tag: String
    protected var dialog: Dialog? = null
    protected open val cancellable = true

    abstract fun onContentView(contentView: View)

    override fun takeContext(): Activity {

        return context
    }

    override fun onDismiss(dialog: DialogInterface?) {

        Console.log("$tag Dialog is dismissed")
    }

    override fun show() {

        if (dialog == null) {

            val contentView = buildDialog(context, dialogStyle)

            onContentView(contentView)

            dialog?.show()

        } else {

            Console.log("$tag Dialog is already shown")
        }
    }

    override fun dismiss() {

        dialog?.let {

            Console.log("$tag We are about to dismiss dialog")

            it.dismiss()
            dialog = null
        }
    }

    private fun buildDialog(ctx: Context, style: Int): View {

        val contentView: View =
            LayoutInflater.from(ctx).inflate(layout, null)

        val context = if (style > 0) {

            ContextThemeWrapper(ctx, style)

        } else {

            ctx
        }

        dialog = AlertDialog.Builder(context)
            .setView(contentView)
            .setCancelable(cancellable)
            .setOnCancelListener { dismiss() }
            .setOnDismissListener(this)
            .create()

        return contentView
    }
}