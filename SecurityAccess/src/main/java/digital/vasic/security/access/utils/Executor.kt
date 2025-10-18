package digital.vasic.security.access.utils

import android.os.Handler
import android.os.Looper

enum class Executor {
    MAIN {
        override fun execute(action: Runnable) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                action.run()
            } else {
                Handler(Looper.getMainLooper()).post(action)
            }
        }
    },

    BACKGROUND {
        override fun execute(action: Runnable) {
            Thread(action).start()
        }
    };

    abstract fun execute(action: Runnable)
}