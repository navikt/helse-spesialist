package no.nav.helse.spesialist.db.dao

import kotliquery.Session
import no.nav.helse.db.HelseDao.Companion.asSQL
import no.nav.helse.db.MedDataSource
import no.nav.helse.db.MedSession
import no.nav.helse.db.QueryRunner
import no.nav.helse.db.SaksbehandlerDao
import no.nav.helse.db.SaksbehandlerFraDatabase
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.Tilgangskontroll
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

class PgSaksbehandlerDao private constructor(private val queryRunner: QueryRunner, private val tilgangskontroll: Tilgangskontroll) : SaksbehandlerDao, QueryRunner by queryRunner {
    internal constructor(session: Session, tilgangskontroll: Tilgangskontroll) : this(MedSession(session), tilgangskontroll)
    internal constructor(dataSource: DataSource, tilgangskontroll: Tilgangskontroll) : this(MedDataSource(dataSource), tilgangskontroll)

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

    override fun finnSaksbehandlerFraDatabase(oid: UUID) =
        asSQL("SELECT * FROM saksbehandler WHERE oid = :oid LIMIT 1", "oid" to oid)
            .singleOrNull { row ->
                SaksbehandlerFraDatabase(
                    epostadresse = row.string("epost"),
                    oid = row.uuid("oid"),
                    navn = row.string("navn"),
                    ident = row.string("ident"),
                )
            }

    override fun finnSaksbehandler(oid: UUID) =
        asSQL("SELECT * FROM saksbehandler WHERE oid = :oid LIMIT 1", "oid" to oid)
            .singleOrNull { row ->
                Saksbehandler(
                    epostadresse = row.string("epost"),
                    oid = row.uuid("oid"),
                    navn = row.string("navn"),
                    ident = row.string("ident"),
                    tilgangskontroll = tilgangskontroll,
                )
            }
}
