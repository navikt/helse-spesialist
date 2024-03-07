package no.nav.helse.modell.person

import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeDto

data class PersonDto(
    val fødselsnummer: String,
    val vedtaksperioder: List<VedtaksperiodeDto>
)