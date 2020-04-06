package no.nav.helse.model

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import java.time.LocalDate
import javax.sql.DataSource

internal class TestPersonDao(private val dataSource: DataSource) {


    internal fun setEnhetOppdatert(fødselsnummer: Long, enhetOppdatert: LocalDate) = using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf(
                "UPDATE person SET enhet_ref_oppdatert=? WHERE fodselsnummer=?;",
                enhetOppdatert,
                fødselsnummer
            ).asUpdate
        )
    }
}
