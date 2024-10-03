package no.nav.helse.db

import kotliquery.TransactionalSession
import kotliquery.queryOf
import org.intellij.lang.annotations.Language
import java.util.UUID
import javax.naming.OperationNotSupportedException

class TransactionalOppgaveDao(private val transactionalSession: TransactionalSession) : OppgaveRepository {
    override fun finnOppgave(id: Long): OppgaveFraDatabase = throw OperationNotSupportedException()

    override fun finnOppgaveId(fødselsnummer: String): Long = throw OperationNotSupportedException()

    override fun finnVedtaksperiodeId(fødselsnummer: String): UUID = throw OperationNotSupportedException()

    override fun harGyldigOppgave(utbetalingId: UUID): Boolean = throw OperationNotSupportedException()

    override fun finnHendelseId(id: Long): UUID = throw OperationNotSupportedException()

    override fun invaliderOppgaveFor(fødselsnummer: String) {
        @Language("PostgreSQL")
        val statement =
            """
            UPDATE oppgave o
            SET status = 'Invalidert'
            FROM oppgave o2
            JOIN vedtak v on v.id = o2.vedtak_ref
            JOIN person p on v.person_ref = p.id
            WHERE p.fodselsnummer = :fodselsnummer
            and o.id = o2.id
            AND o.status = 'AvventerSaksbehandler'::oppgavestatus;             
            """.trimIndent()
        transactionalSession.run(
            queryOf(
                statement,
                mapOf("fodselsnummer" to fødselsnummer.toLong()),
            ).asUpdate,
        )
    }
}
