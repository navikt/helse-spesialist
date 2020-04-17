package no.nav.helse.modell.oppgave

import no.nav.helse.Løsningstype
import no.nav.helse.modell.løsning.SaksbehandlerLøsning
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

internal class SaksbehandlerGodkjenningCommand(
    behovId: UUID,
    parent: Command,
    ferdigstilt: LocalDateTime? = null
) : Command(
    behovId = behovId,
    ferdigstilt = ferdigstilt,
    løsningstype = Løsningstype.Saksbehandler,
    parent = parent,
    timeout = Duration.ofDays(14)
) {
    override fun execute() {}

    override fun fortsett(saksbehandlerLøsning: SaksbehandlerLøsning) {
        ferdigstilt = LocalDateTime.now()
    }
}
