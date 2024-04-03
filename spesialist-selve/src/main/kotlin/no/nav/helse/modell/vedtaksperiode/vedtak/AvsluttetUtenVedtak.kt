package no.nav.helse.modell.vedtaksperiode.vedtak

import java.time.LocalDateTime
import java.util.UUID

internal class AvsluttetUtenVedtak(
    private val vedtaksperiodeId: UUID,
    private val hendelser: List<UUID>,
    private val spleisBehandlingId: UUID,
) {
    fun vedtaksperiodeId() = vedtaksperiodeId

    fun spleisBehandlingId() = spleisBehandlingId

    fun byggMelding(vedtakBuilder: SykepengevedtakBuilder) {
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
