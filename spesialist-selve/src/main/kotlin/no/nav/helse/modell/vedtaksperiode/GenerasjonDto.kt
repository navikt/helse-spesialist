package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.modell.varsel.VarselDto

class GenerasjonDto(
    val id: UUID,
    val vedtaksperiodeId: UUID,
    val utbetalingId: UUID?,
    val skjæringstidspunkt: LocalDate,
    val periode: Periode,
    val tilstand: TilstandDto,
    val varsler: List<VarselDto>
) {
    override fun equals(other: Any?): Boolean =
        this === other || (
                other is GenerasjonDto
                        && id == other.id
                        && utbetalingId == other.utbetalingId
                        && skjæringstidspunkt == other.skjæringstidspunkt
                        && vedtaksperiodeId == other.vedtaksperiodeId
                        && periode == other.periode
                        && tilstand == other.tilstand
                        && varsler == other.varsler
                )

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + utbetalingId.hashCode()
        result = 31 * result + skjæringstidspunkt.hashCode()
        result = 31 * result + vedtaksperiodeId.hashCode()
        result = 31 * result + periode.hashCode()
        result = 31 * result + tilstand.hashCode()
        result = 31 * result + varsler.hashCode()
        return result
    }
}

enum class TilstandDto {
    Låst,
    Ulåst,
    AvsluttetUtenUtbetaling,
    UtenUtbetalingMåVurderes

}
