package no.nav.helse.spesialist.db.dao.api

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.db.api.VarselDbDto
import no.nav.helse.db.api.VarselDbDto.VarseldefinisjonDbDto
import no.nav.helse.db.api.VarselDbDto.Varselstatus
import no.nav.helse.db.api.VarselDbDto.VarselvurderingDbDto
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import no.nav.helse.spesialist.api.vedtaksperiode.Inntektskilde
import no.nav.helse.spesialist.api.vedtaksperiode.Periodetype
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.testfixtures.feb
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.lagAktørId
import no.nav.helse.spesialist.domain.testfixtures.lagEtternavn
import no.nav.helse.spesialist.domain.testfixtures.lagFornavn
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnavn
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Isolated
internal class PgVarselApiDaoTest : AbstractDBIntegrationTest() {
    private val apiVarselDao = PgVarselApiDao(dataSource)

    private val varseldefinisjoner: List<TestVarseldefinisjon> =
        listOf("EN_KODE", "EN_ANNEN_KODE").map { kode ->
            TestVarseldefinisjon(
                kode = kode,
                dto = opprettVarseldefinisjon(kode = kode)
            )
        }

    @Test
    fun `Tom liste ved manglende varsler`() {
        // Given:
        // Ingenting satt opp

        // When:
        val varsler = apiVarselDao.finnVarslerSomIkkeErInaktiveFor(
            vedtaksperiodeId = UUID.randomUUID(),
            utbetalingId = UUID.randomUUID()
        )

        // Then:
        assertTrue(varsler.isEmpty())
    }

    @Test
    fun `Finner varsler med vedtaksperiodeId og utbetalingId`() {
        // Given:
        val utbetalingId = UUID.randomUUID()
        val vedtaksperiode = opprettVedtaksperiode(utbetalingId = utbetalingId)
        val generasjonId = opprettGenerasjon(vedtaksperiode = vedtaksperiode, utbetalingId = utbetalingId)
        opprettVarsel(
            varseldefinisjon = varseldefinisjoner[0],
            generasjonId = generasjonId,
            vedtaksperiodeId = vedtaksperiode.id
        )
        opprettVarsel(
            varseldefinisjon = varseldefinisjoner[1],
            generasjonId = generasjonId,
            vedtaksperiodeId = vedtaksperiode.id
        )

        // When:
        val varsler = apiVarselDao.finnVarslerSomIkkeErInaktiveFor(
            vedtaksperiodeId = vedtaksperiode.id,
            utbetalingId = utbetalingId
        )

        // Then:
        assertEquals(2, varsler.size)
    }

    @Test
    fun `Ignorerer varsel når varseldefinisjon mangler`() {
        // Given:
        val utbetalingId = UUID.randomUUID()
        val vedtaksperiode = opprettVedtaksperiode(utbetalingId = utbetalingId)
        val generasjonId = opprettGenerasjon(vedtaksperiode = vedtaksperiode, utbetalingId = utbetalingId)
        opprettVarsel(
            varseldefinisjon = varseldefinisjoner[0],
            generasjonId = generasjonId,
            vedtaksperiodeId = vedtaksperiode.id
        )
        opprettVarselUtenDefinisjon(
            varseldefinisjonKode = "EN_KODE_UTEN_DEFINISJON",
            generasjonId = generasjonId,
            vedtaksperiodeId = vedtaksperiode.id
        )

        // When:
        val varsler = apiVarselDao.finnVarslerSomIkkeErInaktiveFor(
            vedtaksperiodeId = vedtaksperiode.id,
            utbetalingId = utbetalingId
        )

        // Then:
        // Forventer kun varsler for varsel med definisjon, men det skal error-logges at definisjonen mangler.
        assertEquals(1, varsler.size)
    }

    @Test
    fun `Finner varsler som skal med for siste snapshotgenerasjon`() {
        // Given:
        val utbetalingId = UUID.randomUUID()
        val vedtaksperiode = opprettVedtaksperiode(utbetalingId = utbetalingId)

        val generasjonId1 = opprettGenerasjon(vedtaksperiode = vedtaksperiode, utbetalingId = UUID.randomUUID())
        opprettVarsel(
            varseldefinisjon = varseldefinisjoner[0],
            generasjonId = generasjonId1,
            vedtaksperiodeId = vedtaksperiode.id
        )

        val generasjonId2 = opprettGenerasjon(vedtaksperiode = vedtaksperiode, utbetalingId = utbetalingId)
        opprettVarsel(
            varseldefinisjon = varseldefinisjoner[0],
            generasjonId = generasjonId2,
            vedtaksperiodeId = vedtaksperiode.id
        )
        opprettVarsel(
            varseldefinisjon = varseldefinisjoner[1],
            generasjonId = generasjonId2,
            vedtaksperiodeId = vedtaksperiode.id
        )

        val generasjonId3 = opprettGenerasjon(vedtaksperiode = vedtaksperiode, utbetalingId = UUID.randomUUID())
        opprettVarsel(
            varseldefinisjon = varseldefinisjoner[0],
            generasjonId = generasjonId3,
            vedtaksperiodeId = vedtaksperiode.id
        )
        opprettVarselUtenDefinisjon(
            varseldefinisjonKode = "EN_KODE_UTEN_DEFINISJON",
            generasjonId = generasjonId3,
            vedtaksperiodeId = vedtaksperiode.id
        )

        // When:
        val varsler = apiVarselDao.finnVarslerSomIkkeErInaktiveForSisteGenerasjon(
            vedtaksperiodeId = vedtaksperiode.id,
            utbetalingId = utbetalingId
        )

        // Then:
        assertEquals(3, varsler.size)
    }

    @Test
    fun `Finner aktive varsler for uberegnet periode`() {
        // Given:
        val utbetalingId = UUID.randomUUID()
        val vedtaksperiode = opprettVedtaksperiode(utbetalingId = utbetalingId)

        val generasjonId1 = opprettGenerasjon(vedtaksperiode = vedtaksperiode, utbetalingId = UUID.randomUUID())
        opprettVarsel(
            varseldefinisjon = varseldefinisjoner[0],
            generasjonId = generasjonId1,
            vedtaksperiodeId = vedtaksperiode.id
        )
        opprettVarsel(
            varseldefinisjon = varseldefinisjoner[1],
            generasjonId = generasjonId1,
            vedtaksperiodeId = vedtaksperiode.id
        )

        val generasjonId2 = opprettGenerasjon(vedtaksperiode = vedtaksperiode, utbetalingId = UUID.randomUUID())
        opprettVarsel(
            varseldefinisjon = varseldefinisjoner[0],
            generasjonId = generasjonId2,
            vedtaksperiodeId = vedtaksperiode.id
        )
        opprettVarsel(
            varseldefinisjon = varseldefinisjoner[1],
            generasjonId = generasjonId2,
            vedtaksperiodeId = vedtaksperiode.id
        )
        opprettVarselUtenDefinisjon(
            varseldefinisjonKode = "EN_KODE_UTEN_DEFINISJON",
            generasjonId = generasjonId2,
            vedtaksperiodeId = vedtaksperiode.id
        )

        // When:
        val varsler = apiVarselDao.finnVarslerForUberegnetPeriode(vedtaksperiodeId = vedtaksperiode.id)

        // Then:
        assertEquals(4, varsler.size)
    }

    @Test
    fun `Finner godkjente varsler for uberegnet periode`() {
        // Given:
        val utbetalingId = UUID.randomUUID()
        val vedtaksperiode = opprettVedtaksperiode(utbetalingId = utbetalingId)

        val generasjonId1 = opprettGenerasjon(vedtaksperiode = vedtaksperiode, utbetalingId = UUID.randomUUID())
        opprettVarsel(
            status = Varselstatus.GODKJENT,
            endret = true,
            varseldefinisjon = varseldefinisjoner[0],
            generasjonId = generasjonId1,
            vedtaksperiodeId = vedtaksperiode.id
        )
        opprettVarsel(
            varseldefinisjon = varseldefinisjoner[1],
            generasjonId = generasjonId1,
            vedtaksperiodeId = vedtaksperiode.id
        )

        val generasjonId2 = opprettGenerasjon(vedtaksperiode = vedtaksperiode, utbetalingId = UUID.randomUUID())
        opprettVarsel(
            varseldefinisjon = varseldefinisjoner[0],
            generasjonId = generasjonId2,
            vedtaksperiodeId = vedtaksperiode.id
        )
        opprettVarsel(
            varseldefinisjon = varseldefinisjoner[1],
            generasjonId = generasjonId2,
            vedtaksperiodeId = vedtaksperiode.id
        )
        opprettVarselUtenDefinisjon(
            varseldefinisjonKode = "EN_KODE_UTEN_DEFINISJON",
            generasjonId = generasjonId2,
            vedtaksperiodeId = vedtaksperiode.id
        )

        // When:
        val varsler = apiVarselDao.finnGodkjenteVarslerForUberegnetPeriode(vedtaksperiodeId = vedtaksperiode.id)

        // Then:
        assertEquals(1, varsler.size)
    }

    @Test
    fun `Finner varsler for en gitt generasjon`() {
        // Given:
        val utbetalingId = UUID.randomUUID()
        val vedtaksperiode = opprettVedtaksperiode(utbetalingId = utbetalingId)

        val generasjonId = opprettGenerasjon(vedtaksperiode = vedtaksperiode, utbetalingId = utbetalingId)
        val varsel1 = opprettVarsel(
            varseldefinisjon = varseldefinisjoner[0],
            generasjonId = generasjonId,
            vedtaksperiodeId = vedtaksperiode.id
        )
        val varsel2 = opprettVarsel(
            varseldefinisjon = varseldefinisjoner[1],
            generasjonId = generasjonId,
            vedtaksperiodeId = vedtaksperiode.id
        )

        // When:
        val varsler = apiVarselDao.finnVarslerFor(generasjonId = generasjonId.unikId)

        // Then:
        assertEquals(setOf(varsel1, varsel2), varsler)
    }

    @Test
    fun `Godkjenner vurderte varsler for en liste vedtaksperioder`() {
        // Given:
        val utbetalingId = UUID.randomUUID()

        val vedtaksperiode1 = opprettVedtaksperiode(skjæringstidspunkt = 1 jan 2021, utbetalingId = utbetalingId)
        val generasjonId1 = opprettGenerasjon(vedtaksperiode = vedtaksperiode1, utbetalingId = utbetalingId)
        opprettVarsel(
            status = Varselstatus.VURDERT,
            varseldefinisjon = varseldefinisjoner[0],
            generasjonId = generasjonId1,
            vedtaksperiodeId = vedtaksperiode1.id
        )

        val vedtaksperiode2 = opprettVedtaksperiode(skjæringstidspunkt = 1 feb 2022, utbetalingId = utbetalingId)
        val generasjonId2 = opprettGenerasjon(vedtaksperiode = vedtaksperiode2, utbetalingId = utbetalingId)
        opprettVarsel(
            status = Varselstatus.VURDERT,
            varseldefinisjon = varseldefinisjoner[0],
            generasjonId = generasjonId2,
            vedtaksperiodeId = vedtaksperiode2.id
        )

        // When:
        apiVarselDao.godkjennVarslerFor(listOf(vedtaksperiode1, vedtaksperiode2).map(TestVedtaksperiodeDto::id))

        // Then:
        assertEquals(1, tellGodkjenteVarsel(generasjonId1))
        assertEquals(1, tellGodkjenteVarsel(generasjonId2))
    }

    @Test
    fun `Godkjenner ikke varsler med ulik utbetalingId gitt oppgaveId`() {
        // Given:
        val utbetalingId = UUID.randomUUID()

        val vedtaksperiode = opprettVedtaksperiode(utbetalingId = utbetalingId)
        val generasjonId1 = opprettGenerasjon(vedtaksperiode = vedtaksperiode, utbetalingId = utbetalingId)
        val generasjonId2 = opprettGenerasjon(vedtaksperiode = vedtaksperiode, utbetalingId = UUID.randomUUID())
        opprettVarsel(
            status = Varselstatus.VURDERT,
            endret = true,
            varseldefinisjon = varseldefinisjoner[0],
            generasjonId = generasjonId1,
            vedtaksperiodeId = vedtaksperiode.id
        )
        opprettVarsel(
            varseldefinisjon = varseldefinisjoner[0],
            generasjonId = generasjonId2,
            vedtaksperiodeId = vedtaksperiode.id
        )

        // When:
        apiVarselDao.godkjennVarslerFor(listOf(vedtaksperiode.id))

        // Then:
        assertEquals(
            1,
            tellVarslerMedStatus(status = Varselstatus.GODKJENT, vedtaksperiodeId = vedtaksperiode.id)
        )
    }

    @Test
    fun `setter status til VURDERT`() {
        val utbetalingId = UUID.randomUUID()
        val vedtaksperiode = opprettVedtaksperiode(utbetalingId = utbetalingId)
        val generasjonId = opprettGenerasjon(vedtaksperiode = vedtaksperiode, utbetalingId = utbetalingId)
        opprettVarsel(
            varseldefinisjon = varseldefinisjoner[0],
            generasjonId = generasjonId,
            vedtaksperiodeId = vedtaksperiode.id
        )
        val varselEndret = LocalDateTime.of(2020, 1, 1, 12, 0, 0)

        // When:
        apiVarselDao.settStatusVurdert(
            generasjonId = generasjonId.unikId,
            definisjonId = varseldefinisjoner[0].dto.definisjonId,
            varselkode = varseldefinisjoner[0].kode,
            ident = "EN_IDENT",
            endretTidspunkt = varselEndret
        )

        // Then:
        val lagretVarsel = apiVarselDao.finnVarslerSomIkkeErInaktiveFor(vedtaksperiode.id, utbetalingId).single()
        assertEquals(Varselstatus.VURDERT, lagretVarsel.status)
        assertEquals(VarselvurderingDbDto("EN_IDENT", varselEndret), lagretVarsel.varselvurdering)
    }

    @Test
    fun `kan ikke sette varsel til vurdert dersom det allerede er vurdert`() {
        val utbetalingId = UUID.randomUUID()
        val vedtaksperiode = opprettVedtaksperiode(utbetalingId = utbetalingId)
        val generasjonId = opprettGenerasjon(vedtaksperiode = vedtaksperiode, utbetalingId = utbetalingId)
        opprettVarsel(
            varseldefinisjon = varseldefinisjoner[0],
            generasjonId = generasjonId,
            vedtaksperiodeId = vedtaksperiode.id
        )
        val varselEndret = LocalDateTime.of(2020, 1, 1, 12, 0, 0)
        apiVarselDao.settStatusVurdert(
            generasjonId = generasjonId.unikId,
            definisjonId = varseldefinisjoner[0].dto.definisjonId,
            varselkode = varseldefinisjoner[0].kode,
            ident = "EN_IDENT",
            endretTidspunkt = varselEndret
        )

        // When:
        apiVarselDao.settStatusVurdert(
            generasjonId = generasjonId.unikId,
            definisjonId = varseldefinisjoner[0].dto.definisjonId,
            varselkode = varseldefinisjoner[0].kode,
            ident = "EN_ANNEN_IDENT",
            endretTidspunkt = varselEndret.plusDays(1L)
        )

        // Then:
        val lagretVarsel = apiVarselDao.finnVarslerSomIkkeErInaktiveFor(vedtaksperiode.id, utbetalingId).single()
        assertEquals(Varselstatus.VURDERT, lagretVarsel.status)
        assertEquals(VarselvurderingDbDto("EN_IDENT", varselEndret), lagretVarsel.varselvurdering)
    }

    @Test
    fun `kan ikke sette varsel til vurdert dersom det er godkjent`() {
        val utbetalingId = UUID.randomUUID()
        val vedtaksperiode = opprettVedtaksperiode(utbetalingId = utbetalingId)
        val generasjonId = opprettGenerasjon(vedtaksperiode = vedtaksperiode, utbetalingId = utbetalingId)
        opprettVarsel(
            varseldefinisjon = varseldefinisjoner[0],
            generasjonId = generasjonId,
            vedtaksperiodeId = vedtaksperiode.id
        )
        val varselEndret = LocalDateTime.of(2020, 1, 1, 12, 0, 0)
        apiVarselDao.settStatusVurdert(
            generasjonId = generasjonId.unikId,
            definisjonId = varseldefinisjoner[0].dto.definisjonId,
            varselkode = varseldefinisjoner[0].kode,
            ident = "EN_IDENT",
            endretTidspunkt = varselEndret
        )
        apiVarselDao.godkjennVarslerFor(listOf(vedtaksperiode.id))

        // When:
        apiVarselDao.settStatusVurdert(
            generasjonId = generasjonId.unikId,
            definisjonId = varseldefinisjoner[0].dto.definisjonId,
            varselkode = varseldefinisjoner[0].kode,
            ident = "EN_ANNEN_IDENT",
            endretTidspunkt = varselEndret.plusDays(1L)
        )

        // Then:
        val lagretVarsel = apiVarselDao.finnVarslerSomIkkeErInaktiveFor(vedtaksperiode.id, utbetalingId).single()
        assertEquals(Varselstatus.GODKJENT, lagretVarsel.status)
        assertEquals(VarselvurderingDbDto("EN_IDENT", varselEndret), lagretVarsel.varselvurdering)
    }

    @Test
    fun `kan ikke sette varsel til AKTIV dersom det allerede er GODKJENT`() {
        val utbetalingId = UUID.randomUUID()
        val vedtaksperiode = opprettVedtaksperiode(utbetalingId = utbetalingId)
        val generasjonId = opprettGenerasjon(vedtaksperiode = vedtaksperiode, utbetalingId = utbetalingId)
        opprettVarsel(
            varseldefinisjon = varseldefinisjoner[0],
            generasjonId = generasjonId,
            vedtaksperiodeId = vedtaksperiode.id
        )
        val varselEndret = LocalDateTime.of(2020, 1, 1, 12, 0, 0)
        apiVarselDao.settStatusVurdert(
            generasjonId = generasjonId.unikId,
            definisjonId = varseldefinisjoner[0].dto.definisjonId,
            varselkode = varseldefinisjoner[0].kode,
            ident = "EN_IDENT",
            endretTidspunkt = varselEndret
        )
        apiVarselDao.godkjennVarslerFor(listOf(vedtaksperiode.id))

        // When:
        apiVarselDao.settStatusAktiv(
            generasjonId = generasjonId.unikId,
            varselkode = varseldefinisjoner[0].kode,
            ident = "EN_ANNEN_IDENT",
            endretTidspunkt = varselEndret.plusDays(1L)
        )

        // Then:
        val lagretVarsel = apiVarselDao.finnVarslerSomIkkeErInaktiveFor(vedtaksperiode.id, utbetalingId).single()
        assertEquals(Varselstatus.GODKJENT, lagretVarsel.status)
        assertEquals(VarselvurderingDbDto("EN_IDENT", varselEndret), lagretVarsel.varselvurdering)
    }

    @Test
    fun `beholder status AKTIV hvis den allerede er AKTIV når den settes til AKTIV`() {
        val utbetalingId = UUID.randomUUID()
        val vedtaksperiode = opprettVedtaksperiode(utbetalingId = utbetalingId)
        val generasjonId = opprettGenerasjon(vedtaksperiode = vedtaksperiode, utbetalingId = utbetalingId)
        opprettVarsel(
            status = Varselstatus.AKTIV,
            varseldefinisjon = varseldefinisjoner[0],
            generasjonId = generasjonId,
            vedtaksperiodeId = vedtaksperiode.id
        )

        // When:
        apiVarselDao.settStatusAktiv(
            generasjonId = generasjonId.unikId,
            varselkode = varseldefinisjoner[0].kode,
            ident = "EN_ANNEN_IDENT",
            endretTidspunkt = (2 jan 2020).atTime(12, 0, 0)
        )

        // Then:
        val lagretVarsel = apiVarselDao.finnVarslerSomIkkeErInaktiveFor(vedtaksperiode.id, utbetalingId).single()
        assertEquals(Varselstatus.AKTIV, lagretVarsel.status)
        assertEquals(null, lagretVarsel.varselvurdering)
    }

    @Test
    fun `vurder varsel`() {
        val utbetalingId = UUID.randomUUID()
        val vedtaksperiode = opprettVedtaksperiode(utbetalingId = utbetalingId)
        val generasjonId = opprettGenerasjon(vedtaksperiode = vedtaksperiode, utbetalingId = utbetalingId)
        val varselId = opprettVarsel(
            status = Varselstatus.AKTIV,
            varseldefinisjon = varseldefinisjoner[0],
            generasjonId = generasjonId,
            vedtaksperiodeId = vedtaksperiode.id
        ).varselId
        val vurdering1 = finnVurderingFor(varselId)
        assertEquals(Varselstatus.AKTIV, vurdering1?.status)
        assertNull(vurdering1?.ident)
        assertNull(vurdering1?.tidspunkt)

        // When:
        apiVarselDao.vurderVarselFor(varselId, Varselstatus.VURDERT, "ident")

        // Then:
        val vurdering2 = finnVurderingFor(varselId)
        assertEquals(Varselstatus.VURDERT, vurdering2?.status)
        assertEquals("ident", vurdering2?.ident)
        assertNotNull(vurdering2?.tidspunkt)
    }

    @Test
    fun `godkjenning av varsel setter ikke ident eller endret_tidspunkt`() {
        val utbetalingId = UUID.randomUUID()
        val vedtaksperiode = opprettVedtaksperiode(utbetalingId = utbetalingId)
        val generasjonId = opprettGenerasjon(vedtaksperiode = vedtaksperiode, utbetalingId = utbetalingId)
        val varselId = opprettVarsel(
            varseldefinisjon = varseldefinisjoner[0],
            generasjonId = generasjonId,
            vedtaksperiodeId = vedtaksperiode.id
        ).varselId
        apiVarselDao.vurderVarselFor(varselId, Varselstatus.VURDERT, "ident")

        // When:
        apiVarselDao.vurderVarselFor(varselId, Varselstatus.GODKJENT, "annen ident")

        // Then:
        val vurdering = finnVurderingFor(varselId)
        assertEquals(Varselstatus.GODKJENT, vurdering?.status)
        assertEquals("ident", vurdering?.ident)
        assertNotNull(vurdering?.tidspunkt)
    }

    private fun opprettVedtaksperiode(
        utbetalingId: UUID,
        skjæringstidspunkt: LocalDate = 1 jan 2021
    ): TestVedtaksperiodeDto =
        TestVedtaksperiodeDto(
            id = UUID.randomUUID(),
            fom = skjæringstidspunkt,
            tom = skjæringstidspunkt.plusDays(30L),
            skjæringstidspunkt = skjæringstidspunkt
        ).also { dto ->
            opprettVedtaksperiode(
                personId = opprettPerson(
                    fødselsnummer = lagFødselsnummer(),
                    aktørId = lagAktørId()
                ),
                arbeidsgiverId = opprettArbeidsgiver(
                    organisasjonsnummer = lagOrganisasjonsnummer()
                ),
                utbetalingId = utbetalingId,
                vedtaksperiodeId = dto.id,
                fom = dto.fom,
                tom = dto.tom,
                skjæringstidspunkt = dto.skjæringstidspunkt,
                forkastet = false,
                kanAvvises = true
            )
        }

    private fun tellVarslerMedStatus(status: Varselstatus, vedtaksperiodeId: UUID): Int =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query =
                "SELECT count(1) FROM selve_varsel WHERE status = :status and vedtaksperiode_id = :vedtaksperiodeId;"
            return requireNotNull(
                session.run(
                    queryOf(
                        query,
                        mapOf("status" to status.name, "vedtaksperiodeId" to vedtaksperiodeId)
                    ).map { it.int(1) }.asSingle
                )
            )
        }

    private fun finnVurderingFor(varselId: UUID): TestVurdering? {
        @Language("PostgreSQL")
        val query = "SELECT status, status_endret_ident, status_endret_tidspunkt FROM selve_varsel WHERE unik_id = ?;"

        return sessionOf(dataSource).use { session ->
            session.run(queryOf(query, varselId).map {
                TestVurdering(
                    status = enumValueOf(it.string("status")),
                    ident = it.stringOrNull("status_endret_ident"),
                    tidspunkt = it.localDateTimeOrNull("status_endret_tidspunkt")
                )
            }.asSingle)
        }
    }

    private fun opprettVedtaksperiode(
        personId: Long,
        arbeidsgiverId: Long,
        utbetalingId: UUID,
        vedtaksperiodeId: UUID,
        fom: LocalDate,
        tom: LocalDate,
        skjæringstidspunkt: LocalDate,
        forkastet: Boolean,
        kanAvvises: Boolean
    ): Long = opprettVedtak(
        personId = personId,
        arbeidsgiverId = arbeidsgiverId,
        vedtaksperiodeId = vedtaksperiodeId,
        fom = fom,
        tom = tom,
        skjæringstidspunkt = skjæringstidspunkt,
        forkastet = forkastet
    ).also {
        klargjørVedtak(
            vedtakId = it,
            utbetalingId = utbetalingId,
            vedtaksperiodeId = vedtaksperiodeId,
            kanAvvises = kanAvvises
        )
    }

    private fun opprettGenerasjon(
        vedtaksperiodeId: UUID,
        fom: LocalDate,
        tom: LocalDate,
        skjæringstidspunkt: LocalDate
    ): Int = requireNotNull(
        dbQuery.update(
            """
                INSERT INTO behandling (unik_id, vedtaksperiode_id, opprettet_av_hendelse, tilstand, fom, tom, skjæringstidspunkt)
                VALUES (:unik_id, :vedtaksperiode_id, :hendelse_id, 'VidereBehandlingAvklares',:fom, :tom, :skjaeringstidspunkt)
                """.trimIndent(),
            "unik_id" to UUID.randomUUID(),
            "vedtaksperiode_id" to vedtaksperiodeId,
            "hendelse_id" to UUID.randomUUID(),
            "fom" to fom,
            "tom" to tom,
            "skjaeringstidspunkt" to skjæringstidspunkt,
        )
    )

    private fun insertOpprinneligSoknadsdato(vedtaksperiodeId: UUID): Int =
        dbQuery.update(
            """
                INSERT INTO opprinnelig_soknadsdato
                (vedtaksperiode_id, soknad_mottatt)
                VALUES
                (:vedtaksperiode_id, :soknad_mottatt)
            """.trimIndent(),
            "vedtaksperiode_id" to vedtaksperiodeId,
            "soknad_mottatt" to LocalDateTime.now(),
        )

    private fun opprettVedtak(
        personId: Long,
        arbeidsgiverId: Long,
        vedtaksperiodeId: UUID,
        fom: LocalDate,
        tom: LocalDate,
        skjæringstidspunkt: LocalDate,
        forkastet: Boolean
    ): Long {
        opprettGenerasjon(
            vedtaksperiodeId = vedtaksperiodeId,
            fom = fom,
            tom = tom,
            skjæringstidspunkt = skjæringstidspunkt
        )
        insertOpprinneligSoknadsdato(vedtaksperiodeId = vedtaksperiodeId)
        return dbQuery.updateAndReturnGeneratedKey(
            """
                INSERT INTO vedtak (vedtaksperiode_id, fom, tom, arbeidsgiver_ref, person_ref, forkastet)
                VALUES (:id, :fom, :tom, :arbeidsgiverId, :personId, :forkastet)
                """.trimMargin(),
            "id" to vedtaksperiodeId,
            "fom" to fom,
            "tom" to tom,
            "arbeidsgiverId" to arbeidsgiverId,
            "personId" to personId,
            "forkastet" to forkastet,
        )!!
    }

    private fun nyGenerasjon(
        vedtaksperiodeId: UUID,
        unikId: UUID,
        utbetalingId: UUID,
        fom: LocalDate,
        tom: LocalDate,
        tilstandEndretTidspunkt: LocalDateTime?,
        skjæringstidspunkt: LocalDate
    ): Long = requireNotNull(
        dbQuery.updateAndReturnGeneratedKey(
            """
                INSERT INTO behandling (vedtaksperiode_id, unik_id, utbetaling_id, opprettet_av_hendelse, tilstand_endret_tidspunkt, tilstand_endret_av_hendelse, tilstand, fom, tom, skjæringstidspunkt) 
                VALUES (:vedtaksperiodeId, :unik_id, :utbetalingId, :opprettetAvHendelse, :tilstandEndretTidspunkt, :tilstandEndretAvHendelse, 'VidereBehandlingAvklares', :fom, :tom, :skjaeringstidspunkt)
                """.trimIndent(),
            "vedtaksperiodeId" to vedtaksperiodeId,
            "unik_id" to unikId,
            "utbetalingId" to utbetalingId,
            "opprettetAvHendelse" to UUID.randomUUID(),
            "tilstandEndretTidspunkt" to tilstandEndretTidspunkt,
            "tilstandEndretAvHendelse" to UUID.randomUUID(),
            "fom" to fom,
            "tom" to tom,
            "skjaeringstidspunkt" to skjæringstidspunkt,
        )
    )

    private fun klargjørVedtak(
        vedtakId: Long,
        utbetalingId: UUID,
        vedtaksperiodeId: UUID,
        kanAvvises: Boolean
    ) {
        opprettSaksbehandleroppgavetype(Periodetype.FØRSTEGANGSBEHANDLING, Inntektskilde.EN_ARBEIDSGIVER, vedtakId)
        val hendelseId = UUID.randomUUID()
        opprettHendelse(
            hendelseId = hendelseId,
            fødselsnummer = lagFødselsnummer()
        )
        opprettAutomatisering(
            automatisert = false,
            stikkprøve = false,
            vedtaksperiodeId = vedtaksperiodeId,
            hendelseId = hendelseId,
            utbetalingId = UUID.randomUUID()
        )
        opprettOppgave(
            status = Oppgavestatus.AvventerSaksbehandler,
            vedtakRef = vedtakId,
            utbetalingId = utbetalingId,
            opprettet = LocalDateTime.now(),
            kanAvvises = kanAvvises
        )
    }

    private fun opprettSaksbehandleroppgavetype(
        type: Periodetype,
        inntektskilde: Inntektskilde,
        vedtakRef: Long,
    ) = dbQuery.update(
        "INSERT INTO saksbehandleroppgavetype (type, vedtak_ref, inntektskilde) VALUES (:type, :vedtakRef, :inntektskilde)",
        "type" to type.toString(),
        "vedtakRef" to vedtakRef,
        "inntektskilde" to inntektskilde.toString()
    )

    private fun opprettPerson(
        fødselsnummer: String,
        aktørId: String
    ): Long = insertPerson(
        fødselsnummer = fødselsnummer,
        aktørId = aktørId,
        infoRef = insertPersonInfo(
            fornavn = lagFornavn(),
            mellomnavn = lagFornavn(),
            etternavn = lagEtternavn(),
            foedselsdato = 1 jan 1970,
            kjoenn = "Ukjent",
            adressebeskyttelse = "Ugradert"
        ),
        infotrygdutbetalingerRef = insertInfotrygdutbetalinger(),
        enhetRef = 101
    ).also { personId ->
        insertEgenAnsatt(
            personRef = personId,
            erEgenAnsatt = false
        )
    }

    private fun insertPerson(
        fødselsnummer: String,
        aktørId: String,
        infoRef: Long,
        infotrygdutbetalingerRef: Long,
        enhetRef: Int
    ): Long = requireNotNull(
        dbQuery.updateAndReturnGeneratedKey(
            """
                    INSERT INTO person
                    (fødselsnummer, aktør_id, info_ref, personinfo_oppdatert, infotrygdutbetalinger_ref, infotrygdutbetalinger_oppdatert, enhet_ref, enhet_ref_oppdatert)
                    VALUES
                    (:foedselsnummer, :aktoer_id, :info_ref, now(), :infotrygdutbetalinger_ref, now(), :enhet_ref, now())
                """.trimIndent(),
            "foedselsnummer" to fødselsnummer,
            "aktoer_id" to aktørId,
            "info_ref" to infoRef,
            "infotrygdutbetalinger_ref" to infotrygdutbetalingerRef,
            "enhet_ref" to enhetRef,
        )
    )

    private fun insertPersonInfo(
        fornavn: String,
        mellomnavn: String,
        etternavn: String,
        foedselsdato: LocalDate,
        kjoenn: String,
        adressebeskyttelse: String
    ): Long = requireNotNull(
        dbQuery.updateAndReturnGeneratedKey(
            """
            INSERT INTO person_info (fornavn, mellomnavn, etternavn, fodselsdato, kjonn, adressebeskyttelse)
            VALUES (:fornavn, :mellomnavn, :etternavn, :foedselsdato::date, :kjoenn::person_kjonn, :adressebeskyttelse)
            """.trimIndent(),
            "fornavn" to fornavn,
            "mellomnavn" to mellomnavn,
            "etternavn" to etternavn,
            "foedselsdato" to foedselsdato,
            "kjoenn" to kjoenn,
            "adressebeskyttelse" to adressebeskyttelse,
        )
    )

    private fun insertEgenAnsatt(
        personRef: Long,
        erEgenAnsatt: Boolean,
    ): Int = dbQuery.update(
        "INSERT INTO egen_ansatt (person_ref, er_egen_ansatt, opprettet) VALUES (:person_ref, :er_egen_ansatt, now())",
        "person_ref" to personRef,
        "er_egen_ansatt" to erEgenAnsatt,
    )

    private fun opprettArbeidsgiver(
        organisasjonsnummer: String
    ): Long = requireNotNull(
        dbQuery.updateAndReturnGeneratedKey(
            """
                INSERT INTO arbeidsgiver (organisasjonsnummer, navn_ref)
                VALUES (:organisasjonsnummer, :navnId) ON CONFLICT DO NOTHING
                """.trimIndent(),
            "organisasjonsnummer" to organisasjonsnummer,
            "navnId" to opprettArbeidsgivernavn(),
        )
    )

    private fun opprettArbeidsgivernavn(): Long = requireNotNull(
        dbQuery.updateAndReturnGeneratedKey(
            "INSERT INTO arbeidsgiver_navn (navn) VALUES (:arbeidsgivernavn)",
            "arbeidsgivernavn" to lagOrganisasjonsnavn()
        )
    )

    private fun insertInfotrygdutbetalinger(): Long = requireNotNull(
        dbQuery.updateAndReturnGeneratedKey(
            "INSERT INTO infotrygdutbetalinger (data) VALUES ('[]')"
        )
    )

    private fun opprettOppgave(
        status: Oppgavestatus,
        vedtakRef: Long,
        utbetalingId: UUID,
        opprettet: LocalDateTime,
        kanAvvises: Boolean
    ): Long = requireNotNull(
        dbQuery.updateAndReturnGeneratedKey(
            """
                INSERT INTO oppgave (utbetaling_id, opprettet, oppdatert, status, vedtak_ref, hendelse_id_godkjenningsbehov, kan_avvises)
                VALUES (:utbetalingId, :opprettet, now(), CAST(:status as oppgavestatus), :vedtakRef, :godkjenningsbehovId, :kanAvvises)
                """.trimIndent(),
            "utbetalingId" to utbetalingId,
            "opprettet" to opprettet,
            "status" to status.name,
            "vedtakRef" to vedtakRef,
            "godkjenningsbehovId" to UUID.randomUUID(),
            "kanAvvises" to kanAvvises,
        )
    )

    private fun opprettHendelse(
        hendelseId: UUID,
        fødselsnummer: String
    ): Int = dbQuery.update(
        """
            INSERT INTO hendelse (id, data, type)
            VALUES (:hendelseId, :data::json, 'type')
            """.trimIndent(),
        "hendelseId" to hendelseId,
        "data" to """ { "fødselsnummer": "$fødselsnummer" } """
    )

    private fun opprettAutomatisering(
        automatisert: Boolean,
        stikkprøve: Boolean,
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
        utbetalingId: UUID
    ): Int = dbQuery.update(
        """
            INSERT INTO automatisering (vedtaksperiode_ref, hendelse_ref, automatisert, stikkprøve, utbetaling_id)
            VALUES ((SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId), :hendelseId, :automatisert, :stikkproeve, :utbetalingId);
            """.trimIndent(),
        "vedtaksperiodeId" to vedtaksperiodeId,
        "hendelseId" to hendelseId,
        "automatisert" to automatisert,
        "stikkproeve" to stikkprøve,
        "utbetalingId" to utbetalingId,
    )

    private data class TestVurdering(
        val status: Varselstatus,
        val ident: String?,
        val tidspunkt: LocalDateTime?
    )

    private fun opprettVarsel(
        status: Varselstatus = Varselstatus.AKTIV,
        endret: Boolean = false,
        varseldefinisjon: TestVarseldefinisjon,
        generasjonId: TestGenerasjonId,
        vedtaksperiodeId: UUID,
    ): VarselDbDto = opprettVarsel(
        status = status,
        endret = endret,
        varseldefinisjonKode = varseldefinisjon.kode,
        varseldefinisjon = varseldefinisjon.dto,
        generasjonId = generasjonId,
        vedtaksperiodeId = vedtaksperiodeId
    )

    private fun opprettVarselUtenDefinisjon(
        status: Varselstatus = Varselstatus.AKTIV,
        endret: Boolean = false,
        varseldefinisjonKode: String,
        generasjonId: TestGenerasjonId,
        vedtaksperiodeId: UUID,
    ) {
        opprettVarsel(
            status = status,
            endret = endret,
            varseldefinisjonKode = varseldefinisjonKode,
            varseldefinisjon = null,
            generasjonId = generasjonId,
            vedtaksperiodeId = vedtaksperiodeId
        )
    }

    private fun opprettVarsel(
        status: Varselstatus,
        endret: Boolean,
        varseldefinisjonKode: String,
        varseldefinisjon: VarseldefinisjonDbDto?,
        generasjonId: TestGenerasjonId,
        vedtaksperiodeId: UUID,
    ): VarselDbDto = VarselDbDto(
        varselId = UUID.randomUUID(),
        generasjonId = generasjonId.unikId,
        opprettet = (1 jan 2020).atTime(12, 0, 0),
        kode = varseldefinisjonKode,
        status = status,
        varseldefinisjon = varseldefinisjon,
        varselvurdering = null,
    ).also {
        insertSelveVarsel(
            dto = it,
            generasjonId = generasjonId.id,
            vedtaksperiodeId = vedtaksperiodeId,
            endret = endret
        )
    }

    private class TestVarseldefinisjon(val kode: String, val dto: VarseldefinisjonDbDto)
    private class TestGenerasjonId(val id: Long, val unikId: UUID)

    private fun opprettGenerasjon(vedtaksperiode: TestVedtaksperiodeDto, utbetalingId: UUID): TestGenerasjonId =
        UUID.randomUUID().let { generasjonUnikId ->
            TestGenerasjonId(
                id = nyGenerasjon(
                    vedtaksperiodeId = vedtaksperiode.id,
                    unikId = generasjonUnikId,
                    utbetalingId = utbetalingId,
                    fom = vedtaksperiode.fom,
                    tom = vedtaksperiode.fom,
                    tilstandEndretTidspunkt = null,
                    skjæringstidspunkt = vedtaksperiode.skjæringstidspunkt
                ),
                unikId = generasjonUnikId
            )

        }

    private fun insertSelveVarsel(
        dto: VarselDbDto,
        generasjonId: Long,
        vedtaksperiodeId: UUID,
        endret: Boolean,
    ) {
        dbQuery.update(
            """
            INSERT INTO selve_varsel (unik_id, kode, vedtaksperiode_id, generasjon_ref, opprettet, status, status_endret_ident, status_endret_tidspunkt) 
            VALUES (:unik_id, :kode, :vedtaksperiode_id, :generasjon_ref, :opprettet, :status, :status_endret_ident, :status_endret_tidspunkt)
            """.trimIndent(),
            "unik_id" to dto.varselId,
            "kode" to dto.kode,
            "vedtaksperiode_id" to vedtaksperiodeId,
            "generasjon_ref" to generasjonId,
            "opprettet" to dto.opprettet,
            "status" to dto.status.name,
            "status_endret_ident" to "EN_IDENT".takeIf { endret },
            "status_endret_tidspunkt" to LocalDateTime.now().takeIf { endret },
        )
    }

    private fun opprettVarseldefinisjon(kode: String) =
        VarseldefinisjonDbDto(
            definisjonId = UUID.randomUUID(),
            tittel = "EN_TITTEL",
            forklaring = null,
            handling = null,
        ).also { insertApiVarseldefinisjon(kode = kode, dto = it) }

    private fun insertApiVarseldefinisjon(kode: String, dto: VarseldefinisjonDbDto) {
        requireNotNull(
            dbQuery.updateAndReturnGeneratedKey(
                """
                INSERT INTO api_varseldefinisjon (unik_id, kode, tittel, forklaring, handling, opprettet) 
                VALUES (:unik_id, :kode, :tittel, :forklaring, :handling, :opprettet)
                """.trimIndent(),
                "unik_id" to dto.definisjonId,
                "kode" to kode,
                "tittel" to dto.tittel,
                "forklaring" to dto.forklaring,
                "handling" to dto.handling,
                "opprettet" to LocalDateTime.now(),
            ),
        )
    }

    private fun tellGodkjenteVarsel(generasjonId: TestGenerasjonId): Int =
        dbQuery.single(
            "SELECT COUNT(*) FROM selve_varsel sv WHERE sv.generasjon_ref = :generasjonRef AND status = 'GODKJENT'",
            "generasjonRef" to generasjonId.id
        ) { it.int(1) }

    class TestVedtaksperiodeDto(val id: UUID, val fom: LocalDate, val tom: LocalDate, val skjæringstidspunkt: LocalDate)
}
