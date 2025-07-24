package com.redelf.commons.activity.fragment

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.FrameLayout
import androidx.core.graphics.drawable.toDrawable
import com.redelf.commons.R
import com.redelf.commons.activity.base.BaseActivity
import com.redelf.commons.activity.popup.Popup
import com.redelf.commons.activity.popup.PopupFragment
import com.redelf.commons.activity.transition.TransitionEffectsActivity
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.Obtain
import java.util.concurrent.ConcurrentHashMap


open class FragmentWrapperActivity : BaseActivity() {

    companion object {

        const val EXTRA_FRAGMENT = "fragment"

        private val FRAGMENTS = ConcurrentHashMap<Int, Obtain<Popup>>()

        fun createIntent(

            context: TransitionEffectsActivity,
            creator: Obtain<Popup>,
            wrapperClass: Class<*> = FragmentWrapperActivity::class.java

        ): Intent {

            val intent = Intent(context, wrapperClass)
            intent.putExtra(EXTRA_FRAGMENT, creator.hashCode())

            FRAGMENTS[creator.hashCode()] = creator

            return intent
        }
    }

    private var hash = -1
    private val tag = "Fragment Wrapper Activity ::"
    private var dialogFragment: PopupFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Console.log("$tag onCreate")

        window.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        setContentView(R.layout.activity_fragment_wrapper)

        hash = intent.getIntExtra(EXTRA_FRAGMENT, -1)

        if (hash > 0) {

            dialogFragment = FRAGMENTS[hash]?.obtain()

            if (dialogFragment == null) {

                Console.log("$tag No dialog found for hash $hash")

                finishFrom("dialogFragment.null")

                return
            }

            Console.log("$tag Dialog fragment found for hash $hash :: Dialog='$dialogFragment'")

            val container = findViewById<FrameLayout?>(R.id.container)

            container?.let {

                if (savedInstanceState == null) {

                    dialogFragment?.let { f ->

                        try {

                            supportFragmentManager.beginTransaction()
                                .replace(R.id.container, f)
                                .commit()

                        } catch (e: Throwable) {

                            recordException(e)
                        }
                    }
                }
            }

        } else {

            finishFrom("hash.notValid")
        }
    }

    override fun onBack() {
        super.onBack()

        Console.log("$tag On back")
    }

    override fun onDestroy() {

        FRAGMENTS.remove(hash)

        super.onDestroy()

        Console.log("$tag onDestroy")
    }

    protected fun getFragment(): PopupFragment? {

        return dialogFragment
    }
}