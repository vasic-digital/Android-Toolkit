package com.redelf.commons.session

import com.redelf.commons.execution.ExecuteWithResult
import com.redelf.commons.reset.Resettable
import timber.log.Timber
import java.util.UUID

class Session(

    private var identifier: UUID = UUID.randomUUID(),
    private var name: String = identifier.toString()

) :

    Resettable, ExecuteWithResult<SessionOperation>

{

    init {

        Timber.v("$name :: Created")
    }

    fun takeName() = name

    fun takeIdentifier() = identifier

    override fun execute(what: SessionOperation): Boolean {

        val transactionId = identifier

        Timber.v("$name :: Execute :: START: $transactionId")

        val started = what.start()

        if (started) {

            Timber.v("$name :: Execute :: STARTED: $transactionId")

            val success = what.perform()

            if (success) {

                Timber.v("$name :: Execute :: PERFORMED :: $transactionId :: Success")

            } else {

                Timber.e("$name :: Execute :: PERFORMED :: $transactionId :: Failure")
            }

            if (success && transactionId == identifier) {

                Timber.v("$name :: Execute :: ENDING :: $transactionId")

                val ended = what.end()

                if (ended) {

                    Timber.v("$name :: Execute :: ENDED :: $transactionId :: Success")

                } else {

                    Timber.v("$name :: Execute :: ENDED :: $transactionId :: Failure")
                }

                return ended

            } else {

                if (transactionId != identifier) {

                    Timber.w("$name :: Execute :: ENDED :: Skipped")
                }
            }
        }

        return false
    }

    override fun reset(): Boolean {

        val oldId = identifier
        val oldName = name

        identifier = UUID.randomUUID()

        if (name == oldId.toString()) {

            name = identifier.toString()
        }

        Timber.v("$oldName :: Reset: $name")

        return oldId != identifier
    }
}