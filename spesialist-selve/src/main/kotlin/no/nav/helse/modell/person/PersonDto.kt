package no.nav.helse.modell.person

import no.nav.helse.modell.sykefraværstilfelle.SkjønnsfastsattSykepengegrunnlagDto
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeDto

data class PersonDto(
    val aktørId: String,
    val fødselsnummer: String,
    val vedtaksperioder: List<VedtaksperiodeDto>,
    val skjønnsfastsatteSykepengegrunnlag: List<SkjønnsfastsattSykepengegrunnlagDto>,
)
