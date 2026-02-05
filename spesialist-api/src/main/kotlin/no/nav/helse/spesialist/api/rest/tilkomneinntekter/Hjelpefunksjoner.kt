package no.nav.helse.spesialist.api.rest.tilkomneinntekter

import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.spesialist.application.TotrinnsvurderingRepository
import no.nav.helse.spesialist.domain.Identitetsnummer

internal fun finnEllerOpprettTotrinnsvurdering(
    identitetsnummer: Identitetsnummer,
    totrinnsvurderingRepository: TotrinnsvurderingRepository,
): Totrinnsvurdering =
    totrinnsvurderingRepository.finnAktivForPerson(identitetsnummer.value)
        ?: Totrinnsvurdering.ny(fødselsnummer = identitetsnummer.value).also(totrinnsvurderingRepository::lagre)
