package com.redelf.commons.session

import com.redelf.commons.execution.ExecuteWithResult
import com.redelf.commons.reset.Resettable
import com.redelf.commons.logging.Timber
import java.util.UUID

class Session(

    private var identifier: UUID = UUID.randomUUID(),
    private var name: String = identifier.toString()

) :

    Resettable, ExecuteWithResult<SessionOperation>

{

    init {

        Timber.d("Created :: Session: $identifier @ $name")
    }

    fun takeName() = name

    fun takeIdentifier() = identifier

    override fun execute(what: SessionOperation): Boolean {

        val transactionId = identifier

        Timber.v("$name :: Execute :: START :: Session: $transactionId")

        val started = what.start()

        if (started) {

            Timber.v("$name :: Execute :: STARTED :: Session: $transactionId")

            val success = what.perform()

            if (success) {

                Timber.v(

                    "$name :: Execute :: PERFORMED :: Session: $transactionId :: Success"
                )

            } else {

                Timber.e(

                    "$name :: Execute :: PERFORMED :: Session: $transactionId :: Failure"
                )
            }

            if (success && transactionId == identifier) {

                Timber.v("$name :: Execute :: ENDING :: Session: $transactionId")

                val ended = what.end()

                if (ended) {

                    Timber.v(

                        "$name :: Execute :: ENDED :: Session: $transactionId :: Success"
                    )

                } else {

                    Timber.v(

                        "$name :: Execute :: ENDED :: Session: $transactionId :: Failure"
                    )
                }

                return ended

            } else {

                if (transactionId != identifier) {

                    Timber.w("$name :: Execute :: ENDED :: Session: Skipped")
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

        Timber.d("$oldName :: Reset :: Session: $oldId -> $identifier")

        return oldId != identifier
    }
}