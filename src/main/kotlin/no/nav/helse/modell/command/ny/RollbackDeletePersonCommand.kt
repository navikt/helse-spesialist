package no.nav.helse.modell.command.ny

import kotliquery.Session
import no.nav.helse.api.RollbackDelete
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import java.time.LocalDateTime
import java.util.*

class RollbackDeletePersonCommand(
    private val rapidsConnection: RapidsConnection,
    private val rollback: RollbackDelete
) : NyCommand {
    override val type = "RollbackDeletePersonCommand"

    override fun execute(session: Session): NyCommand.Resultat {
        rapidsConnection.publish(
            JsonMessage.newMessage(
                mutableMapOf(
                    "@id" to UUID.randomUUID(),
                    "@event_name" to "rollback_person_delete",
                    "@opprettet" to LocalDateTime.now(),
                    "aktørId" to rollback.aktørId,
                    "fødselsnummer" to rollback.fødselsnummer
                )
            ).toJson()
        )
        return NyCommand.Resultat.Ok
    }
}
