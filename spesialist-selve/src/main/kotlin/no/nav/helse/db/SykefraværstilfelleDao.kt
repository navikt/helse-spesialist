package no.nav.helse.db

import no.nav.helse.modell.vedtak.SkjønnsfastsattSykepengegrunnlagDto

interface SykefraværstilfelleDao {
    fun finnSkjønnsfastsatteSykepengegrunnlag(fødselsnummer: String): List<SkjønnsfastsattSykepengegrunnlagDto>
}
