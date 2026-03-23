package no.nav.helse.spesialist.db.dao.api

import no.nav.helse.db.api.OppgaveApiDao
import no.nav.helse.spesialist.api.oppgave.OppgaveForPeriodevisningDto
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedDataSource
import no.nav.helse.spesialist.db.QueryRunner
import java.util.UUID
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

    override fun finnPeriodeoppgave(vedtaksperiodeId: UUID) =
        asSQL(
            """
            SELECT o.id, o.kan_avvises
            FROM oppgave o
            INNER JOIN vedtaksperiode v ON o.vedtak_ref = v.id
            WHERE v.vedtaksperiode_id = :vedtaksperiodeId 
                AND status = 'AvventerSaksbehandler'::oppgavestatus 
            """.trimIndent(),
            "vedtaksperiodeId" to vedtaksperiodeId,
        ).singleOrNull { OppgaveForPeriodevisningDto(id = it.string("id"), kanAvvises = it.boolean("kan_avvises")) }
}
