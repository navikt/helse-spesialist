package no.nav.helse.modell.automatisering

import DatabaseIntegrationTest
import kotliquery.queryOf
import kotliquery.sessionOf
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.postgresql.util.PSQLException
import java.util.*

internal class AutomatiseringDaoTest : DatabaseIntegrationTest() {

    @BeforeEach
    fun setup() {
        testhendelse()
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
    }

    @Test
    fun `lagre og lese false`() {
        automatiseringDao.manuellSaksbehandling(listOf("Problem"), VEDTAKSPERIODE, HENDELSE_ID, UTBETALING_ID)
        val automatiseringSvar = requireNotNull(automatiseringDao.hentAutomatisering(VEDTAKSPERIODE, HENDELSE_ID))

        assertEquals(false, automatiseringSvar.automatisert)
        assertEquals(VEDTAKSPERIODE, automatiseringSvar.vedtaksperiodeId)
        assertEquals(HENDELSE_ID, automatiseringSvar.hendelseId)
        assertEquals(1, automatiseringSvar.problemer.size)
    }

    @Test
    fun `lagre og lese false basert på utbetalingsId`() {
        automatiseringDao.manuellSaksbehandling(listOf("Problem"), VEDTAKSPERIODE, HENDELSE_ID, UTBETALING_ID)
        val automatiseringSvar = requireNotNull(automatiseringDao.hentAutomatisering( UTBETALING_ID))

        assertEquals(false, automatiseringSvar.automatisert)
        assertEquals(VEDTAKSPERIODE, automatiseringSvar.vedtaksperiodeId)
        assertEquals(HENDELSE_ID, automatiseringSvar.hendelseId)
        assertEquals(1, automatiseringSvar.problemer.size)
    }
    @Test
    fun `lagre og lese false uten utbetalingsId`() {
        insertAutomatisering(false, false, VEDTAKSPERIODE, HENDELSE_ID,listOf("Problem"), null)
        val automatiseringSvar = requireNotNull(automatiseringDao.hentAutomatisering( VEDTAKSPERIODE, HENDELSE_ID))

        assertEquals(false, automatiseringSvar.automatisert)
        assertEquals(VEDTAKSPERIODE, automatiseringSvar.vedtaksperiodeId)
        assertEquals(HENDELSE_ID, automatiseringSvar.hendelseId)
        assertEquals(1, automatiseringSvar.problemer.size)
        assertNull(automatiseringSvar.utbetalingId)

    }

    @Test
    fun `lagre og lese true`() {
        automatiseringDao.automatisert(VEDTAKSPERIODE, HENDELSE_ID, UTBETALING_ID)
        val automatiseringSvar = requireNotNull(automatiseringDao.hentAutomatisering(VEDTAKSPERIODE, HENDELSE_ID))

        assertEquals(true, automatiseringSvar.automatisert)
        assertEquals(VEDTAKSPERIODE, automatiseringSvar.vedtaksperiodeId)
        assertEquals(HENDELSE_ID, automatiseringSvar.hendelseId)
        assertEquals(0, automatiseringSvar.problemer.size)
    }

    @Test
    fun `finner ikke automatisering`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val hendelseId = UUID.randomUUID()
        assertNull(automatiseringDao.hentAutomatisering(vedtaksperiodeId, hendelseId))
    }

    @Test
    fun `lagre to automatiseringer på samme vedtaksperiode`() {
        val hendelseId2 = UUID.randomUUID()
        testhendelse(hendelseId = hendelseId2)

        automatiseringDao.manuellSaksbehandling(listOf("problem"), VEDTAKSPERIODE, HENDELSE_ID, UTBETALING_ID)
        automatiseringDao.automatisert(VEDTAKSPERIODE, hendelseId2, UTBETALING_ID)

        val automatiseringSvar1 = requireNotNull(automatiseringDao.hentAutomatisering(VEDTAKSPERIODE, HENDELSE_ID))
        val automatiseringSvar2 = requireNotNull(automatiseringDao.hentAutomatisering(VEDTAKSPERIODE, hendelseId2))

        assertEquals(false, automatiseringSvar1.automatisert)
        assertEquals(1, automatiseringSvar1.problemer.size)
        assertEquals(true, automatiseringSvar2.automatisert)
        assertEquals(0, automatiseringSvar2.problemer.size)
    }

    @Test
    fun `to automatiseringer på samme vedtaksperiode og samme hendelseID kræsjer`() {
        automatiseringDao.manuellSaksbehandling(listOf("problem"), VEDTAKSPERIODE, HENDELSE_ID, UTBETALING_ID)
        assertThrows<PSQLException> { automatiseringDao.automatisert(VEDTAKSPERIODE, HENDELSE_ID, UTBETALING_ID) }

        val automatiseringSvar = requireNotNull(automatiseringDao.hentAutomatisering(VEDTAKSPERIODE, HENDELSE_ID))

        assertEquals(false, automatiseringSvar.automatisert)
        assertEquals(VEDTAKSPERIODE, automatiseringSvar.vedtaksperiodeId)
        assertEquals(HENDELSE_ID, automatiseringSvar.hendelseId)
        assertEquals(1, automatiseringSvar.problemer.size)
    }

    @Test
    fun `ikke stikkprøve hvis manglende innslag i tabell`() {
        assertFalse(automatiseringDao.plukketUtTilStikkprøve(VEDTAKSPERIODE, HENDELSE_ID))
    }

    @Test
    fun `ikke stikkprøve hvis automatisert`() {
        automatiseringDao.automatisert(VEDTAKSPERIODE, HENDELSE_ID, UTBETALING_ID)
        assertFalse(automatiseringDao.plukketUtTilStikkprøve(VEDTAKSPERIODE, HENDELSE_ID))
    }

    @Test
    fun `ikke stikkprøve hvis manglende vedtak`() {
        assertFalse(automatiseringDao.plukketUtTilStikkprøve(UUID.randomUUID(), HENDELSE_ID))
    }

    @Test
    fun `stikkprøve happy case`() {
        automatiseringDao.stikkprøve(VEDTAKSPERIODE, HENDELSE_ID, UTBETALING_ID)
        assertTrue(automatiseringDao.plukketUtTilStikkprøve(VEDTAKSPERIODE, HENDELSE_ID))
    }


    private fun insertAutomatisering(automatisert: Boolean, stikkprøve: Boolean, vedtaksperiodeId: UUID, hendelseId: UUID, problems: List<String> = emptyList(), utbetalingId: UUID?) {
        sessionOf(dataSource).use { session ->
            session.transaction { transactionalSession ->
                transactionalSession.run(
                    queryOf(
                        """
                            INSERT INTO automatisering (vedtaksperiode_ref, hendelse_ref, automatisert, stikkprøve, utbetaling_id)
                            VALUES ((SELECT id FROM vedtak WHERE vedtaksperiode_id = ?), ?, ?, ?, ?)
                        """,
                        vedtaksperiodeId,
                        hendelseId,
                        automatisert,
                        stikkprøve,
                        utbetalingId
                    ).asUpdate
                )

                problems.forEach { problem ->
                    transactionalSession.run(
                        queryOf(
                            "INSERT INTO automatisering_problem(vedtaksperiode_ref, hendelse_ref, problem) VALUES ((SELECT id FROM vedtak WHERE vedtaksperiode_id = ?), ?, ?)",
                            vedtaksperiodeId, hendelseId, problem
                        ).asUpdate
                    )
                }
            }
        }
    }
}
