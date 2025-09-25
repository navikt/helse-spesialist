package no.nav.helse.spesialist.api.rest.tilkommeninntekt

import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.spesialist.application.TotrinnsvurderingRepository

internal fun finnEllerOpprettTotrinnsvurdering(
    fodselsnummer: String,
    totrinnsvurderingRepository: TotrinnsvurderingRepository,
): Totrinnsvurdering =
    totrinnsvurderingRepository.finnAktivForPerson(fodselsnummer)
        ?: Totrinnsvurdering.ny(fødselsnummer = fodselsnummer).also(totrinnsvurderingRepository::lagre)
