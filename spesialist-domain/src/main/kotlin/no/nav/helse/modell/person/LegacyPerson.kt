package no.nav.helse.modell.person

import no.nav.helse.modell.person.vedtaksperiode.LegacyVedtaksperiode
import no.nav.helse.modell.person.vedtaksperiode.VedtaksperiodeDto
import no.nav.helse.modell.vedtak.SkjønnsfastsattSykepengegrunnlag
import no.nav.helse.modell.vedtak.SkjønnsfastsattSykepengegrunnlagDto
import org.slf4j.LoggerFactory
import java.util.UUID

class LegacyPerson(
    val aktørId: String,
    val fødselsnummer: String,
    vedtaksperioder: List<LegacyVedtaksperiode>,
    val skjønnsfastsatteSykepengegrunnlag: List<SkjønnsfastsattSykepengegrunnlag>,
) {
    private val vedtaksperioder = vedtaksperioder.toMutableList()

    fun vedtaksperioder(): List<LegacyVedtaksperiode> = vedtaksperioder

    fun toDto() =
        PersonDto(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            vedtaksperioder = vedtaksperioder.map { it.toDto() },
            skjønnsfastsatteSykepengegrunnlag = skjønnsfastsatteSykepengegrunnlag.map { it.toDto() },
        )

    fun vedtaksperiodeOrNull(vedtaksperiodeId: UUID): LegacyVedtaksperiode? {
        return vedtaksperioder.find { it.vedtaksperiodeId() == vedtaksperiodeId }
            ?: logg.warn("Vedtaksperiode med id={} finnes ikke", vedtaksperiodeId).let { return null }
    }

    fun utbetalingForkastet(utbetalingId: UUID) {
        vedtaksperioder.forEach {
            it.utbetalingForkastet(utbetalingId)
        }
    }

    fun nyUtbetalingForVedtaksperiode(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ) {
        vedtaksperiodeOrNull(vedtaksperiodeId)
            ?.nyUtbetaling(utbetalingId)
    }

    companion object {
        private val logg = LoggerFactory.getLogger(this::class.java)

        fun gjenopprett(
            aktørId: String,
            fødselsnummer: String,
            vedtaksperioder: List<VedtaksperiodeDto>,
            skjønnsfastsattSykepengegrunnlag: List<SkjønnsfastsattSykepengegrunnlagDto>,
        ): LegacyPerson =
            LegacyPerson(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                vedtaksperioder =
                    vedtaksperioder.map {
                        LegacyVedtaksperiode.gjenopprett(
                            organisasjonsnummer = it.organisasjonsnummer,
                            vedtaksperiodeId = it.vedtaksperiodeId,
                            forkastet = it.forkastet,
                            behandlinger = it.behandlinger,
                        )
                    },
                skjønnsfastsatteSykepengegrunnlag =
                    skjønnsfastsattSykepengegrunnlag
                        .sortedBy { it.opprettet }
                        .map {
                            SkjønnsfastsattSykepengegrunnlag.gjenopprett(
                                type = it.type,
                                årsak = it.årsak,
                                skjæringstidspunkt = it.skjæringstidspunkt,
                                begrunnelseFraMal = it.begrunnelseFraMal,
                                begrunnelseFraFritekst = it.begrunnelseFraFritekst,
                                begrunnelseFraKonklusjon = it.begrunnelseFraKonklusjon,
                                opprettet = it.opprettet,
                            )
                        },
            )
    }
}
