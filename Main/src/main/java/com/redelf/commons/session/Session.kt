package com.redelf.commons.session

import com.redelf.commons.destruction.reset.Resettable
import com.redelf.commons.execution.ExecuteWithResult
import com.redelf.commons.logging.Console
import java.util.UUID

class Session(

    private var identifier: UUID = UUID.randomUUID(),
    private var name: String = identifier.toString()

) :

    Resettable, ExecuteWithResult<SessionOperation>

{

    init {

        Console.debug("Created :: Session: $identifier @ $name")
    }

    fun takeName() = name

    fun takeIdentifier() = identifier

    override fun execute(what: SessionOperation): Boolean {

        val transactionId = identifier

        Console.log("$name :: Execute :: START :: Session: $transactionId")

        val started = what.start()

        if (started) {

            Console.log("$name :: Execute :: STARTED :: Session: $transactionId")

            val success = what.perform()

            if (success) {

                Console.log(

                    "$name :: Execute :: PERFORMED :: Session: $transactionId :: Success"
                )

            } else {

                Console.error(

                    "$name :: Execute :: PERFORMED :: Session: $transactionId :: Failure"
                )
            }

            if (success && transactionId == identifier) {

                Console.log("$name :: Execute :: ENDING :: Session: $transactionId")

                val ended = what.end()

                if (ended) {

                    Console.log(

                        "$name :: Execute :: ENDED :: Session: $transactionId :: Success"
                    )

                } else {

                    Console.log(

                        "$name :: Execute :: ENDED :: Session: $transactionId :: Failure"
                    )
                }

                return ended

            } else {

                if (transactionId != identifier) {

                    Console.warning("$name :: Execute :: ENDED :: Session: Skipped")
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

        Console.debug("$oldName :: Reset :: Session: $oldId -> $identifier")

        return oldId != identifier
    }
}