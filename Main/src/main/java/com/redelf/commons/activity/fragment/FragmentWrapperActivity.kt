/*
 * Copyright (c) 2025 MeTube Share
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


package com.redelf.commons.activity.fragment

import android.content.Intent
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

        window.setBackgroundDrawable(background.toDrawable())

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