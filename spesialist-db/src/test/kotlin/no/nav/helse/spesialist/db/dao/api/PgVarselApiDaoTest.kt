package no.nav.helse.spesialist.db.dao.api

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.db.DbQuery
import no.nav.helse.db.api.PgVarselApiDao
import no.nav.helse.db.api.VarselDbDto
import no.nav.helse.db.api.VarselDbDto.VarseldefinisjonDbDto
import no.nav.helse.db.api.VarselDbDto.Varselstatus
import no.nav.helse.db.api.VarselDbDto.VarselvurderingDbDto
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.api.vedtaksperiode.Inntektskilde
import no.nav.helse.spesialist.api.vedtaksperiode.Periodetype
import no.nav.helse.spesialist.db.AbstractDatabaseTest
import no.nav.helse.spesialist.db.lagAktørId
import no.nav.helse.spesialist.db.lagEtternavn
import no.nav.helse.spesialist.db.lagFornavn
import no.nav.helse.spesialist.db.lagFødselsnummer
import no.nav.helse.spesialist.db.lagOrganisasjonsnavn
import no.nav.helse.spesialist.db.lagOrganisasjonsnummer
import no.nav.helse.spesialist.db.objectMapper
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
internal class PgVarselApiDaoTest : AbstractDatabaseTest() {
    private val apiVarselDao = PgVarselApiDao(dataSource)
    private val dbQuery = DbQuery(dataSource)
    private val NAVN = Navn(lagFornavn(), lagFornavn(), lagEtternavn())
    private val ENHET = Enhet(101, "Halden")
    private val PERIODE = Periode(UUID.randomUUID(), LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 31))
    private val FØDSELSNUMMER = lagFødselsnummer()
    private val AKTØRID = lagAktørId()
    private val ARBEIDSGIVER_NAVN = lagOrganisasjonsnavn()
    private val ORGANISASJONSNUMMER = lagOrganisasjonsnummer()

    @Test
    fun `Tom liste ved manglende varsler`() {
        assertTrue(apiVarselDao.finnVarslerSomIkkeErInaktiveFor(PERIODE.id, UUID.randomUUID()).isEmpty())
    }

    @Test
    fun `Finner varsler med vedtaksperiodeId og utbetalingId`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        opprettVarseldefinisjon(kode = "EN_KODE")
        opprettVarseldefinisjon(kode = "EN_ANNEN_KODE")
        val generasjonRef = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId, utbetalingId = utbetalingId)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef)
        nyttVarsel(kode = "EN_ANNEN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef)
        val varsler = apiVarselDao.finnVarslerSomIkkeErInaktiveFor(vedtaksperiodeId, utbetalingId)

        assertTrue(varsler.isNotEmpty())
        assertEquals(2, varsler.size)
    }

    @Test
    fun `Ignorerer varsel når varseldefinisjon mangler`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        opprettVarseldefinisjon(kode = "EN_KODE")
        val generasjonRef = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId, utbetalingId = utbetalingId)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef)
        nyttVarsel(kode = "EN_KODE_UTEN_DEFINISJON", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef)
        val varsler = apiVarselDao.finnVarslerSomIkkeErInaktiveFor(vedtaksperiodeId, utbetalingId)

        assertTrue(varsler.isNotEmpty())
        // Forventer kun varsler for varsel med definisjon, men det skal error-logges at definisjonen mangler.
        assertEquals(1, varsler.size)
    }

    @Test
    fun `Finner varsler som skal med for siste snapshotgenerasjon`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        opprettVarseldefinisjon(kode = "EN_KODE")
        opprettVarseldefinisjon(kode = "EN_ANNEN_KODE")
        val generasjonRef1 = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId)
        val generasjonRef2 = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId, utbetalingId = utbetalingId)
        val generasjonRef3 = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef1)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef2)
        nyttVarsel(kode = "EN_ANNEN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef2)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef3)
        nyttVarsel(
            kode = "EN_KODE_UTEN_DEFINISJON",
            vedtaksperiodeId = vedtaksperiodeId,
            generasjonRef = generasjonRef3
        )
        val varsler = apiVarselDao.finnVarslerSomIkkeErInaktiveForSisteGenerasjon(vedtaksperiodeId, utbetalingId)

        assertTrue(varsler.isNotEmpty())
        assertEquals(3, varsler.size)
    }

    @Test
    fun `Finner aktive varsler for uberegnet periode`() {
        val vedtaksperiodeId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        opprettVarseldefinisjon(kode = "EN_KODE")
        opprettVarseldefinisjon(kode = "EN_ANNEN_KODE")
        val generasjonRef1 = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef1)
        nyttVarsel(kode = "EN_ANNEN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef1)
        val generasjonRef2 = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef2)
        nyttVarsel(kode = "EN_ANNEN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef2)
        nyttVarsel(
            kode = "EN_KODE_UTEN_DEFINISJON",
            vedtaksperiodeId = vedtaksperiodeId,
            generasjonRef = generasjonRef2
        )
        val varsler = apiVarselDao.finnVarslerForUberegnetPeriode(vedtaksperiodeId)

        assertEquals(4, varsler.size)
    }

    @Test
    fun `Finner godkjente varsler for uberegnet periode`() {
        val vedtaksperiodeId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        opprettVarseldefinisjon(kode = "EN_KODE")
        opprettVarseldefinisjon(kode = "EN_ANNEN_KODE")
        val generasjonRef1 = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId)
        nyttVarsel(
            kode = "EN_KODE",
            status = "GODKJENT",
            vedtaksperiodeId = vedtaksperiodeId,
            generasjonRef = generasjonRef1
        )
        nyttVarsel(kode = "EN_ANNEN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef1)
        val generasjonRef2 = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef2)
        nyttVarsel(kode = "EN_ANNEN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef2)
        nyttVarsel(
            kode = "EN_KODE_UTEN_DEFINISJON",
            vedtaksperiodeId = vedtaksperiodeId,
            generasjonRef = generasjonRef2
        )
        val varsler = apiVarselDao.finnGodkjenteVarslerForUberegnetPeriode(vedtaksperiodeId)

        assertEquals(1, varsler.size)
    }

    @Test
    fun `Finner varsler for en gitt generasjon`() {
        val personRef = opprettPerson()
        val arbeidsgiverRef = opprettArbeidsgiver()
        opprettVedtaksperiode(personRef, arbeidsgiverRef)
        val definisjonId1 = UUID.randomUUID()
        val definisjonId2 = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        opprettVarseldefinisjon(kode = "EN_KODE", definisjonId = definisjonId1)
        opprettVarseldefinisjon(kode = "EN_ANNEN_KODE", definisjonId = definisjonId2)
        val generasjonRef = nyGenerasjon(vedtaksperiodeId = PERIODE.id, generasjonId)
        val varselId1 = UUID.randomUUID()
        val varselId2 = UUID.randomUUID()
        val varsel1Opprettet = LocalDateTime.of(2020, 1, 1, 12, 0, 0)
        val varsel2Opprettet = LocalDateTime.of(2020, 1, 1, 12, 0, 0)
        nyttVarsel(
            id = varselId1,
            vedtaksperiodeId = PERIODE.id,
            opprettet = varsel1Opprettet,
            kode = "EN_KODE",
            generasjonRef = generasjonRef
        )
        nyttVarsel(
            id = varselId2,
            vedtaksperiodeId = PERIODE.id,
            opprettet = varsel2Opprettet,
            kode = "EN_ANNEN_KODE",
            generasjonRef = generasjonRef
        )

        val forventetVarsel1 = VarselDbDto(
            varselId = varselId1,
            generasjonId = generasjonId,
            opprettet = varsel1Opprettet,
            kode = "EN_KODE",
            status = Varselstatus.AKTIV,
            varseldefinisjon = VarseldefinisjonDbDto(
                definisjonId = definisjonId1,
                tittel = "EN_TITTEL",
                forklaring = null,
                handling = null,
            ),
            varselvurdering = null,
        )
        val forventetVarsel2 = VarselDbDto(
            varselId = varselId2,
            generasjonId = generasjonId,
            opprettet = varsel2Opprettet,
            kode = "EN_ANNEN_KODE",
            status = Varselstatus.AKTIV,
            varseldefinisjon = VarseldefinisjonDbDto(
                definisjonId = definisjonId2,
                tittel = "EN_TITTEL",
                forklaring = null,
                handling = null,
            ),
            varselvurdering = null,
        )

        val varsler = apiVarselDao.finnVarslerFor(generasjonId)

        assertEquals(setOf(forventetVarsel1, forventetVarsel2), varsler)
    }

    @Test
    fun `Godkjenner vurderte varsler for en liste vedtaksperioder`() {
        val utbetalingId = UUID.randomUUID()
        val personRef = opprettPerson()
        val arbeidsgiverRef = opprettArbeidsgiver()
        opprettVedtaksperiode(personId = personRef, arbeidsgiverId = arbeidsgiverRef, utbetalingId = utbetalingId)
        val periode2 = Periode(UUID.randomUUID(), LocalDate.now(), LocalDate.now())
        opprettVedtaksperiode(personRef, arbeidsgiverRef, periode = periode2)
        val definisjonId1 = UUID.randomUUID()
        val generasjonId1 = UUID.randomUUID()
        val generasjonId2 = UUID.randomUUID()
        opprettVarseldefinisjon(kode = "EN_KODE", definisjonId = definisjonId1)
        val generasjonRef1 =
            nyGenerasjon(vedtaksperiodeId = PERIODE.id, generasjonId = generasjonId1, utbetalingId = utbetalingId)
        val generasjonRef2 =
            nyGenerasjon(vedtaksperiodeId = periode2.id, generasjonId = generasjonId2, utbetalingId = utbetalingId)
        val varselId1 = UUID.randomUUID()
        val varselId2 = UUID.randomUUID()
        nyttVarsel(id = varselId1, kode = "EN_KODE", vedtaksperiodeId = PERIODE.id, generasjonRef = generasjonRef1)
        nyttVarsel(id = varselId2, kode = "EN_KODE", vedtaksperiodeId = periode2.id, generasjonRef = generasjonRef2)
        apiVarselDao.settStatusVurdert(generasjonId1, definisjonId1, "EN_KODE", "EN_IDENT")
        apiVarselDao.settStatusVurdert(generasjonId2, definisjonId1, "EN_KODE", "EN_IDENT")

        apiVarselDao.godkjennVarslerFor(listOf(PERIODE.id, periode2.id))
        assertGodkjenteVarsler(generasjonRef1, 1)
        assertGodkjenteVarsler(generasjonRef2, 1)
    }

    @Test
    fun `Godkjenner ikke varsler med ulik utbetalingId gitt oppgaveId`() {
        val definisjonId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        opprettVedtaksperiode(
            personId = opprettPerson(),
            arbeidsgiverId = opprettArbeidsgiver(),
            utbetalingId = utbetalingId
        )
        opprettVarseldefinisjon(kode = "EN_KODE", definisjonId = definisjonId)
        val generasjonRef1 =
            nyGenerasjon(vedtaksperiodeId = PERIODE.id, generasjonId = generasjonId, utbetalingId = utbetalingId)
        val generasjonRef2 = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId, utbetalingId = UUID.randomUUID())
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = PERIODE.id, generasjonRef = generasjonRef1)
        nyttVarsel(kode = "EN_KODE", vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef2)
        apiVarselDao.settStatusVurdert(generasjonId, definisjonId, "EN_KODE", "EN_IDENT")
        apiVarselDao.godkjennVarslerFor(listOf(PERIODE.id))
        val antallGodkjenteVarsler = finnVarslerFor(Varselstatus.GODKJENT, PERIODE.id)

        assertEquals(1, antallGodkjenteVarsler)
    }

    @Test
    fun `Finner siste definisjon for varsler når definisjonsRef ikke finnes`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val definisjonId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        opprettVarseldefinisjon()
        opprettVarseldefinisjon(tittel = "EN_NY_TITTEL", definisjonId = definisjonId)
        val generasjonRef =
            nyGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = vedtaksperiodeId, utbetalingId = utbetalingId)
        val varselId = UUID.randomUUID()
        val varselOpprettet = LocalDateTime.of(2020, 1, 1, 12, 0, 0)
        nyttVarsel(
            id = varselId,
            vedtaksperiodeId = vedtaksperiodeId,
            opprettet = varselOpprettet,
            generasjonRef = generasjonRef
        )
        val forventetVarsel = VarselDbDto(
            varselId = varselId,
            generasjonId = generasjonId,
            opprettet = varselOpprettet,
            kode = "EN_KODE",
            status = Varselstatus.AKTIV,
            varseldefinisjon = VarseldefinisjonDbDto(
                definisjonId = definisjonId,
                tittel = "EN_NY_TITTEL",
                forklaring = null,
                handling = null,
            ),
            varselvurdering = null,
        )

        assertEquals(
            forventetVarsel,
            apiVarselDao.finnVarslerSomIkkeErInaktiveFor(vedtaksperiodeId, utbetalingId).single()
        )
    }

    @Test
    fun `Finner riktig definisjon for varsler når varselet har definisjonRef`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val definisjonId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val definisjonRef = opprettVarseldefinisjon(tittel = "EN_TITTEL", definisjonId = definisjonId)
        opprettVarseldefinisjon("EN_NY_TITTEL")
        val generasjonRef =
            nyGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = vedtaksperiodeId, utbetalingId = utbetalingId)
        val varselId = UUID.randomUUID()
        val varselOpprettet = LocalDateTime.of(2020, 1, 1, 12, 0, 0)
        nyttVarsel(
            id = varselId,
            vedtaksperiodeId = vedtaksperiodeId,
            opprettet = varselOpprettet,
            generasjonRef = generasjonRef,
            definisjonRef = definisjonRef
        )
        val forventetVarsel = VarselDbDto(
            varselId = varselId,
            generasjonId = generasjonId,
            opprettet = varselOpprettet,
            kode = "EN_KODE",
            status = Varselstatus.AKTIV,
            varseldefinisjon = VarseldefinisjonDbDto(
                definisjonId = definisjonId,
                tittel = "EN_TITTEL",
                forklaring = null,
                handling = null,
            ),
            varselvurdering = null,
        )

        assertEquals(
            forventetVarsel,
            apiVarselDao.finnVarslerSomIkkeErInaktiveFor(vedtaksperiodeId, utbetalingId).single()
        )
    }

    @Test
    fun `setter status til VURDERT`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val definisjonId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val definisjonRef = opprettVarseldefinisjon(definisjonId = definisjonId)
        val generasjonRef =
            nyGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = vedtaksperiodeId, utbetalingId = utbetalingId)
        val varselId = UUID.randomUUID()
        val varselOpprettet = LocalDateTime.of(2020, 1, 1, 12, 0, 0)
        nyttVarsel(
            id = varselId,
            vedtaksperiodeId = vedtaksperiodeId,
            opprettet = varselOpprettet,
            generasjonRef = generasjonRef,
            definisjonRef = definisjonRef
        )
        val varselEndret = LocalDateTime.of(2020, 1, 1, 12, 0, 0)
        val forventetVarsel = VarselDbDto(
            varselId = varselId,
            generasjonId = generasjonId,
            opprettet = varselOpprettet,
            kode = "EN_KODE",
            status = Varselstatus.VURDERT,
            varseldefinisjon = VarseldefinisjonDbDto(
                definisjonId = definisjonId,
                tittel = "EN_TITTEL",
                forklaring = null,
                handling = null,
            ),
            varselvurdering = VarselvurderingDbDto("EN_IDENT", varselEndret),
        )
        apiVarselDao.settStatusVurdert(generasjonId, definisjonId, "EN_KODE", "EN_IDENT", varselEndret)

        assertEquals(
            forventetVarsel,
            apiVarselDao.finnVarslerSomIkkeErInaktiveFor(vedtaksperiodeId, utbetalingId).single()
        )
    }

    @Test
    fun `kan ikke sette varsel til vurdert dersom det allerede er vurdert`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val definisjonId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val definisjonRef = opprettVarseldefinisjon(definisjonId = definisjonId)
        val generasjonRef =
            nyGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = vedtaksperiodeId, utbetalingId = utbetalingId)
        val varselId = UUID.randomUUID()
        val varselOpprettet = LocalDateTime.of(2020, 1, 1, 12, 0, 0)
        nyttVarsel(
            id = varselId,
            vedtaksperiodeId = vedtaksperiodeId,
            opprettet = varselOpprettet,
            generasjonRef = generasjonRef,
            definisjonRef = definisjonRef
        )
        val varselEndret = LocalDateTime.of(2020, 1, 1, 12, 0, 0)
        val oppdatertVarsel =
            apiVarselDao.settStatusVurdert(generasjonId, definisjonId, "EN_KODE", "EN_IDENT", varselEndret)
        val forsøktOppdatertVarsel = apiVarselDao.settStatusVurdert(generasjonId, definisjonId, "EN_KODE", "EN_IDENT")

        assertNotNull(oppdatertVarsel)
        assertEquals(
            VarselDbDto(
                varselId = varselId,
                generasjonId = generasjonId,
                opprettet = varselOpprettet,
                kode = "EN_KODE",
                status = Varselstatus.VURDERT,
                varseldefinisjon = VarseldefinisjonDbDto(
                    definisjonId = definisjonId,
                    tittel = "EN_TITTEL",
                    forklaring = null,
                    handling = null,
                ),
                varselvurdering = VarselvurderingDbDto("EN_IDENT", varselEndret),
            ),
            apiVarselDao.finnVarslerSomIkkeErInaktiveFor(vedtaksperiodeId, utbetalingId).single()
        )
        assertNull(forsøktOppdatertVarsel)
    }

    @Test
    fun `kan ikke sette varsel til vurdert dersom det er godkjent`() {
        val definisjonId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver(), utbetalingId)

        val definisjonRef = opprettVarseldefinisjon(definisjonId = definisjonId)
        val generasjonRef =
            nyGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = PERIODE.id, utbetalingId = utbetalingId)
        val varselId = UUID.randomUUID()
        val varselOpprettet = LocalDateTime.of(2020, 1, 1, 12, 0, 0)
        nyttVarsel(
            id = varselId,
            vedtaksperiodeId = PERIODE.id,
            opprettet = varselOpprettet,
            generasjonRef = generasjonRef,
            definisjonRef = definisjonRef
        )
        val varselEndret = LocalDateTime.of(2020, 1, 1, 12, 0, 0)
        val oppdatertVarsel =
            apiVarselDao.settStatusVurdert(generasjonId, definisjonId, "EN_KODE", "EN_IDENT", varselEndret)
        apiVarselDao.godkjennVarslerFor(listOf(PERIODE.id))
        val forsøktOppdatertVarsel = apiVarselDao.settStatusVurdert(generasjonId, definisjonId, "EN_KODE", "EN_IDENT")

        assertNotNull(oppdatertVarsel)
        assertEquals(
            VarselDbDto(
                varselId = varselId,
                generasjonId = generasjonId,
                opprettet = varselOpprettet,
                kode = "EN_KODE",
                status = Varselstatus.GODKJENT,
                varseldefinisjon = VarseldefinisjonDbDto(
                    definisjonId = definisjonId,
                    tittel = "EN_TITTEL",
                    forklaring = null,
                    handling = null,
                ),
                varselvurdering = VarselvurderingDbDto("EN_IDENT", varselEndret),
            ),
            apiVarselDao.finnVarslerSomIkkeErInaktiveFor(PERIODE.id, utbetalingId).single()
        )
        assertNull(forsøktOppdatertVarsel)
    }

    @Test
    fun `kan ikke sette varsel til AKTIV dersom det allerede er GODKJENT`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val definisjonId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val definisjonRef = opprettVarseldefinisjon(definisjonId = definisjonId)
        val generasjonRef =
            nyGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = vedtaksperiodeId, utbetalingId = utbetalingId)
        val varselId = UUID.randomUUID()
        val varselOpprettet = LocalDateTime.of(2020, 1, 1, 12, 0, 0)
        val varselGodkjent = LocalDateTime.of(2020, 1, 1, 12, 0, 0)
        nyttVarsel(
            id = varselId,
            vedtaksperiodeId = vedtaksperiodeId,
            opprettet = varselOpprettet,
            generasjonRef = generasjonRef,
            definisjonRef = definisjonRef,
            status = "GODKJENT",
            endretTidspunkt = varselGodkjent,
        )
        val forsøktOppdatertVarsel =
            apiVarselDao.settStatusAktiv(generasjonId, "EN_KODE", "EN_IDENT", LocalDateTime.of(2020, 1, 1, 12, 0, 0))

        assertEquals(
            VarselDbDto(
                varselId = varselId,
                generasjonId = generasjonId,
                opprettet = varselOpprettet,
                kode = "EN_KODE",
                status = Varselstatus.GODKJENT,
                varseldefinisjon = VarseldefinisjonDbDto(
                    definisjonId = definisjonId,
                    tittel = "EN_TITTEL",
                    forklaring = null,
                    handling = null,
                ),
                varselvurdering = VarselvurderingDbDto("EN_IDENT", varselGodkjent),
            ),
            apiVarselDao.finnVarslerSomIkkeErInaktiveFor(vedtaksperiodeId, utbetalingId).single()
        )
        assertNull(forsøktOppdatertVarsel)
    }

    @Test
    fun `setter status til AKTIV`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val definisjonId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val definisjonRef = opprettVarseldefinisjon(definisjonId = definisjonId)
        val generasjonRef =
            nyGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = vedtaksperiodeId, utbetalingId = utbetalingId)
        val varselId = UUID.randomUUID()
        val varselOpprettet = LocalDateTime.of(2020, 1, 1, 12, 0, 0)
        nyttVarsel(
            id = varselId,
            vedtaksperiodeId = vedtaksperiodeId,
            opprettet = varselOpprettet,
            generasjonRef = generasjonRef,
            definisjonRef = definisjonRef
        )
        val forventetVarsel = VarselDbDto(
            varselId = varselId,
            generasjonId = generasjonId,
            opprettet = varselOpprettet,
            kode = "EN_KODE",
            status = Varselstatus.AKTIV,
            varseldefinisjon = VarseldefinisjonDbDto(
                definisjonId = definisjonId,
                tittel = "EN_TITTEL",
                forklaring = null,
                handling = null,
            ),
            varselvurdering = null,
        )
        apiVarselDao.settStatusAktiv(generasjonId, "EN_KODE", "EN_IDENT")

        assertEquals(
            forventetVarsel,
            apiVarselDao.finnVarslerSomIkkeErInaktiveFor(vedtaksperiodeId, utbetalingId).single()
        )
    }

    @Test
    fun `vurder varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val generasjonRef =
            nyGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = vedtaksperiodeId, utbetalingId = utbetalingId)
        val varselId = UUID.randomUUID()
        nyttVarsel(id = varselId, vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef)
        val vurdering1 = finnVurderingFor(varselId)
        assertEquals(Varselstatus.AKTIV, vurdering1?.status)
        assertNull(vurdering1?.ident)
        assertNull(vurdering1?.tidspunkt)

        apiVarselDao.vurderVarselFor(varselId, Varselstatus.VURDERT, "ident")
        val vurdering2 = finnVurderingFor(varselId)
        assertEquals(Varselstatus.VURDERT, vurdering2?.status)
        assertEquals("ident", vurdering2?.ident)
        assertNotNull(vurdering2?.tidspunkt)
    }

    @Test
    fun `godkjenning av varsel setter ikke ident eller endret_tidspunkt`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        opprettVedtaksperiode(opprettPerson(), opprettArbeidsgiver())
        val generasjonRef =
            nyGenerasjon(generasjonId = generasjonId, vedtaksperiodeId = vedtaksperiodeId, utbetalingId = utbetalingId)
        val varselId = UUID.randomUUID()
        nyttVarsel(id = varselId, vedtaksperiodeId = vedtaksperiodeId, generasjonRef = generasjonRef)

        apiVarselDao.vurderVarselFor(varselId, Varselstatus.VURDERT, "ident")
        apiVarselDao.vurderVarselFor(varselId, Varselstatus.GODKJENT, "annen ident")
        val vurdering = finnVurderingFor(varselId)
        assertEquals(Varselstatus.GODKJENT, vurdering?.status)
        assertEquals("ident", vurdering?.ident)
        assertNotNull(vurdering?.tidspunkt)
    }

    private fun finnVarslerFor(status: Varselstatus, vedtaksperiodeId: UUID): Int =
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
                    enumValueOf(it.string("status")),
                    it.stringOrNull("status_endret_ident"),
                    it.localDateTimeOrNull("status_endret_tidspunkt")
                )
            }.asSingle)
        }
    }

    private fun opprettVedtaksperiode(
        personId: Long,
        arbeidsgiverId: Long,
        utbetalingId: UUID = UUID.randomUUID(),
        periode: Periode = PERIODE,
        skjæringstidspunkt: LocalDate = periode.fom,
        forkastet: Boolean = false,
        kanAvvises: Boolean = true,
    ) = opprettVedtak(personId, arbeidsgiverId, periode, skjæringstidspunkt, forkastet).also {
        klargjørVedtak(
            it,
            utbetalingId,
            periode,
            kanAvvises = kanAvvises,
        )
    }

    private fun opprettGenerasjon(
        periode: Periode,
        skjæringstidspunkt: LocalDate = periode.fom,
    ) = requireNotNull(
        dbQuery.update(
            """
            INSERT INTO behandling (unik_id, vedtaksperiode_id, opprettet_av_hendelse, tilstand, fom, tom, skjæringstidspunkt)
            VALUES (:unik_id, :vedtaksperiode_id, :hendelse_id, 'VidereBehandlingAvklares',:fom, :tom, :skjaeringstidspunkt)
            """.trimIndent(),
            "unik_id" to UUID.randomUUID(),
            "vedtaksperiode_id" to periode.id,
            "hendelse_id" to UUID.randomUUID(),
            "fom" to periode.fom,
            "tom" to periode.tom,
            "skjaeringstidspunkt" to skjæringstidspunkt,
        )
    )

    private fun opprettOpprinneligSøknadsdato(periode: Periode) = dbQuery.update(
        "INSERT INTO opprinnelig_soknadsdato VALUES (:vedtaksperiode_id, now())",
        "vedtaksperiode_id" to periode.id,
    )

    private fun opprettVedtak(
        personId: Long,
        arbeidsgiverId: Long,
        periode: Periode = PERIODE,
        skjæringstidspunkt: LocalDate = periode.fom,
        forkastet: Boolean = false,
    ): Long {
        opprettGenerasjon(periode, skjæringstidspunkt)
        opprettOpprinneligSøknadsdato(periode)
        return dbQuery.updateAndReturnGeneratedKey(
            """
            INSERT INTO vedtak (vedtaksperiode_id, fom, tom, arbeidsgiver_ref, person_ref, forkastet)
            VALUES (:id, :fom, :tom, :arbeidsgiverId, :personId, :forkastet)
            """.trimMargin(),
            "id" to periode.id,
            "fom" to periode.fom,
            "tom" to periode.tom,
            "arbeidsgiverId" to arbeidsgiverId,
            "personId" to personId,
            "forkastet" to forkastet,
        )!!
    }

    private fun opprettVarseldefinisjon(
        tittel: String = "EN_TITTEL",
        kode: String = "EN_KODE",
        definisjonId: UUID = UUID.randomUUID(),
    ): Long = requireNotNull(
        dbQuery.updateAndReturnGeneratedKey(
            """
            INSERT INTO api_varseldefinisjon (unik_id, kode, tittel, forklaring, handling, opprettet) 
            VALUES (:definisjonId, :kode, :tittel, null, null, :opprettet)
            """.trimIndent(),
            "definisjonId" to definisjonId,
            "kode" to kode,
            "tittel" to tittel,
            "opprettet" to LocalDateTime.now(),
        ),
    )

    private fun nyGenerasjon(
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        generasjonId: UUID = UUID.randomUUID(),
        utbetalingId: UUID = UUID.randomUUID(),
        periode: Periode = PERIODE,
        tilstandEndretTidspunkt: LocalDateTime? = null,
        skjæringstidspunkt: LocalDate = periode.fom,
    ): Long = requireNotNull(
        dbQuery.updateAndReturnGeneratedKey(
            """
            INSERT INTO behandling (vedtaksperiode_id, unik_id, utbetaling_id, opprettet_av_hendelse, tilstand_endret_tidspunkt, tilstand_endret_av_hendelse, tilstand, fom, tom, skjæringstidspunkt) 
            VALUES (:vedtaksperiodeId, :generasjonId, :utbetalingId, :opprettetAvHendelse, :tilstandEndretTidspunkt, :tilstandEndretAvHendelse, 'VidereBehandlingAvklares', :fom, :tom, :skjaeringstidspunkt)
            """.trimIndent(),
            "vedtaksperiodeId" to vedtaksperiodeId,
            "generasjonId" to generasjonId,
            "utbetalingId" to utbetalingId,
            "opprettetAvHendelse" to UUID.randomUUID(),
            "tilstandEndretTidspunkt" to tilstandEndretTidspunkt,
            "tilstandEndretAvHendelse" to UUID.randomUUID(),
            "fom" to periode.fom,
            "tom" to periode.tom,
            "skjaeringstidspunkt" to skjæringstidspunkt,
        )
    )

    private fun nyttVarsel(
        id: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        opprettet: LocalDateTime? = LocalDateTime.now(),
        kode: String = "EN_KODE",
        generasjonRef: Long,
        definisjonRef: Long? = null,
    ) = nyttVarsel(id, vedtaksperiodeId, opprettet, kode, generasjonRef, definisjonRef, "AKTIV", null)

    private fun nyttVarsel(
        id: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        opprettet: LocalDateTime? = LocalDateTime.now(),
        kode: String = "EN_KODE",
        generasjonRef: Long,
        definisjonRef: Long? = null,
        status: String,
        endretTidspunkt: LocalDateTime? = LocalDateTime.now(),
    ) = dbQuery.update(
        """
        INSERT INTO selve_varsel (unik_id, kode, vedtaksperiode_id, generasjon_ref, definisjon_ref, opprettet, status, status_endret_ident, status_endret_tidspunkt) 
        VALUES (:id, :kode, :vedtaksperiodeId, :generasjonRef, :definisjonRef, :opprettet, :status, :ident, :endretTidspunkt)
        """.trimIndent(),
        "id" to id,
        "kode" to kode,
        "vedtaksperiodeId" to vedtaksperiodeId,
        "generasjonRef" to generasjonRef,
        "definisjonRef" to definisjonRef,
        "opprettet" to opprettet,
        "status" to status,
        "ident" to if (endretTidspunkt != null) "EN_IDENT" else null,
        "endretTidspunkt" to endretTidspunkt,
    )

    private fun klargjørVedtak(
        vedtakId: Long,
        utbetalingId: UUID = UUID.randomUUID(),
        periode: Periode,
        kanAvvises: Boolean = true,
    ) {
        opprettSaksbehandleroppgavetype(Periodetype.FØRSTEGANGSBEHANDLING, Inntektskilde.EN_ARBEIDSGIVER, vedtakId)
        val hendelseId = UUID.randomUUID()
        opprettHendelse(hendelseId)
        opprettAutomatisering(false, vedtaksperiodeId = periode.id, hendelseId = hendelseId)
        opprettOppgave(Oppgavestatus.AvventerSaksbehandler, vedtakId, utbetalingId, kanAvvises = kanAvvises)
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
        fødselsnummer: String = FØDSELSNUMMER,
        aktørId: String = AKTØRID,
        adressebeskyttelse: Adressebeskyttelse = Adressebeskyttelse.Ugradert,
        bostedId: Int = ENHET.id,
        erEgenAnsatt: Boolean = false,
    ): Long {
        val personId = opprettMinimalPerson(fødselsnummer, aktørId)
        val personinfoid = opprettPersoninfo(adressebeskyttelse)
        val infotrygdutbetalingerid = opprettInfotrygdutbetalinger()
        oppdaterPersonpekere(fødselsnummer, personinfoid, infotrygdutbetalingerid)
        opprettEgenAnsatt(personId, erEgenAnsatt)
        oppdaterEnhet(personId, bostedId)
        return personId
    }

    private fun opprettMinimalPerson(
        fødselsnummer: String = FØDSELSNUMMER,
        aktørId: String = AKTØRID,
    ) = opprettHelPerson(fødselsnummer, aktørId, null, null, null)

    private fun opprettHelPerson(
        fødselsnummer: String,
        aktørId: String,
        personinfoid: Long?,
        bostedId: Int?,
        infotrygdutbetalingerid: Long?,
    ) = requireNotNull(
        dbQuery.updateAndReturnGeneratedKey(
            """
            INSERT INTO person (fødselsnummer, aktør_id, info_ref, enhet_ref, infotrygdutbetalinger_ref)
            VALUES (:foedselsnummer, :aktoerId, :personinfoId, :enhetId, :infotrygdutbetalingerId)
            """.trimIndent(),
            "foedselsnummer" to fødselsnummer,
            "aktoerId" to aktørId,
            "personinfoId" to personinfoid,
            "enhetId" to bostedId,
            "infotrygdutbetalingerId" to infotrygdutbetalingerid,
        )
    )

    private fun opprettPersoninfo(adressebeskyttelse: Adressebeskyttelse) = dbQuery.updateAndReturnGeneratedKey(
        """
        INSERT INTO person_info (fornavn, mellomnavn, etternavn, fodselsdato, kjonn, adressebeskyttelse)
        VALUES (:fornavn, :mellomnavn, :etternavn, :foedselsdato::date, :kjoenn::person_kjonn, :adressebeskyttelse)
        """.trimIndent(),
        "fornavn" to NAVN.fornavn,
        "mellomnavn" to NAVN.mellomnavn,
        "etternavn" to NAVN.etternavn,
        "foedselsdato" to LocalDate.of(1970, 1, 1),
        "kjoenn" to "Ukjent",
        "adressebeskyttelse" to adressebeskyttelse.name,
    )

    private fun oppdaterPersonpekere(
        fødselsnummer: String,
        personinfoId: Long? = null,
        infotrygdutbetalingerId: Long? = null,
    ) {
        dbQuery.update(
            """
            update person
            set info_ref=:personinfoId,
                infotrygdutbetalinger_ref=:infotrygdutbetalingerRef,
                personinfo_oppdatert = (
                    CASE 
                        when (:harPersoninfoId is not null) then now()
                    END
                ),
                infotrygdutbetalinger_oppdatert = (
                    CASE 
                        when (:harInfotrygdutbetalingerRef is not null) then now()
                    END
                )
            where fødselsnummer = :foedselsnummer
            """.trimIndent(),
            "personinfoId" to personinfoId,
            "harPersoninfoId" to (personinfoId != null),
            "infotrygdutbetalingerRef" to infotrygdutbetalingerId,
            "harInfotrygdutbetalingerRef" to (infotrygdutbetalingerId != null),
            "foedselsnummer" to fødselsnummer,
        )
    }

    private fun oppdaterEnhet(
        personId: Long,
        enhetNr: Int,
    ) = dbQuery.update(
        "update person set enhet_ref = :enhetNr, enhet_ref_oppdatert = now() where id = :personId",
        "enhetNr" to enhetNr,
        "personId" to personId,
    )

    private fun opprettEgenAnsatt(
        personId: Long,
        erEgenAnsatt: Boolean,
    ) = dbQuery.update(
        "INSERT INTO egen_ansatt VALUES (:personId, :erEgenAnsatt, now())",
        "personId" to personId,
        "erEgenAnsatt" to erEgenAnsatt,
    )

    private fun opprettArbeidsgiver(
        organisasjonsnummer: String = ORGANISASJONSNUMMER,
        bransjer: List<String> = emptyList(),
    ): Long {
        val bransjeId = opprettBransjer(bransjer)
        val navnId = opprettArbeidsgivernavn()

        return requireNotNull(
            dbQuery.updateAndReturnGeneratedKey(
                """
                INSERT INTO arbeidsgiver (organisasjonsnummer, navn_ref, bransjer_ref)
                VALUES (:organisasjonsnummer, :navnId, :bransjeId) ON CONFLICT DO NOTHING
                """.trimIndent(),
                "organisasjonsnummer" to organisasjonsnummer,
                "navnId" to navnId,
                "bransjeId" to bransjeId,
            )
        )
    }

    private fun opprettBransjer(bransjer: List<String>) = dbQuery.updateAndReturnGeneratedKey(
        "INSERT INTO arbeidsgiver_bransjer (bransjer) VALUES (:bransjer::json)",
        "bransjer" to objectMapper.writeValueAsString(bransjer),
    )

    private fun opprettArbeidsgivernavn() = dbQuery.updateAndReturnGeneratedKey(
        "INSERT INTO arbeidsgiver_navn (navn) VALUES (:arbeidsgivernavn)", "arbeidsgivernavn" to ARBEIDSGIVER_NAVN
    )

    private fun opprettInfotrygdutbetalinger() = dbQuery.updateAndReturnGeneratedKey(
        "INSERT INTO infotrygdutbetalinger (data) VALUES ('[]')"
    )

    private fun opprettOppgave(
        status: Oppgavestatus = Oppgavestatus.AvventerSaksbehandler,
        vedtakRef: Long,
        utbetalingId: UUID = UUID.randomUUID(),
        opprettet: LocalDateTime = LocalDateTime.now(),
        kanAvvises: Boolean = true,
    ): Long {
        val oppgaveId = dbQuery.updateAndReturnGeneratedKey(
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
        return requireNotNull(oppgaveId)
    }

    private fun opprettHendelse(
        hendelseId: UUID,
        fødselsnummer: String = FØDSELSNUMMER,
    ) = dbQuery.update(
        """
        INSERT INTO hendelse (id, data, type)
        VALUES (:hendelseId, :data::json, 'type')
        """.trimIndent(),
        "hendelseId" to hendelseId,
        "data" to """ { "fødselsnummer": "$fødselsnummer" } """
    )

    private fun opprettAutomatisering(
        automatisert: Boolean,
        stikkprøve: Boolean = false,
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
        utbetalingId: UUID = UUID.randomUUID(),
    ) = dbQuery.update(
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

    private fun assertGodkjenteVarsler(
        generasjonRef: Long,
        forventetAntall: Int,
    ) {
        val antall = dbQuery.single(
            "SELECT COUNT(1) FROM selve_varsel sv WHERE sv.generasjon_ref = :generasjonRef AND status = 'GODKJENT'",
            "generasjonRef" to generasjonRef
        ) { it.int(1) }
        assertEquals(forventetAntall, antall)
    }

    private data class TestVurdering(
        val status: Varselstatus,
        val ident: String?,
        val tidspunkt: LocalDateTime?
    )

    private data class Navn(
        val fornavn: String,
        val mellomnavn: String?,
        val etternavn: String,
    )

    private data class Enhet(
        val id: Int,
        val navn: String,
    )

    private data class Periode(
        val id: UUID,
        val fom: LocalDate,
        val tom: LocalDate,
    )
}
