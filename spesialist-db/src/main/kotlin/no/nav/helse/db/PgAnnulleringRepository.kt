package no.nav.helse.db

import no.nav.helse.modell.Annullering
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.handlinger.AnnulleringDto
import java.util.UUID
import javax.sql.DataSource

class PgAnnulleringRepository internal constructor(dataSource: DataSource) : AnnulleringRepository {
    private val dbQuery = DbQuery(dataSource)

    override fun lagreAnnullering(
        annulleringDto: AnnulleringDto,
        saksbehandler: Saksbehandler,
    ) {
        val begrunnelseId =
            annulleringDto.kommentar?.let {
                lagreBegrunnelse(it, saksbehandler.oid())
            }
        dbQuery.update(
            """
            INSERT INTO annullert_av_saksbehandler (annullert_tidspunkt, saksbehandler_ref, årsaker, begrunnelse_ref, arbeidsgiver_fagsystem_id, person_fagsystem_id) 
            VALUES (now(), :saksbehandler, :arsaker::varchar[], :begrunnelseRef, :arbeidsgiverFagsystemId, :personFagsystemId)
            """.trimIndent(),
            "saksbehandler" to saksbehandler.oid(),
            "arsaker" to annulleringDto.årsaker.somDbArray { it.arsak },
            "begrunnelseRef" to begrunnelseId,
            "arbeidsgiverFagsystemId" to annulleringDto.arbeidsgiverFagsystemId,
            "personFagsystemId" to annulleringDto.personFagsystemId,
        )
    }

    private fun lagreBegrunnelse(
        begrunnelse: String?,
        saksbehandlerOid: UUID,
    ) = dbQuery.updateAndReturnGeneratedKey(
        """
        INSERT INTO begrunnelse(tekst, type, saksbehandler_ref) VALUES (:tekst, :type, :saksbehandler_ref)
        """.trimIndent(),
        "tekst" to begrunnelse,
        "type" to "ANNULLERING",
        "saksbehandler_ref" to saksbehandlerOid,
    )

    override fun finnAnnullering(
        arbeidsgiverFagsystemId: String,
        personFagsystemId: String,
    ): Annullering? =
        dbQuery.singleOrNull(
            """
            select aas.id, aas.annullert_tidspunkt, aas.arbeidsgiver_fagsystem_id, aas.person_fagsystem_id, s.ident, aas.årsaker, b.tekst from annullert_av_saksbehandler aas
            inner join saksbehandler s on s.oid = aas.saksbehandler_ref
            left join begrunnelse b on b.id = aas.begrunnelse_ref
            where arbeidsgiver_fagsystem_id = :arbeidsgiverFagsystemId or person_fagsystem_id = :personFagsystemId;
            """.trimIndent(),
            "arbeidsgiverFagsystemId" to arbeidsgiverFagsystemId,
            "personFagsystemId" to personFagsystemId,
        ) {
            Annullering(
                saksbehandlerIdent = it.string("ident"),
                arbeidsgiverFagsystemId = it.stringOrNull("arbeidsgiver_fagsystem_id"),
                personFagsystemId = it.stringOrNull("person_fagsystem_id"),
                tidspunkt = it.localDateTime("annullert_tidspunkt"),
                arsaker = it.array<String>("årsaker").toList(),
                begrunnelse = it.stringOrNull("tekst"),
            )
        }

    override fun finnAnnullering(annulleringDto: AnnulleringDto): Annullering? {
        return finnAnnullering(annulleringDto.arbeidsgiverFagsystemId, annulleringDto.personFagsystemId)
    }
}

private fun <T> Iterable<T>.somDbArray(transform: (T) -> String) = joinToString(prefix = "{", postfix = "}", transform = transform)
