import com.redelf.commons.lifecycle.LifecycleCallback

interface TerminationParametrized<P, T> {

    fun terminate(param: P, callback: LifecycleCallback<T>)
}