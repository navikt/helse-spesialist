package no.nav.helse.spesialist.api.rest.tilkomneinntekter

import no.nav.helse.spesialist.application.TotrinnsvurderingRepository
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Totrinnsvurdering

internal fun finnEllerOpprettTotrinnsvurdering(
    identitetsnummer: Identitetsnummer,
    totrinnsvurderingRepository: TotrinnsvurderingRepository,
): Totrinnsvurdering =
    totrinnsvurderingRepository.finnAktivForPerson(identitetsnummer.value)
        ?: Totrinnsvurdering.ny(fødselsnummer = identitetsnummer.value).also(totrinnsvurderingRepository::lagre)
