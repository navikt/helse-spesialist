package no.nav.helse.modell.overstyring

import kotliquery.Session
import no.nav.helse.mediator.kafka.meldinger.OverstyringMessage
import no.nav.helse.modell.command.Command
import no.nav.helse.modell.command.Løsninger
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

internal class OverstyringCommand(
    eventId: UUID,
    parent: Command?,
    private val rapidsConnection: RapidsConnection
) : Command(eventId, parent, Duration.ofDays(1)) {
    private var resultat: Resultat = Resultat.TrengerSaksbehandlerInput
    override fun execute(session: Session): Resultat {
        return resultat
    }

    override fun resume(session: Session, løsninger: Løsninger) {
        val overstyringMessage = løsninger.løsning<OverstyringMessage>()
        session.persisterOverstyring(
            hendelseId = eventId,
            fødselsnummer = overstyringMessage.fødselsnummer,
            organisasjonsnummer = overstyringMessage.organisasjonsnummer,
            begrunnelse = overstyringMessage.begrunnelse,
            unntaFraInnsyn = overstyringMessage.unntaFraInnsyn,
            overstyrteDager = overstyringMessage.dager
        )

        val overstyring = mapOf<String, Any>(
            "@id" to eventId,
            "@event_name" to "overstyr_tidslinje",
            "@opprettet" to LocalDateTime.now(),
            "aktørId" to overstyringMessage.aktørId,
            "fødselsnummer" to overstyringMessage.fødselsnummer,
            "organisasjonsnummer" to overstyringMessage.organisasjonsnummer,
            "dager" to overstyringMessage.dager
        )

        resultat =
            Resultat.Ok.Løst(overstyringMessage.saksbehandlerEpost, overstyringMessage.saksbehandlerOid, overstyring)

        log.info("Publiserer overstyring")
        rapidsConnection.publish(overstyringMessage.fødselsnummer, JsonMessage.newMessage(overstyring).toJson())
    }
}
