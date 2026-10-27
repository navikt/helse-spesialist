package no.nav.helse.spesialist.api.rest.andreYtelser

import no.nav.helse.modell.melding.GraderteAndreYtelserEndringerEvent
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserEndretEvent

internal fun KallKontekst.leggTilGraderteAndreYtelserEndringsevent(
    identitetsnummer: Identitetsnummer,
    event: GraderteAndreYtelserEndretEvent,
    årsak: String,
) {
    outbox.leggTil(
        identitetsnummer = identitetsnummer,
        hendelse =
            GraderteAndreYtelserEndringerEvent(
                fødselsnummer = identitetsnummer,
                fom = event.dager.minOf { it.dato },
            ),
        årsak = årsak,
    )
}
