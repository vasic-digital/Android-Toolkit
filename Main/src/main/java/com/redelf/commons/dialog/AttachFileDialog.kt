package com.redelf.commons.dialog

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.FileProvider
import com.redelf.commons.R
import com.redelf.commons.extensions.randomInteger
import com.redelf.commons.logging.Timber
import java.io.File

class AttachFileDialog(

    private val ctx: Activity,
    private val onPickFromCameraCallback: OnPickFromCameraCallback,
    private val multiple: Boolean = false

) {

    companion object {

        val REQUEST_DOCUMENT = randomInteger()
        val REQUEST_CAMERA_PHOTO = randomInteger()
        val REQUEST_GALLERY_PHOTO = randomInteger()
    }

    private var dialog: Dialog? = null

    fun show(style: Int = 0) {

        if (dialog == null) {

            val contentView: View =
                LayoutInflater.from(ctx).inflate(R.layout.dialog_attach_file, null)

            val context = if (style > 0) {

                ContextThemeWrapper(ctx, style)

            } else {

                ctx
            }

            dialog = AlertDialog.Builder(context)
                .setView(contentView)
                .setCancelable(true)
                .setOnCancelListener { dismiss() }
                .create()

            val fromCamera = contentView.findViewById<Button>(R.id.from_camera)
            val fromGallery = contentView.findViewById<Button>(R.id.from_gallery)
            val fromDocuments = contentView.findViewById<Button>(R.id.from_documents)

            fromCamera.setOnClickListener {

                dismiss()
                pickFromCamera()
            }

            fromGallery.setOnClickListener {

                dismiss()
                pickFromGallery(multiple)
            }

            fromDocuments.setOnClickListener {

                dismiss()
                pickFromDocuments(multiple)
            }

            dialog?.show()

        } else {

            Timber.w("Attach file dialog is already opened")
        }
    }

    fun dismiss() {

        dialog?.let {

            Timber.v("We are about to dismiss attach file dialog")

            it.dismiss()
            dialog = null
        }
    }

    private fun pickFromCamera() {

        Timber.v("Pick from camera")

        val external = ctx.getExternalFilesDir(null)

        external?.let { ext ->

            val dir = ext.absolutePath +
                    File.separator +
                    ctx.getString(R.string.app_name).replace(" ", "_") +
                    File.separator

            val newDir = File(dir)
            if (!newDir.exists() && !newDir.mkdirs()) {

                Timber.e("Could not make directory: %s", newDir.absolutePath)
            }

            val file = dir + System.currentTimeMillis() + ".jpg"
            val outputFile = File(file)

            try {

                if (!outputFile.createNewFile()) {

                    Timber.e("Could not create file: %s", outputFile)
                }

            } catch (e: Exception) {

                Timber.e(e)
            }

            val authority: String = ctx.applicationContext.packageName.toString() +
                    ".generic.provider"

            val outputFileUri = FileProvider.getUriForFile(

                ctx,
                authority,
                outputFile
            )

            Timber.d("File output uri: %s", outputFileUri)

            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri)
            takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

            val title: String = ctx.getString(R.string.attach_file_from_camera)

            onPickFromCameraCallback.onDataAccessPrepared(outputFile, outputFileUri)

            ctx.startActivityForResult(

                Intent.createChooser(takePictureIntent, title),
                REQUEST_CAMERA_PHOTO
            )
        }
    }

    private fun pickFromGallery(multiple: Boolean = false) {

        Timber.v("Pick from gallery")

        val intent = Intent(Intent.ACTION_GET_CONTENT)
        val mimeTypes = arrayOf("image/*", "video/*", "audio/*")

        intent.type = "image/*, video/*"

        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, multiple)

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

        val title = ctx.getString(R.string.attach_file_from_gallery)

        ctx.startActivityForResult(

            Intent.createChooser(intent, title),
            REQUEST_GALLERY_PHOTO
        )
    }

    private fun pickFromDocuments(multiple: Boolean = false) {

        Timber.v("Pick from documents")

        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, multiple)

        ctx.startActivityForResult(

            Intent.createChooser(

                intent,
                ctx.getString(R.string.attach_file_from_documents)
            ),

            REQUEST_DOCUMENT
        )
    }
}