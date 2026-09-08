package no.nav.helse.spesialist.application

import no.nav.helse.db.BegrunnelseForSkjønnsfastsettelseRepository
import no.nav.helse.modell.vedtak.BegrunnelseForSkjønnsfastsattSykepengegrunnlag
import no.nav.helse.modell.vedtak.Skjønnsfastsettingstype
import no.nav.helse.modell.vedtak.Skjønnsfastsettingsårsak
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.overstyringer.SkjønnsfastsattArbeidsgiver
import no.nav.helse.spesialist.domain.overstyringer.SkjønnsfastsattSykepengegrunnlag

class DelegatingBegrunnelseForSkjønnsfastsettelseRepository(
    private val overstyringRepository: InMemoryOverstyringRepository,
) : BegrunnelseForSkjønnsfastsettelseRepository {
    override fun finnBegrunnelseForSkjønnsfastsattSykepengegrunnlag(identitetsnummer: Identitetsnummer): List<BegrunnelseForSkjønnsfastsattSykepengegrunnlag> =
        overstyringRepository.data.values
            .flatten()
            .filter { it.fødselsnummer == identitetsnummer.value }
            .filterIsInstance<SkjønnsfastsattSykepengegrunnlag>()
            .map { skjønnsfastsatt ->
                BegrunnelseForSkjønnsfastsattSykepengegrunnlag(
                    type =
                        when (skjønnsfastsatt.type) {
                            SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype.OMREGNET_ÅRSINNTEKT -> Skjønnsfastsettingstype.OMREGNET_ÅRSINNTEKT
                            SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype.RAPPORTERT_ÅRSINNTEKT -> Skjønnsfastsettingstype.RAPPORTERT_ÅRSINNTEKT
                            SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype.ANNET -> Skjønnsfastsettingstype.ANNET
                        },
                    årsak = skjønnsfastsatt.lovhjemmel.ledd!!.tilÅrsak(),
                    skjæringstidspunkt = skjønnsfastsatt.skjæringstidspunkt,
                    begrunnelseFraMal = skjønnsfastsatt.begrunnelseMal!!,
                    begrunnelseFraFritekst = skjønnsfastsatt.begrunnelseFritekst!!,
                    begrunnelseFraKonklusjon = skjønnsfastsatt.begrunnelseKonklusjon!!,
                    opprettet = skjønnsfastsatt.opprettet,
                )
            }

    private fun String.tilÅrsak(): Skjønnsfastsettingsårsak =
        when (this) {
            "2" -> Skjønnsfastsettingsårsak.ANDRE_AVSNITT
            "3" -> Skjønnsfastsettingsårsak.TREDJE_AVSNITT
            else -> Skjønnsfastsettingsårsak.ANDRE_AVSNITT
        }
}
