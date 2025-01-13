package no.nav.helse

import no.nav.helse.mediator.KommandokjedeEndretEvent
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.melding.UtgåendeHendelse
import java.util.UUID

interface MeldingPubliserer {
    fun publiser(
        fødselsnummer: String,
        hendelse: UtgåendeHendelse,
        hendelseNavn: String,
    )

    fun publiser(
        hendelseId: UUID,
        commandContextId: UUID,
        fødselsnummer: String,
        behov: Map<String, Behov>,
    )

    fun publiser(
        event: KommandokjedeEndretEvent,
        hendelseNavn: String,
    )
}
