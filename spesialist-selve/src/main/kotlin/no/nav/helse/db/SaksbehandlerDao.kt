package no.nav.helse.db

import no.nav.helse.HelseDao
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

interface SaksbehandlerRepository {
    fun finnSaksbehandler(oid: UUID): SaksbehandlerFraDatabase?
}

class SaksbehandlerDao(dataSource: DataSource) : HelseDao(dataSource), SaksbehandlerRepository {
    fun opprettEllerOppdater(
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
        """.trimIndent(),
        mapOf("oid" to oid, "navn" to navn, "epost" to epost, "ident" to ident),
    ).update()

    fun oppdaterSistObservert(
        oid: UUID,
        sisteHandlingUtført: LocalDateTime = LocalDateTime.now(),
    ) = asSQL(
        """ 
        UPDATE saksbehandler
        SET siste_handling_utført_tidspunkt = :siste_handling_utfort_tidspunkt
        WHERE oid = :oid
        """.trimIndent(),
        mapOf("oid" to oid, "siste_handling_utfort_tidspunkt" to sisteHandlingUtført),
    ).update()

    override fun finnSaksbehandler(oid: UUID) =
        asSQL(
            " SELECT * FROM saksbehandler WHERE oid = :oid LIMIT 1; ",
            mapOf("oid" to oid),
        ).single { row ->
            SaksbehandlerFraDatabase(
                epostadresse = row.string("epost"),
                oid = row.uuid("oid"),
                navn = row.string("navn"),
                ident = row.string("ident"),
            )
        }
}
