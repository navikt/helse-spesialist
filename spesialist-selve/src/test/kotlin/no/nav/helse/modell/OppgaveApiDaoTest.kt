package no.nav.helse.modell

import DatabaseIntegrationTest
import java.sql.SQLException
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.TestHendelse
import no.nav.helse.modell.vedtaksperiode.Generasjon
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.spesialist.api.graphql.schema.Mottaker.ARBEIDSGIVER
import no.nav.helse.spesialist.api.graphql.schema.Mottaker.BEGGE
import no.nav.helse.spesialist.api.graphql.schema.Mottaker.SYKMELDT
import no.nav.helse.spesialist.api.graphql.schema.Oppgavetype.RISK_QA
import no.nav.helse.spesialist.api.graphql.schema.Periodetype.FORSTEGANGSBEHANDLING
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import no.nav.helse.spesialist.api.oppgave.Oppgavetype
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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
    fun `finner oppgavetype`() {
        nyPerson()
        val type = oppgaveApiDao.finnOppgavetype(VEDTAKSPERIODE)
        assertEquals(OPPGAVETYPE, type)
    }

    @Test
    fun `finner oppgavetype når det fins flere oppgaver for en vedtaksperiode`() {
        nyPerson()
        oppgaveDao.updateOppgave(oppgaveId, Oppgavestatus.Invalidert)
        opprettOppgave(utbetalingId = UUID.randomUUID())

        val type = oppgaveApiDao.finnOppgavetype(VEDTAKSPERIODE)
        assertEquals(OPPGAVETYPE, type)
    }

    @Test
    fun `finner oppgaver`() {
        nyPerson()
        val oppgaver = oppgaveApiDao.finnOppgaver(SAKSBEHANDLERTILGANGER_MED_INGEN)
        val oppgave = oppgaver.first()
        assertTrue(oppgaver.isNotEmpty())
        assertEquals(oppgaveId.toString(), oppgave.id)
    }

    @Test
    fun `invaliderer oppgave`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave()
        val oppgaveId1 = oppgaveId

        oppgaveApiDao.invaliderOppgaveFor(fødselsnummer = FNR)

        assertOppgaveStatus(oppgaveId1, "Invalidert")
    }

    @Test
    fun `Finner oppgaveId basert på vedtaksperiodeId`() {
        nyPerson()
        val oppgaveId = oppgaveApiDao.finnOppgaveId(VEDTAKSPERIODE)
        assertNotNull(oppgaveId)
        assertEquals(this.oppgaveId, oppgaveId)
    }

    @Test
    fun `Finner ikke oppgaveId basert på vedtaksperiodeId dersom vedtaksperiode ikke finnes`() {
        opprettPerson()
        opprettArbeidsgiver()
        val oppgaveId = oppgaveApiDao.finnOppgaveId(VEDTAKSPERIODE)
        assertNull(oppgaveId)
    }

    @Test
    fun `Får feil dersom det finnes flere oppgaver som avventer saksbehandler for en person`() {
        nyPerson()
        opprettOppgave()
        assertThrows<SQLException> {
            oppgaveApiDao.finnOppgaveId(VEDTAKSPERIODE)
        }
    }

    @Test
    fun `Finner oppgave basert på fødselsnummer`() {
        nyPerson()
        val oppgaveId = oppgaveApiDao.finnOppgaveId(FNR)
        assertNotNull(oppgaveId)
    }

    @Test
    fun `Feiler på oppslag på oppgave om det fins flere oppgaver for personen`() {
        nyPerson()
        opprettOppgave()
        assertThrows<SQLException> {
            oppgaveApiDao.finnOppgaveId(FNR)
        }
    }

    @Test
    fun `Finner ikke oppgave basert på fødselsnummer dersom person ikke finnes`() {
        val oppgaveId = oppgaveApiDao.finnOppgaveId(FNR)
        assertNull(oppgaveId)
    }

    @Test
    fun `inkluder risk qa oppgaver bare for supersaksbehandlere`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettGenerasjon()
        opprettVedtaksperiode()
        opprettOppgave(vedtaksperiodeId = VEDTAKSPERIODE, oppgavetype = Oppgavetype.RISK_QA)

        val oppgaver = oppgaveApiDao.finnOppgaver(SAKSBEHANDLERTILGANGER_MED_RISK)
        assertTrue(oppgaver.isNotEmpty())
        val oppgave = oppgaver.first()
        assertEquals(RISK_QA, oppgave.type)
        assertEquals(oppgaveId.toString(), oppgave.id)
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
        opprettGenerasjon()
        opprettVedtaksperiode()
        opprettOppgave(vedtaksperiodeId = VEDTAKSPERIODE)

        val oppgaver = oppgaveApiDao.finnOppgaver(SAKSBEHANDLERTILGANGER_MED_KODE7)
        assertTrue(oppgaver.isNotEmpty())
    }

    @Test
    fun `ekskluder stikkprøve-oppgaver for vanlige saksbehandlere`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave(oppgavetype = Oppgavetype.STIKKPRØVE)

        val oppgaver = oppgaveApiDao.finnOppgaver(SAKSBEHANDLERTILGANGER_MED_INGEN)
        assertTrue(oppgaver.isEmpty())
    }

    @Test
    fun `inkluder stikkprøve-oppgaver bare for noen utvalgte saksbehandlere`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettGenerasjon()
        opprettVedtaksperiode()
        opprettOppgave(oppgavetype = Oppgavetype.STIKKPRØVE)

        val oppgaver = oppgaveApiDao.finnOppgaver(SAKSBEHANDLERTILGANGER_MED_STIKKPRØVE)
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
    fun `En oppgave får haster true dersom det finnes et varsel om negativt beløp og det er delvis refusjon eller utbetaling til sykmeldt`() {
        nyPerson(inntektskilde = Inntektskilde.FLERE_ARBEIDSGIVERE)
        val arbeidsgiverFagsystemId = fagsystemId()
        val personFagsystemId = fagsystemId()
        val arbeidsgiverOppdragId = lagArbeidsgiveroppdrag(arbeidsgiverFagsystemId)
        val personOppdragId = lagPersonoppdrag(personFagsystemId)
        lagUtbetalingId(arbeidsgiverOppdragId, personOppdragId, UTBETALING_ID, 2000, 2000)

        generasjonDao.finnSisteGenerasjonFor(VEDTAKSPERIODE)?.also {
            generasjonDao.oppdaterTilstandFor(generasjonId = it, ny = Generasjon.Låst, endretAv = UUID.randomUUID())
        }
         val generasjonRef = nyGenerasjon(vedtaksperiodeId = VEDTAKSPERIODE, utbetalingId = UTBETALING_ID)
        nyttVarsel(kode = "RV_UT_23", generasjonId = generasjonRef, vedtaksperiodeId = VEDTAKSPERIODE)
        ferdigstillSistOpprettedeOppgaveOgOpprettNy()
        val oppgaver = oppgaveApiDao.finnOppgaver(SAKSBEHANDLERTILGANGER_MED_RISK)
        assertTrue(oppgaver.first().haster ?: false)
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
                .first().tildeling?.paaVent
        )
        assertEquals(
            SAKSBEHANDLER_OID.toString(),
            oppgaveApiDao.finnOppgaver(SAKSBEHANDLERTILGANGER_MED_INGEN).first().tildeling?.oid
        )
    }

    @Test
    fun `en oppgave har riktig oppgavetype og inntektskilde`() {
        nyPerson(inntektskilde = Inntektskilde.FLERE_ARBEIDSGIVERE)
        val oppgaver = oppgaveApiDao.finnOppgaver(SAKSBEHANDLERTILGANGER_MED_RISK)
        assertEquals(FORSTEGANGSBEHANDLING, oppgaver.first().periodetype)
        assertTrue(oppgaver.first().flereArbeidsgivere)
    }

    @Test
    fun `bruker tidspunkt fra tidligste generasjon`() {
        nyPerson()
        opprettGenerasjon()
        opprettGenerasjon()
        val generasjonstidspunkt = finnOpprettetTidspunkterFor(VEDTAKSPERIODE).first()
        val oppgave = oppgaveApiDao.finnOppgaver(SAKSBEHANDLERTILGANGER_MED_INGEN).first()

        assertEquals(generasjonstidspunkt, oppgave.opprinneligSoknadsdato)
    }

    @Test
    fun `Mottaker er null når utbetaling_id-tabellen er tom`() {
        nyPerson()
        val oppgaver = oppgaveApiDao.finnOppgaver(SAKSBEHANDLERTILGANGER_MED_INGEN)
        val oppgave = oppgaver.first()
        assertNull(oppgave.mottaker)
    }

    @Test
    fun `Mottaker er NULL når begge beløpene er 0`() {
        nyPerson()
        val arbeidsgiverFagsystemId = fagsystemId()
        val personFagsystemId = fagsystemId()
        val arbeidsgiverOppdragId = lagArbeidsgiveroppdrag(arbeidsgiverFagsystemId)
        val personOppdragId = lagPersonoppdrag(personFagsystemId)
        lagUtbetalingId(arbeidsgiverOppdragId, personOppdragId, UTBETALING_ID, 0, 0)
        val oppgaver = oppgaveApiDao.finnOppgaver(SAKSBEHANDLERTILGANGER_MED_INGEN)
        val oppgave = oppgaver.first()
        assertNull(oppgave.mottaker)
    }

    @Test
    fun `Mottaker er BEGGE når det er utbetaling til både sykmeldt og arbeidsgiver`() {
        nyPerson()
        val arbeidsgiverFagsystemId = fagsystemId()
        val personFagsystemId = fagsystemId()
        val arbeidsgiverOppdragId = lagArbeidsgiveroppdrag(arbeidsgiverFagsystemId)
        val personOppdragId = lagPersonoppdrag(personFagsystemId)
        lagUtbetalingId(arbeidsgiverOppdragId, personOppdragId, UTBETALING_ID, 2000, 2000)
        utbetalingForSisteGenerasjon(utbetalingId = UTBETALING_ID)
        ferdigstillSistOpprettedeOppgaveOgOpprettNy()
        val oppgaver = oppgaveApiDao.finnOppgaver(SAKSBEHANDLERTILGANGER_MED_INGEN)
        val oppgave = oppgaver.first()
        assertEquals(BEGGE, oppgave.mottaker)
    }

    @Test
    fun `Mottaker er SYKMELDT når det bare er utbetaling til sykmeldt`() {
        nyPerson()
        val arbeidsgiverFagsystemId = fagsystemId()
        val personFagsystemId = fagsystemId()
        val arbeidsgiverOppdragId = lagArbeidsgiveroppdrag(arbeidsgiverFagsystemId)
        val personOppdragId = lagPersonoppdrag(personFagsystemId)
        lagUtbetalingId(arbeidsgiverOppdragId, personOppdragId, UTBETALING_ID, 0, 2000)
        utbetalingForSisteGenerasjon(utbetalingId = UTBETALING_ID)
        ferdigstillSistOpprettedeOppgaveOgOpprettNy()
        val oppgaver = oppgaveApiDao.finnOppgaver(SAKSBEHANDLERTILGANGER_MED_INGEN)
        val oppgave = oppgaver.first()
        assertEquals(SYKMELDT, oppgave.mottaker)
    }

    @Test
    fun `Mottaker er SYKMELDT når det bare er utbetaling til sykmeldt, selvom beløp er negativt`() {
        nyPerson()
        val arbeidsgiverFagsystemId = fagsystemId()
        val personFagsystemId = fagsystemId()
        val arbeidsgiverOppdragId = lagArbeidsgiveroppdrag(arbeidsgiverFagsystemId)
        val personOppdragId = lagPersonoppdrag(personFagsystemId)
        lagUtbetalingId(arbeidsgiverOppdragId, personOppdragId, UTBETALING_ID, 0, -2000)
        utbetalingForSisteGenerasjon(utbetalingId = UTBETALING_ID)
        ferdigstillSistOpprettedeOppgaveOgOpprettNy()
        val oppgaver = oppgaveApiDao.finnOppgaver(SAKSBEHANDLERTILGANGER_MED_INGEN)
        val oppgave = oppgaver.first()
        assertEquals(SYKMELDT, oppgave.mottaker)
    }

    @Test
    fun `Mottaker er BEGGE, basert på utbetalinger for overlappende perioder med samme skjæringstidspunkt`() {
        nyPerson()
        val arbeidsgiverFagsystemId = fagsystemId()
        val personFagsystemId = fagsystemId()

        lagArbeidsgiveroppdrag(arbeidsgiverFagsystemId).also { arbeidsgiverOppdragId ->
            lagPersonoppdrag(personFagsystemId).also { personOppdragId ->
                lagUtbetalingId(arbeidsgiverOppdragId, personOppdragId, UTBETALING_ID, 0, -2000)
            }
        }

        utbetalingForSisteGenerasjon(utbetalingId = UTBETALING_ID)
        ferdigstillSistOpprettedeOppgaveOgOpprettNy()
        var oppgaver = oppgaveApiDao.finnOppgaver(SAKSBEHANDLERTILGANGER_MED_INGEN)
        var oppgave = oppgaver.first()
        assertEquals(SYKMELDT, oppgave.mottaker)

        val utbetalingIdForAnnenArbeidsgiverperiode = UUID.randomUUID()
        opprettArbeidsgiver(organisasjonsnummer = ORGNUMMER.reversed())
        val vedtaksperiodeForAnnenArbeidsgiverperiode = UUID.randomUUID()
        opprettGenerasjon(vedtaksperiodeId = vedtaksperiodeForAnnenArbeidsgiverperiode)
        opprettVedtaksperiode(vedtaksperiodeId = vedtaksperiodeForAnnenArbeidsgiverperiode)

        lagArbeidsgiveroppdrag(arbeidsgiverFagsystemId, mottaker = ORGNUMMER.reversed()).also { arbeidsgiverOppdragId2 ->
            lagPersonoppdrag(personFagsystemId).also { personOppdragId2 ->
                lagUtbetalingId(arbeidsgiverOppdragId2, personOppdragId2, utbetalingIdForAnnenArbeidsgiverperiode, 1000, 0)
            }
        }

        utbetalingForSisteGenerasjon(vedtaksperiodeId = vedtaksperiodeForAnnenArbeidsgiverperiode, utbetalingId = utbetalingIdForAnnenArbeidsgiverperiode)
        ferdigstillSistOpprettedeOppgaveOgOpprettNy()
        oppgaver = oppgaveApiDao.finnOppgaver(SAKSBEHANDLERTILGANGER_MED_INGEN)
        oppgave = oppgaver.first()
        assertEquals(BEGGE, oppgave.mottaker)
    }

    @Test
    fun `Mottaker er ARBEIDSGIVER når det bare er utbetaling til arbeidsgiver`() {
        nyPerson()
        val arbeidsgiverFagsystemId = fagsystemId()
        val personFagsystemId = fagsystemId()
        val arbeidsgiverOppdragId = lagArbeidsgiveroppdrag(arbeidsgiverFagsystemId)
        val personOppdragId = lagPersonoppdrag(personFagsystemId)
        lagUtbetalingId(arbeidsgiverOppdragId, personOppdragId, UTBETALING_ID, 2000, 0)
        utbetalingForSisteGenerasjon(utbetalingId = UTBETALING_ID)
        ferdigstillSistOpprettedeOppgaveOgOpprettNy()
        val oppgaver = oppgaveApiDao.finnOppgaver(SAKSBEHANDLERTILGANGER_MED_INGEN)
        val oppgave = oppgaver.first()
        assertEquals(ARBEIDSGIVER, oppgave.mottaker)
    }

    @Test
    fun `Mottaker er ARBEIDSGIVER når det bare er utbetaling til arbeidsgiver, selvom beløp er negativt`() {
        nyPerson()
        val arbeidsgiverFagsystemId = fagsystemId()
        val personFagsystemId = fagsystemId()
        val arbeidsgiverOppdragId = lagArbeidsgiveroppdrag(arbeidsgiverFagsystemId)
        val personOppdragId = lagPersonoppdrag(personFagsystemId)
        lagUtbetalingId(arbeidsgiverOppdragId, personOppdragId, UTBETALING_ID, -2000, 0)
        utbetalingForSisteGenerasjon(utbetalingId = UTBETALING_ID)
        ferdigstillSistOpprettedeOppgaveOgOpprettNy()
        val oppgaver = oppgaveApiDao.finnOppgaver(SAKSBEHANDLERTILGANGER_MED_INGEN)
        val oppgave = oppgaver.first()
        assertEquals(ARBEIDSGIVER, oppgave.mottaker)
    }

    @Test
    fun `Får totrinnsoppgaver selv om man ikke har besluttertilgang`() {
        nyPerson()
        opprettSaksbehandler()
        opprettTotrinnsvurdering()
        val oppgaver = oppgaveApiDao.finnOppgaver(SAKSBEHANDLERTILGANGER_MED_INGEN)
        assertTrue(oppgaver.isNotEmpty())
    }

    @Test
    fun `Får beslutteroppgaver dersom man har besluttertilgang`() {
        nyPerson()
        opprettSaksbehandler()
        opprettTotrinnsvurdering(saksbehandler = SAKSBEHANDLER_OID)
        val oppgaver = oppgaveApiDao.finnOppgaver(SAKSBEHANDLERTILGANGER_MED_BESLUTTER)
        val oppgave = oppgaver.first()
        assertEquals(SAKSBEHANDLER_OID, UUID.fromString(oppgave.totrinnsvurdering?.saksbehandler))
    }

    @Test
    fun `Får ikke beslutteroppgaver dersom man ikke har besluttertilgang`() {
        nyPerson()
        opprettSaksbehandler()
        opprettTotrinnsvurdering(saksbehandler = SAKSBEHANDLER_OID)
        val oppgaver = oppgaveApiDao.finnOppgaver(SAKSBEHANDLERTILGANGER_MED_INGEN)
        assertTrue(oppgaver.isEmpty())
    }

    @Test
    fun `Får returoppgaver selv om man ikke har besluttertilgang`() {
        nyPerson()
        opprettSaksbehandler()
        opprettTotrinnsvurdering(saksbehandler = SAKSBEHANDLER_OID, erRetur = true)
        val oppgaver = oppgaveApiDao.finnOppgaver(SAKSBEHANDLERTILGANGER_MED_INGEN)
        val oppgave = oppgaver.first()
        assertEquals(SAKSBEHANDLER_OID, UUID.fromString(oppgave.totrinnsvurdering?.saksbehandler))
    }

    @Test
    fun `lagre behandlingsreferanse`() {
        val oppgaveId = 1L
        val behandlingId = UUID.randomUUID()
        oppgaveApiDao.lagreBehandlingsreferanse(oppgaveId, behandlingId)
        assertOppgaveBehandlingKobling(oppgaveId, behandlingId)
    }

    private fun assertOppgaveBehandlingKobling(oppgaveId: Long, forventetBehandlingId: UUID) {
        @Language("PostgreSQL")
        val query =
            "SELECT behandling_id FROM oppgave_behandling_kobling obk WHERE obk.oppgave_id = ?"
        val behandlingId = sessionOf(dataSource).use { session ->
            session.run(queryOf(query, oppgaveId).map { it.uuid("behandling_id") }.asSingle)
        }

        assertEquals(forventetBehandlingId, behandlingId)
    }

    // Sortert stigende
    private fun finnOpprettetTidspunkterFor(vedtaksperiodeId: UUID): List<String> {
        @Language("PostgreSQL")
        val query =
            "SELECT opprettet_tidspunkt FROM selve_vedtaksperiode_generasjon WHERE vedtaksperiode_id = ? order by id;"
        return sessionOf(dataSource).use { session ->
            session.run(queryOf(query, vedtaksperiodeId).map { it.string("opprettet_tidspunkt") }.asList)
        }
    }

    private fun assertOppgaveStatus(oppgaveId: Long, forventetStatus: String) {
        val status = sessionOf(dataSource).use { session ->
            session.run(
                queryOf("SELECT * FROM oppgave where id = :id", mapOf("id" to oppgaveId))
                    .map { it.string("status") }.asSingle
            )
        }
        assertEquals(forventetStatus, status)
    }
}
