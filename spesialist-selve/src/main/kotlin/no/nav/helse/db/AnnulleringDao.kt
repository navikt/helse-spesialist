package no.nav.helse.db

import no.nav.helse.HelseDao
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.handlinger.AnnulleringDto
import java.util.UUID
import javax.sql.DataSource

class AnnulleringDao(
    dataSource: DataSource,
) : HelseDao(dataSource) {
    internal fun lagreAnnullering(
        annulleringDto: AnnulleringDto,
        saksbehandler: Saksbehandler,
    ) = lagreBegrunnelse(annulleringDto.begrunnelse ?: "Ingen begrunnelse", saksbehandler.oid()).let { begrunnelseId ->
        asSQL(
            """
            INSERT INTO annullert_av_saksbehandler (annullert_tidspunkt, saksbehandler_ref, årsaker, begrunnelse_ref, vedtaksperiode_id, utbetaling_id) 
            VALUES (now(), :saksbehandler, '{${
                annulleringDto.årsaker.somDbArray()
            }}', :begrunnelseRef, :vedtaksperiodeId, :utbetalingId)
            """.trimIndent(),
            mapOf(
                "saksbehandler" to saksbehandler.oid(),
                "årsaker" to annulleringDto.årsaker,
                "begrunnelseRef" to begrunnelseId,
                "vedtaksperiodeId" to annulleringDto.vedtaksperiodeId,
                "utbetalingId" to annulleringDto.utbetalingId,
            ),
        ).update()
    }

    private fun lagreBegrunnelse(
        begrunnelse: String,
        saksbehandlerOid: UUID,
    ) = asSQL(
        """
        INSERT INTO begrunnelse(tekst, type, saksbehandler_ref) VALUES (:tekst, :type, :saksbehandler_ref)
        """.trimIndent(),
        mapOf(
            "tekst" to begrunnelse,
            "type" to "ANNULLERING",
            "saksbehandler_ref" to saksbehandlerOid,
        ),
    ).updateAndReturnGeneratedKey()

    internal fun finnAnnulleringId(utbetalingId: UUID) =
        asSQL(
            """
            select id from annullert_av_saksbehandler where utbetaling_id = :utbetalingId limit 1;
            """.trimIndent(),
            mapOf(
                "utbetalingId" to utbetalingId,
            ),
        ).single { it.long("id") }
}
