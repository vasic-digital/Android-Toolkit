package com.redelf.commons.data.list

import android.content.Context
import com.redelf.commons.extensions.isNotEmpty
import com.redelf.commons.extensions.readRawTextFile
import com.redelf.commons.extensions.recordException

class RawStringsListDataSource(

    private val ctx: Context,
    private val resId: Int,
    private val throwOnError: Boolean = false

) : ListDataSource<String> {

    override fun getList(): List<String> {

        try {

            val raw = ctx.readRawTextFile(resId)

            if (isNotEmpty(raw)) {

                return raw.split("\n")
            }

        } catch (e: Exception) {

            if (throwOnError) {

                throw e
            }

            recordException(e)
        }

        return emptyList()
    }
}