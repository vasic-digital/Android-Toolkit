package com.redelf.commons.data.wrapper.list

import com.redelf.commons.data.access.DataAccess
import com.redelf.commons.data.wrapper.VersionableWrapper
import com.redelf.commons.extensions.recordException
import com.redelf.commons.modification.OnChangeCompleted
import com.redelf.commons.obtain.Obtain
import java.util.concurrent.CopyOnWriteArrayList

class DefaultListWrapper<T>(

    onUi: Boolean,
    identifier: String,
    creator: Obtain<VersionableWrapper<CopyOnWriteArrayList<T>>>,

    lazySaving: Boolean = true,
    persistData: Boolean = true,
    environment: String = "default",
    onChange: OnChangeCompleted? = null

) : ListWrapper<T, ListWrapperManager<T>>(

    identifier = identifier,
    environment = environment,
    onUi = onUi,
    onChange = onChange,

    dataAccess = object : DataAccess<T, ListWrapperManager<T>>(

        managerAccess = object : Obtain<ListWrapperManager<T>> {

            override fun obtain(): ListWrapperManager<T> {

                return ListWrapperManager.instantiate(

                    identifier = identifier,
                    creator = creator,
                    lazySavingData = lazySaving,
                    persistData = persistData
                )
            }
        }

    ) {

        override fun obtain(): Collection<T?>? {

            try {

                return managerAccess.obtain().obtain()?.takeData()

            } catch (e: Throwable) {

                recordException(e)
            }

            return null
        }
    }
)