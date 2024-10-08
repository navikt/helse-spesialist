package no.nav.helse.modell.vedtaksperiode

import no.nav.helse.modell.person.vedtaksperiode.VarselDto
import no.nav.helse.modell.vedtak.AvslagDto
import java.time.LocalDate
import java.util.UUID

data class GenerasjonDto(
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
    val avslag: AvslagDto?,
)

enum class TilstandDto {
    VedtakFattet,
    VidereBehandlingAvklares,
    AvsluttetUtenVedtak,
    AvsluttetUtenVedtakMedVarsler,
    KlarTilBehandling,
}
