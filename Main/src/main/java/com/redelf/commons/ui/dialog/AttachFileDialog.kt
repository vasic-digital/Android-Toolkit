package com.redelf.commons.ui.dialog

import android.app.Activity
import android.content.Intent
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import androidx.core.content.FileProvider
import com.redelf.commons.R
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.extensions.randomInteger
import com.redelf.commons.logging.Console
import java.io.File

class AttachFileDialog(

    ctx: Activity,
    dialogStyle: Int = 0,
    private val multiple: Boolean = false,
    private val onPickFromCameraCallback: OnPickFromCameraCallback

) : BaseDialog(ctx, dialogStyle) {

    companion object {

        val REQUEST_DOCUMENT = randomInteger()
        val REQUEST_CAMERA_PHOTO = randomInteger()
        val REQUEST_GALLERY_PHOTO = randomInteger()
    }

    override val tag = "Attach file dialog ::"
    override val layout = R.layout.dialog_attach_file

    override fun onContentView(contentView: View) {

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
    }

    private fun pickFromCamera() {

        Console.log("Pick from camera")

        val external = takeContext().getExternalFilesDir(null)

        external?.let { ext ->

            val dir = ext.absolutePath +
                    File.separator +
                    BaseApplication.getName().replace(" ", "_") +
                    File.separator

            val newDir = File(dir)
            if (!newDir.exists() && !newDir.mkdirs()) {

                Console.error("Could not make directory: %s", newDir.absolutePath)
            }

            val file = dir + System.currentTimeMillis() + ".jpg"
            val outputFile = File(file)

            try {

                if (!outputFile.createNewFile()) {

                    Console.error("Could not create file: %s", outputFile)
                }

            } catch (e: Exception) {

                Console.error(e)
            }

            val authority: String = takeContext().applicationContext.packageName.toString() +
                    ".generic.provider"

            val outputFileUri = FileProvider.getUriForFile(

                takeContext(),
                authority,
                outputFile
            )

            Console.debug("File output uri: %s", outputFileUri)

            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri)
            takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

            val title: String = takeContext().getString(R.string.attach_file_from_camera)

            onPickFromCameraCallback.onDataAccessPrepared(outputFile, outputFileUri)

            takeContext().startActivityForResult(

                Intent.createChooser(takePictureIntent, title),
                REQUEST_CAMERA_PHOTO
            )
        }
    }

    private fun pickFromGallery(multiple: Boolean = false) {

        Console.log("Pick from gallery")

        val intent = Intent(Intent.ACTION_GET_CONTENT)
        val mimeTypes = arrayOf("image/*", "video/*", "audio/*")

        intent.type = "image/*, video/*"

        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, multiple)

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

        val title = takeContext().getString(R.string.attach_file_from_gallery)

        takeContext().startActivityForResult(

            Intent.createChooser(intent, title),
            REQUEST_GALLERY_PHOTO
        )
    }

    private fun pickFromDocuments(multiple: Boolean = false) {

        Console.log("Pick from documents")

        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, multiple)

        takeContext().startActivityForResult(

            Intent.createChooser(

                intent,
                takeContext().getString(R.string.attach_file_from_documents)
            ),

            REQUEST_DOCUMENT
        )
    }
}