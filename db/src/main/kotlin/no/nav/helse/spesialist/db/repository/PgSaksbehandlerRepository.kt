package no.nav.helse.spesialist.db.repository

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.spesialist.application.SaksbehandlerRepository
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedDataSource
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import no.nav.helse.spesialist.domain.NAVIdent
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

class PgSaksbehandlerRepository private constructor(
    private val queryRunner: QueryRunner,
) : SaksbehandlerRepository,
    QueryRunner by queryRunner {
    internal constructor(session: Session) : this(MedSession(session))
    internal constructor(dataSource: DataSource) : this(MedDataSource(dataSource))

    override fun finn(oid: SaksbehandlerOid): Saksbehandler? =
        asSQL(
            """ 
            SELECT * FROM saksbehandler WHERE oid = :oid
            """.trimIndent(),
            "oid" to oid.value,
        ).singleOrNull { it.toSaksbehandler() }

    override fun finn(ident: NAVIdent): Saksbehandler? =
        asSQL(
            """ 
            SELECT * FROM saksbehandler WHERE ident = :ident
            """.trimIndent(),
            "ident" to ident.value,
        ).singleOrNull { it.toSaksbehandler() }

    override fun finnAlle(oider: Set<SaksbehandlerOid>): List<Saksbehandler> =
        if (oider.isEmpty()) {
            emptyList()
        } else {
            asSQL(
                "SELECT * FROM saksbehandler WHERE oid = ANY (:oider)",
                "oider" to oider.map { it.value }.toTypedArray(),
            ).list { it.toSaksbehandler() }
        }

    private fun Row.toSaksbehandler(): Saksbehandler =
        Saksbehandler(
            id = SaksbehandlerOid(UUID.fromString(string("oid"))),
            navn = string("navn"),
            epost = string("epost"),
            ident = NAVIdent(string("ident")),
        )

    override fun lagre(saksbehandler: Saksbehandler) {
        asSQL(
            """ 
            INSERT INTO saksbehandler (oid, navn, epost, ident, siste_handling_utført_tidspunkt)
            VALUES (:oid, :navn, :epost, :ident, :siste_handling_utfort_tidspunkt)
            ON CONFLICT (oid)
                DO UPDATE SET navn = :navn, epost = :epost, ident = :ident, siste_handling_utført_tidspunkt = :siste_handling_utfort_tidspunkt
                WHERE (saksbehandler.navn, saksbehandler.epost, saksbehandler.ident, saksbehandler.siste_handling_utført_tidspunkt)
                    IS DISTINCT FROM (excluded.navn, excluded.epost, excluded.ident, excluded.siste_handling_utført_tidspunkt)
            """.trimIndent(),
            "oid" to saksbehandler.id.value,
            "navn" to saksbehandler.navn,
            "epost" to saksbehandler.epost,
            "ident" to saksbehandler.ident.value,
            "siste_handling_utfort_tidspunkt" to LocalDateTime.now(),
        ).update()
    }
}
