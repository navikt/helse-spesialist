package no.nav.helse.db

import no.nav.helse.modell.vedtak.BegrunnelseForSkjønnsfastsattSykepengegrunnlag
import no.nav.helse.modell.vedtak.SkjønnsfastsattSykepengegrunnlagDto
import no.nav.helse.spesialist.domain.Identitetsnummer

interface SykefraværstilfelleDao {
    fun finnSkjønnsfastsatteSykepengegrunnlag(fødselsnummer: String): List<SkjønnsfastsattSykepengegrunnlagDto>

    fun finnBegrunnelseForSkjønnsfastsattSykepengegrunnlag(identitetsnummer: Identitetsnummer): List<BegrunnelseForSkjønnsfastsattSykepengegrunnlag> =
        finnSkjønnsfastsatteSykepengegrunnlag(identitetsnummer.value).map {
            BegrunnelseForSkjønnsfastsattSykepengegrunnlag.gjenopprett(
                type = it.type,
                årsak = it.årsak,
                skjæringstidspunkt = it.skjæringstidspunkt,
                begrunnelseFraMal = it.begrunnelseFraMal,
                begrunnelseFraFritekst = it.begrunnelseFraFritekst,
                begrunnelseFraKonklusjon = it.begrunnelseFraKonklusjon,
                opprettet = it.opprettet,
            )
        }
}
