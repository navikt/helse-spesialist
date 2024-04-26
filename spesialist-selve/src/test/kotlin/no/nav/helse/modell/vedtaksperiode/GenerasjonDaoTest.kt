package no.nav.helse.modell.vedtaksperiode

import DatabaseIntegrationTest
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.mediator.builders.GenerasjonBuilder
import no.nav.helse.modell.varsel.VarselDao
import no.nav.helse.modell.varsel.VarselDto
import no.nav.helse.modell.varsel.VarselRepository
import no.nav.helse.modell.varsel.VarselStatusDto
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

internal class GenerasjonDaoTest : DatabaseIntegrationTest() {
    private val varselDao = VarselDao(dataSource)
    private val varselRepository = VarselRepository(dataSource)
    private val generasjonRepository = GenerasjonRepository(dataSource)

    @Test
    fun `bygg generasjon`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        generasjonDao.opprettFor(
            generasjonId,
            vedtaksperiodeId,
            UUID.randomUUID(),
            1.januar,
            Periode(1.januar, 31.januar),
            Generasjon.VidereBehandlingAvklares,
        )
        val builder = GenerasjonBuilder(vedtaksperiodeId)
        generasjonDao.byggSisteFor(vedtaksperiodeId, builder)
        builder.varsler(emptyList())
        val generasjon = builder.build(generasjonRepository, varselRepository)
        val forventetGenerasjon = Generasjon(generasjonId, vedtaksperiodeId, 1.januar, 31.januar, 1.januar)
        assertEquals(
            forventetGenerasjon,
            generasjon,
        )
    }

    @Test
    fun `bygg kun siste generasjon`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId1 = UUID.randomUUID()
        val generasjonId2 = UUID.randomUUID()
        generasjonDao.opprettFor(
            generasjonId1,
            vedtaksperiodeId,
            UUID.randomUUID(),
            1.januar,
            Periode(1.januar, 31.januar),
            Generasjon.VidereBehandlingAvklares,
        )
        generasjonDao.oppdaterTilstandFor(generasjonId1, Generasjon.VedtakFattet.navn(), UUID.randomUUID())
        generasjonDao.opprettFor(
            generasjonId2,
            vedtaksperiodeId,
            UUID.randomUUID(),
            1.januar,
            Periode(1.januar, 31.januar),
            Generasjon.VidereBehandlingAvklares,
        )
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
        val spleisBehandlingId = UUID.randomUUID()
        val varselId = UUID.randomUUID()
        val varselOpprettet = LocalDateTime.now()
        val varselstatus = VarselStatusDto.AKTIV
        val tags = listOf("tag 1", "tag 2")
        val generasjonDto =
            nyGenerasjonDto(
                id = id,
                vedtaksperiodeId = vedtaksperiodeId,
                utbetalingId = utbetalingId,
                spleisBehandlingId = spleisBehandlingId,
                tags = tags,
                varsler =
                    listOf(
                        VarselDto(varselId, "RV_IM_1", varselOpprettet, vedtaksperiodeId, varselstatus),
                    ),
            )
        generasjonDao.lagre(generasjonDto)
        val lagretGenerasjon = generasjonDao.finnGjeldendeGenerasjon(vedtaksperiodeId)
        checkNotNull(lagretGenerasjon)
        assertEquals(id, lagretGenerasjon.id)
        assertEquals(vedtaksperiodeId, lagretGenerasjon.vedtaksperiodeId)
        assertEquals(1.januar, lagretGenerasjon.fom)
        assertEquals(31.januar, lagretGenerasjon.tom)
        assertEquals(1.januar, lagretGenerasjon.skjæringstidspunkt)
        assertEquals(TilstandDto.VidereBehandlingAvklares, lagretGenerasjon.tilstand)
        assertEquals(utbetalingId, lagretGenerasjon.utbetalingId)
        assertEquals(spleisBehandlingId, lagretGenerasjon.spleisBehandlingId)
        assertEquals(1, lagretGenerasjon.varsler.size)
        val varsel = lagretGenerasjon.varsler.single()
        assertEquals(varselId, varsel.id)
        assertEquals("RV_IM_1", varsel.varselkode)
        assertEquals(varselOpprettet.truncatedTo(ChronoUnit.MILLIS), varsel.opprettet.truncatedTo(ChronoUnit.MILLIS))
        assertEquals(varselstatus, varsel.status)
        assertEquals(tags, lagretGenerasjon.tags)
        assertEquals(vedtaksperiodeId, varsel.vedtaksperiodeId)
    }

    @Test
    fun `finn generasjoner`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon1 = nyGenerasjonDto(vedtaksperiodeId)
        val generasjon2 = nyGenerasjonDto(vedtaksperiodeId)
        generasjonDao.lagre(generasjon1)
        generasjonDao.lagre(generasjon2)
        val generasjoner =
            with(generasjonDao) {
                sessionOf(dataSource).use { session ->
                    session.transaction { tx ->
                        tx.finnGenerasjoner(vedtaksperiodeId)
                    }
                }
            }

        assertEquals(2, generasjoner.size)
        assertEquals(generasjon1, generasjoner[0])
        assertEquals(generasjon2, generasjoner[1])
    }

    @Test
    fun `oppdatere generasjon`() {
        val id = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val nyUtbetalingId = UUID.randomUUID()
        val generasjonDto = nyGenerasjonDto(vedtaksperiodeId, id)
        generasjonDao.lagre(generasjonDto)
        generasjonDao.lagre(
            generasjonDto.copy(
                utbetalingId = nyUtbetalingId,
                fom = 2.januar,
                tom = 30.januar,
                skjæringstidspunkt = 2.januar,
                tilstand = TilstandDto.VedtakFattet,
            ),
        )
        val lagretGenerasjon = generasjonDao.finnGjeldendeGenerasjon(vedtaksperiodeId)
        assertNotNull(lagretGenerasjon)
        assertEquals(id, lagretGenerasjon?.id)
        assertEquals(vedtaksperiodeId, lagretGenerasjon?.vedtaksperiodeId)
        assertEquals(2.januar, lagretGenerasjon?.fom)
        assertEquals(30.januar, lagretGenerasjon?.tom)
        assertEquals(2.januar, lagretGenerasjon?.skjæringstidspunkt)
        assertEquals(TilstandDto.VedtakFattet, lagretGenerasjon?.tilstand)
        assertEquals(nyUtbetalingId, lagretGenerasjon?.utbetalingId)
    }

    @Test
    fun `legg til varsel`() {
        val id = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val varselId = UUID.randomUUID()
        val varselOpprettet = LocalDateTime.now()
        val varselstatus = VarselStatusDto.AKTIV
        val generasjonDto = nyGenerasjonDto(vedtaksperiodeId, id)
        generasjonDao.lagre(generasjonDto)
        generasjonDao.lagre(
            generasjonDto.copy(varsler = listOf(VarselDto(varselId, "RV_IM_1", varselOpprettet, vedtaksperiodeId, varselstatus))),
        )
        val lagretGenerasjon = generasjonDao.finnGjeldendeGenerasjon(vedtaksperiodeId)

        assertNotNull(lagretGenerasjon)
        assertEquals(1, lagretGenerasjon?.varsler?.size)
    }

    @Test
    fun `oppdatere varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val varselId = UUID.randomUUID()
        val varselOpprettet = LocalDateTime.now()
        val generasjonDto =
            nyGenerasjonDto(
                vedtaksperiodeId = vedtaksperiodeId,
                varsler = listOf(VarselDto(varselId, "RV_IM_1", varselOpprettet, vedtaksperiodeId, VarselStatusDto.AKTIV)),
            )
        generasjonDao.lagre(generasjonDto)
        generasjonDao.lagre(
            generasjonDto.copy(
                varsler = listOf(VarselDto(varselId, "RV_IM_1", varselOpprettet, vedtaksperiodeId, VarselStatusDto.VURDERT)),
            ),
        )
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
        val generasjonDto =
            nyGenerasjonDto(
                vedtaksperiodeId = vedtaksperiodeId,
                id = id,
                varsler =
                    listOf(
                        VarselDto(
                            varselIdSomIkkeBlirSlettet,
                            "RV_IM_1",
                            varselOpprettetSomIkkeBlirSlettet,
                            vedtaksperiodeId,
                            VarselStatusDto.AKTIV,
                        ),
                        VarselDto(UUID.randomUUID(), "RV_IM_2", LocalDateTime.now(), vedtaksperiodeId, VarselStatusDto.AKTIV),
                    ),
            )
        generasjonDao.lagre(generasjonDto)
        generasjonDao.lagre(
            generasjonDto.copy(
                varsler =
                    listOf(
                        VarselDto(
                            varselIdSomIkkeBlirSlettet,
                            "RV_IM_1",
                            varselOpprettetSomIkkeBlirSlettet,
                            vedtaksperiodeId,
                            VarselStatusDto.AKTIV,
                        ),
                    ),
            ),
        )
        val lagretGenerasjon = generasjonDao.finnGjeldendeGenerasjon(vedtaksperiodeId)

        assertNotNull(lagretGenerasjon)
        assertEquals(1, lagretGenerasjon?.varsler?.size)
    }

    @Test
    fun `fjerne varsel slik at det ikke er noen varsler igjen på generasjonen`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val varselId = UUID.randomUUID()
        val varselOpprettet = LocalDateTime.now()
        val generasjonDto =
            nyGenerasjonDto(
                vedtaksperiodeId = vedtaksperiodeId,
                varsler = listOf(VarselDto(varselId, "RV_IM_1", varselOpprettet, vedtaksperiodeId, VarselStatusDto.AKTIV)),
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
        val generasjon =
            generasjonDao.opprettFor(
                generasjonId,
                VEDTAKSPERIODE,
                UUID.randomUUID(),
                1.januar,
                Periode(1.januar, 31.januar),
                Generasjon.VidereBehandlingAvklares,
            )

        val forventetGenerasjon = Generasjon(generasjonId, VEDTAKSPERIODE, 1.januar, 31.januar, 1.januar)
        assertEquals(forventetGenerasjon, generasjon)
    }

    @Test
    fun `oppretter generasjon for vedtaksperiode med skjæringstidspunkt og periode`() {
        val generasjonId = UUID.randomUUID()
        val periode = Periode(1.januar, 5.januar)
        val generasjon =
            generasjonDao.opprettFor(
                generasjonId,
                VEDTAKSPERIODE,
                UUID.randomUUID(),
                1.januar,
                periode,
                Generasjon.VidereBehandlingAvklares,
            )

        val forventetGenerasjon = Generasjon(generasjonId, VEDTAKSPERIODE, 1.januar, 5.januar, 1.januar)
        assertEquals(forventetGenerasjon, generasjon)
    }

    @Test
    fun `kan bytte tilstand for generasjon`() {
        val vedtaksperiodeEndretId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        generasjonDao.opprettFor(
            generasjonId,
            VEDTAKSPERIODE,
            vedtaksperiodeEndretId,
            1.januar,
            Periode(1.januar, 31.januar),
            Generasjon.VidereBehandlingAvklares,
        )
        generasjonDao.oppdaterTilstandFor(generasjonId, Generasjon.VedtakFattet.navn(), UUID.randomUUID())

        assertTilstand(VEDTAKSPERIODE, Generasjon.VedtakFattet)
    }

    @Test
    fun `slår opp tags på ikke-eksisterende behandling`() {
        val tulleId = UUID.randomUUID()
        val tags = generasjonDao.finnTagsFor(tulleId)
        assertNull(tags)
    }

    @Test
    fun `behandling uten tags gir tom liste`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val spleisBehandlingId = UUID.randomUUID()
        val generasjon1 = nyGenerasjonDto(vedtaksperiodeId = vedtaksperiodeId, spleisBehandlingId = spleisBehandlingId)
        generasjonDao.lagre(generasjon1)
        val tags = generasjonDao.finnTagsFor(spleisBehandlingId)
        assertTrue(tags != null && tags.isEmpty())
    }

    @Test
    fun `gir false tilbake dersom vi ikke finner noen generasjon`() {
        val funnet = generasjonDao.harGenerasjonFor(VEDTAKSPERIODE)
        assertFalse(funnet)
    }

    @Test
    fun `har generasjon`() {
        generasjonDao.opprettFor(
            UUID.randomUUID(),
            VEDTAKSPERIODE,
            UUID.randomUUID(),
            1.januar,
            Periode(1.januar, 31.januar),
            Generasjon.VidereBehandlingAvklares,
        )
        val funnet = generasjonDao.harGenerasjonFor(VEDTAKSPERIODE)
        assertTrue(funnet)
    }

    @Test
    fun `finn skjæringstidspunkt`() {
        generasjonDao.opprettFor(
            UUID.randomUUID(),
            VEDTAKSPERIODE,
            UUID.randomUUID(),
            1.januar,
            Periode(1.januar, 31.januar),
            Generasjon.VidereBehandlingAvklares,
        )
        val skjæringstidspunkt = generasjonDao.finnSkjæringstidspunktFor(VEDTAKSPERIODE)
        assertEquals(1.januar, skjæringstidspunkt)
    }

    @Test
    fun `finn skjæringstidspunkt for siste generasjon`() {
        generasjonDao.opprettFor(
            UUID.randomUUID(),
            VEDTAKSPERIODE,
            UUID.randomUUID(),
            1.januar,
            Periode(1.januar, 31.januar),
            Generasjon.VedtakFattet,
        )
        val generasjonId = UUID.randomUUID()
        generasjonDao.opprettFor(
            generasjonId,
            VEDTAKSPERIODE,
            UUID.randomUUID(),
            2.januar,
            Periode(1.januar, 31.januar),
            Generasjon.VidereBehandlingAvklares,
        )

        val skjæringstidspunkt = generasjonDao.finnSkjæringstidspunktFor(VEDTAKSPERIODE)
        assertEquals(2.januar, skjæringstidspunkt)
    }

    @Test
    fun `kan sette utbetaling_id for siste generasjon hvis den er åpen`() {
        val generasjonId = UUID.randomUUID()
        generasjonDao.opprettFor(
            generasjonId,
            VEDTAKSPERIODE,
            UUID.randomUUID(),
            1.januar,
            Periode(1.januar, 31.januar),
            Generasjon.VidereBehandlingAvklares,
        )
        generasjonDao.utbetalingFor(generasjonId, UTBETALING_ID)
        assertUtbetaling(generasjonId, UTBETALING_ID)
    }

    @Test
    fun `generasjon hentes opp sammen med varsler`() {
        generasjonDao.opprettFor(
            UUID.randomUUID(),
            VEDTAKSPERIODE,
            UUID.randomUUID(),
            1.januar,
            Periode(1.januar, 31.januar),
            Generasjon.VidereBehandlingAvklares,
        )
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

        val vedtaksperiodeIder =
            with(generasjonDao) {
                sessionOf(dataSource).use { session ->
                    session.transaction { tx ->
                        tx.finnVedtaksperiodeIderFor(FNR)
                    }
                }
            }
        assertEquals(2, vedtaksperiodeIder.size)
        assertTrue(vedtaksperiodeIder.containsAll(setOf(vedtaksperiodeId1, vedtaksperiodeId2)))
    }

    @Test
    fun `finner ikke vedtaksperiodeIder for forkastede perioder`() {
        val vedtaksperiodeId = UUID.randomUUID()
        nyPerson()
        val generasjonId2 = UUID.randomUUID()

        opprettVedtaksperiode(vedtaksperiodeId, forkastet = true)
        opprettGenerasjon(vedtaksperiodeId, generasjonId2)
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
            Generasjon.VidereBehandlingAvklares,
        )
        generasjonDao.opprettFor(
            generasjonId2,
            UUID.randomUUID(),
            UUID.randomUUID(),
            1.januar,
            Periode(1.januar, 31.januar),
            Generasjon.VidereBehandlingAvklares,
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
            Generasjon.VidereBehandlingAvklares,
        )
        generasjonDao.utbetalingFor(generasjonId, UTBETALING_ID)
        assertUtbetaling(generasjonId, UTBETALING_ID)
        generasjonDao.fjernUtbetalingFor(generasjonId)
        assertUtbetaling(generasjonId, null)
    }

    @Test
    fun `Lager innslag i opprinnelig_soknadsdato`() {
        generasjonDao.opprettFor(
            UUID.randomUUID(),
            VEDTAKSPERIODE,
            UUID.randomUUID(),
            1.januar,
            Periode(1.januar, 31.januar),
            Generasjon.VidereBehandlingAvklares,
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
            Generasjon.VedtakFattet,
        )
        val opprinneligSøknadsdato = finnSøknadMottatt(VEDTAKSPERIODE)
        generasjonDao.opprettFor(
            UUID.randomUUID(),
            VEDTAKSPERIODE,
            UUID.randomUUID(),
            1.januar,
            Periode(1.januar, 31.januar),
            Generasjon.VidereBehandlingAvklares,
        )

        assertEquals(opprinneligSøknadsdato, finnSøknadMottatt(VEDTAKSPERIODE))
    }

    @Test
    fun `Finner første generasjons VedtakFattet tidspunkt`() {
        val vedtaksperiodeEndretId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        generasjonDao.opprettFor(
            generasjonId,
            VEDTAKSPERIODE,
            vedtaksperiodeEndretId,
            1.januar,
            Periode(1.januar, 31.januar),
            Generasjon.VidereBehandlingAvklares,
        )
        generasjonDao.oppdaterTilstandFor(generasjonId, Generasjon.VedtakFattet.navn(), UUID.randomUUID())

        assertNotNull(generasjonDao.førsteGenerasjonVedtakFattetTidspunkt(VEDTAKSPERIODE))
    }

    private fun Pair<LocalDate, LocalDate>.tilPeriode() = Periode(first, second)

    @Test
    fun `Finner første kjente dato for person`() {
        opprettPerson()
        opprettArbeidsgiver()
        val førstePeriode = 1.januar to 5.januar
        val vedtaksperiodeId1 = UUID.randomUUID()
        opprettVedtaksperiode(vedtaksperiodeId1, fom = førstePeriode.first, tom = førstePeriode.second)
        generasjonDao.opprettFor(
            UUID.randomUUID(),
            vedtaksperiodeId1,
            UUID.randomUUID(),
            førstePeriode.first,
            førstePeriode.tilPeriode(),
            Generasjon.VedtakFattet,
        )

        val vedtaksperiodeId2 = UUID.randomUUID()
        val senerePeriode = 10.februar(2022) to 20.februar(2022)
        opprettVedtaksperiode(vedtaksperiodeId2, fom = senerePeriode.first, tom = senerePeriode.second)
        generasjonDao.opprettFor(
            UUID.randomUUID(),
            vedtaksperiodeId2,
            UUID.randomUUID(),
            senerePeriode.first,
            senerePeriode.tilPeriode(),
            Generasjon.VedtakFattet,
        )

        assertEquals(1.januar, generasjonDao.førsteKjenteDag(FNR))
    }

    private fun nyGenerasjonDto(
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        id: UUID = UUID.randomUUID(),
        utbetalingId: UUID? = UUID.randomUUID(),
        spleisBehandlingId: UUID? = UUID.randomUUID(),
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar,
        skjæringstidspunkt: LocalDate = 1.januar,
        tilstand: TilstandDto = TilstandDto.VidereBehandlingAvklares,
        tags: List<String> = emptyList(),
        varsler: List<VarselDto> = emptyList(),
    ): GenerasjonDto {
        return GenerasjonDto(
            id = id,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            spleisBehandlingId = spleisBehandlingId,
            skjæringstidspunkt = skjæringstidspunkt,
            fom = fom,
            tom = tom,
            tilstand = tilstand,
            tags = tags,
            varsler = varsler,
        )
    }

    private fun assertVarsler(
        generasjonId: UUID,
        vararg forventedeVarselkoder: String,
    ) {
        @Language("PostgreSQL")
        val query =
            "SELECT kode FROM selve_varsel WHERE generasjon_ref = (SELECT id FROM selve_vedtaksperiode_generasjon WHERE unik_id = ?)"

        val varselkoder =
            sessionOf(dataSource).use { session ->
                session.run(queryOf(query, generasjonId).map { it.string("kode") }.asList).toSet()
            }
        assertEquals(forventedeVarselkoder.toSet(), varselkoder)
    }

    private fun generasjonIdFor(vedtaksperiodeId: UUID): UUID {
        @Language("PostgreSQL")
        val query =
            "SELECT unik_id FROM selve_vedtaksperiode_generasjon WHERE vedtaksperiode_id = ? ORDER BY id"

        return sessionOf(dataSource).use { session ->
            session.run(queryOf(query, vedtaksperiodeId).map { it.uuid("unik_id") }.asList).single()
        }
    }

    private fun assertTilstand(
        vedtaksperiodeId: UUID,
        forventetTilstand: Generasjon.Tilstand,
    ) {
        val tilstand =
            sessionOf(dataSource).use { session ->
                @Language("PostgreSQL")
                val query =
                    "SELECT tilstand FROM selve_vedtaksperiode_generasjon WHERE vedtaksperiode_id = ?;"

                session.run(
                    queryOf(query, vedtaksperiodeId).map {
                        it.string("tilstand")
                    }.asSingle,
                )
            }

        assertEquals(forventetTilstand.navn(), tilstand)
    }

    private fun assertUtbetaling(
        generasjonId: UUID,
        forventetUtbetalingId: UUID?,
    ) {
        val utbetalingId =
            sessionOf(dataSource).use { session ->
                @Language("PostgreSQL")
                val query =
                    "SELECT utbetaling_id FROM selve_vedtaksperiode_generasjon WHERE unik_id = ?"

                session.run(
                    queryOf(query, generasjonId).map {
                        it.uuidOrNull("utbetaling_id")
                    }.asSingle,
                )
            }

        assertEquals(forventetUtbetalingId, utbetalingId)
    }

    private fun finnSøknadMottatt(vedtaksperiodeId: UUID) =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT soknad_mottatt FROM opprinnelig_soknadsdato WHERE vedtaksperiode_id = ?"
            session.run(
                queryOf(query, vedtaksperiodeId).map {
                    it.localDateTimeOrNull("soknad_mottatt")
                }.asSingle,
            )
        }

    private fun finnTidligsteGenerasjonOpprettetTidspunkt(vedtaksperiodeId: UUID) =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT min(opprettet_tidspunkt) as opprettet_tidspunkt FROM selve_vedtaksperiode_generasjon WHERE vedtaksperiode_id = ? GROUP BY vedtaksperiode_id"
            session.run(
                queryOf(query, vedtaksperiodeId).map {
                    it.localDateTimeOrNull("opprettet_tidspunkt")
                }.asSingle,
            )
        }
}
