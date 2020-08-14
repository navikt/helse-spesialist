package no.nav.helse.modell.overstyring

import kotliquery.Session
import no.nav.helse.mediator.kafka.meldinger.OverstyringMessage
import no.nav.helse.modell.command.Command
import no.nav.helse.modell.command.Løsninger
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

internal class OverstyringCommand(
    eventId: UUID,
    parent: Command?
) : Command(eventId, parent, Duration.ofDays(1)) {
    private var resultat: Resultat = Resultat.TrengerSaksbehandlerInput
    override fun execute(session: Session): Resultat {
        return resultat
    }

    override fun resume(session: Session, løsninger: Løsninger) {
        val overstyringMessage = løsninger.løsning<OverstyringMessage>()
        session.persisterOverstyring(
            fødselsnummer = overstyringMessage.fødselsnummer,
            organisasjonsnummer = overstyringMessage.organisasjonsnummer,
            begrunnelse = overstyringMessage.begrunnelse,
            unntaFraInnsyn = overstyringMessage.unntaFraInnsyn,
            overstyrteDager = overstyringMessage.dager
        )

        resultat = Resultat.Ok.Løst(
            overstyringMessage.saksbehandlerEpost, overstyringMessage.saksbehandlerOid, mapOf<String, Any?>(
                "@id" to UUID.randomUUID(),
                "@event_name" to "overstyr_dager",
                "@opprettet" to LocalDateTime.now(),
                "aktørId" to overstyringMessage.aktørId,
                "fødselsnummer" to overstyringMessage.fødselsnummer,
                "organisasjonsnummer" to overstyringMessage.organisasjonsnummer,
                "dager" to overstyringMessage.dager
            )
        )

    }
}
