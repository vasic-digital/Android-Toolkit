package com.redelf.commons.data.wrapper.list

import android.annotation.SuppressLint
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.context.ContextualManager
import com.redelf.commons.data.wrapper.VersionableWrapper
import com.redelf.commons.destruction.reset.ResettableParametrized
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.Obtain
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

@SuppressLint("StaticFieldLeak")
open class ListWrapperManager<T> private constructor(

    private val identifier: String,
    private val creator: Obtain<VersionableWrapper<CopyOnWriteArrayList<T>>>,

    lazySavingData: Boolean = true,
    persistData: Boolean = true

) :

    ResettableParametrized<String>,
    ContextualManager<VersionableWrapper<CopyOnWriteArrayList<T>>>()

{

    companion object {

        val DEBUG = AtomicBoolean()
        val INSTANCES = ConcurrentHashMap<String, ListWrapperManager<*>>()

        @Suppress("UNCHECKED_CAST")
        @Throws(IllegalArgumentException::class, InstantiationException::class)
        fun <T> instantiate(

            identifier: String,
            creator: Obtain<VersionableWrapper<CopyOnWriteArrayList<T>>>,

            lazySavingData: Boolean = true,
            persistData: Boolean = true

        ): ListWrapperManager<T> {

            if (identifier.isEmpty()) {

                throw IllegalArgumentException("Empty string not allowed for identifier")
            }

            val tag = "Instantiate :: " +
                    "${ListWrapperManager::class.java.simpleName} :: Identifier = '$identifier' ::"

            if (DEBUG.get()) Console.log("$tag START")

            val instance = INSTANCES[identifier]

            instance?.let {

                if (DEBUG.get()) Console.log("$tag END :: Existing instance obtained")

                try {

                    return it as ListWrapperManager<T>

                } catch (e: Throwable) {

                    throw InstantiationException(e.message)
                }
            }

            val ctx = BaseApplication.takeContext()
            val manager = ListWrapperManager(

                identifier, creator,
                lazySavingData = lazySavingData,
                persistData = persistData
            )

            INSTANCES[identifier] = manager

            if (DEBUG.get()) Console.log("$tag Injecting context")

            manager.injectContext(ctx)

            val mlTag = "$tag Registering to app lifecycle events ::"

            if (DEBUG.get()) Console.log("$mlTag START")

            manager.register(ctx)

            if (DEBUG.get()) Console.log(

                "$mlTag END :: ${manager.isRegistered(ctx)}"
            )

            Console.debug("$tag END :: Hash = '${manager.hashCode()}'")

            return manager
        }
    }

    override val lazySaving = lazySavingData
    override val persist = persistData
    override val instantiateDataObject = true

    override val storageKey = "${getEnvironment()}." +
            "${BaseApplication.getVersion()}.${javaClass.simpleName}.$identifier"

    override fun getLogTag() = "${javaClass.simpleName}.$identifier manager :: ${hashCode()} ::"

    override fun createDataObject() = creator.obtain()

    override fun getEnvironment() = identifier
}