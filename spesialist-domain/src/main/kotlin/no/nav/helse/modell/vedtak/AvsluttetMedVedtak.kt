package no.nav.helse.modell.vedtak

import java.time.LocalDateTime
import java.util.UUID

class AvsluttetMedVedtak(
    val spleisBehandlingId: UUID,
    val yrkesaktivitetstype: String,
    val hendelser: List<UUID>,
    val sykepengegrunnlag: Double,
    val sykepengegrunnlagsfakta: Sykepengegrunnlagsfakta,
    val vedtakFattetTidspunkt: LocalDateTime,
) {
    fun byggVedtak(vedtakBuilder: SykepengevedtakBuilder) {
        vedtakBuilder.hendelser(hendelser)
        vedtakBuilder.sykepengegrunnlag(sykepengegrunnlag)
        vedtakBuilder.vedtakFattetTidspunkt(vedtakFattetTidspunkt)
        vedtakBuilder.sykepengegrunnlagsfakta(sykepengegrunnlagsfakta)
    }
}
