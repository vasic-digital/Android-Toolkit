package com.redelf.commons.data.wrappers

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import com.redelf.commons.execution.Executor
import com.redelf.commons.logging.Console
import java.util.concurrent.atomic.AtomicBoolean

class ListWrapper<T>(

    from: Any,
    environment: String,

    @JsonProperty("onUi")
    @SerializedName("onUi")
    private val onUi: Boolean,

    @JsonProperty("list")
    @SerializedName("list")
    private var list: MutableList<T>

) {

    companion object {

        val DEBUG = AtomicBoolean()
    }

    private val tag = "${from::class.simpleName} :: ${from.hashCode()} :: $environment :: " +
            "Data list hash = ${getHashCode()} ::"

    fun isEmpty() = list.isEmpty()

    fun isNotEmpty() = list.isNotEmpty()

    fun add(from: String, value: T, callback: (() -> Unit)? = null) {

        if (DEBUG.get()) Console.log("$tag add(value=$value) from '$from'")

        if (onUi) {

            Executor.UI.execute {

                list.add(value)

                callback?.let {

                    it()
                }
            }

        } else {

            list.add(value)

            callback?.let {

                it()
            }
        }
    }

    fun get(index: Int): T? {

        return list[index]
    }

    fun remove(from: String, index: Int, callback: (() -> Unit)? = null) {

        Console.warning("$tag remove(index=$index) from '$from'")

        if (onUi) {

            Executor.UI.execute {

                list.removeAt(index)

                callback?.let {

                    it()
                }
            }

        } else {

            list.removeAt(index)

            callback?.let {

                it()
            }
        }
    }

    fun remove(from: String, what: T, callback: (() -> Unit)? = null) {

        Console.warning("$tag remove(index=$what) from '$from'")

        if (onUi) {

            Executor.UI.execute {

                list.remove(what)

                callback?.let {

                    it()
                }
            }

        } else {

            list.remove(what)

            callback?.let {

                it()
            }
        }
    }

    fun clear(from: String, callback: (() -> Unit)? = null) {

        Console.warning("$tag doClear() from '$from'")

        if (onUi) {

            Executor.UI.execute {

                list.clear()

                callback?.let {

                    it()
                }
            }

        } else {

            list.clear()

            callback?.let {

                it()
            }
        }
    }

    fun addAll(what: Collection<T>, from: String, callback: (() -> Unit)? = null) {

        if (DEBUG.get()) Console.log("$tag doClear() from '$from'")

        if (onUi) {

            Executor.UI.execute {

                list.addAll(what)

                callback?.let {

                    it()
                }
            }

        } else {

            list.addAll(what)

            callback?.let {

                it()
            }
        }
    }

    fun getHashCode(): Int {

        return list.hashCode()
    }

    fun getSize(): Int {

        return list.size
    }

    fun getList() = list.toList()

    fun contains(what: T) = list.contains(what)
}