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
    internal fun oppdaterAnnullering(
        arbeidsgiverFagsystemId: String,
        utbetalingId: UUID,
    ) = asSQL(
        """
        UPDATE annullert_av_saksbehandler SET utbetaling_id = :utbetalingId WHERE arbeidsgiver_fagsystem_id = :arbeidsgiverFagsystemId
        """.trimIndent(),
        mapOf(
            "utbetalingId" to utbetalingId,
            "arbeidsgiverFagsystemId" to arbeidsgiverFagsystemId,
        ),
    ).update()

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
            INSERT INTO annullert_av_saksbehandler (annullert_tidspunkt, saksbehandler_ref, årsaker, begrunnelse_ref, vedtaksperiode_id, utbetaling_id, arbeidsgiver_fagsystem_id) 
            VALUES (now(), :saksbehandler, '{${
                annulleringDto.årsaker?.map { it.arsak }?.somDbArray()
            }}', :begrunnelseRef, :vedtaksperiodeId, :utbetalingId, :arbeidsgiverFagsystemId)
            """.trimIndent(),
            mapOf(
                "saksbehandler" to saksbehandler.oid(),
                "årsaker" to annulleringDto.årsaker,
                "begrunnelseRef" to begrunnelseId,
                "vedtaksperiodeId" to annulleringDto.vedtaksperiodeId,
                "utbetalingId" to annulleringDto.utbetalingId,
                "arbeidsgiverFagsystemId" to annulleringDto.arbeidsgiverFagsystemId,
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

    internal fun finnAnnulleringId(arbeidsgiverFagsystemId: String) =
        asSQL(
            """
            select id from annullert_av_saksbehandler where arbeidsgiver_fagsystem_id = :arbeidsgiverFagsystemId limit 1;
            """.trimIndent(),
            mapOf(
                "arbeidsgiverFagsystemId" to arbeidsgiverFagsystemId,
            ),
        ).single { it.long("id") }

    fun finnAnnullering(arbeidsgiverFagsystemId: String): Annullering? =
        asSQL(
            """
            select aas.id, aas.annullert_tidspunkt, aas.arbeidsgiver_fagsystem_id, aas.utbetaling_id, s.ident, aas.årsaker, b.tekst from annullert_av_saksbehandler aas
            inner join saksbehandler s on s.oid = aas.saksbehandler_ref
            left join begrunnelse b on b.id = aas.begrunnelse_ref
            where arbeidsgiver_fagsystem_id = :arbeidsgiverFagsystemId;
            """.trimIndent(),
            mapOf(
                "arbeidsgiverFagsystemId" to arbeidsgiverFagsystemId,
            ),
        ).single {
            Annullering(
                saksbehandlerIdent = it.string("ident"),
                utbetalingId = it.uuid("utbetaling_id"),
                arbeidsgiverFagsystemId = it.string("arbeidsgiver_fagsystem_id"),
                tidspunkt = it.localDateTime("annullert_tidspunkt"),
                arsaker = it.array<String>("årsaker").toList(),
                begrunnelse = it.stringOrNull("tekst"),
            )
        }
}
