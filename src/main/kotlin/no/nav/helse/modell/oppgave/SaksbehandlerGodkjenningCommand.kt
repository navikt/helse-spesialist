package no.nav.helse.modell.oppgave

import no.nav.helse.Oppgavestatus
import no.nav.helse.modell.løsning.SaksbehandlerLøsning
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

internal class SaksbehandlerGodkjenningCommand(
    behovId: UUID,
    parent: Command
) : Command(
    behovId = behovId,
    initiellStatus = Oppgavestatus.AvventerSaksbehandler,
    parent = parent,
    timeout = Duration.ofDays(14)
) {
    override fun execute() {}

    override fun fortsett(saksbehandlerLøsning: SaksbehandlerLøsning) {
        ferdigstill(saksbehandlerLøsning.saksbehandlerIdent)
    }
}
