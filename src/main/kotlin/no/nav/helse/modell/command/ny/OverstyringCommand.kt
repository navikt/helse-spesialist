package no.nav.helse.modell.command.ny

import kotliquery.Session
import no.nav.helse.mediator.kafka.meldinger.OverstyringMessage
import no.nav.helse.modell.overstyring.persisterOverstyring
import no.nav.helse.objectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.asLocalDate
import java.time.LocalDateTime
import java.util.*

class OverstyringCommand(
    private val rapidsConnection: RapidsConnection,
    private val overstyringMessage: OverstyringMessage
) : NyCommand {
    override val type = "OverstyringCommand"

    override fun execute(session: Session): NyCommand.Resultat {
        session.persisterOverstyring(
            fødselsnummer = overstyringMessage.fødselsnummer,
            organisasjonsnummer = overstyringMessage.organisasjonsnummer,
            begrunnelse = overstyringMessage.begrunnelse,
            unntaFraInnsyn = overstyringMessage.unntaFraInnsyn,
            overstyrteDager = objectMapper.readTree(overstyringMessage.dager).map { it["dato"].asLocalDate() }
        )

        rapidsConnection.publish(
            overstyringMessage.fødselsnummer, JsonMessage.newMessage(
                mapOf(
                    "@id" to UUID.randomUUID(),
                    "@event_name" to "overstyr_dager",
                    "@opprettet" to LocalDateTime.now(),
                    "aktørId" to overstyringMessage.aktørId,
                    "fødselsnummer" to overstyringMessage.fødselsnummer,
                    "organisasjonsnummer" to overstyringMessage.organisasjonsnummer,
                    "dager" to overstyringMessage.dager
                )
            ).toJson()
        )

        return NyCommand.Resultat.Ok
    }
}
