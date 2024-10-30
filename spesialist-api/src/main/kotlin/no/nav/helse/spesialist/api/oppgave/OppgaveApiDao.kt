package no.nav.helse.spesialist.api.oppgave

import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.db.MedDataSource
import no.nav.helse.db.QueryRunner
import java.util.UUID
import javax.sql.DataSource

class OppgaveApiDao(dataSource: DataSource) : QueryRunner by MedDataSource(dataSource) {
    fun finnOppgaveId(fødselsnummer: String) =
        asSQL(
            """
            SELECT o.id as oppgaveId FROM oppgave o
            JOIN vedtak v ON v.id = o.vedtak_ref
            JOIN person p ON v.person_ref = p.id
            WHERE p.fodselsnummer = :fodselsnummer AND status = 'AvventerSaksbehandler'::oppgavestatus;
            """.trimIndent(),
            "fodselsnummer" to fødselsnummer.toLong(),
        ).singleOrNull { it.long("oppgaveId") }

    fun finnPeriodeoppgave(vedtaksperiodeId: UUID) =
        asSQL(
            """
            SELECT o.id, o.kan_avvises
            FROM oppgave o
            INNER JOIN vedtak v ON o.vedtak_ref = v.id
            WHERE v.vedtaksperiode_id = :vedtaksperiodeId 
                AND status = 'AvventerSaksbehandler'::oppgavestatus 
            """.trimIndent(),
            "vedtaksperiodeId" to vedtaksperiodeId,
        ).singleOrNull { OppgaveForPeriodevisningDto(id = it.string("id"), kanAvvises = it.boolean("kan_avvises")) }

    fun finnFødselsnummer(oppgaveId: Long) =
        asSQL(
            """
            SELECT fodselsnummer from person
            INNER JOIN vedtak v on person.id = v.person_ref
            INNER JOIN oppgave o on v.id = o.vedtak_ref
            WHERE o.id = :oppgaveId
            """.trimIndent(),
            "oppgaveId" to oppgaveId,
        ).single { it.long("fodselsnummer").toFødselsnummer() }

    companion object {
        private fun Long.toFødselsnummer() = if (this < 10000000000) "0$this" else this.toString()
    }
}
