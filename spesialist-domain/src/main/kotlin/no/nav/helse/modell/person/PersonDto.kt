package no.nav.helse.modell.person

import no.nav.helse.modell.person.vedtaksperiode.VedtaksperiodeDto
import no.nav.helse.modell.vedtak.SkjønnsfastsattSykepengegrunnlagDto

data class PersonDto(
    val aktørId: String,
    val fødselsnummer: String,
    val vedtaksperioder: List<VedtaksperiodeDto>,
    val skjønnsfastsatteSykepengegrunnlag: List<SkjønnsfastsattSykepengegrunnlagDto>,
)
