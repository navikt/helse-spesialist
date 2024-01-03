package no.nav.helse.spesialist.api.oppgave

import java.util.UUID
import javax.sql.DataSource
import no.nav.helse.HelseDao

class OppgaveApiDao(dataSource: DataSource) : HelseDao(dataSource) {

    fun lagreBehandlingsreferanse(oppgaveId: Long, behandlingId: UUID) {
        asSQL(
            "INSERT INTO oppgave_behandling_kobling(oppgave_id, behandling_id) VALUES (:oppgaveId, :behandlingId)",
            mapOf("oppgaveId" to oppgaveId, "behandlingId" to behandlingId)
        ).update()
    }

    fun finnOppgaveId(vedtaksperiodeId: UUID) = asSQL(
        """ SELECT id FROM oppgave
            WHERE vedtak_ref =
                (SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId)
            AND status = 'AvventerSaksbehandler'::oppgavestatus
        """,
        mapOf("vedtaksperiodeId" to vedtaksperiodeId)
    ).single { it.long("id") }

    fun finnOppgaveId(fødselsnummer: String) = asSQL(
        """ SELECT o.id as oppgaveId FROM oppgave o
            JOIN vedtak v ON v.id = o.vedtak_ref
            JOIN person p ON v.person_ref = p.id
            WHERE p.fodselsnummer = :fodselsnummer AND status = 'AvventerSaksbehandler'::oppgavestatus;
        """,
        mapOf("fodselsnummer" to fødselsnummer.toLong())
    ).single { it.long("oppgaveId") }

    fun finnPeriodeoppgave(vedtaksperiodeId: UUID) = asSQL(
        """ SELECT o.id, o.kan_avvises
            FROM oppgave o
            INNER JOIN vedtak v ON o.vedtak_ref = v.id
            WHERE v.vedtaksperiode_id = :vedtaksperiodeId 
                AND status = 'AvventerSaksbehandler'::oppgavestatus 
        """,
        mapOf("vedtaksperiodeId" to vedtaksperiodeId)
    ).single { OppgaveForPeriodevisningDto(id = it.string("id"), kanAvvises = it.boolean("kan_avvises")) }

    fun finnFødselsnummer(oppgaveId: Long) = requireNotNull(asSQL(
        """ SELECT fodselsnummer from person
            INNER JOIN vedtak v on person.id = v.person_ref
            INNER JOIN oppgave o on v.id = o.vedtak_ref
            WHERE o.id = :oppgaveId
        """,
        mapOf("oppgaveId" to oppgaveId)
    ).single { it.long("fodselsnummer").toFødselsnummer() })

    companion object {
        private fun Long.toFødselsnummer() = if (this < 10000000000) "0$this" else this.toString()
    }
}
