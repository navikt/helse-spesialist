package no.nav.helse.db

import java.util.UUID
import javax.sql.DataSource
import no.nav.helse.HelseDao


interface SaksbehandlerRepository {
    fun finnSaksbehandler(oid: UUID): SaksbehandlerFraDatabase?
}

class SaksbehandlerDao(dataSource: DataSource) : HelseDao(dataSource), SaksbehandlerRepository {
    fun opprettSaksbehandler(oid: UUID, navn: String, epost: String, ident: String) =
        asSQL(
            """ 
            INSERT INTO saksbehandler(oid, navn, epost, ident) VALUES (:oid, :navn, :epost, :ident)
            ON CONFLICT (oid)
                DO UPDATE SET navn = :navn, epost = :epost, ident = :ident
                WHERE (saksbehandler.navn, saksbehandler.epost, saksbehandler.ident) IS DISTINCT FROM
                    (excluded.navn, excluded.epost, excluded.ident)
            """, mapOf("oid" to oid, "navn" to navn, "epost" to epost, "ident" to ident)
        ).update()

    override fun finnSaksbehandler(oid: UUID) = asSQL(
        " SELECT * FROM saksbehandler WHERE oid = :oid LIMIT 1; ",
        mapOf("oid" to oid)
    ).single { row ->
        SaksbehandlerFraDatabase(
            epostadresse = row.string("epost"),
            oid = row.uuid("oid"),
            navn = row.string("navn"),
            ident = row.string("ident")
        )
    }

    fun finnOid(epost: String): UUID? = asSQL(
        "SELECT oid FROM saksbehandler WHERE epost ILIKE :epost LIMIT 1", mapOf("epost" to epost)
    ).single { row ->
        row.uuid("oid")
    }
}
