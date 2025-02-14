package no.nav.helse

import no.nav.helse.mediator.KommandokjedeEndretEvent
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.melding.SubsumsjonEvent
import no.nav.helse.modell.melding.UtgåendeHendelse
import java.util.UUID

interface MeldingPubliserer {
    fun publiser(
        fødselsnummer: String,
        hendelse: UtgåendeHendelse,
        årsak: String,
    )

    fun publiser(
        fødselsnummer: String,
        subsumsjonEvent: SubsumsjonEvent,
        versjonAvKode: String,
    )

    fun publiser(
        hendelseId: UUID,
        commandContextId: UUID,
        fødselsnummer: String,
        behov: List<Behov>,
    )

    fun publiser(
        event: KommandokjedeEndretEvent,
        hendelseNavn: String,
    )
}
