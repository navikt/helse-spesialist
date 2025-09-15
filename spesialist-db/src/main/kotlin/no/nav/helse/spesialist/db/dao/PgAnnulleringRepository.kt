package no.nav.helse.spesialist.db.dao

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.db.AnnulleringRepository
import no.nav.helse.modell.Annullering
import no.nav.helse.modell.AnnulleringId
import no.nav.helse.spesialist.db.DataSourceDbQuery
import no.nav.helse.spesialist.db.DbQuery
import no.nav.helse.spesialist.db.HelseDao.Companion.somDbArray
import no.nav.helse.spesialist.db.SessionDbQuery
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import javax.sql.DataSource

class PgAnnulleringRepository private constructor(
    private val dbQuery: DbQuery,
) : AnnulleringRepository {
    internal constructor(session: Session) : this(SessionDbQuery(session))
    internal constructor(dataSource: DataSource) : this(DataSourceDbQuery(dataSource))

    override fun lagreAnnullering(annullering: Annullering) {
        val begrunnelseId =
            annullering.kommentar?.let {
                lagreBegrunnelse(
                    begrunnelse = it,
                    saksbehandlerOid = annullering.saksbehandlerOid,
                )
            }

        val id =
            dbQuery.updateAndReturnGeneratedKey(
                """
                INSERT INTO annullert_av_saksbehandler (annullert_tidspunkt, saksbehandler_ref, årsaker, begrunnelse_ref, arbeidsgiver_fagsystem_id, person_fagsystem_id, vedtaksperiode_id) 
                VALUES (:annullert_tidspunkt, :saksbehandler, :arsaker::varchar[], :begrunnelseRef, :arbeidsgiverFagsystemId, :personFagsystemId, :vedtaksperiodeId)
                """.trimIndent(),
                "annullert_tidspunkt" to annullering.tidspunkt,
                "saksbehandler" to annullering.saksbehandlerOid.value,
                "arsaker" to annullering.årsaker.somDbArray(),
                "begrunnelseRef" to begrunnelseId,
                "arbeidsgiverFagsystemId" to annullering.arbeidsgiverFagsystemId,
                "personFagsystemId" to annullering.personFagsystemId,
                "vedtaksperiodeId" to annullering.vedtaksperiodeId,
            )

        annullering.tildelId(AnnulleringId(id!!.toInt()))
    }

    private fun lagreBegrunnelse(
        begrunnelse: String?,
        saksbehandlerOid: SaksbehandlerOid,
    ) = dbQuery.updateAndReturnGeneratedKey(
        """
        INSERT INTO begrunnelse(tekst, type, saksbehandler_ref) VALUES (:tekst, :type, :saksbehandler_ref)
        """.trimIndent(),
        "tekst" to begrunnelse,
        "type" to "ANNULLERING",
        "saksbehandler_ref" to saksbehandlerOid.value,
    )

    override fun finnAnnullering(id: AnnulleringId): Annullering? =
        dbQuery.singleOrNull(
            """
            select aas.id, aas.annullert_tidspunkt, aas.arbeidsgiver_fagsystem_id, aas.person_fagsystem_id, aas.saksbehandler_ref, aas.årsaker, b.tekst, vedtaksperiode_id
            from annullert_av_saksbehandler aas
                left join begrunnelse b on b.id = aas.begrunnelse_ref
            where aas.id = :id
            """.trimIndent(),
            "id" to id.value,
        ) { it.tilAnnullering() }

    override fun finnAnnulleringMedEnAv(
        arbeidsgiverFagsystemId: String,
        personFagsystemId: String,
    ): Annullering? =
        dbQuery.singleOrNull(
            """
            select aas.id, aas.annullert_tidspunkt, aas.arbeidsgiver_fagsystem_id, aas.person_fagsystem_id, aas.saksbehandler_ref, aas.årsaker, b.tekst, vedtaksperiode_id
            from annullert_av_saksbehandler aas
                left join begrunnelse b on b.id = aas.begrunnelse_ref
            where arbeidsgiver_fagsystem_id = :arbeidsgiverFagsystemId or person_fagsystem_id = :personFagsystemId;
            """.trimIndent(),
            "arbeidsgiverFagsystemId" to arbeidsgiverFagsystemId,
            "personFagsystemId" to personFagsystemId,
        ) { it.tilAnnullering() }

    private fun Row.tilAnnullering(): Annullering =
        Annullering.Factory.fraLagring(
            id = AnnulleringId(int("id")),
            arbeidsgiverFagsystemId = stringOrNull("arbeidsgiver_fagsystem_id"),
            personFagsystemId = stringOrNull("person_fagsystem_id"),
            saksbehandlerOid = SaksbehandlerOid(uuid("saksbehandler_ref")),
            vedtaksperiodeId = uuid("vedtaksperiode_id"),
            tidspunkt = localDateTime("annullert_tidspunkt"),
            årsaker = array<String>("årsaker").toList(),
            kommentar = stringOrNull("tekst"),
        )
}
