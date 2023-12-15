package com.redelf.commons.dialog

import android.net.Uri
import java.io.File

interface OnPickFromCameraCallback {

    fun onDataAccessPrepared(

        file: File,
        uri: Uri
    )
}