package no.nav.helse.db

import no.nav.helse.HelseDao
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.handlinger.AnnulleringDto
import no.nav.helse.spesialist.api.graphql.schema.Annullering
import java.util.UUID
import javax.sql.DataSource

class AnnulleringDao(
    dataSource: DataSource,
) : HelseDao(dataSource) {
    internal fun lagreAnnullering(
        annulleringDto: AnnulleringDto,
        saksbehandler: Saksbehandler,
    ) {
        val begrunnelseId =
            annulleringDto.kommentar?.let {
                lagreBegrunnelse(it, saksbehandler.oid())
            }
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
        begrunnelse: String?,
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

    fun finnAnnullering(utbetalingId: UUID): Annullering? =
        asSQL(
            """
            select aas.annullert_tidspunkt, s.ident, aas.årsaker, b.tekst from annullert_av_saksbehandler aas
            inner join public.saksbehandler s on s.oid = aas.saksbehandler_ref
            left join public.begrunnelse b on b.id = aas.begrunnelse_ref
            where utbetaling_id = :utbetalingId;
            """.trimIndent(),
            mapOf(
                "utbetalingId" to utbetalingId,
            ),
        ).single {
            Annullering(
                saksbehandlerIdent = it.string("ident"),
                utbetalingId = utbetalingId,
                tidspunkt = it.localDateTime("annullert_tidspunkt"),
                årsaker = it.array<String>("årsaker").toList(),
                begrunnelse = it.stringOrNull("tekst"),
            )
        }
}
