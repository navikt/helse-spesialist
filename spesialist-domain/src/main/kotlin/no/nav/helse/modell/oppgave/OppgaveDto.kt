package no.nav.helse.modell.oppgave

import no.nav.helse.modell.saksbehandler.SaksbehandlerDto
import java.util.UUID

data class OppgaveDto(
    val id: Long,
    val tilstand: TilstandDto,
    val vedtaksperiodeId: UUID,
    val behandlingId: UUID,
    val utbetalingId: UUID,
    val godkjenningsbehovId: UUID,
    val kanAvvises: Boolean,
    val egenskaper: List<EgenskapDto>,
    val ferdigstiltAvIdent: String?,
    val ferdigstiltAvOid: UUID?,
    val tildeltTil: SaksbehandlerDto?,
) {
    enum class TilstandDto {
        AvventerSaksbehandler,
        AvventerSystem,
        Ferdigstilt,
        Invalidert,
    }
}
