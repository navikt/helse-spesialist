package no.nav.helse.modell

import DatabaseIntegrationTest
import java.util.UUID
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.TestHendelse
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.spesialist.api.oppgave.BESLUTTEROPPGAVE_PREFIX
import no.nav.helse.spesialist.api.oppgave.Oppgavetype
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import no.nav.helse.spesialist.api.vedtaksperiode.Inntektskilde as InntektskildeForApi
import no.nav.helse.spesialist.api.vedtaksperiode.Periodetype as PeriodetypeForApi

class OppgaveApiDaoTest : DatabaseIntegrationTest() {
    private companion object {
        private val CONTEXT_ID = UUID.randomUUID()
        private val TESTHENDELSE = TestHendelse(HENDELSE_ID, UUID.randomUUID(), FNR)
    }

    @BeforeEach
    fun setupDaoTest() {
        godkjenningsbehov(TESTHENDELSE.id)
        CommandContext(CONTEXT_ID).opprett(CommandContextDao(dataSource), TESTHENDELSE)
    }

    @Test
    fun `finner oppgaver`() {
        nyPerson()
        val oppgaver = oppgaveApiDao.finnOppgaver(SAKSBEHANDLERTILGANGER_MED_INGEN)
        val oppgave = oppgaver.first()
        assertTrue(oppgaver.isNotEmpty())
        assertEquals(oppgaveId.toString(), oppgave.oppgavereferanse)
    }

    @Test
    fun `inkluder risk qa oppgaver bare for supersaksbehandlere`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave(vedtaksperiodeId = VEDTAKSPERIODE, oppgavetype = Oppgavetype.RISK_QA)

        val oppgaver = oppgaveApiDao.finnOppgaver(SAKSBEHANDLERTILGANGER_MED_RISK)
        assertTrue(oppgaver.isNotEmpty())
        val oppgave = oppgaver.first()
        assertEquals("RISK_QA", oppgave.oppgavetype)
        assertEquals(oppgaveId.toString(), oppgave.oppgavereferanse)
        assertTrue(oppgaveApiDao.finnOppgaver(SAKSBEHANDLERTILGANGER_MED_INGEN).isEmpty())
    }

    @Test
    fun `ekskluder kode-7 oppgaver for vanlige saksbehandlere`() {
        opprettPerson(adressebeskyttelse = Adressebeskyttelse.Fortrolig)
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave(vedtaksperiodeId = VEDTAKSPERIODE)

        val oppgaver = oppgaveApiDao.finnOppgaver(SAKSBEHANDLERTILGANGER_MED_INGEN)
        assertTrue(oppgaver.isEmpty())
    }

    @Test
    fun `inkluder kode-7 oppgaver bare for noen utvalgte saksbehandlere`() {
        opprettPerson(adressebeskyttelse = Adressebeskyttelse.Fortrolig)
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave(vedtaksperiodeId = VEDTAKSPERIODE)

        val oppgaver = oppgaveApiDao.finnOppgaver(SAKSBEHANDLERTILGANGER_MED_KODE7)
        assertTrue(oppgaver.isNotEmpty())
    }

    @Test
    fun `ekskluder oppgaver med strengt fortrolig som adressebeskyttelse for alle saksbehandlere`() {
        opprettPerson(adressebeskyttelse = Adressebeskyttelse.StrengtFortrolig)
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave(vedtaksperiodeId = VEDTAKSPERIODE)

        val oppgaver = oppgaveApiDao.finnOppgaver(SAKSBEHANDLERTILGANGER_MED_KODE7)
        assertTrue(oppgaver.isEmpty())
    }

    @Test
    fun `ekskluder oppgaver med strengt fortrolig utland som adressebeskyttelse for alle saksbehandlere`() {
        opprettPerson(adressebeskyttelse = Adressebeskyttelse.StrengtFortroligUtland)
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave(vedtaksperiodeId = VEDTAKSPERIODE)

        val oppgaver = oppgaveApiDao.finnOppgaver(SAKSBEHANDLERTILGANGER_MED_KODE7)
        assertTrue(oppgaver.isEmpty())
    }

    @Test
    fun `ekskluder oppgaver med ukjent som adressebeskyttelse for alle saksbehandlere`() {
        opprettPerson(adressebeskyttelse = Adressebeskyttelse.Ukjent)
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave(vedtaksperiodeId = VEDTAKSPERIODE)

        val oppgaver = oppgaveApiDao.finnOppgaver(SAKSBEHANDLERTILGANGER_MED_KODE7)
        assertTrue(oppgaver.isEmpty())
    }

    @Test
    fun `sorterer STIKKPRØVE-oppgaver først, så RISK_QA, så resten, eldste først`() {
        opprettPerson()
        opprettArbeidsgiver()

        fun opprettVedtaksperiodeOgOppgave(periodetype: Periodetype, oppgavetype: Oppgavetype = OPPGAVETYPE) {
            val randomUUID = UUID.randomUUID()
            opprettVedtaksperiode(vedtaksperiodeId = randomUUID, periodetype = periodetype)
            opprettOppgave(vedtaksperiodeId = randomUUID, oppgavetype = oppgavetype)
        }

        opprettVedtaksperiodeOgOppgave(Periodetype.FØRSTEGANGSBEHANDLING)
        opprettVedtaksperiodeOgOppgave(Periodetype.FORLENGELSE, Oppgavetype.RISK_QA)
        opprettVedtaksperiodeOgOppgave(Periodetype.FØRSTEGANGSBEHANDLING, Oppgavetype.RISK_QA)
        opprettVedtaksperiodeOgOppgave(Periodetype.OVERGANG_FRA_IT, Oppgavetype.RISK_QA)
        opprettVedtaksperiodeOgOppgave(Periodetype.INFOTRYGDFORLENGELSE)
        opprettVedtaksperiodeOgOppgave(Periodetype.INFOTRYGDFORLENGELSE, Oppgavetype.RISK_QA)
        opprettVedtaksperiodeOgOppgave(Periodetype.FORLENGELSE, Oppgavetype.STIKKPRØVE)
        opprettVedtaksperiodeOgOppgave(Periodetype.OVERGANG_FRA_IT, Oppgavetype.STIKKPRØVE)

        val oppgaver = oppgaveApiDao.finnOppgaver(SAKSBEHANDLERTILGANGER_MED_RISK)
        oppgaver.filter { it.oppgavetype == "RISK_QA" }.let { riskoppgaver ->
            assertTrue(riskoppgaver.map { it.opprettet }.zipWithNext { a, b -> a <= b }.all { it }) {
                "Oops, skulle ha vært sortert stigende , men er det ikke: $riskoppgaver"
            }
        }
        oppgaver.filter { it.oppgavetype == "STIKKPRØVER" }.let { stikkprøver ->
            assertTrue(stikkprøver.map { it.opprettet }.zipWithNext { a, b -> a <= b }.all { it }) {
                "Oops, skulle ha vært sortert stigende , men er det ikke: $stikkprøver"
            }
        }
        listOf(
            "STIKKPRØVE" to PeriodetypeForApi.FORLENGELSE,
            "STIKKPRØVE" to PeriodetypeForApi.OVERGANG_FRA_IT,
            "RISK_QA" to PeriodetypeForApi.FORLENGELSE,
            "RISK_QA" to PeriodetypeForApi.FØRSTEGANGSBEHANDLING,
            "RISK_QA" to PeriodetypeForApi.OVERGANG_FRA_IT,
            "RISK_QA" to PeriodetypeForApi.INFOTRYGDFORLENGELSE,
            "SØKNAD" to PeriodetypeForApi.FØRSTEGANGSBEHANDLING,
            "SØKNAD" to PeriodetypeForApi.INFOTRYGDFORLENGELSE,
        ).let { ønsketRekkefølge ->
            assertEquals(ønsketRekkefølge.map { it.first }, oppgaver.map { it.oppgavetype })
            assertEquals(ønsketRekkefølge.map { it.second }, oppgaver.map { it.type })
        }
    }

    @Test
    fun `finner oppgaver med tildeling`() {
        nyPerson()
        assertEquals(
            null,
            oppgaveApiDao.finnOppgaver(SAKSBEHANDLERTILGANGER_MED_INGEN)
                .first().tildeling?.epost
        )
        saksbehandlerDao.opprettSaksbehandler(
            SAKSBEHANDLER_OID,
            "Navn Navnesen",
            SAKSBEHANDLEREPOST,
            SAKSBEHANDLER_IDENT
        )
        tildelingDao.opprettTildeling(oppgaveId, SAKSBEHANDLER_OID)
        assertEquals(
            SAKSBEHANDLEREPOST, oppgaveApiDao.finnOppgaver(SAKSBEHANDLERTILGANGER_MED_INGEN).first().tildeling?.epost
        )
        assertEquals(
            SAKSBEHANDLEREPOST, oppgaveApiDao.finnOppgaver(SAKSBEHANDLERTILGANGER_MED_INGEN).first().tildeling?.epost
        )
        assertEquals(
            false,
            oppgaveApiDao.finnOppgaver(SAKSBEHANDLERTILGANGER_MED_INGEN)
                .first().tildeling?.påVent
        )
        assertEquals(
            SAKSBEHANDLER_OID, oppgaveApiDao.finnOppgaver(SAKSBEHANDLERTILGANGER_MED_INGEN).first().tildeling?.oid
        )
    }

    @Test
    fun `en oppgave har riktig oppgavetype og inntektskilde`() {
        nyPerson(inntektskilde = Inntektskilde.FLERE_ARBEIDSGIVERE)
        val oppgaver = oppgaveApiDao.finnOppgaver(SAKSBEHANDLERTILGANGER_MED_RISK)
        assertEquals(PeriodetypeForApi.FØRSTEGANGSBEHANDLING, oppgaver.first().type)
        assertEquals(InntektskildeForApi.FLERE_ARBEIDSGIVERE, oppgaver.first().inntektskilde)
    }

    @Test
    fun `ikke tell varsler som er beslutteroppgaver`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave(contextId = CONTEXT_ID)
        opprettWarning(melding = "$BESLUTTEROPPGAVE_PREFIX Dette er feil")
        opprettWarning()

        val oppgave = oppgaveApiDao.finnOppgaver(SAKSBEHANDLERTILGANGER_MED_INGEN).first()
        assertEquals(1, oppgave.antallVarsler)
    }

}
