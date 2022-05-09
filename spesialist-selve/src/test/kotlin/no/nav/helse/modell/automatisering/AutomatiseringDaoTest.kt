package no.nav.helse.modell.automatisering

import DatabaseIntegrationTest
import java.time.LocalDateTime
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

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
        val automatiseringSvar = requireNotNull(automatiseringDao.hentAktivAutomatisering(VEDTAKSPERIODE, HENDELSE_ID))

        assertEquals(false, automatiseringSvar.automatisert)
        assertEquals(VEDTAKSPERIODE, automatiseringSvar.vedtaksperiodeId)
        assertEquals(HENDELSE_ID, automatiseringSvar.hendelseId)
        assertEquals(UTBETALING_ID, automatiseringSvar.utbetalingId)
        assertEquals(1, automatiseringSvar.problemer.size)
    }


    @Test
    fun `lagre og lese false uten utbetalingsId`() {
        insertAutomatisering(false, false, VEDTAKSPERIODE, HENDELSE_ID, listOf("Problem"), null)
        val automatiseringSvar = requireNotNull(automatiseringDao.hentAktivAutomatisering(VEDTAKSPERIODE, HENDELSE_ID))

        assertEquals(false, automatiseringSvar.automatisert)
        assertEquals(VEDTAKSPERIODE, automatiseringSvar.vedtaksperiodeId)
        assertEquals(HENDELSE_ID, automatiseringSvar.hendelseId)
        assertEquals(1, automatiseringSvar.problemer.size)
        assertNull(automatiseringSvar.utbetalingId)
    }

    @Test
    fun `lagre og lese true`() {
        automatiseringDao.automatisert(VEDTAKSPERIODE, HENDELSE_ID, UTBETALING_ID)
        val automatiseringSvar = requireNotNull(automatiseringDao.hentAktivAutomatisering(VEDTAKSPERIODE, HENDELSE_ID))

        assertEquals(true, automatiseringSvar.automatisert)
        assertEquals(VEDTAKSPERIODE, automatiseringSvar.vedtaksperiodeId)
        assertEquals(HENDELSE_ID, automatiseringSvar.hendelseId)
        assertEquals(0, automatiseringSvar.problemer.size)
    }

    @Test
    fun `finner ikke automatisering`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val hendelseId = UUID.randomUUID()
        assertNull(automatiseringDao.hentAktivAutomatisering(vedtaksperiodeId, hendelseId))
    }

    @Test
    fun `lagre to automatiseringer på samme vedtaksperiode`() {
        val hendelseId2 = UUID.randomUUID()
        testhendelse(hendelseId = hendelseId2)

        automatiseringDao.manuellSaksbehandling(listOf("problem"), VEDTAKSPERIODE, HENDELSE_ID, UTBETALING_ID)
        automatiseringDao.automatisert(VEDTAKSPERIODE, hendelseId2, UTBETALING_ID)

        val automatiseringSvar1 = requireNotNull(automatiseringDao.hentAktivAutomatisering(VEDTAKSPERIODE, HENDELSE_ID))
        val automatiseringSvar2 = requireNotNull(automatiseringDao.hentAktivAutomatisering(VEDTAKSPERIODE, hendelseId2))

        assertEquals(false, automatiseringSvar1.automatisert)
        assertEquals(1, automatiseringSvar1.problemer.size)
        assertEquals(true, automatiseringSvar2.automatisert)
        assertEquals(0, automatiseringSvar2.problemer.size)
    }

    @Test
    fun `to automatiseringer på samme vedtaksperiode og samme hendelseID kræsjer`() {
        automatiseringDao.manuellSaksbehandling(listOf("problem"), VEDTAKSPERIODE, HENDELSE_ID, UTBETALING_ID)

        val actualException = assertFailsWith<Exception>(block = { automatiseringDao.automatisert(VEDTAKSPERIODE, HENDELSE_ID, UTBETALING_ID) })
        assertEquals("Det kan bare finnes 1 aktiv automatisering. Klarer ikke opprette ny automatisering for vedtaksperiodeId $VEDTAKSPERIODE og hendelseId $HENDELSE_ID.", actualException.message)

        val automatiseringSvar = requireNotNull(automatiseringDao.hentAktivAutomatisering(VEDTAKSPERIODE, HENDELSE_ID))

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

    @Test
    fun `inaktivering av automatisering og automatisering_problem fungerer`() {
        automatiseringDao.manuellSaksbehandling(listOf("problem"), VEDTAKSPERIODE, HENDELSE_ID, UTBETALING_ID)

        val automatiseringFørInaktivering = automatiseringDao.hentAktivAutomatisering(VEDTAKSPERIODE, HENDELSE_ID)
        val vedtaksperiodeRefFørInaktivering = automatiseringDao.finnVedtaksperiode(VEDTAKSPERIODE)
        val problemerFørInaktivering = automatiseringDao.finnAktiveProblemer(vedtaksperiodeRefFørInaktivering!!, HENDELSE_ID)

        automatiseringDao.settAutomatiseringInaktiv(VEDTAKSPERIODE, HENDELSE_ID, LocalDateTime.now().minusSeconds(2))
        automatiseringDao.settAutomatiseringProblemInaktiv(VEDTAKSPERIODE, HENDELSE_ID, LocalDateTime.now().minusSeconds(2))

        val automatiseringEtterInaktivering = automatiseringDao.hentAktivAutomatisering(VEDTAKSPERIODE, HENDELSE_ID)
        val vedtaksperiodeRefEtterInaktivering = automatiseringDao.finnVedtaksperiode(VEDTAKSPERIODE)
        val problemerEtterInaktivering = automatiseringDao.finnAktiveProblemer(vedtaksperiodeRefEtterInaktivering!!, HENDELSE_ID)

        assertNotNull(automatiseringFørInaktivering)
        assertEquals(1, problemerFørInaktivering.size)

        assertNull(automatiseringEtterInaktivering)
        assertEquals(0, problemerEtterInaktivering.size)
    }

    private fun insertAutomatisering(
        automatisert: Boolean,
        stikkprøve: Boolean,
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
        problems: List<String> = emptyList(),
        utbetalingId: UUID?
    ) {
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
