package no.nav.helse.spesialist.api.rest.andreYtelser

import no.nav.helse.modell.melding.GraderteAndreYtelserEndringerEvent
import no.nav.helse.spesialist.api.rest.KallKontekst
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelser

internal fun KallKontekst.leggTilGraderteAndreYtelserEndringshendelse(
    graderteAndreYtelser: GraderteAndreYtelser,
    årsak: String,
) {
    outbox.leggTil(
        identitetsnummer = graderteAndreYtelser.identitetsnummer,
        hendelse =
            GraderteAndreYtelserEndringerEvent(
                fødselsnummer = graderteAndreYtelser.identitetsnummer,
                fom = graderteAndreYtelser.tidligsteFom(),
            ),
        årsak = årsak,
    )
}

internal fun GraderteAndreYtelser.tidligsteFom() = perioder.minByOrNull { it.periode.fom }!!.periode.fom
