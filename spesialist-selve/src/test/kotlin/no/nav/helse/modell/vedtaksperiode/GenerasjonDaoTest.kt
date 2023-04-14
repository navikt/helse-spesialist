package no.nav.helse.modell.vedtaksperiode

import DatabaseIntegrationTest
import java.time.LocalDateTime
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.mediator.builders.GenerasjonBuilder
import no.nav.helse.modell.varsel.ActualVarselRepository
import no.nav.helse.modell.varsel.Varsel
import no.nav.helse.modell.varsel.VarselDao
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class GenerasjonDaoTest : DatabaseIntegrationTest() {
    private val varselDao = VarselDao(dataSource)
    private val varselRepository = ActualVarselRepository(dataSource)
    private val generasjonRepository = ActualGenerasjonRepository(dataSource)

    @Test
    fun `bygg generasjon`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        generasjonDao.opprettFor(generasjonId, vedtaksperiodeId, UUID.randomUUID(), 1.januar, Periode(1.januar, 31.januar))
        val builder = GenerasjonBuilder(vedtaksperiodeId)
        generasjonDao.byggSisteFor(vedtaksperiodeId, builder)
        builder.varsler(emptyList())
        val generasjon = builder.build(generasjonRepository, varselRepository)
        val forventetGenerasjon = Generasjon(generasjonId, vedtaksperiodeId, 1.januar, 31.januar, 1.januar)
        assertEquals(
            forventetGenerasjon,
            generasjon
        )
    }

    @Test
    fun `bygg kun siste generasjon`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId1 = UUID.randomUUID()
        val generasjonId2 = UUID.randomUUID()
        generasjonDao.opprettFor(generasjonId1, vedtaksperiodeId, UUID.randomUUID(), 1.januar, Periode(1.januar, 31.januar))
        generasjonDao.låsFor(generasjonId1, UUID.randomUUID())
        generasjonDao.opprettFor(generasjonId2, vedtaksperiodeId, UUID.randomUUID(), 1.januar, Periode(1.januar, 31.januar))
        val builder = GenerasjonBuilder(vedtaksperiodeId)
        generasjonDao.byggSisteFor(vedtaksperiodeId, builder)
        builder.varsler(emptyList())
        val generasjon = builder.build(generasjonRepository, varselRepository)
        val forventetGenerasjon = Generasjon(generasjonId2, vedtaksperiodeId, 1.januar, 31.januar, 1.januar)

        assertEquals(forventetGenerasjon, generasjon)
    }

    @Test
    fun `oppretter generasjon for vedtaksperiode`() {
        val generasjonId = UUID.randomUUID()
        val generasjon = generasjonDao.opprettFor(generasjonId, VEDTAKSPERIODE_ID, UUID.randomUUID(), 1.januar, Periode(1.januar, 31.januar))
        val siste = generasjonDao.finnSisteFor(VEDTAKSPERIODE_ID)

        val forventetGenerasjon = Generasjon(generasjonId, VEDTAKSPERIODE_ID, 1.januar, 31.januar, 1.januar)

        assertEquals(generasjon, siste)
        assertEquals(forventetGenerasjon, generasjon)
    }

    @Test
    fun `oppretter generasjon for vedtaksperiode med skjæringstidspunkt og periode`() {
        val generasjonId = UUID.randomUUID()
        val periode = Periode(1.januar, 5.januar)
        val generasjon = generasjonDao.opprettFor(generasjonId, VEDTAKSPERIODE_ID, UUID.randomUUID(), 1.januar, periode)
        val siste = generasjonDao.finnSisteFor(VEDTAKSPERIODE_ID)

        val forventetGenerasjon = Generasjon(generasjonId, VEDTAKSPERIODE_ID, 1.januar, 5.januar, 1.januar)

        assertEquals(generasjon, siste)
        assertEquals(forventetGenerasjon, generasjon)
    }

    @Test
    fun `kan låse generasjon`() {
        val vedtaksperiodeEndretId = UUID.randomUUID()
        val vedtakFattetId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val generasjon = generasjonDao.opprettFor(generasjonId, VEDTAKSPERIODE_ID, vedtaksperiodeEndretId, 1.januar, Periode(1.januar, 31.januar))
        val låstGenerasjon = generasjonDao.låsFor(generasjonId, vedtakFattetId)

        assertNotEquals(generasjon, låstGenerasjon)
        assertLåst(VEDTAKSPERIODE_ID, vedtaksperiodeEndretId, vedtakFattetId)
    }

    @Test
    fun `gir null tilbake dersom det ikke finnes noen generasjon å låse`() {
        val vedtaksperiodeEndretId = UUID.randomUUID()
        val vedtakFattetId = UUID.randomUUID()
        val låstGenerasjon = generasjonDao.låsFor(VEDTAKSPERIODE_ID, vedtakFattetId)
        assertNull(låstGenerasjon)
        assertIkkeLåst(VEDTAKSPERIODE_ID, vedtaksperiodeEndretId, vedtakFattetId)
    }

    @Test
    fun `gir null tilbake dersom vi ikke finner noen generasjon`() {
        val låstGenerasjon = generasjonDao.finnSisteFor(VEDTAKSPERIODE_ID)
        assertNull(låstGenerasjon)
    }

    @Test
    fun `siste generasjon blir returnert`() {
        val første = generasjonDao.opprettFor(UUID.randomUUID(), VEDTAKSPERIODE_ID, UUID.randomUUID(), 1.januar, Periode(1.januar, 31.januar))
        generasjonDao.låsFor(VEDTAKSPERIODE_ID, UUID.randomUUID())
        val siste = generasjonDao.opprettFor(UUID.randomUUID(), VEDTAKSPERIODE_ID, UUID.randomUUID(), 1.januar, Periode(1.januar, 31.januar))
        val funnet = generasjonDao.finnSisteFor(VEDTAKSPERIODE_ID)

        assertNotEquals(første, funnet)
        assertEquals(siste, funnet)
    }

    @Test
    fun `finn skjæringstidspunkt`() {
        generasjonDao.opprettFor(UUID.randomUUID(), VEDTAKSPERIODE_ID, UUID.randomUUID(), 1.januar, Periode(1.januar, 31.januar))
        val skjæringstidspunkt = generasjonDao.finnSkjæringstidspunktFor(VEDTAKSPERIODE_ID)
        assertEquals(1.januar, skjæringstidspunkt)
    }

    @Test
    fun `finn skjæringstidspunkt for siste generasjon`() {
        generasjonDao.opprettFor(UUID.randomUUID(), VEDTAKSPERIODE_ID, UUID.randomUUID(), 1.januar, Periode(1.januar, 31.januar))
        generasjonDao.låsFor(VEDTAKSPERIODE_ID, UUID.randomUUID())
        val generasjonId = UUID.randomUUID()
        generasjonDao.opprettFor(generasjonId, VEDTAKSPERIODE_ID, UUID.randomUUID(), 1.januar, Periode(1.januar, 31.januar))
        generasjonDao.oppdaterSykefraværstilfelle(generasjonId, 2.januar, Periode(2.januar, 31.januar))

        val skjæringstidspunkt = generasjonDao.finnSkjæringstidspunktFor(VEDTAKSPERIODE_ID)
        assertEquals(2.januar, skjæringstidspunkt)
    }

    @Test
    fun `kan sette utbetaling_id for siste generasjon hvis den er åpen`() {
        val generasjonId = UUID.randomUUID()
        generasjonDao.opprettFor(generasjonId, VEDTAKSPERIODE_ID, UUID.randomUUID(), 1.januar, Periode(1.januar, 31.januar))
        generasjonDao.utbetalingFor(generasjonId, UTBETALING_ID)
        assertUtbetaling(generasjonId, UTBETALING_ID)
    }

    @Test
    fun `generasjon hentes opp sammen med varsler`() {
        generasjonDao.opprettFor(UUID.randomUUID(), VEDTAKSPERIODE_ID, UUID.randomUUID(), 1.januar, Periode(1.januar, 31.januar))
        val varselId = UUID.randomUUID()
        val varselOpprettet = LocalDateTime.now()
        val generasjonId = generasjonIdFor(VEDTAKSPERIODE_ID)
        varselDao.lagreVarsel(varselId, "EN_KODE", varselOpprettet, VEDTAKSPERIODE_ID, generasjonId)
        val generasjon = generasjonDao.finnSisteFor(VEDTAKSPERIODE_ID)
        val forventetGenerasjon = Generasjon(generasjonId, VEDTAKSPERIODE_ID, 1.januar, 31.januar, 1.januar)
        forventetGenerasjon.håndter(Varsel(varselId, "EN_KODE", varselOpprettet, VEDTAKSPERIODE_ID))
        assertEquals(
            forventetGenerasjon,
            generasjon
        )
    }

    @Test
    fun `finner liste av unike vedtaksperiodeIder med fnr og skjæringstidspunkt`() {
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val generasjonId1 = UUID.randomUUID()
        val generasjonId2 = UUID.randomUUID()
        val generasjonId3 = UUID.randomUUID()

        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode(vedtaksperiodeId1)
        opprettGenerasjon(vedtaksperiodeId1, generasjonId1)
        opprettVedtaksperiode(vedtaksperiodeId2)
        opprettGenerasjon(vedtaksperiodeId2, generasjonId2)
        opprettGenerasjon(vedtaksperiodeId2, generasjonId3)

        val vedtaksperiodeIder = generasjonDao.finnVedtaksperiodeIderFor(FNR, 1.januar)
        assertEquals(2, vedtaksperiodeIder.size)
        assertTrue(vedtaksperiodeIder.containsAll(setOf(vedtaksperiodeId1, vedtaksperiodeId2)))
    }

    @Test
    fun `finner liste av unike vedtaksperiodeIder med fnr`() {
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val generasjonId1 = UUID.randomUUID()
        val generasjonId2 = UUID.randomUUID()
        val generasjonId3 = UUID.randomUUID()

        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode(vedtaksperiodeId1)
        opprettGenerasjon(vedtaksperiodeId1, generasjonId1)
        opprettVedtaksperiode(vedtaksperiodeId2)
        opprettGenerasjon(vedtaksperiodeId2, generasjonId2)
        opprettGenerasjon(vedtaksperiodeId2, generasjonId3)

        val vedtaksperiodeIder = generasjonDao.finnVedtaksperiodeIderFor(FNR)
        assertEquals(2, vedtaksperiodeIder.size)
        assertTrue(vedtaksperiodeIder.containsAll(setOf(vedtaksperiodeId1, vedtaksperiodeId2)))
    }

    @Test
    fun `finner ikke vedtaksperiodeIder for forkastede perioder`() {
        val vedtaksperiodeId = UUID.randomUUID()
        nyPerson()
        val generasjonId1 = generasjonIdFor(VEDTAKSPERIODE)
        val generasjonId2 = UUID.randomUUID()

        opprettVedtaksperiode(vedtaksperiodeId, forkastet = true)
        opprettGenerasjon(vedtaksperiodeId, generasjonId2)
        generasjonDao.oppdaterSykefraværstilfelle(generasjonId1, 1.januar, Periode(1.februar, 28.februar))
        generasjonDao.oppdaterSykefraværstilfelle(generasjonId2, 1.januar, Periode(1.januar, 31.januar))
        val vedtaksperiodeIder = generasjonDao.finnVedtaksperiodeIderFor(FNR, 1.januar)
        assertEquals(1, vedtaksperiodeIder.size)
        assertTrue(vedtaksperiodeIder.contains(VEDTAKSPERIODE))
    }

    @Test
    fun `finner alle generasjoner knyttet til en utbetalingId`() {
        val generasjonId1 = UUID.randomUUID()
        val generasjonId2 = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        generasjonDao.opprettFor(generasjonId1, UUID.randomUUID(), UUID.randomUUID(), 1.januar, Periode(1.januar, 31.januar))
        generasjonDao.opprettFor(generasjonId2, UUID.randomUUID(), UUID.randomUUID(), 1.januar, Periode(1.januar, 31.januar))
        generasjonDao.utbetalingFor(generasjonId1, utbetalingId)
        generasjonDao.utbetalingFor(generasjonId2, utbetalingId)

        assertEquals(2, generasjonDao.alleFor(utbetalingId).size)
    }

    @Test
    fun `Kan fjerne utbetaling fra generasjon`() {
        val generasjonId = UUID.randomUUID()
        generasjonDao.opprettFor(generasjonId, VEDTAKSPERIODE_ID, UUID.randomUUID(), 1.januar, Periode(1.januar, 31.januar))
        generasjonDao.utbetalingFor(generasjonId, UTBETALING_ID)
        assertUtbetaling(generasjonId, UTBETALING_ID)
        generasjonDao.fjernUtbetalingFor(generasjonId)
        assertUtbetaling(generasjonId, null)
    }

    @Test
    fun `Finner åpen generasjon for vedtaksperiode-id`() {
        val generasjonId1 = UUID.randomUUID()
        val generasjonId2 = UUID.randomUUID()
        generasjonDao.opprettFor(generasjonId1, VEDTAKSPERIODE_ID, UUID.randomUUID(), 1.januar, Periode(1.januar, 31.januar))
        generasjonDao.låsFor(generasjonId1, UUID.randomUUID())
        generasjonDao.opprettFor(generasjonId2, VEDTAKSPERIODE_ID, UUID.randomUUID(), 1.januar, Periode(1.januar, 31.januar))
        val generasjon = generasjonDao.åpenGenerasjonForVedtaksperiode(VEDTAKSPERIODE_ID)
        val forventetGenerasjon = Generasjon(generasjonId2, VEDTAKSPERIODE_ID, 1.januar, 31.januar, 1.januar)

        assertEquals(forventetGenerasjon, generasjon)
    }

    @Test
    fun `Forsøk å oppdaterere sykefraværstilfelle på vedtaksperiode uten åpen generasjon`() {
        val generasjonId = UUID.randomUUID()
        generasjonDao.opprettFor(generasjonId, VEDTAKSPERIODE_ID, UUID.randomUUID(), 1.januar, Periode(1.januar, 31.januar))
        generasjonDao.låsFor(generasjonId, UUID.randomUUID())
        val generasjon = generasjonDao.åpenGenerasjonForVedtaksperiode(VEDTAKSPERIODE_ID)

        assertNull(generasjon)
    }

    @Test
    fun `Oppdaterer sykefraværstilfelle på generasjon`() {
        val generasjonId = UUID.randomUUID()
        val skjæringstidspunkt = 1.januar
        generasjonDao.opprettFor(generasjonId, VEDTAKSPERIODE_ID, UUID.randomUUID(), 1.januar, Periode(1.januar, 31.januar))
        generasjonDao.oppdaterSykefraværstilfelle(generasjonId, skjæringstidspunkt, Periode(1.januar, 5.januar))
        val generasjon = generasjonDao.finnSisteFor(VEDTAKSPERIODE_ID)
        val forventetGenerasjon =
            Generasjon(generasjonId, VEDTAKSPERIODE_ID, 1.januar, 5.januar, skjæringstidspunkt)

        assertEquals(forventetGenerasjon, generasjon)
    }

    @Test
    fun `Henter generasjon`() {
        generasjonDao.opprettFor(UUID.randomUUID(), VEDTAKSPERIODE_ID, UUID.randomUUID(), 1.januar, Periode(1.januar, 31.januar))
        val generasjonId = generasjonIdFor(VEDTAKSPERIODE_ID)
        val generasjon = generasjonDao.finnSisteFor(VEDTAKSPERIODE_ID)
        assertEquals(
            Generasjon(generasjonId, VEDTAKSPERIODE_ID, 1.januar, 31.januar, 1.januar),
            generasjon
        )
    }

    @Test
    fun `Henter generasjon med fom, tom og skjæringstidspunkt`() {
        generasjonDao.opprettFor(UUID.randomUUID(), VEDTAKSPERIODE_ID, UUID.randomUUID(), 1.januar, Periode(1.januar, 31.januar))
        val generasjonId = generasjonIdFor(VEDTAKSPERIODE_ID)
        val generasjon = generasjonDao.finnSisteFor(VEDTAKSPERIODE_ID)
        assertEquals(
            Generasjon(generasjonId, VEDTAKSPERIODE_ID, 1.januar, 31.januar, 1.januar),
            generasjon
        )
    }

    @Test
    fun `Lager innslag i opprinnelig_soknadsdato`() {
        generasjonDao.opprettFor(UUID.randomUUID(), VEDTAKSPERIODE_ID, UUID.randomUUID(), 1.januar, Periode(1.januar, 31.januar))

        assertEquals(finnTidligsteGenerasjonOpprettetTidspunkt(VEDTAKSPERIODE_ID), finnSøknadMottatt(VEDTAKSPERIODE_ID))
    }

    @Test
    fun `Lager ikke innslag i opprinnelig_soknadsdato for ettergølgende generasjoner`() {
        generasjonDao.opprettFor(UUID.randomUUID(), VEDTAKSPERIODE_ID, UUID.randomUUID(), 1.januar, Periode(1.januar, 31.januar))
        generasjonDao.låsFor(VEDTAKSPERIODE_ID, UUID.randomUUID())
        val opprinneligSøknadsdato = finnSøknadMottatt(VEDTAKSPERIODE_ID)
        generasjonDao.opprettFor(UUID.randomUUID(), VEDTAKSPERIODE_ID, UUID.randomUUID(), 1.januar, Periode(1.januar, 31.januar))

        assertEquals(opprinneligSøknadsdato, finnSøknadMottatt(VEDTAKSPERIODE_ID))
    }

    private fun generasjonIdFor(vedtaksperiodeId: UUID): UUID {
        @Language("PostgreSQL")
        val query =
            "SELECT unik_id FROM selve_vedtaksperiode_generasjon WHERE vedtaksperiode_id = ? ORDER BY id"

        return sessionOf(dataSource).use { session ->
            session.run(queryOf(query, vedtaksperiodeId).map { it.uuid("unik_id") }.asList).single()
        }
    }

    private fun assertLåst(vedtaksperiodeId: UUID, opprettetAvId: UUID, låstAvId: UUID) {
        val låst = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query =
                "SELECT låst FROM selve_vedtaksperiode_generasjon WHERE vedtaksperiode_id = ? AND opprettet_av_hendelse = ? AND låst_av_hendelse = ?;"

            session.run(queryOf(query, vedtaksperiodeId, opprettetAvId, låstAvId).map {
                it.boolean("låst")
            }.asSingle)
        } ?: false

        assertTrue(låst) { "Generasjonen er ikke låst" }
    }

    private fun assertIkkeLåst(vedtaksperiodeId: UUID, opprettetAvId: UUID, låstAvId: UUID) {
        val låst = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query =
                "SELECT låst FROM selve_vedtaksperiode_generasjon WHERE vedtaksperiode_id = ? AND opprettet_av_hendelse = ? AND låst_av_hendelse = ?;"

            session.run(queryOf(query, vedtaksperiodeId, opprettetAvId, låstAvId).map {
                it.boolean("låst")
            }.asSingle)
        } ?: false

        assertFalse(låst) { "Generasjonen er låst" }
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

    private fun finnSøknadMottatt(vedtaksperiodeId: UUID) =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT soknad_mottatt FROM opprinnelig_soknadsdato WHERE vedtaksperiode_id = ?"
            session.run(queryOf(query, vedtaksperiodeId).map {
                it.localDateTimeOrNull("soknad_mottatt")
            }.asSingle)
        }

    private fun finnTidligsteGenerasjonOpprettetTidspunkt(vedtaksperiodeId: UUID) =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT min(opprettet_tidspunkt) as opprettet_tidspunkt FROM selve_vedtaksperiode_generasjon WHERE vedtaksperiode_id = ? GROUP BY vedtaksperiode_id"
            session.run(queryOf(query, vedtaksperiodeId).map {
                it.localDateTimeOrNull("opprettet_tidspunkt")
            }.asSingle)
        }
}
