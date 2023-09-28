package no.nav.helse.spesialist.api.saksbehandler

import java.util.UUID
import javax.sql.DataSource
import no.nav.helse.HelseDao

class SaksbehandlerDao(dataSource: DataSource) : HelseDao(dataSource) {
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
}
