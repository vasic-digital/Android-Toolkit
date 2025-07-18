package com.redelf.commons.activity.fragment

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


open class FragmentWrapperActivity : BaseActivity() {

    companion object {

        const val EXTRA_FRAGMENT = "extra.fragment"

        fun createIntent(

            context: StatefulActivity,
            dialogFragment: DialogFragment

        ): Intent {

            return Intent(context, FragmentWrapperActivity::class.java).apply {

                extras?.let {

                    context.supportFragmentManager.putFragment(

                        it, EXTRA_FRAGMENT, dialogFragment
                    )
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        setContentView(R.layout.activity_fragment_wrapper)

        val dialogFragment = supportFragmentManager.getFragment(

            intent.extras ?: Bundle(),
            EXTRA_FRAGMENT

        ) as? DialogFragment

        // TODO: Support this
        //            df.setStyle(
        //                intent.getIntExtra(EXTRA_STYLE, DialogFragment.STYLE_NORMAL),
        //                intent.getIntExtra(EXTRA_THEME, 0)
        //            )

        dialogFragment?.show(supportFragmentManager, "dialog_host") ?: run {

            finishFrom("fragment.none")
        }
    }
}