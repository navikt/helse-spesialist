package no.nav.helse.modell.command.ny

import kotliquery.Session
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.kafka.meldinger.AnnulleringMessage
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

class AnnulleringCommand(
    private val rapidsConnection: RapidsConnection,
    private val annullering: AnnulleringMessage
) : NyCommand {
    private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

    override val type = "AnnulleringCommand"

    override fun execute(session: Session): NyCommand.Resultat {
        rapidsConnection.publish(annullering.fødselsnummer, annullering.toJson().also {
            sikkerLogg.info(
                "sender annullering for {}\n\t$it",
                keyValue("fagsystemId", annullering.fagsystemId)
            )
        })
        return NyCommand.Resultat.Ok
    }

    private fun AnnulleringMessage.toJson() = JsonMessage.newMessage(
        mapOf(
            "@event_name" to "kanseller_utbetaling",
            "@id" to UUID.randomUUID(),
            "@opprettet" to LocalDateTime.now(),
            "fødselsnummer" to fødselsnummer,
            "organisasjonsnummer" to organisasjonsnummer,
            "aktørId" to aktørId,
            "fagsystemId" to fagsystemId,
            "saksbehandler" to saksbehandler
        )
    ).toJson()
}
