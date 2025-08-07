package no.nav.helse.spesialist.db.dao

import no.nav.helse.db.AnnulleringRepository
import no.nav.helse.modell.Annullering
import no.nav.helse.modell.saksbehandler.handlinger.AnnulleringDto
import no.nav.helse.spesialist.db.DbQuery
import no.nav.helse.spesialist.db.HelseDao.Companion.somDbArray
import no.nav.helse.spesialist.domain.legacy.LegacySaksbehandler
import java.util.UUID
import javax.sql.DataSource

class PgAnnulleringRepository internal constructor(
    dataSource: DataSource,
) : AnnulleringRepository {
    private val dbQuery = DbQuery(dataSource)

    override fun lagreAnnullering(
        annulleringDto: AnnulleringDto,
        legacySaksbehandler: LegacySaksbehandler,
    ) {
        val begrunnelseId =
            annulleringDto.kommentar?.let {
                lagreBegrunnelse(it, legacySaksbehandler.oid())
            }
        val annullertAvSaksbehandlerKey =
            dbQuery.updateAndReturnGeneratedKey(
                """
                INSERT INTO annullert_av_saksbehandler (annullert_tidspunkt, saksbehandler_ref, årsaker, begrunnelse_ref, arbeidsgiver_fagsystem_id, person_fagsystem_id, vedtaksperiode_id) 
                VALUES (now(), :saksbehandler, :arsaker::varchar[], :begrunnelseRef, :arbeidsgiverFagsystemId, :personFagsystemId, :vedtaksperiodeId)
                """.trimIndent(),
                "saksbehandler" to legacySaksbehandler.oid(),
                "arsaker" to annulleringDto.årsaker.somDbArray { it.arsak },
                "begrunnelseRef" to begrunnelseId,
                "arbeidsgiverFagsystemId" to annulleringDto.arbeidsgiverFagsystemId,
                "personFagsystemId" to annulleringDto.personFagsystemId,
                "vedtaksperiodeId" to annulleringDto.vedtaksperiodeId,
            )
        annulleringDto.annulleringskandidater.forEach { kandidat ->
            dbQuery.update(
                """
                insert into annulleringskandidater_annullert_av_saksbehandler (vedtaksperiode_id, annullert_av_saksbehandler_ref)
                values (:vedtaksperiodeId, :annullertAvSaksbehandlerKey)
                """.trimIndent(),
                "vedtaksperiodeId" to kandidat,
                "annullertAvSaksbehandlerKey" to annullertAvSaksbehandlerKey,
            )
        }
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
    ): Annullering? {
        val annulleringskandidater =
            dbQuery
                .list(
                    """
                    select aaas.vedtaksperiode_id
                    from annulleringskandidater_annullert_av_saksbehandler aaas
                    inner join annullert_av_saksbehandler as aas on aas.id = aaas.annullert_av_saksbehandler_ref
                    where arbeidsgiver_fagsystem_id = :arbeidsgiverFagsystemId or person_fagsystem_id = :personFagsystemId;
                    """.trimIndent(),
                    "arbeidsgiverFagsystemId" to arbeidsgiverFagsystemId,
                    "personFagsystemId" to personFagsystemId,
                ) {
                    it.uuid("vedtaksperiode_id")
                }.toSet()
        return dbQuery.singleOrNull(
            """
            select aas.id, aas.annullert_tidspunkt, aas.arbeidsgiver_fagsystem_id, aas.person_fagsystem_id, s.ident, aas.årsaker, b.tekst, vedtaksperiode_id
            from annullert_av_saksbehandler aas
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
                vedtaksperiodeId = it.stringOrNull("vedtaksperiode_id")?.let(UUID::fromString),
                annulleringskandidater = annulleringskandidater,
            )
        }
    }

    override fun finnAnnullering(annulleringDto: AnnulleringDto): Annullering? = finnAnnullering(annulleringDto.arbeidsgiverFagsystemId, annulleringDto.personFagsystemId)
}
