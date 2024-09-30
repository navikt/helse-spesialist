package no.nav.helse.spesialist.api.tildeling

import kotliquery.Row
import no.nav.helse.HelseDao
import java.util.UUID
import javax.sql.DataSource

class TildelingApiDao(dataSource: DataSource) : HelseDao(dataSource) {
    fun tildelingForPerson(fødselsnummer: String) =
        asSQL(
            """ 
            SELECT s.epost, s.oid, s.navn FROM person
                 RIGHT JOIN vedtak v on person.id = v.person_ref
                 RIGHT JOIN oppgave o on v.id = o.vedtak_ref
                 RIGHT JOIN tildeling t on o.id = t.oppgave_id_ref
                 RIGHT JOIN saksbehandler s on t.saksbehandler_ref = s.oid
            WHERE fodselsnummer = :fnr AND o.status = 'AvventerSaksbehandler'
            ORDER BY o.opprettet DESC;
        """,
            mapOf("fnr" to fødselsnummer.toLong()),
        ).single(::tildelingDto)

    private fun tildelingDto(it: Row) =
        TildelingApiDto(
            epost = it.string("epost"),
            oid = UUID.fromString(it.string("oid")),
            navn = it.string("navn"),
        )
}
