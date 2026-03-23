package no.nav.helse.modell.person

import no.nav.helse.modell.person.vedtaksperiode.VedtaksperiodeDto
import no.nav.helse.modell.vedtak.SkjønnsfastsattSykepengegrunnlagDto
import no.nav.helse.modell.vilkårsprøving.Avviksvurdering

data class PersonDto(
    val aktørId: String,
    val fødselsnummer: String,
    val vedtaksperioder: List<VedtaksperiodeDto>,
    val avviksvurderinger: List<Avviksvurdering>,
    val skjønnsfastsatteSykepengegrunnlag: List<SkjønnsfastsattSykepengegrunnlagDto>,
)
