package no.nav.helse.mediator.meldinger.hendelser

import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.varsel.Varseldefinisjon
import java.time.LocalDateTime
import java.util.UUID

internal class VarseldefinisjonMessage(
    private val id: UUID,
    private val varselkode: String,
    private val tittel: String,
    private val forklaring: String?,
    private val handling: String?,
    private val avviklet: Boolean,
    private val opprettet: LocalDateTime,
) {
    private val varseldefinisjon get() = Varseldefinisjon(id, varselkode, tittel, forklaring, handling, avviklet, opprettet)

    internal fun sendInnTil(mediator: MeldingMediator) {
        mediator.håndter(varseldefinisjon)
    }
}
