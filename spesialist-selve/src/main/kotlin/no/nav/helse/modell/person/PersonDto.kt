package no.nav.helse.modell.person

import no.nav.helse.modell.vedtak.SkjønnsfastsattSykepengegrunnlagDto
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeDto
import no.nav.helse.modell.vilkårsprøving.AvviksvurderingDto

data class PersonDto(
    val aktørId: String,
    val fødselsnummer: String,
    val vedtaksperioder: List<VedtaksperiodeDto>,
    val avviksvurderinger: List<AvviksvurderingDto>,
    val skjønnsfastsatteSykepengegrunnlag: List<SkjønnsfastsattSykepengegrunnlagDto>,
)
