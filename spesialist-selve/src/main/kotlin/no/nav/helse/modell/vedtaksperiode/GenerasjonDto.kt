package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.modell.varsel.VarselDto

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
    val varsler: List<VarselDto>
)

enum class TilstandDto {
    Låst,
    Ulåst,
    AvsluttetUtenUtbetaling,
    UtenUtbetalingMåVurderes
}
