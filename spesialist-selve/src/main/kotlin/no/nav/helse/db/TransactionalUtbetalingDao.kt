package no.nav.helse.db

import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingtype
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.UUID
import javax.naming.OperationNotSupportedException

class TransactionalUtbetalingDao(private val transactionalSession: TransactionalSession) : UtbetalingRepository {
    override fun sisteUtbetalingIdFor(fødselsnummer: String): UUID? {
        @Language("PostgreSQL")
        val query =
            """
            select ui.utbetaling_id from utbetaling_id ui 
            join person p on ui.person_ref = p.id 
            where p.fodselsnummer = :fnr
            order by ui.id desc
            limit 1;
            """.trimIndent()
        return transactionalSession.run(
            queryOf(query, mapOf("fnr" to fødselsnummer.toLong())).map {
                it.uuid("utbetaling_id")
            }.asSingle,
        )
    }

    override fun finnUtbetalingIdRef(utbetalingId: UUID): Long = throw OperationNotSupportedException()

    override fun hentUtbetaling(utbetalingId: UUID): Utbetaling =
        checkNotNull(utbetalingFor(utbetalingId)) { "Finner ikke utbetaling, utbetalingId=$utbetalingId" }

    override fun utbetalingFor(utbetalingId: UUID): Utbetaling? {
        @Language("PostgreSQL")
        val query =
            "SELECT arbeidsgiverbeløp, personbeløp, type FROM utbetaling_id u WHERE u.utbetaling_id = :utbetaling_id"
        return transactionalSession.run(
            queryOf(query, mapOf("utbetaling_id" to utbetalingId)).map {
                Utbetaling(
                    utbetalingId,
                    it.int("arbeidsgiverbeløp"),
                    it.int("personbeløp"),
                    enumValueOf(it.string("type")),
                )
            }.asSingle,
        )
    }

    override fun nyUtbetalingStatus(
        utbetalingIdRef: Long,
        status: Utbetalingsstatus,
        opprettet: LocalDateTime,
        json: String,
    ) {
        throw OperationNotSupportedException()
    }

    override fun nyttOppdrag(
        fagsystemId: String,
        mottaker: String,
    ): Long = throw OperationNotSupportedException()

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
    ): Long = throw OperationNotSupportedException()

    override fun opprettKobling(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ) {
        @Language("PostgreSQL")
        val statement = """
            INSERT INTO vedtaksperiode_utbetaling_id(vedtaksperiode_id, utbetaling_id) VALUES (?, ?)
            ON CONFLICT DO NOTHING
        """
        transactionalSession.run(queryOf(statement, vedtaksperiodeId, utbetalingId).asUpdate)
    }

    override fun fjernKobling(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ) {
        @Language("PostgreSQL")
        val statement = "DELETE FROM vedtaksperiode_utbetaling_id WHERE utbetaling_id = ? AND vedtaksperiode_id = ?"
        transactionalSession.run(queryOf(statement, utbetalingId, vedtaksperiodeId).asUpdate)
    }

    override fun utbetalingerForVedtaksperiode(vedtaksperiodeId: UUID): List<UtbetalingDao.TidligereUtbetalingerForVedtaksperiodeDto> {
        throw OperationNotSupportedException()
    }

    override fun erUtbetalingForkastet(utbetalingId: UUID): Boolean = throw OperationNotSupportedException()
}
