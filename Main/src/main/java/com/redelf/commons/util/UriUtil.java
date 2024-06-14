package com.redelf.commons.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import com.redelf.commons.logging.Console;

public class UriUtil {

    @SuppressLint("Range")
    public String getFileName(final Uri uri, final Context context) {

        String result = "";

        if (uri.getScheme().equals("content")) {

            try (

                    Cursor cursor = context.getContentResolver().query(

                            uri, null, null, null, null
                    )
            ) {

                if (cursor != null && cursor.moveToFirst()) {

                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }

            } catch (Exception e) {

                Console.warning(e);
            }
        }

        if (result == null) {

            result = uri.getPath();

            int cut = result.lastIndexOf('/');
            if (cut != -1) {

                result = result.substring(cut + 1);
            }
        }

        return result.replace(" ", "_");
    }
}
