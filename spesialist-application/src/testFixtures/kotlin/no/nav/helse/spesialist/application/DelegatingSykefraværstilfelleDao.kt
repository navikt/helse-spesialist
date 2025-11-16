package no.nav.helse.spesialist.application

import no.nav.helse.db.SykefraværstilfelleDao
import no.nav.helse.modell.saksbehandler.handlinger.SkjønnsfastsattSykepengegrunnlag
import no.nav.helse.modell.vedtak.SkjønnsfastsattSykepengegrunnlagDto
import no.nav.helse.modell.vedtak.SkjønnsfastsettingsårsakDto

class DelegatingSykefraværstilfelleDao(
    private val overstyringRepository: InMemoryOverstyringRepository,
) : SykefraværstilfelleDao {
    override fun finnSkjønnsfastsatteSykepengegrunnlag(fødselsnummer: String): List<SkjønnsfastsattSykepengegrunnlagDto> =
        overstyringRepository.data.values.flatten()
            .filter { it.fødselsnummer == fødselsnummer }
            .filterIsInstance<SkjønnsfastsattSykepengegrunnlag>()
            .map { skjønnsfastsatt ->
                val førsteArbeidsgiver = skjønnsfastsatt.arbeidsgivere.first()
                SkjønnsfastsattSykepengegrunnlagDto(
                    type = enumValueOf(førsteArbeidsgiver.type.name),
                    årsak = førsteArbeidsgiver.lovhjemmel!!.ledd!!.tilÅrsakDto(),
                    skjæringstidspunkt = skjønnsfastsatt.skjæringstidspunkt,
                    begrunnelseFraMal = førsteArbeidsgiver.begrunnelseMal!!,
                    begrunnelseFraFritekst = førsteArbeidsgiver.begrunnelseFritekst!!,
                    begrunnelseFraKonklusjon = førsteArbeidsgiver.begrunnelseKonklusjon!!,
                    opprettet = skjønnsfastsatt.opprettet
                )
            }

    private fun String.tilÅrsakDto(): SkjønnsfastsettingsårsakDto =
        when (this) {
            "2" -> SkjønnsfastsettingsårsakDto.ANDRE_AVSNITT
            "3" -> SkjønnsfastsettingsårsakDto.TREDJE_AVSNITT
            else -> SkjønnsfastsettingsårsakDto.ANDRE_AVSNITT
        }
}
