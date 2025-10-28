package no.nav.helse.spesialist.domain

import no.nav.helse.modell.vedtak.Utfall
import no.nav.helse.spesialist.domain.ddd.Entity

@JvmInline
value class VedtakBegrunnelseId(
    val value: Long,
)

class VedtakBegrunnelse private constructor(
    val id: VedtakBegrunnelseId?,
    val behandlingId: SpleisBehandlingId,
    val tekst: String,
    val utfall: Utfall,
    invalidert: Boolean,
    val saksbehandlerOid: SaksbehandlerOid,
) : Entity<VedtakBegrunnelseId>(id) {
    var invalidert: Boolean = invalidert
        private set

    fun erForskjelligFra(
        tekst: String,
        utfall: Utfall,
    ): Boolean = this.tekst != tekst || this.utfall != utfall

    fun invalider() {
        this.invalidert = true
    }

    companion object {
        fun fraLagring(
            id: VedtakBegrunnelseId,
            spleisBehandlingId: SpleisBehandlingId,
            tekst: String,
            utfall: Utfall,
            invalidert: Boolean,
            saksbehandlerOid: SaksbehandlerOid,
        ) = VedtakBegrunnelse(
            id = id,
            behandlingId = spleisBehandlingId,
            tekst = tekst,
            utfall = utfall,
            invalidert = invalidert,
            saksbehandlerOid = saksbehandlerOid,
        )

        fun ny(
            spleisBehandlingId: SpleisBehandlingId,
            tekst: String,
            utfall: Utfall,
            saksbehandlerOid: SaksbehandlerOid,
        ) = VedtakBegrunnelse(
            id = null,
            behandlingId = spleisBehandlingId,
            tekst = tekst,
            utfall = utfall,
            invalidert = false,
            saksbehandlerOid = saksbehandlerOid,
        )
    }
}
