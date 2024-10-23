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
            INSERT INTO annullert_av_saksbehandler (annullert_tidspunkt, saksbehandler_ref, årsaker, begrunnelse_ref, arbeidsgiver_fagsystem_id, person_fagsystem_id) 
            VALUES (now(), :saksbehandler, :arsaker::varchar[], :begrunnelseRef, :arbeidsgiverFagsystemId, :personFagsystemId)
            """.trimIndent(),
            mapOf(
                "saksbehandler" to saksbehandler.oid(),
                "arsaker" to annulleringDto.årsaker.map { it.arsak }.somDbArray(),
                "begrunnelseRef" to begrunnelseId,
                "arbeidsgiverFagsystemId" to annulleringDto.arbeidsgiverFagsystemId,
                "personFagsystemId" to annulleringDto.personFagsystemId,
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

    fun finnAnnullering(
        arbeidsgiverFagsystemId: String,
        personFagsystemId: String,
    ): Annullering? =
        asSQL(
            """
            select aas.id, aas.annullert_tidspunkt, aas.arbeidsgiver_fagsystem_id, aas.person_fagsystem_id, s.ident, aas.årsaker, b.tekst from annullert_av_saksbehandler aas
            inner join saksbehandler s on s.oid = aas.saksbehandler_ref
            left join begrunnelse b on b.id = aas.begrunnelse_ref
            where arbeidsgiver_fagsystem_id = :arbeidsgiverFagsystemId or person_fagsystem_id = :personFagsystemId;
            """.trimIndent(),
            mapOf(
                "arbeidsgiverFagsystemId" to arbeidsgiverFagsystemId,
                "personFagsystemId" to personFagsystemId,
            ),
        ).single {
            Annullering(
                saksbehandlerIdent = it.string("ident"),
                arbeidsgiverFagsystemId = it.stringOrNull("arbeidsgiver_fagsystem_id"),
                personFagsystemId = it.stringOrNull("person_fagsystem_id"),
                tidspunkt = it.localDateTime("annullert_tidspunkt"),
                arsaker = it.array<String>("årsaker").toList(),
                begrunnelse = it.stringOrNull("tekst"),
            )
        }

    fun finnAnnullering(annulleringDto: AnnulleringDto): Annullering? {
        return finnAnnullering(annulleringDto.arbeidsgiverFagsystemId, annulleringDto.personFagsystemId)
    }
}

fun <T> Iterable<T>.somDbArray() = joinToString(prefix = "{", postfix = "}")
