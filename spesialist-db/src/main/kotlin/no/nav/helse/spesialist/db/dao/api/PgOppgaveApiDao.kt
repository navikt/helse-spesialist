package no.nav.helse.spesialist.db.dao.api

import no.nav.helse.db.api.OppgaveApiDao
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedDataSource
import no.nav.helse.spesialist.db.QueryRunner
import javax.sql.DataSource

class PgOppgaveApiDao internal constructor(
    dataSource: DataSource,
) : QueryRunner by MedDataSource(dataSource),
    OppgaveApiDao {
    override fun finnOppgaveId(fødselsnummer: String) =
        asSQL(
            """
            SELECT o.id as oppgaveId FROM oppgave o
            JOIN vedtaksperiode v ON v.id = o.vedtak_ref
            JOIN person p ON v.person_ref = p.id
            WHERE p.fødselsnummer = :fodselsnummer AND status = 'AvventerSaksbehandler'::oppgavestatus;
            """.trimIndent(),
            "fodselsnummer" to fødselsnummer,
        ).singleOrNull { it.long("oppgaveId") }
}
