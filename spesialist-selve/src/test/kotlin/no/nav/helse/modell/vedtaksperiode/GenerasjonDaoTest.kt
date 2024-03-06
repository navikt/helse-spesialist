package no.nav.helse.modell.vedtaksperiode

import DatabaseIntegrationTest
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.mediator.builders.GenerasjonBuilder
import no.nav.helse.modell.varsel.ActualVarselRepository
import no.nav.helse.modell.varsel.VarselDao
import no.nav.helse.modell.varsel.VarselDto
import no.nav.helse.modell.varsel.VarselStatusDto
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
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
        generasjonDao.opprettFor(generasjonId, vedtaksperiodeId, UUID.randomUUID(), 1.januar, Periode(1.januar, 31.januar), Generasjon.Ulåst)
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
        generasjonDao.opprettFor(generasjonId1, vedtaksperiodeId, UUID.randomUUID(), 1.januar, Periode(1.januar, 31.januar), Generasjon.Ulåst)
        generasjonDao.oppdaterTilstandFor(generasjonId1, Generasjon.Låst, UUID.randomUUID())
        generasjonDao.opprettFor(generasjonId2, vedtaksperiodeId, UUID.randomUUID(), 1.januar, Periode(1.januar, 31.januar), Generasjon.Ulåst)
        val builder = GenerasjonBuilder(vedtaksperiodeId)
        generasjonDao.byggSisteFor(vedtaksperiodeId, builder)
        builder.varsler(emptyList())
        val generasjon = builder.build(generasjonRepository, varselRepository)
        val forventetGenerasjon = Generasjon(generasjonId2, vedtaksperiodeId, 1.januar, 31.januar, 1.januar)

        assertEquals(forventetGenerasjon, generasjon)
    }

    @Test
    fun `lagre generasjon`() {
        val id = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        val varselId = UUID.randomUUID()
        val varselOpprettet = LocalDateTime.now()
        val varselstatus = VarselStatusDto.AKTIV
        val generasjonDto = GenerasjonDto(
            id = id,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            skjæringstidspunkt = 1.januar,
            fom = 1.januar,
            tom = 31.januar,
            tilstand = TilstandDto.Ulåst,
            varsler = listOf(
                VarselDto(varselId, "RV_IM_1", varselOpprettet, vedtaksperiodeId, varselstatus)
            )
        )
        generasjonDao.lagre(generasjonDto)
        val lagretGenerasjon = generasjonDao.finnGjeldendeGenerasjon(vedtaksperiodeId)
        assertNotNull(lagretGenerasjon)
        assertEquals(id, lagretGenerasjon?.id)
        assertEquals(vedtaksperiodeId, lagretGenerasjon?.vedtaksperiodeId)
        assertEquals(1.januar, lagretGenerasjon?.fom)
        assertEquals(31.januar, lagretGenerasjon?.tom)
        assertEquals(1.januar, lagretGenerasjon?.skjæringstidspunkt)
        assertEquals(TilstandDto.Ulåst, lagretGenerasjon?.tilstand)
        assertEquals(utbetalingId, lagretGenerasjon?.utbetalingId)
        assertEquals(1, lagretGenerasjon?.varsler?.size)
        val varsel = lagretGenerasjon?.varsler?.single()
        assertEquals(varselId, varsel?.id)
        assertEquals("RV_IM_1", varsel?.varselkode)
        assertEquals(varselOpprettet.withNano(0), varsel?.opprettet?.withNano(0))
        assertEquals(varselstatus, varsel?.status)
        assertEquals(vedtaksperiodeId, varsel?.vedtaksperiodeId)
    }

    @Test
    fun `oppdatere generasjon`() {
        val id = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val nyUtbetalingId = UUID.randomUUID()
        val generasjonDto = GenerasjonDto(
            id = id,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = UUID.randomUUID(),
            skjæringstidspunkt = 1.januar,
            fom = 1.januar,
            tom = 31.januar,
            tilstand = TilstandDto.Ulåst,
            varsler = emptyList()
        )
        generasjonDao.lagre(generasjonDto)
        generasjonDao.lagre(generasjonDto.copy(utbetalingId = nyUtbetalingId, fom = 2.januar, tom = 30.januar, skjæringstidspunkt = 2.januar, tilstand = TilstandDto.Låst))
        val lagretGenerasjon = generasjonDao.finnGjeldendeGenerasjon(vedtaksperiodeId)
        assertNotNull(lagretGenerasjon)
        assertEquals(id, lagretGenerasjon?.id)
        assertEquals(vedtaksperiodeId, lagretGenerasjon?.vedtaksperiodeId)
        assertEquals(2.januar, lagretGenerasjon?.fom)
        assertEquals(30.januar, lagretGenerasjon?.tom)
        assertEquals(2.januar, lagretGenerasjon?.skjæringstidspunkt)
        assertEquals(TilstandDto.Låst, lagretGenerasjon?.tilstand)
        assertEquals(nyUtbetalingId, lagretGenerasjon?.utbetalingId)
    }

    @Test
    fun `legg til varsel`() {
        val id = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val varselId = UUID.randomUUID()
        val varselOpprettet = LocalDateTime.now()
        val varselstatus = VarselStatusDto.AKTIV
        val generasjonDto = GenerasjonDto(
            id = id,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = UUID.randomUUID(),
            skjæringstidspunkt = 1.januar,
            fom = 1.januar,
            tom = 31.januar,
            tilstand = TilstandDto.Ulåst,
            varsler = emptyList()
        )
        generasjonDao.lagre(generasjonDto)
        generasjonDao.lagre(generasjonDto.copy(varsler = listOf(VarselDto(varselId, "RV_IM_1", varselOpprettet, vedtaksperiodeId, varselstatus))))
        val lagretGenerasjon = generasjonDao.finnGjeldendeGenerasjon(vedtaksperiodeId)

        assertNotNull(lagretGenerasjon)
        assertEquals(1, lagretGenerasjon?.varsler?.size)
    }

    @Test
    fun `oppdatere varsel`() {
        val id = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val varselId = UUID.randomUUID()
        val varselOpprettet = LocalDateTime.now()
        val generasjonDto = GenerasjonDto(
            id = id,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = UUID.randomUUID(),
            skjæringstidspunkt = 1.januar,
            fom = 1.januar,
            tom = 31.januar,
            tilstand = TilstandDto.Ulåst,
            varsler = listOf(VarselDto(varselId, "RV_IM_1", varselOpprettet, vedtaksperiodeId, VarselStatusDto.AKTIV))
        )
        generasjonDao.lagre(generasjonDto)
        generasjonDao.lagre(generasjonDto.copy(varsler = listOf(VarselDto(varselId, "RV_IM_1", varselOpprettet, vedtaksperiodeId, VarselStatusDto.VURDERT))))
        val lagretGenerasjon = generasjonDao.finnGjeldendeGenerasjon(vedtaksperiodeId)

        assertNotNull(lagretGenerasjon)
        assertEquals(1, lagretGenerasjon?.varsler?.size)
        val varsel = lagretGenerasjon?.varsler?.single()
        assertEquals(VarselStatusDto.VURDERT, varsel?.status)
    }

    @Test
    fun `fjerne varsel`() {
        val id = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val varselIdSomIkkeBlirSlettet = UUID.randomUUID()
        val varselOpprettetSomIkkeBlirSlettet = LocalDateTime.now()
        val generasjonDto = GenerasjonDto(
            id = id,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = UUID.randomUUID(),
            skjæringstidspunkt = 1.januar,
            fom = 1.januar,
            tom = 31.januar,
            tilstand = TilstandDto.Ulåst,
            varsler = listOf(
                VarselDto(varselIdSomIkkeBlirSlettet, "RV_IM_1",
                    varselOpprettetSomIkkeBlirSlettet, vedtaksperiodeId, VarselStatusDto.AKTIV),
                VarselDto(UUID.randomUUID(), "RV_IM_2", LocalDateTime.now(), vedtaksperiodeId, VarselStatusDto.AKTIV),
            )
        )
        generasjonDao.lagre(generasjonDto)
        generasjonDao.lagre(
            generasjonDto.copy(
                varsler = listOf(
                    VarselDto(varselIdSomIkkeBlirSlettet, "RV_IM_1", varselOpprettetSomIkkeBlirSlettet, vedtaksperiodeId, VarselStatusDto.AKTIV)
                )
            )
        )
        val lagretGenerasjon = generasjonDao.finnGjeldendeGenerasjon(vedtaksperiodeId)

        assertNotNull(lagretGenerasjon)
        assertEquals(1, lagretGenerasjon?.varsler?.size)
    }

    @Test
    fun `fjerne varsel slik at det ikke er noen varsler igjen på generasjonen`() {
        val id = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val varselId = UUID.randomUUID()
        val varselOpprettet = LocalDateTime.now()
        val generasjonDto = GenerasjonDto(
            id = id,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = UUID.randomUUID(),
            skjæringstidspunkt = 1.januar,
            fom = 1.januar,
            tom = 31.januar,
            tilstand = TilstandDto.Ulåst,
            varsler = listOf(VarselDto(varselId, "RV_IM_1", varselOpprettet, vedtaksperiodeId, VarselStatusDto.AKTIV))
        )
        generasjonDao.lagre(generasjonDto)
        generasjonDao.lagre(generasjonDto.copy(varsler = emptyList()))
        val lagretGenerasjon = generasjonDao.finnGjeldendeGenerasjon(vedtaksperiodeId)

        assertNotNull(lagretGenerasjon)
        assertEquals(0, lagretGenerasjon?.varsler?.size)
    }

    @Test
    fun `oppretter generasjon for vedtaksperiode`() {
        val generasjonId = UUID.randomUUID()
        val generasjon = generasjonDao.opprettFor(generasjonId, VEDTAKSPERIODE, UUID.randomUUID(), 1.januar, Periode(1.januar, 31.januar), Generasjon.Ulåst)

        val forventetGenerasjon = Generasjon(generasjonId, VEDTAKSPERIODE, 1.januar, 31.januar, 1.januar)
        assertEquals(forventetGenerasjon, generasjon)
    }

    @Test
    fun `oppretter generasjon for vedtaksperiode med skjæringstidspunkt og periode`() {
        val generasjonId = UUID.randomUUID()
        val periode = Periode(1.januar, 5.januar)
        val generasjon = generasjonDao.opprettFor(generasjonId, VEDTAKSPERIODE, UUID.randomUUID(), 1.januar, periode, Generasjon.Ulåst)

        val forventetGenerasjon = Generasjon(generasjonId, VEDTAKSPERIODE, 1.januar, 5.januar, 1.januar)
        assertEquals(forventetGenerasjon, generasjon)
    }

    @Test
    fun `kan bytte tilstand for generasjon`() {
        val vedtaksperiodeEndretId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        generasjonDao.opprettFor(generasjonId, VEDTAKSPERIODE, vedtaksperiodeEndretId, 1.januar, Periode(1.januar, 31.januar), Generasjon.Ulåst)
        generasjonDao.oppdaterTilstandFor(generasjonId, Generasjon.Låst, UUID.randomUUID())

        assertTilstand(VEDTAKSPERIODE, Generasjon.Låst)
    }
    @Test
    fun `oppdaterer generasjon med behandlingsinformasjon`() {
        val vedtaksperiodeEndretId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        generasjonDao.opprettFor(generasjonId, VEDTAKSPERIODE, vedtaksperiodeEndretId, 1.januar, Periode(1.januar, 31.januar), Generasjon.Ulåst)

        val spleisBehandlingId = UUID.randomUUID()
        val tags = listOf("ARBEIDSGIVERUTBETALING", "PERSONUTBETALING")

        generasjonDao.oppdaterMedBehandlingsInformasjon(generasjonId, spleisBehandlingId, tags)

        assertTags(generasjonId, tags)
        assertSpleisBehandlingId(generasjonId, spleisBehandlingId)
    }
    @Test
    fun `ingen tags`() {
        val vedtaksperiodeEndretId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        generasjonDao.opprettFor(generasjonId, VEDTAKSPERIODE, vedtaksperiodeEndretId, 1.januar, Periode(1.januar, 31.januar), Generasjon.Ulåst)
        val spleisBehandlingId = UUID.randomUUID()
        val tags = emptyList<String>()
        generasjonDao.oppdaterMedBehandlingsInformasjon(generasjonId, spleisBehandlingId, tags)
        assertEquals(emptyList<String>(), generasjonDao.finnTagsFor(spleisBehandlingId))
    }

    @Test
    fun `mangler tags`() {
        val tulleId = UUID.randomUUID()
        val tags = generasjonDao.finnTagsFor(tulleId)
        assertEquals(null, tags)
    }

    @Test
    fun `gir false tilbake dersom vi ikke finner noen generasjon`() {
        val funnet = generasjonDao.harGenerasjonFor(VEDTAKSPERIODE)
        assertFalse(funnet)
    }

    @Test
    fun `har generasjon`() {
        generasjonDao.opprettFor(UUID.randomUUID(), VEDTAKSPERIODE, UUID.randomUUID(), 1.januar, Periode(1.januar, 31.januar), Generasjon.Ulåst)
        val funnet = generasjonDao.harGenerasjonFor(VEDTAKSPERIODE)
        assertTrue(funnet)
    }

    @Test
    fun `finn skjæringstidspunkt`() {
        generasjonDao.opprettFor(UUID.randomUUID(), VEDTAKSPERIODE, UUID.randomUUID(), 1.januar, Periode(1.januar, 31.januar), Generasjon.Ulåst)
        val skjæringstidspunkt = generasjonDao.finnSkjæringstidspunktFor(VEDTAKSPERIODE)
        assertEquals(1.januar, skjæringstidspunkt)
    }

    @Test
    fun `finn skjæringstidspunkt for siste generasjon`() {
        generasjonDao.opprettFor(UUID.randomUUID(), VEDTAKSPERIODE, UUID.randomUUID(), 1.januar, Periode(1.januar, 31.januar), Generasjon.Låst)
        val generasjonId = UUID.randomUUID()
        generasjonDao.opprettFor(generasjonId, VEDTAKSPERIODE, UUID.randomUUID(), 1.januar, Periode(1.januar, 31.januar), Generasjon.Ulåst)
        generasjonDao.oppdaterSykefraværstilfelle(generasjonId, 2.januar, Periode(2.januar, 31.januar))

        val skjæringstidspunkt = generasjonDao.finnSkjæringstidspunktFor(VEDTAKSPERIODE)
        assertEquals(2.januar, skjæringstidspunkt)
    }

    @Test
    fun `kan sette utbetaling_id for siste generasjon hvis den er åpen`() {
        val generasjonId = UUID.randomUUID()
        generasjonDao.opprettFor(generasjonId, VEDTAKSPERIODE, UUID.randomUUID(), 1.januar, Periode(1.januar, 31.januar), Generasjon.Ulåst)
        generasjonDao.utbetalingFor(generasjonId, UTBETALING_ID)
        assertUtbetaling(generasjonId, UTBETALING_ID)
    }

    @Test
    fun `generasjon hentes opp sammen med varsler`() {
        generasjonDao.opprettFor(UUID.randomUUID(), VEDTAKSPERIODE, UUID.randomUUID(), 1.januar, Periode(1.januar, 31.januar), Generasjon.Ulåst)
        val varselId = UUID.randomUUID()
        val varselOpprettet = LocalDateTime.now()
        val generasjonId = generasjonIdFor(VEDTAKSPERIODE)
        varselDao.lagreVarsel(varselId, "EN_KODE", varselOpprettet, VEDTAKSPERIODE, generasjonId)
        assertVarsler(generasjonId, "EN_KODE")
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
        generasjonDao.opprettFor(
            generasjonId1,
            UUID.randomUUID(),
            UUID.randomUUID(),
            1.januar,
            Periode(1.januar, 31.januar),
            Generasjon.Ulåst
        )
        generasjonDao.opprettFor(
            generasjonId2,
            UUID.randomUUID(),
            UUID.randomUUID(),
            1.januar,
            Periode(1.januar, 31.januar),
            Generasjon.Ulåst
        )
        generasjonDao.utbetalingFor(generasjonId1, utbetalingId)
        generasjonDao.utbetalingFor(generasjonId2, utbetalingId)

        assertEquals(2, generasjonDao.finnVedtaksperiodeIderFor(utbetalingId).size)
    }

    @Test
    fun `Kan fjerne utbetaling fra generasjon`() {
        val generasjonId = UUID.randomUUID()
        generasjonDao.opprettFor(
            generasjonId,
            VEDTAKSPERIODE,
            UUID.randomUUID(),
            1.januar,
            Periode(1.januar, 31.januar),
            Generasjon.Ulåst
        )
        generasjonDao.utbetalingFor(generasjonId, UTBETALING_ID)
        assertUtbetaling(generasjonId, UTBETALING_ID)
        generasjonDao.fjernUtbetalingFor(generasjonId)
        assertUtbetaling(generasjonId, null)
    }

    @Test
    fun `Oppdaterer sykefraværstilfelle på generasjon`() {
        val generasjonId = UUID.randomUUID()
        val skjæringstidspunkt = 1.februar
        generasjonDao.opprettFor(
            generasjonId,
            VEDTAKSPERIODE,
            UUID.randomUUID(),
            1.januar,
            Periode(1.januar, 31.januar),
            Generasjon.Ulåst
        )
        generasjonDao.oppdaterSykefraværstilfelle(generasjonId, skjæringstidspunkt, Periode(1.februar, 5.februar))
        assertTidslinje(generasjonId, 1.februar, 5.februar, skjæringstidspunkt)
    }

    @Test
    fun `Lager innslag i opprinnelig_soknadsdato`() {
        generasjonDao.opprettFor(
            UUID.randomUUID(),
            VEDTAKSPERIODE,
            UUID.randomUUID(),
            1.januar,
            Periode(1.januar, 31.januar),
            Generasjon.Ulåst
        )

        assertEquals(finnTidligsteGenerasjonOpprettetTidspunkt(VEDTAKSPERIODE), finnSøknadMottatt(VEDTAKSPERIODE))
    }

    @Test
    fun `Lager ikke innslag i opprinnelig_soknadsdato for ettergølgende generasjoner`() {
        generasjonDao.opprettFor(
            UUID.randomUUID(),
            VEDTAKSPERIODE,
            UUID.randomUUID(),
            1.januar,
            Periode(1.januar, 31.januar),
            Generasjon.Låst
        )
        val opprinneligSøknadsdato = finnSøknadMottatt(VEDTAKSPERIODE)
        generasjonDao.opprettFor(
            UUID.randomUUID(),
            VEDTAKSPERIODE,
            UUID.randomUUID(),
            1.januar,
            Periode(1.januar, 31.januar),
            Generasjon.Ulåst
        )

        assertEquals(opprinneligSøknadsdato, finnSøknadMottatt(VEDTAKSPERIODE))
    }

    @Test
    fun `Finner første generasjons låst tidspunkt`() {
        val vedtaksperiodeEndretId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        generasjonDao.opprettFor(generasjonId, VEDTAKSPERIODE, vedtaksperiodeEndretId, 1.januar, Periode(1.januar, 31.januar), Generasjon.Ulåst)
        generasjonDao.oppdaterTilstandFor(generasjonId, Generasjon.Låst, UUID.randomUUID())

        assertNotNull(generasjonDao.førsteGenerasjonLåstTidspunkt(VEDTAKSPERIODE))
    }

    private fun Pair<LocalDate, LocalDate>.tilPeriode() = Periode(first, second)
    @Test
    fun `Finner første kjente dato for person`() {
        opprettPerson()
        opprettArbeidsgiver()
        val førstePeriode = 1.januar to 5.januar
        val vedtaksperiodeId1 = UUID.randomUUID()
        opprettVedtaksperiode(vedtaksperiodeId1, fom = førstePeriode.first, tom = førstePeriode.second)
        generasjonDao.opprettFor(UUID.randomUUID(), vedtaksperiodeId1, UUID.randomUUID(), førstePeriode.first, førstePeriode.tilPeriode(), Generasjon.Låst)

        val vedtaksperiodeId2 = UUID.randomUUID()
        val senerePeriode = 10.februar(2022) to 20.februar(2022)
        opprettVedtaksperiode(vedtaksperiodeId2, fom = senerePeriode.first, tom = senerePeriode.second)
        generasjonDao.opprettFor(UUID.randomUUID(), vedtaksperiodeId2, UUID.randomUUID(), senerePeriode.first, senerePeriode.tilPeriode(), Generasjon.Låst)

        assertEquals(1.januar, generasjonDao.førsteKjenteDag(FNR))
    }

    private fun assertVarsler(generasjonId: UUID, vararg forventedeVarselkoder: String) {
        @Language("PostgreSQL")
        val query =
            "SELECT kode FROM selve_varsel WHERE generasjon_ref = (SELECT id FROM selve_vedtaksperiode_generasjon WHERE unik_id = ?)"

        val varselkoder = sessionOf(dataSource).use { session ->
            session.run(queryOf(query, generasjonId).map { it.string("kode") }.asList).toSet()
        }
        assertEquals(forventedeVarselkoder.toSet(), varselkoder)
    }

    private fun assertTidslinje(generasjonId: UUID, forventetFom: LocalDate, forventetTom: LocalDate, forventetSkjæringstidspunkt: LocalDate) {
        @Language("PostgreSQL")
        val query =
            "SELECT fom, tom, skjæringstidspunkt FROM selve_vedtaksperiode_generasjon WHERE unik_id = ?"

        val (fom, tom, skjæringstidspunkt) = sessionOf(dataSource).use { session ->
            session.run(queryOf(query, generasjonId).map { Triple(it.localDate("fom"), it.localDate("tom"), it.localDate("skjæringstidspunkt")) }.asSingle)
        }!!

        assertEquals(forventetFom, fom)
        assertEquals(forventetTom, tom)
        assertEquals(forventetSkjæringstidspunkt, skjæringstidspunkt)
    }

    private fun generasjonIdFor(vedtaksperiodeId: UUID): UUID {
        @Language("PostgreSQL")
        val query =
            "SELECT unik_id FROM selve_vedtaksperiode_generasjon WHERE vedtaksperiode_id = ? ORDER BY id"

        return sessionOf(dataSource).use { session ->
            session.run(queryOf(query, vedtaksperiodeId).map { it.uuid("unik_id") }.asList).single()
        }
    }

    private fun assertTilstand(vedtaksperiodeId: UUID, forventetTilstand: Generasjon.Tilstand) {
        val tilstand = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query =
                "SELECT tilstand FROM selve_vedtaksperiode_generasjon WHERE vedtaksperiode_id = ?;"

            session.run(queryOf(query, vedtaksperiodeId).map {
                it.string("tilstand")
            }.asSingle)
        }

        assertEquals(forventetTilstand.navn(), tilstand)
    }

    private fun assertTags(generasjonId: UUID, forventedeTags: List<String>) {
        val tags = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query =
                "SELECT tags FROM selve_vedtaksperiode_generasjon WHERE unik_id = ?;"

            session.run(queryOf(query, generasjonId).map {
                it.array<String>("tags").toList()
            }.asSingle)
        }

        assertEquals(forventedeTags, tags)
    }

    private fun assertSpleisBehandlingId(generasjonId: UUID, forventetSpleisBehandlingId: UUID) {
        val actualSpleisBehandlingId = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query =
                "SELECT spleis_behandling_id FROM selve_vedtaksperiode_generasjon WHERE unik_id = ?;"

            session.run(queryOf(query, generasjonId).map {
                it.uuidOrNull("spleis_behandling_id")
            }.asSingle)
        }

        assertEquals(forventetSpleisBehandlingId, actualSpleisBehandlingId)
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
