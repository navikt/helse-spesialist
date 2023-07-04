package no.nav.helse.modell.vedtaksperiode.vedtak

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal sealed class Sykepengevedtak(
    private val fødselsnummer: String,
    private val aktørId: String,
    private val vedtaksperiodeId: UUID,
    private val organisasjonsnummer: String,
    private val fom: LocalDate,
    private val tom: LocalDate,
    private val skjæringstidspunkt: LocalDate,
    private val hendelser: List<UUID>,
    private val sykepengegrunnlag: Double,
    private val grunnlagForSykepengegrunnlag: Double,
    private val grunnlagForSykepengegrunnlagPerArbeidsgiver: Map<String, Double>,
    private val begrensning: String,
    private val inntekt: Double,
    private val vedtakFattetTidspunkt: LocalDateTime,
) {
    private companion object {
        private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")
    }

    internal class AuuVedtak(
        fødselsnummer: String,
        aktørId: String,
        vedtaksperiodeId: UUID,
        organisasjonsnummer: String,
        fom: LocalDate,
        tom: LocalDate,
        skjæringstidspunkt: LocalDate,
        hendelser: List<UUID>,
        sykepengegrunnlag: Double,
        grunnlagForSykepengegrunnlag: Double,
        grunnlagForSykepengegrunnlagPerArbeidsgiver: Map<String, Double>,
        begrensning: String,
        inntekt: Double,
        vedtakFattetTidspunkt: LocalDateTime,
    ) : Sykepengevedtak(
        fødselsnummer,
        aktørId,
        vedtaksperiodeId,
        organisasjonsnummer,
        fom,
        tom,
        skjæringstidspunkt,
        hendelser,
        sykepengegrunnlag,
        grunnlagForSykepengegrunnlag,
        grunnlagForSykepengegrunnlagPerArbeidsgiver,
        begrensning,
        inntekt,
        vedtakFattetTidspunkt
    )

    internal class Vedtak(
        fødselsnummer: String,
        aktørId: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        fom: LocalDate,
        tom: LocalDate,
        skjæringstidspunkt: LocalDate,
        hendelser: List<UUID>,
        sykepengegrunnlag: Double,
        grunnlagForSykepengegrunnlag: Double,
        grunnlagForSykepengegrunnlagPerArbeidsgiver: Map<String, Double>,
        begrensning: String,
        inntekt: Double,
        sykepengegrunnlagsfakta: Sykepengegrunnlagsfakta,
        vedtakFattetTidspunkt: LocalDateTime
    ): Sykepengevedtak(
        fødselsnummer,
        aktørId,
        vedtaksperiodeId,
        organisasjonsnummer,
        fom,
        tom,
        skjæringstidspunkt,
        hendelser,
        sykepengegrunnlag,
        grunnlagForSykepengegrunnlag,
        grunnlagForSykepengegrunnlagPerArbeidsgiver,
        begrensning,
        inntekt,
        vedtakFattetTidspunkt,
    )
}

