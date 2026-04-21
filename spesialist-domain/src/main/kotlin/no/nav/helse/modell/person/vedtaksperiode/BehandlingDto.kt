package no.nav.helse.modell.person.vedtaksperiode

import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import java.time.LocalDate
import java.util.UUID

data class BehandlingDto(
    val id: UUID,
    val vedtaksperiodeId: UUID,
    val utbetalingId: UUID?,
    val spleisBehandlingId: UUID?,
    val skjæringstidspunkt: LocalDate,
    val fom: LocalDate,
    val tom: LocalDate,
    val tilstand: TilstandDto,
    val tags: List<String>,
    val varsler: List<VarselDto>,
    val yrkesaktivitetstype: Yrkesaktivitetstype,
)

enum class TilstandDto {
    VedtakFattet,
    VidereBehandlingAvklares,
    AvsluttetUtenVedtak,
    AvsluttetUtenVedtakMedVarsler,
    KlarTilBehandling,
}
