package no.nav.helse.modell.vedtaksperiode

import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.AbstractDatabaseTest
import no.nav.helse.januar
import no.nav.helse.modell.varsel.ActualVarselRepository
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

internal class GenerasjonRepositoryTest : AbstractDatabaseTest() {

    private val repository = ActualGenerasjonRepository(dataSource)
    private val varselRepository = ActualVarselRepository(dataSource)

    @Test
    fun `kan opprette første generasjon`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val hendelseId = UUID.randomUUID()

        repository.førsteGenerasjonOpprettet(UUID.randomUUID(), vedtaksperiodeId, hendelseId, 1.januar, 31.januar, 1.januar)
        
        assertGenerasjon(vedtaksperiodeId, hendelseId)
    }

    @Test
    fun `hente ut liste av vedtaksperioder`() {
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val generasjonId1 = UUID.randomUUID()
        val generasjonId2 = UUID.randomUUID()

        repository.førsteGenerasjonOpprettet(generasjonId1, vedtaksperiodeId1, UUID.randomUUID(), 1.januar, 31.januar, 1.januar)
        repository.førsteGenerasjonOpprettet(generasjonId2, vedtaksperiodeId2, UUID.randomUUID(), 1.januar, 31.januar, 1.januar)

        val perioder = repository.finnVedtaksperioder(listOf(vedtaksperiodeId1, vedtaksperiodeId2))
        assertEquals(2, perioder.size)
        assertEquals(
            Vedtaksperiode(vedtaksperiodeId1, Generasjon(generasjonId1, vedtaksperiodeId1, null, false, 1.januar, Periode(1.januar, 31.januar), emptySet())),
            perioder[0]
        )
        assertEquals(
            Vedtaksperiode(vedtaksperiodeId2, Generasjon(generasjonId2, vedtaksperiodeId2, null, false, 1.januar, Periode(1.januar, 31.januar), emptySet())),
            perioder[1]
        )
    }

    @Test
    fun `hente kun ut vedtaksperioder der det finnes en generasjon for perioden`() {
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val generasjonId1 = UUID.randomUUID()

        repository.førsteGenerasjonOpprettet(generasjonId1, vedtaksperiodeId1, UUID.randomUUID(), 1.januar, 31.januar, 1.januar)

        val perioder = repository.finnVedtaksperioder(listOf(vedtaksperiodeId1, vedtaksperiodeId2))
        assertEquals(1, perioder.size)
        assertEquals(
            Vedtaksperiode(vedtaksperiodeId1, Generasjon(generasjonId1, vedtaksperiodeId1, null, false, 1.januar, Periode(1.januar, 31.januar), emptySet())),
            perioder[0]
        )
    }

    @Test
    fun `kan ikke opprette FØRSTE generasjon når det eksisterer generasjoner fra før av`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val vedtaksperiodeOpprettet1 = UUID.randomUUID()
        val vedtaksperiodeOpprettet2 = UUID.randomUUID()

        repository.førsteGenerasjonOpprettet(UUID.randomUUID(), vedtaksperiodeId, vedtaksperiodeOpprettet1, 1.januar, 31.januar, 1.januar)
        repository.førsteGenerasjonOpprettet(UUID.randomUUID(), vedtaksperiodeId, vedtaksperiodeOpprettet2, 1.januar, 31.januar, 1.januar)

        assertGenerasjon(vedtaksperiodeId, vedtaksperiodeOpprettet1)
        assertIngenGenerasjon(vedtaksperiodeId, vedtaksperiodeOpprettet2)
    }

    @Test
    fun `kan opprette neste generasjon`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val vedtaksperiodeOpprettet = UUID.randomUUID()
        val vedtaksperiodeEndret = UUID.randomUUID()
        val vedtakFattet = UUID.randomUUID()
        val førsteGenerasjonId = UUID.randomUUID()
        val andreGenerasjonId = UUID.randomUUID()

        val generasjon = Generasjon(førsteGenerasjonId, vedtaksperiodeId, null, false, 1.januar, Periode(1.januar, 31.januar), emptySet())
        generasjon.registrer(repository)
        generasjon.opprettFørste(vedtaksperiodeOpprettet)
        generasjon.håndterVedtakFattet(vedtakFattet)
        generasjon.håndterNyGenerasjon(varselRepository, vedtaksperiodeEndret, andreGenerasjonId)

        assertLåstGenerasjon(førsteGenerasjonId, vedtakFattet)
        assertUlåstGenerasjon(andreGenerasjonId)
    }

    @Test
    fun `kan ikke opprette ny generasjon når tidligere er ulåst`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val vedtaksperiodeOpprettet = UUID.randomUUID()
        val vedtaksperiodeEndret = UUID.randomUUID()
        val generasjon = Generasjon(UUID.randomUUID(), vedtaksperiodeId, null, false, 1.januar, Periode(1.januar, 31.januar), emptySet())
        generasjon.registrer(repository)
        generasjon.opprettFørste(vedtaksperiodeOpprettet)
        generasjon.håndterNyGenerasjon(varselRepository, vedtaksperiodeEndret)

        assertGenerasjon(vedtaksperiodeId, vedtaksperiodeOpprettet)
        assertIngenGenerasjon(vedtaksperiodeId, vedtaksperiodeEndret)
    }

    @Test
    fun `kan knytte utbetalingId til generasjon`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()

        val generasjon = Generasjon(generasjonId, vedtaksperiodeId, null, false, 1.januar, Periode(1.januar, 31.januar), emptySet())
        generasjon.registrer(repository)
        generasjon.opprettFørste(UUID.randomUUID())
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId, varselRepository)

        assertUtbetaling(generasjonId, utbetalingId)
    }

    @Test
    fun `kan ikke knytte utbetalingId til låst generasjon som ikke har utbetalingId`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()

        val generasjon = Generasjon(generasjonId, vedtaksperiodeId, null, false, 1.januar, Periode(1.januar, 31.januar), emptySet())
        generasjon.registrer(repository)
        generasjon.opprettFørste(UUID.randomUUID())
        generasjon.håndterVedtakFattet(UUID.randomUUID())
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId, varselRepository)

        assertUtbetaling(generasjonId, null)
    }

    @Test
    fun `kan ikke knytte utbetalingId til låst generasjon som har utbetalingId fra før`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val gammel = UUID.randomUUID()
        val ny = UUID.randomUUID()

        val generasjon = Generasjon(generasjonId, vedtaksperiodeId, null, false, 1.januar, Periode(1.januar, 31.januar), emptySet())
        generasjon.registrer(repository)
        generasjon.opprettFørste(UUID.randomUUID())
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), gammel, varselRepository)
        generasjon.håndterVedtakFattet(UUID.randomUUID())
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), ny, varselRepository)

        assertUtbetaling(generasjonId, gammel)
    }

    @Test
    fun `finner siste generasjon for en periode`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val hendelseId = UUID.randomUUID()

        repository.førsteGenerasjonOpprettet(UUID.randomUUID(), vedtaksperiodeId, hendelseId, 1.januar, 31.januar, 1.januar)
        assertGenerasjon(vedtaksperiodeId, hendelseId)
        assertDoesNotThrow {
            repository.sisteFor(vedtaksperiodeId)
        }
    }

    @Test
    fun `kaster exception dersom vi ikke finner generasjon`() {
        val vedtaksperiodeId = UUID.randomUUID()

        assertThrows<IllegalStateException> {
            repository.sisteFor(vedtaksperiodeId)
        }
    }

    @Test
    fun `finner alle generasjoner knyttet til en utbetalingId`() {
        val generasjonIdV1 = UUID.randomUUID()
        val generasjonIdV2 = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()

        val generasjonV1 = Generasjon(generasjonIdV1, UUID.randomUUID(), utbetalingId, false, 1.januar, Periode(1.januar, 31.januar), emptySet())
        val generasjonV2 = Generasjon(generasjonIdV2, UUID.randomUUID(), utbetalingId, false, 1.januar, Periode(1.januar, 31.januar), emptySet())
        generasjonV1.registrer(repository)
        generasjonV2.registrer(repository)
        generasjonV1.opprettFørste(UUID.randomUUID())
        generasjonV2.opprettFørste(UUID.randomUUID())
        generasjonV1.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId, varselRepository)
        generasjonV2.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId, varselRepository)

        assertEquals(2, repository.tilhørendeFor(utbetalingId).size)
    }

    @Test
    fun `Fjern utbetalingId når utbetaling blir forkastet`() {
        val generasjonId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()

        val generasjon = Generasjon(generasjonId, vedtaksperiodeId, null, false, 1.januar, Periode(1.januar, 31.januar), emptySet())
        generasjon.registrer(repository)
        generasjon.opprettFørste(UUID.randomUUID())
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId, varselRepository)
        assertEquals(1, repository.tilhørendeFor(utbetalingId).size)

        generasjon.invaliderUtbetaling(utbetalingId)
        assertEquals(Generasjon(generasjonId, vedtaksperiodeId, null, false, 1.januar, Periode(1.januar, 31.januar), emptySet()), generasjon)
        assertEquals(0, repository.tilhørendeFor(utbetalingId).size)
    }

    private fun assertGenerasjon(vedtaksperiodeId: UUID, hendelseId: UUID) {
        val generasjon = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT id FROM selve_vedtaksperiode_generasjon WHERE vedtaksperiode_id = ? AND opprettet_av_hendelse = ?;"

            session.run(queryOf(query, vedtaksperiodeId, hendelseId).map {
                it.long(1)
            }.asSingle)
        }
        assertNotNull(generasjon)
    }

    private fun assertLåstGenerasjon(generasjonId: UUID, hendelseId: UUID) {
        val generasjon = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT id FROM selve_vedtaksperiode_generasjon WHERE unik_id = ? AND låst_av_hendelse = ?;"

            session.run(queryOf(query, generasjonId, hendelseId).map {
                it.long(1)
            }.asSingle)
        }
        assertNotNull(generasjon)
    }

    private fun assertUlåstGenerasjon(generasjonId: UUID) {
        val generasjon = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT id FROM selve_vedtaksperiode_generasjon WHERE unik_id = ? AND låst = false;"

            session.run(queryOf(query, generasjonId).map {
                it.long(1)
            }.asSingle)
        }
        assertNotNull(generasjon)
    }

    private fun assertIngenGenerasjon(vedtaksperiodeId: UUID, hendelseId: UUID) {
        val generasjon = sessionOf(dataSource).use {session ->
            @Language("PostgreSQL")
            val query = "SELECT id FROM selve_vedtaksperiode_generasjon WHERE vedtaksperiode_id = ? AND opprettet_av_hendelse = ?;"

            session.run(queryOf(query, vedtaksperiodeId, hendelseId).map {
                it.long(1)
            }.asSingle)
        }
        assertNull(generasjon)
    }

    private fun assertUtbetaling(generasjonId: UUID, forventetUtbetalingId: UUID?) {
        val utbetalingId = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query =
                "SELECT utbetaling_id FROM selve_vedtaksperiode_generasjon WHERE unik_id = ?"

            session.run(queryOf(query, generasjonId).map {
                it.uuidOrNull("utbetaling_id")
            }.asSingle)
        }

        assertEquals(forventetUtbetalingId, utbetalingId)
    }
}