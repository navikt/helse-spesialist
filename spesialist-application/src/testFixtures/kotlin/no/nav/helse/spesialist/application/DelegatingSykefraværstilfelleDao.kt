package no.nav.helse.spesialist.application

import no.nav.helse.db.SykefraværstilfelleDao
import no.nav.helse.modell.vedtak.SkjønnsfastsattSykepengegrunnlagDto
import no.nav.helse.modell.vedtak.SkjønnsfastsettingsårsakDto
import no.nav.helse.spesialist.domain.overstyringer.SkjønnsfastsattSykepengegrunnlag

class DelegatingSykefraværstilfelleDao(
    private val overstyringRepository: InMemoryOverstyringRepository,
) : SykefraværstilfelleDao {
    override fun finnSkjønnsfastsatteSykepengegrunnlag(fødselsnummer: String): List<SkjønnsfastsattSykepengegrunnlagDto> =
        overstyringRepository.data.values
            .flatten()
            .filter { it.fødselsnummer == fødselsnummer }
            .filterIsInstance<SkjønnsfastsattSykepengegrunnlag>()
            .map { skjønnsfastsatt ->
                SkjønnsfastsattSykepengegrunnlagDto(
                    type = enumValueOf(skjønnsfastsatt.type.name),
                    årsak = skjønnsfastsatt.lovhjemmel.ledd!!.tilÅrsakDto(),
                    skjæringstidspunkt = skjønnsfastsatt.skjæringstidspunkt,
                    begrunnelseFraMal = skjønnsfastsatt.begrunnelseMal!!,
                    begrunnelseFraFritekst = skjønnsfastsatt.begrunnelseFritekst!!,
                    begrunnelseFraKonklusjon = skjønnsfastsatt.begrunnelseKonklusjon!!,
                    opprettet = skjønnsfastsatt.opprettet,
                )
            }

    private fun String.tilÅrsakDto(): SkjønnsfastsettingsårsakDto =
        when (this) {
            "2" -> SkjønnsfastsettingsårsakDto.ANDRE_AVSNITT
            "3" -> SkjønnsfastsettingsårsakDto.TREDJE_AVSNITT
            else -> SkjønnsfastsettingsårsakDto.ANDRE_AVSNITT
        }
}
