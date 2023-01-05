package no.nav.helse.spesialist.api.saksbehandler

import java.util.UUID
import javax.sql.DataSource
import no.nav.helse.HelseDao

class SaksbehandlerDao(dataSource: DataSource): HelseDao(dataSource) {
    fun opprettSaksbehandler(oid: UUID, navn: String, epost: String, ident: String) =
        """ INSERT INTO saksbehandler(oid, navn, epost, ident) VALUES (:oid, :navn, :epost, :ident)
            ON CONFLICT (oid)
                DO UPDATE SET navn = :navn, epost = :epost, ident = :ident
                WHERE (saksbehandler.navn, saksbehandler.epost, saksbehandler.ident) IS DISTINCT FROM
                    (excluded.navn, excluded.epost, excluded.ident)
        """.update(mapOf("oid" to oid, "navn" to navn, "epost" to epost, "ident" to ident))

    fun finnSaksbehandler(oid: UUID) =
        """ SELECT * FROM saksbehandler WHERE oid = :oid LIMIT 1"""
            .single(mapOf("oid" to oid)) { row ->
                SaksbehandlerDto(
                    oid = oid,
                    navn = row.string("navn"),
                    epost = row.string("epost"),
                    ident = row.string("ident"))}

    fun finnSaksbehandler(epost: String) =
        """ SELECT * FROM saksbehandler WHERE epost ILIKE :epost LIMIT 1"""
            .single(mapOf("epost" to epost)) { row ->
            SaksbehandlerDto(
                oid = row.uuid("oid"),
                navn = row.string("navn"),
                epost = row.string("epost"),
                ident = row.string("ident"))}

    internal fun finnSaksbehandlerFor(oid: UUID): Saksbehandler? {
        return queryize("SELECT * FROM saksbehandler WHERE oid = :oid").single(mapOf("oid" to oid)) {
            Saksbehandler(
                it.string("epost"),
                it.uuid("oid"),
                it.string("navn"),
                it.string("ident")
            )
        }
    }

    internal fun opprettFra(saksbehandlerDto: SaksbehandlerDto): Saksbehandler {
        val epostadresse = saksbehandlerDto.epost
        val oid = saksbehandlerDto.oid
        val navn = saksbehandlerDto.navn
        val ident = saksbehandlerDto.ident

        opprettSaksbehandler(oid, navn, epostadresse, ident)
        return Saksbehandler(epostadresse, oid, navn, ident)
    }
}
