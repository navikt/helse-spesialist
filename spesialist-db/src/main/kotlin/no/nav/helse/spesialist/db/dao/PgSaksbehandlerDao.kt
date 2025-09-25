package no.nav.helse.spesialist.db.dao

import kotliquery.Session
import no.nav.helse.db.SaksbehandlerDao
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedDataSource
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

class PgSaksbehandlerDao private constructor(
    private val queryRunner: QueryRunner,
) : SaksbehandlerDao,
    QueryRunner by queryRunner {
    internal constructor(session: Session) : this(MedSession(session))
    internal constructor(dataSource: DataSource) : this(MedDataSource(dataSource))

    override fun hent(ident: String): Saksbehandler? =
        asSQL(
            """
            SELECT * 
            FROM saksbehandler
            WHERE ident = :saksbehandler_ident
            """.trimIndent(),
            "saksbehandler_ident" to ident,
        ).singleOrNull {
            Saksbehandler(
                id = SaksbehandlerOid(it.uuid("oid")),
                navn = it.string("navn"),
                epost = it.string("epost"),
                ident = it.string("ident"),
            )
        }

    override fun hentAlleAktiveSisteTreMnderEllerHarTildelteOppgaver(): List<Saksbehandler> =
        asSQL(
            """
            SELECT s.*
            FROM saksbehandler s
            WHERE s.siste_handling_utført_tidspunkt > CURRENT_DATE - INTERVAL '3 months'
               OR EXISTS (
                   SELECT 1
                   FROM tildeling t
                   WHERE t.saksbehandler_ref = s.oid
               );
            """.trimIndent(),
        ).list {
            Saksbehandler(
                id = SaksbehandlerOid(it.uuid("oid")),
                navn = it.string("navn"),
                epost = it.string("epost"),
                ident = it.string("ident"),
            )
        }

    override fun opprettEllerOppdater(
        oid: UUID,
        navn: String,
        epost: String,
        ident: String,
    ) = asSQL(
        """ 
        INSERT INTO saksbehandler (oid, navn, epost, ident)
        VALUES (:oid, :navn, :epost, :ident)
        ON CONFLICT (oid)
            DO UPDATE SET navn = :navn, epost = :epost, ident = :ident
            WHERE (saksbehandler.navn, saksbehandler.epost, saksbehandler.ident)
                IS DISTINCT FROM (excluded.navn, excluded.epost, excluded.ident)
        """.trimIndent(),
        "oid" to oid,
        "navn" to navn,
        "epost" to epost,
        "ident" to ident,
    ).update()

    override fun oppdaterSistObservert(
        oid: UUID,
        sisteHandlingUtført: LocalDateTime,
    ) = asSQL(
        """ 
        UPDATE saksbehandler
        SET siste_handling_utført_tidspunkt = :siste_handling_utfort_tidspunkt
        WHERE oid = :oid
        """.trimIndent(),
        "oid" to oid,
        "siste_handling_utfort_tidspunkt" to sisteHandlingUtført,
    ).update()
}
