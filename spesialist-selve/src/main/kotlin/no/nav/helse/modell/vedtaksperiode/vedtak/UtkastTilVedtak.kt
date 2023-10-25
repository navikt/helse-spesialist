package no.nav.helse.modell.vedtaksperiode.vedtak

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class UtkastTilVedtak(
    private val fødselsnummer: String,
    private val aktørId: String,
    private val organisasjonsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val utbetalingId: UUID?,
    private val skjæringstidspunkt: LocalDate,
    private val hendelser: List<UUID>,
    private val sykepengegrunnlag: Double,
    private val grunnlagForSykepengegrunnlag: Double,
    private val grunnlagForSykepengegrunnlagPerArbeidsgiver: Map<String, Double>,
    private val begrensning: String,
    private val inntekt: Double,
    private val sykepengegrunnlagsfakta: Sykepengegrunnlagsfakta?,
    private val fom: LocalDate,
    private val tom: LocalDate,
    private val vedtakFattetTidspunkt: LocalDateTime,
    private val tags: List<String>
) {
    fun byggVedtak(vedtakBuilder: SykepengevedtakBuilder) {
        vedtakBuilder.fødselsnummer(fødselsnummer)
        vedtakBuilder.organisasjonsnummer(organisasjonsnummer)
        vedtakBuilder.vedtaksperiodeId(vedtaksperiodeId)
        utbetalingId?.also { vedtakBuilder.utbetalingId(it) }
        vedtakBuilder.skjæringstidspunkt(skjæringstidspunkt)
        vedtakBuilder.hendelser(hendelser)
        vedtakBuilder.sykepengegrunnlag(sykepengegrunnlag)
        vedtakBuilder.grunnlagForSykepengegrunnlag(grunnlagForSykepengegrunnlag)
        vedtakBuilder.grunnlagForSykepengegrunnlagPerArbeidsgiver(grunnlagForSykepengegrunnlagPerArbeidsgiver)
        vedtakBuilder.begrensning(begrensning)
        vedtakBuilder.inntekt(inntekt)
        vedtakBuilder.fom(fom)
        vedtakBuilder.tom(tom)
        vedtakBuilder.aktørId(aktørId)
        vedtakBuilder.vedtakFattetTidspunkt(vedtakFattetTidspunkt)
        vedtakBuilder.tags(tags)
        sykepengegrunnlagsfakta?.also { vedtakBuilder.sykepengegrunnlagsfakta(it) }
    }
}
