package no.nav.helse.modell.oppgave

import no.nav.helse.modell.saksbehandler.SaksbehandlerDto
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingDto
import java.util.UUID

data class OppgaveDto(
    val id: Long,
    val tilstand: TilstandDto,
    val vedtaksperiodeId: UUID,
    val utbetalingId: UUID,
    val hendelseId: UUID,
    val kanAvvises: Boolean,
    val egenskaper: List<EgenskapDto>,
    val totrinnsvurdering: TotrinnsvurderingDto?,
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
