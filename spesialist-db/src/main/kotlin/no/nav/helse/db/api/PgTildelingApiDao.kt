package no.nav.helse.db.api

import kotliquery.Row
import no.nav.helse.HelseDao
import no.nav.helse.spesialist.api.tildeling.TildelingApiDto
import java.util.UUID
import javax.sql.DataSource

class PgTildelingApiDao internal constructor(dataSource: DataSource) : HelseDao(dataSource), TildelingApiDao {
    override fun tildelingForPerson(fødselsnummer: String) =
        asSQL(
            """ 
            SELECT s.epost, s.oid, s.navn FROM person
                 RIGHT JOIN vedtak v on person.id = v.person_ref
                 RIGHT JOIN oppgave o on v.id = o.vedtak_ref
                 RIGHT JOIN tildeling t on o.id = t.oppgave_id_ref
                 RIGHT JOIN saksbehandler s on t.saksbehandler_ref = s.oid
            WHERE fødselsnummer = :fnr AND o.status = 'AvventerSaksbehandler'
            ORDER BY o.opprettet DESC;
            """.trimIndent(),
            "fnr" to fødselsnummer,
        ).single(::tildelingDto)

    private fun tildelingDto(it: Row) =
        TildelingApiDto(
            epost = it.string("epost"),
            oid = UUID.fromString(it.string("oid")),
            navn = it.string("navn"),
        )
}
