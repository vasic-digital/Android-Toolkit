package com.redelf.commons.activity.fragment

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.redelf.commons.R
import com.redelf.commons.activity.base.BaseActivity
import com.redelf.commons.activity.stateful.StatefulActivity
import androidx.core.graphics.drawable.toDrawable
import com.redelf.commons.activity.base.BaseDialogFragment
import com.redelf.commons.activity.transition.TransitionEffectsActivity
import com.redelf.commons.logging.Console
import java.util.concurrent.ConcurrentHashMap


open class FragmentWrapperActivity : BaseActivity() {

    companion object {

        const val EXTRA_FRAGMENT = "fragment"

        private val FRAGMENTS = ConcurrentHashMap<Int, BaseDialogFragment>()

        fun createIntent(

            context: TransitionEffectsActivity,
            dialogFragment: BaseDialogFragment

        ): Intent {

            val intent = Intent(context, FragmentWrapperActivity::class.java)
            intent.putExtra(EXTRA_FRAGMENT, dialogFragment.hashCode())

            FRAGMENTS[dialogFragment.hashCode()] = dialogFragment

            return intent
        }
    }

    private var hash = -1
    private val tag = "Fragment Wrapper Activity ::"
    private var dialogFragment: BaseDialogFragment? = null

    private val onDismiss = DialogInterface.OnDismissListener {

        Console.log("$tag onDismiss")

        FRAGMENTS.remove(hash)

        finishFrom("onDismiss")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Console.log("$tag onCreate")

        window.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        setContentView(R.layout.activity_fragment_wrapper)

        hash = intent.getIntExtra(EXTRA_FRAGMENT, -1)

        if (hash > 0) {

            // TODO: Support this
            //            df.setStyle(
            //                intent.getIntExtra(EXTRA_STYLE, DialogFragment.STYLE_NORMAL),
            //                intent.getIntExtra(EXTRA_THEME, 0)
            //            )

            dialogFragment = FRAGMENTS[hash]

            if (dialogFragment == null) {

                Console.log("$tag No dialog found for hash $hash")

                finishFrom("dialogFragment.null")

                return
            }

            Console.log("$tag Dialog fragment found for hash $hash :: Dialog='$dialogFragment'")

            dialogFragment?.register(onDismiss)

            dialogFragment?.show(supportFragmentManager, "dialog_host")

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
}