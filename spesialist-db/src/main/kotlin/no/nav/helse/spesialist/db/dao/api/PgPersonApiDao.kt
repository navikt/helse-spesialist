package no.nav.helse.spesialist.db.dao.api

import no.nav.helse.db.api.PersonApiDao
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedDataSource
import no.nav.helse.spesialist.db.QueryRunner
import javax.sql.DataSource

class PgPersonApiDao internal constructor(
    dataSource: DataSource,
) : QueryRunner by MedDataSource(dataSource),
    PersonApiDao {
    override fun finnInfotrygdutbetalinger(fødselsnummer: String) =
        asSQL(
            """ SELECT data FROM infotrygdutbetalinger
            WHERE id = (SELECT infotrygdutbetalinger_ref FROM person WHERE fødselsnummer = :fodselsnummer);
        """,
            "fodselsnummer" to fødselsnummer,
        ).singleOrNull { row -> row.string("data") }
}
