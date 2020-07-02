package no.nav.helse.modell.command.ny

import kotliquery.Session
import no.nav.helse.api.Rollback
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import java.time.LocalDateTime
import java.util.*

class RollbackPersonCommand(
    private val rapidsConnection: RapidsConnection,
    private val rollback: Rollback
) : NyCommand {
    override val type = "RollbackPersonCommand"

    override fun execute(session: Session): NyCommand.Resultat {
        session.close()
        rapidsConnection.publish(
            rollback.fødselsnummer, JsonMessage.newMessage(
                mutableMapOf(
                    "@id" to UUID.randomUUID(),
                    "@event_name" to "rollback_person",
                    "@opprettet" to LocalDateTime.now(),
                    "aktørId" to rollback.aktørId,
                    "fødselsnummer" to rollback.fødselsnummer,
                    "personVersjon" to rollback.personVersjon
                )
            ).toJson()
        )
        return NyCommand.Resultat.Ok
    }
}
