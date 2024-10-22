package no.nav.helse.modell.utbetaling

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.HelseDao
import no.nav.helse.db.TransactionalUtbetalingDao
import no.nav.helse.db.UtbetalingRepository
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

class UtbetalingDao(private val dataSource: DataSource) : HelseDao(dataSource), UtbetalingRepository {
    fun erUtbetaltFør(aktørId: String): Boolean {
        @Language("PostgreSQL")
        val statement = """
            SELECT 1 FROM utbetaling u 
            JOIN utbetaling_id ui ON u.utbetaling_id_ref = ui.id
            INNER JOIN person p ON ui.person_ref = p.id
            WHERE u.status = 'UTBETALT' AND p.aktor_id = :aktor_id
        """
        return sessionOf(dataSource).use {
            it.run(queryOf(statement, mapOf("aktor_id" to aktørId.toLong())).map { it }.asList).isNotEmpty()
        }
    }

    override fun finnUtbetalingIdRef(utbetalingId: UUID): Long? {
        sessionOf(dataSource).use { session ->
            return TransactionalUtbetalingDao(session).finnUtbetalingIdRef(utbetalingId)
        }
    }

    override fun nyUtbetalingStatus(
        utbetalingIdRef: Long,
        status: Utbetalingsstatus,
        opprettet: LocalDateTime,
        json: String,
    ) {
        sessionOf(dataSource).use { session ->
            TransactionalUtbetalingDao(session).nyUtbetalingStatus(utbetalingIdRef, status, opprettet, json)
        }
    }

    override fun erUtbetalingForkastet(utbetalingId: UUID): Boolean =
        sessionOf(dataSource).use { TransactionalUtbetalingDao(it).erUtbetalingForkastet(utbetalingId) }

    override fun opprettUtbetalingId(
        utbetalingId: UUID,
        fødselsnummer: String,
        orgnummer: String,
        type: Utbetalingtype,
        opprettet: LocalDateTime,
        arbeidsgiverFagsystemIdRef: Long,
        personFagsystemIdRef: Long,
        arbeidsgiverbeløp: Int,
        personbeløp: Int,
    ): Long {
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            return TransactionalUtbetalingDao(
                session,
            ).opprettUtbetalingId(utbetalingId, fødselsnummer, orgnummer, type, opprettet, arbeidsgiverFagsystemIdRef, personFagsystemIdRef, arbeidsgiverbeløp, personbeløp)
        }
    }

    override fun nyttOppdrag(
        fagsystemId: String,
        mottaker: String,
    ): Long? {
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            return TransactionalUtbetalingDao(session).nyttOppdrag(fagsystemId, mottaker)
        }
    }

    override fun opprettKobling(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ) {
        sessionOf(dataSource).use { session ->
            TransactionalUtbetalingDao(session).opprettKobling(vedtaksperiodeId, utbetalingId)
        }
    }

    override fun fjernKobling(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ) {
        sessionOf(dataSource).use { session ->
            TransactionalUtbetalingDao(session).fjernKobling(vedtaksperiodeId, utbetalingId)
        }
    }

    data class TidligereUtbetalingerForVedtaksperiodeDto(
        val utbetalingId: UUID,
        val id: Int,
        val utbetalingsstatus: Utbetalingsstatus,
    )

    override fun hentUtbetaling(utbetalingId: UUID): Utbetaling {
        return sessionOf(dataSource).use { session ->
            TransactionalUtbetalingDao(session).hentUtbetaling(utbetalingId)
        }
    }

    override fun utbetalingFor(utbetalingId: UUID): Utbetaling? {
        return sessionOf(dataSource).use { session ->
            TransactionalUtbetalingDao(session).utbetalingFor(utbetalingId)
        }
    }

    internal fun utbetalingFor(oppgaveId: Long): Utbetaling? {
        @Language("PostgreSQL")
        val query =
            "SELECT utbetaling_id, arbeidsgiverbeløp, personbeløp, type FROM utbetaling_id u WHERE u.utbetaling_id = (SELECT utbetaling_id FROM oppgave o WHERE o.id = :oppgave_id)"
        return sessionOf(dataSource).use { session ->
            session.run(
                queryOf(query, mapOf("oppgave_id" to oppgaveId)).map {
                    Utbetaling(
                        it.uuid("utbetaling_id"),
                        it.int("arbeidsgiverbeløp"),
                        it.int("personbeløp"),
                        enumValueOf(it.string("type")),
                    )
                }.asSingle,
            )
        }
    }

    override fun utbetalingerForVedtaksperiode(vedtaksperiodeId: UUID): List<TidligereUtbetalingerForVedtaksperiodeDto> {
        return sessionOf(dataSource).use { session ->
            TransactionalUtbetalingDao(session).utbetalingerForVedtaksperiode(vedtaksperiodeId)
        }
    }

    override fun sisteUtbetalingIdFor(fødselsnummer: String): UUID? {
        return sessionOf(dataSource).use { session ->
            TransactionalUtbetalingDao(session).sisteUtbetalingIdFor(fødselsnummer)
        }
    }
}
