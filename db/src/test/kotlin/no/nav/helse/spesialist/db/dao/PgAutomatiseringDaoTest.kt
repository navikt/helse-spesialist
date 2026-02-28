package no.nav.helse.spesialist.db.dao

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.UtbetalingId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

internal class PgAutomatiseringDaoTest : AbstractDBIntegrationTest() {
    private val person = opprettPerson()
    private val arbeidsgiver = opprettArbeidsgiver()
    private val utbetalingId = UUID.randomUUID()
    private val vedtaksperiode =
        opprettVedtaksperiode(person, arbeidsgiver).also {
            opprettBehandling(it, utbetalingId = UtbetalingId(utbetalingId))
        }
    private val testhendelse = testhendelse(vedtaksperiodeId = vedtaksperiode.id.value, fødselsnummer = person.id.value, hendelseId = UUID.randomUUID())

    @Test
    fun `lagre og lese false`() {
        automatiseringDao.manuellSaksbehandling(listOf("Problem"), vedtaksperiode.id.value, testhendelse.id, utbetalingId)
        val automatiseringSvar = requireNotNull(automatiseringDao.hentAktivAutomatisering(vedtaksperiode.id.value, testhendelse.id))

        assertEquals(false, automatiseringSvar.automatisert)
        assertEquals(vedtaksperiode.id.value, automatiseringSvar.vedtaksperiodeId)
        assertEquals(testhendelse.id, automatiseringSvar.hendelseId)
        assertEquals(utbetalingId, automatiseringSvar.utbetalingId)
        assertEquals(1, automatiseringSvar.problemer.size)
    }

    @Test
    fun `lagre og lese false uten utbetalingsId`() {
        insertAutomatisering(false, false, vedtaksperiode.id.value, testhendelse.id, listOf("Problem"), null)
        val automatiseringSvar = requireNotNull(automatiseringDao.hentAktivAutomatisering(vedtaksperiode.id.value, testhendelse.id))

        assertEquals(false, automatiseringSvar.automatisert)
        assertEquals(vedtaksperiode.id.value, automatiseringSvar.vedtaksperiodeId)
        assertEquals(testhendelse.id, automatiseringSvar.hendelseId)
        assertEquals(1, automatiseringSvar.problemer.size)
        assertNull(automatiseringSvar.utbetalingId)
    }

    @Test
    fun `lagre og lese true`() {
        automatiseringDao.automatisert(vedtaksperiode.id.value, testhendelse.id, utbetalingId)
        val automatiseringSvar = requireNotNull(automatiseringDao.hentAktivAutomatisering(vedtaksperiode.id.value, testhendelse.id))

        assertEquals(true, automatiseringSvar.automatisert)
        assertEquals(vedtaksperiode.id.value, automatiseringSvar.vedtaksperiodeId)
        assertEquals(testhendelse.id, automatiseringSvar.hendelseId)
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
        testhendelse(hendelseId = hendelseId2, fødselsnummer = person.id.value, vedtaksperiodeId = vedtaksperiode.id.value)

        automatiseringDao.manuellSaksbehandling(listOf("problem"), vedtaksperiode.id.value, testhendelse.id, utbetalingId)
        automatiseringDao.automatisert(vedtaksperiode.id.value, hendelseId2, utbetalingId)

        val automatiseringSvar1 = requireNotNull(automatiseringDao.hentAktivAutomatisering(vedtaksperiode.id.value, testhendelse.id))
        val automatiseringSvar2 = requireNotNull(automatiseringDao.hentAktivAutomatisering(vedtaksperiode.id.value, hendelseId2))

        assertEquals(false, automatiseringSvar1.automatisert)
        assertEquals(1, automatiseringSvar1.problemer.size)
        assertEquals(true, automatiseringSvar2.automatisert)
        assertEquals(0, automatiseringSvar2.problemer.size)
    }

    @Test
    fun `to automatiseringer på samme vedtaksperiode og samme hendelseID kræsjer`() {
        automatiseringDao.manuellSaksbehandling(listOf("problem"), vedtaksperiode.id.value, testhendelse.id, utbetalingId)

        val actualException =
            assertThrows(
                Exception::class.java,
                { automatiseringDao.automatisert(vedtaksperiode.id.value, testhendelse.id, utbetalingId) },
                "Testfeil",
            )
        assertEquals(
            "Det kan bare finnes 1 aktiv automatisering. Klarer ikke opprette ny automatisering for vedtaksperiodeId ${vedtaksperiode.id.value} og hendelseId ${testhendelse.id}.",
            actualException.message,
        )

        val automatiseringSvar = requireNotNull(automatiseringDao.hentAktivAutomatisering(vedtaksperiode.id.value, testhendelse.id))

        assertEquals(false, automatiseringSvar.automatisert)
        assertEquals(vedtaksperiode.id.value, automatiseringSvar.vedtaksperiodeId)
        assertEquals(testhendelse.id, automatiseringSvar.hendelseId)
        assertEquals(1, automatiseringSvar.problemer.size)
    }

    @Test
    fun `ikke stikkprøve hvis manglende innslag i tabell`() {
        assertFalse(automatiseringDao.plukketUtTilStikkprøve(vedtaksperiode.id.value, testhendelse.id))
    }

    @Test
    fun `ikke stikkprøve hvis automatisert`() {
        automatiseringDao.automatisert(vedtaksperiode.id.value, testhendelse.id, utbetalingId)
        assertFalse(automatiseringDao.plukketUtTilStikkprøve(vedtaksperiode.id.value, testhendelse.id))
    }

    @Test
    fun `ikke stikkprøve hvis manglende vedtak`() {
        assertFalse(automatiseringDao.plukketUtTilStikkprøve(UUID.randomUUID(), testhendelse.id))
    }

    @Test
    fun `stikkprøve happy case`() {
        automatiseringDao.stikkprøve(vedtaksperiode.id.value, testhendelse.id, utbetalingId)
        assertTrue(automatiseringDao.plukketUtTilStikkprøve(vedtaksperiode.id.value, testhendelse.id))
    }

    @Test
    fun `inaktivering av automatisering og automatisering_problem fungerer`() {
        automatiseringDao.manuellSaksbehandling(listOf("problem"), vedtaksperiode.id.value, testhendelse.id, utbetalingId)

        val vedtaksperiodeRef = automatiseringDao.finnVedtaksperiode(vedtaksperiode.id.value)!!

        val automatiseringFørInaktivering = automatiseringDao.hentAktivAutomatisering(vedtaksperiode.id.value, testhendelse.id)
        val problemerFørInaktivering = automatiseringDao.finnAktiveProblemer(vedtaksperiodeRef, testhendelse.id)

        automatiseringDao.settAutomatiseringInaktiv(vedtaksperiode.id.value, testhendelse.id)
        automatiseringDao.settAutomatiseringProblemInaktiv(vedtaksperiode.id.value, testhendelse.id)

        val automatiseringEtterInaktivering = automatiseringDao.hentAktivAutomatisering(vedtaksperiode.id.value, testhendelse.id)
        val problemerEtterInaktivering = automatiseringDao.finnAktiveProblemer(vedtaksperiodeRef, testhendelse.id)

        assertNotNull(automatiseringFørInaktivering)
        assertEquals(1, problemerFørInaktivering.size)

        assertNull(automatiseringEtterInaktivering)
        assertEquals(0, problemerEtterInaktivering.size)
    }

    @Test
    fun `kan tvinge auomatisering`() {
        insertForceAutomatisering(vedtaksperiode.id.value)
        assertTrue(automatiseringDao.skalTvingeAutomatisering(vedtaksperiode.id.value))
    }

    private fun insertAutomatisering(
        automatisert: Boolean,
        stikkprøve: Boolean,
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
        problems: List<String> = emptyList(),
        utbetalingId: UUID?,
    ) {
        sessionOf(dataSource).use { session ->
            session.transaction { transactionalSession ->
                transactionalSession.run(
                    queryOf(
                        """
                            INSERT INTO automatisering (vedtaksperiode_ref, hendelse_ref, automatisert, stikkprøve, utbetaling_id)
                            VALUES ((SELECT id FROM vedtaksperiode WHERE vedtaksperiode_id = ?), ?, ?, ?, ?)
                        """,
                        vedtaksperiodeId,
                        hendelseId,
                        automatisert,
                        stikkprøve,
                        utbetalingId,
                    ).asUpdate,
                )

                problems.forEach { problem ->
                    transactionalSession.run(
                        queryOf(
                            "INSERT INTO automatisering_problem(vedtaksperiode_ref, hendelse_ref, problem) VALUES ((SELECT id FROM vedtaksperiode WHERE vedtaksperiode_id = ?), ?, ?)",
                            vedtaksperiodeId,
                            hendelseId,
                            problem,
                        ).asUpdate,
                    )
                }
            }
        }
    }

    private fun insertForceAutomatisering(vedtaksperiodeId: UUID) {
        sessionOf(dataSource).use { session ->
            session.transaction { transactionalSession ->
                transactionalSession.run(
                    queryOf(
                        """
                            INSERT INTO force_automatisering (vedtaksperiode_id)
                            VALUES (?)
                        """,
                        vedtaksperiodeId,
                    ).asUpdate,
                )
            }
        }
    }
}
