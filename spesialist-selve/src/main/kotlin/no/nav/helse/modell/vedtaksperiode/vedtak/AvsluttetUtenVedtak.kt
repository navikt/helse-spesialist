package no.nav.helse.modell.vedtaksperiode.vedtak

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class AvsluttetUtenVedtak(
    private val fødselsnummer: String,
    private val aktørId: String,
    private val organisasjonsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val skjæringstidspunkt: LocalDate,
    private val hendelser: List<UUID>,
    private val fom: LocalDate,
    private val tom: LocalDate,
) {
    fun byggMelding(vedtakBuilder: SykepengevedtakBuilder) {
        vedtakBuilder.fødselsnummer(fødselsnummer)
        vedtakBuilder.organisasjonsnummer(organisasjonsnummer)
        vedtakBuilder.aktørId(aktørId)
        vedtakBuilder.fom(fom)
        vedtakBuilder.tom(tom)
        vedtakBuilder.vedtaksperiodeId(vedtaksperiodeId)
        vedtakBuilder.skjæringstidspunkt(skjæringstidspunkt)
        vedtakBuilder.hendelser(hendelser)
        vedtakBuilder.vedtakFattetTidspunkt(LocalDateTime.now())

        // Default-verdier som kreves i vedtak_fattet-meldingen
        vedtakBuilder.sykepengegrunnlag(0.0)
        vedtakBuilder.grunnlagForSykepengegrunnlag(0.0)
        vedtakBuilder.grunnlagForSykepengegrunnlagPerArbeidsgiver(emptyMap())
        vedtakBuilder.begrensning("VET_IKKE")
        vedtakBuilder.inntekt(0.0)
        vedtakBuilder.tags(emptyList())
    }
}
