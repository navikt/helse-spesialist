package no.nav.helse.modell.oppgave

import no.nav.helse.Løsningstype
import no.nav.helse.modell.løsning.SaksbehandlerLøsning
import java.time.LocalDateTime
import java.util.UUID

internal class SaksbehandlerGodkjenningCommand(
    behovId: UUID,
    parent: Command,
    ferdigstilt: LocalDateTime? = null
) : Command(behovId, ferdigstilt, Løsningstype.Saksbehandler, parent) {
    override fun execute() {}

    override fun fortsett(saksbehandlerLøsning: SaksbehandlerLøsning) {
        ferdigstilt = LocalDateTime.now()
    }
}
