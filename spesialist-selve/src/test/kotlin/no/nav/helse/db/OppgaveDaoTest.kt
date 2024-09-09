package no.nav.helse.db

import DatabaseIntegrationTest
import no.nav.helse.db.EgenskapForDatabase.BESLUTTER
import no.nav.helse.db.EgenskapForDatabase.DELVIS_REFUSJON
import no.nav.helse.db.EgenskapForDatabase.EN_ARBEIDSGIVER
import no.nav.helse.db.EgenskapForDatabase.FLERE_ARBEIDSGIVERE
import no.nav.helse.db.EgenskapForDatabase.FORSTEGANGSBEHANDLING
import no.nav.helse.db.EgenskapForDatabase.FORTROLIG_ADRESSE
import no.nav.helse.db.EgenskapForDatabase.HASTER
import no.nav.helse.db.EgenskapForDatabase.PÅ_VENT
import no.nav.helse.db.EgenskapForDatabase.REVURDERING
import no.nav.helse.db.EgenskapForDatabase.RISK_QA
import no.nav.helse.db.EgenskapForDatabase.STRENGT_FORTROLIG_ADRESSE
import no.nav.helse.db.EgenskapForDatabase.SØKNAD
import no.nav.helse.db.EgenskapForDatabase.UTBETALING_TIL_SYKMELDT
import no.nav.helse.db.EgenskapForDatabase.UTLAND
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.TestMelding
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.test.lagAktørId
import no.nav.helse.spesialist.test.lagFødselsnummer
import no.nav.helse.spesialist.test.lagOrganisasjonsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.Isolated
import java.time.LocalDate
import java.util.UUID

@Isolated
@Execution(ExecutionMode.SAME_THREAD)
class OppgaveDaoTest : DatabaseIntegrationTest() {
    private val CONTEXT_ID = UUID.randomUUID()
    private val TESTHENDELSE = TestMelding(HENDELSE_ID, UUID.randomUUID(), FNR)
    private val OPPGAVETYPE = "SØKNAD"

    @BeforeEach
    fun setupDaoTest() {
        godkjenningsbehov(TESTHENDELSE.id)
        CommandContext(CONTEXT_ID).opprett(CommandContextDao(dataSource), TESTHENDELSE.id)
        query("truncate oppgave restart identity cascade").execute()
    }

    @Test
    fun `Finn neste ledige id`() {
        val id1 = oppgaveDao.reserverNesteId()
        val id2 = oppgaveDao.reserverNesteId()
        assertEquals(1L, id1)
        assertEquals(2L, id2)
    }

    @Test
    fun `lagre oppgave`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave(contextId = CONTEXT_ID)
        assertEquals(1, oppgave().size)
        oppgave().first().assertEquals(
            LocalDate.now(),
            listOf(OPPGAVETYPE),
            OPPGAVESTATUS,
            true,
            null,
            null,
            vedtakId,
            CONTEXT_ID,
        )
    }

    @Test
    fun `finn SpleisBehandlingId`() {
        val spleisBehandlingId = UUID.randomUUID()
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode(spleisBehandlingId = spleisBehandlingId)
        opprettOppgave(contextId = CONTEXT_ID)
        assertEquals(spleisBehandlingId, oppgaveDao.finnSpleisBehandlingId(oppgaveId))
    }

    @Test
    fun `skal ikke lagre ny oppgave dersom det allerede er en eksisterende oppgave på samme person med gitt status`() {
        opprettPerson()
        opprettArbeidsgiver()

        opprettVedtaksperiode()
        opprettOppgave(contextId = CONTEXT_ID)
        val vedtakIdPåOppgave = vedtakId

        val vedtaksperiodeId = UUID.randomUUID()
        opprettVedtaksperiode(
            vedtaksperiodeId = vedtaksperiodeId,
            fom = TOM.plusDays(1),
            tom = TOM.plusDays(10),
            periodetype = Periodetype.FORLENGELSE,
        )
        assertThrows<IllegalArgumentException> {
            opprettOppgave(contextId = CONTEXT_ID, vedtaksperiodeId = vedtaksperiodeId)
        }

        assertEquals(1, oppgave().size)
        oppgave().first().assertEquals(
            LocalDate.now(),
            listOf(OPPGAVETYPE),
            OPPGAVESTATUS,
            true,
            null,
            null,
            vedtakIdPåOppgave,
            CONTEXT_ID,
        )
    }

    @Test
    fun `skal lagre ny oppgave dersom eksisterende oppgave på samme person ikke avventer saksbehandler`() {
        opprettPerson()
        opprettArbeidsgiver()

        opprettVedtaksperiode()
        opprettOppgave(contextId = CONTEXT_ID)
        ferdigstillOppgave(oppgaveId = OPPGAVE_ID)
        assertEquals(1, oppgave().size)

        val vedtaksperiodeId2 = UUID.randomUUID()
        opprettVedtaksperiode(
            vedtaksperiodeId = vedtaksperiodeId2,
            fom = TOM.plusDays(1),
            tom = TOM.plusDays(10),
            periodetype = Periodetype.FORLENGELSE,
        )
        opprettOppgave(contextId = CONTEXT_ID, vedtaksperiodeId = vedtaksperiodeId2)
        assertEquals(1, oppgave(vedtaksperiodeId2).size)
    }

    @Test
    fun `lagre oppgave med flere egenskaper`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave(contextId = CONTEXT_ID, egenskaper = listOf(EGENSKAP, RISK_QA, PÅ_VENT))
        assertEquals(1, oppgave().size)
        oppgave().first().assertEquals(
            LocalDate.now(),
            listOf(OPPGAVETYPE, "RISK_QA", "PÅ_VENT"),
            OPPGAVESTATUS,
            true,
            null,
            null,
            vedtakId,
            CONTEXT_ID,
        )
    }

    @Test
    fun `lagre oppgave med kanAvvises lik false`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave(contextId = CONTEXT_ID, kanAvvises = false)
        assertEquals(1, oppgave().size)
        oppgave().first().assertEquals(
            LocalDate.now(),
            listOf("SØKNAD"),
            OPPGAVESTATUS,
            false,
            null,
            null,
            vedtakId,
            CONTEXT_ID,
        )
    }

    @Test
    fun `lagre oppgave med fortrolig adressebeskyttelse`() {
        opprettPerson(adressebeskyttelse = Adressebeskyttelse.Fortrolig)
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave(contextId = CONTEXT_ID, egenskaper = listOf(FORTROLIG_ADRESSE))
        assertEquals(1, oppgave().size)
        oppgave().first().assertEquals(
            LocalDate.now(),
            listOf("FORTROLIG_ADRESSE"),
            OPPGAVESTATUS,
            true,
            null,
            null,
            vedtakId,
            CONTEXT_ID,
        )
    }

    @Test
    fun `finner utbetalingId`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave(utbetalingId = UTBETALING_ID)
        assertEquals(UTBETALING_ID, oppgaveDao.finnUtbetalingId(oppgaveId))
    }

    @Test
    fun `finner hendelseId`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        val contextId = UUID.randomUUID()
        val hendelseId = UUID.randomUUID()
        opprettOppgave(contextId = contextId, hendelseId = hendelseId)
        assertEquals(hendelseId, oppgaveDao.finnHendelseId(oppgaveId))
    }

    @Test
    fun `finner oppgaveId ved hjelp av fødselsnummer`() {
        nyPerson()
        assertEquals(oppgaveId, oppgaveDao.finnOppgaveId(FNR))
        nyPerson(
            fødselsnummer = FNR.reversed(),
            aktørId = AKTØR.reversed(),
            organisasjonsnummer = ORGNUMMER.reversed(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        assertEquals(oppgaveId, oppgaveDao.finnOppgaveId(FNR.reversed()))
    }

    @Test
    fun `finner oppgaveId for ikke-avsluttet oppgave ved hjelp av vedtaksperiodeId`() {
        nyPerson()
        oppgaveDao.updateOppgave(oppgaveId = OPPGAVE_ID, oppgavestatus = "Ferdigstilt", egenskaper = listOf(EGENSKAP))
        opprettOppgave()

        assertEquals(OPPGAVE_ID, oppgaveDao.finnIdForAktivOppgave(VEDTAKSPERIODE))

        oppgaveDao.invaliderOppgaveFor(fødselsnummer = FNR)
        assertNull(oppgaveDao.finnIdForAktivOppgave(VEDTAKSPERIODE))

        opprettOppgave()
        assertEquals(OPPGAVE_ID, oppgaveDao.finnIdForAktivOppgave(VEDTAKSPERIODE))
    }

    @Test
    fun `finner OppgaveFraDatabase`() {
        val hendelseId = UUID.randomUUID()
        nyPerson(hendelseId = hendelseId)
        val oppgave = oppgaveDao.finnOppgave(oppgaveId) ?: fail { "Fant ikke oppgave" }
        assertEquals(
            OppgaveFraDatabase(
                id = oppgaveId,
                egenskaper = listOf(EGENSKAP),
                status = "AvventerSaksbehandler",
                vedtaksperiodeId = VEDTAKSPERIODE,
                utbetalingId = UTBETALING_ID,
                hendelseId = hendelseId,
                kanAvvises = true,
            ),
            oppgave,
        )
    }

    @Test
    fun `finner OppgaveFraDatabase med flere egenskaper`() {
        val hendelseId = UUID.randomUUID()
        nyPerson(hendelseId = hendelseId, oppgaveEgenskaper = listOf(EGENSKAP, RISK_QA))
        val oppgave = oppgaveDao.finnOppgave(oppgaveId) ?: fail { "Fant ikke oppgave" }
        assertEquals(
            OppgaveFraDatabase(
                id = oppgaveId,
                egenskaper = listOf(EGENSKAP, RISK_QA),
                status = "AvventerSaksbehandler",
                vedtaksperiodeId = VEDTAKSPERIODE,
                utbetalingId = UTBETALING_ID,
                hendelseId = hendelseId,
                kanAvvises = true,
            ),
            oppgave,
        )
    }

    @Test
    fun `Finn oppgave for visning`() {
        val fnr = lagFødselsnummer()
        val aktørId = lagAktørId()
        val arbeidsgiver = lagOrganisasjonsnummer()
        val vedtaksperiodeId = UUID.randomUUID()
        val saksbehandlerOid = UUID.randomUUID()
        nyPerson(
            fødselsnummer = fnr,
            aktørId = aktørId,
            organisasjonsnummer = arbeidsgiver,
            vedtaksperiodeId = vedtaksperiodeId
        )
        tildelOppgave(saksbehandlerOid = saksbehandlerOid)
        val oppgaver = oppgaveDao.finnOppgaverForVisning(emptyList(), UUID.randomUUID())
        assertEquals(1, oppgaver.size)
        val førsteOppgave = oppgaver.first()
        assertEquals(OPPGAVE_ID, førsteOppgave.id)
        assertEquals(aktørId, førsteOppgave.aktørId)
        assertEquals(setOf(EGENSKAP), førsteOppgave.egenskaper)
        assertEquals(FORNAVN, førsteOppgave.navn.fornavn)
        assertEquals(MELLOMNAVN, førsteOppgave.navn.mellomnavn)
        assertEquals(ETTERNAVN, førsteOppgave.navn.etternavn)
        assertEquals(false, førsteOppgave.påVent)
        assertEquals(
            SaksbehandlerFraDatabase(
                SAKSBEHANDLER_EPOST,
                saksbehandlerOid,
                SAKSBEHANDLER_NAVN,
                SAKSBEHANDLER_IDENT,
            ),
            førsteOppgave.tildelt,
        )
        assertEquals(vedtaksperiodeId, førsteOppgave.vedtaksperiodeId)
    }

    @Test
    fun `Finner behandlet oppgave for visning`() {
        val fnr = lagFødselsnummer()
        val aktørId = lagAktørId()
        val arbeidsgiver = lagOrganisasjonsnummer()
        val saksbehandlerOid = UUID.randomUUID()
        nyPerson(fødselsnummer = fnr, aktørId = aktørId, organisasjonsnummer = arbeidsgiver)
        tildelOppgave(saksbehandlerOid = saksbehandlerOid)
        ferdigstillOppgave(OPPGAVE_ID, ferdigstiltAvOid = saksbehandlerOid, ferdigstiltAv = SAKSBEHANDLER_NAVN)

        val oppgaver = oppgaveDao.finnBehandledeOppgaver(saksbehandlerOid)
        assertEquals(1, oppgaver.size)
        val førsteOppgave = oppgaver.first()
        assertEquals(OPPGAVE_ID, førsteOppgave.id)
        assertEquals(aktørId, førsteOppgave.aktørId)
        assertEquals(setOf(EGENSKAP), førsteOppgave.egenskaper)
        assertEquals(SAKSBEHANDLER_NAVN, førsteOppgave.ferdigstiltAv)
        assertEquals(FORNAVN, førsteOppgave.navn.fornavn)
        assertEquals(MELLOMNAVN, førsteOppgave.navn.mellomnavn)
        assertEquals(ETTERNAVN, førsteOppgave.navn.etternavn)
    }

    @Test
    fun `Finn behandlede oppgaver for visning`() {
        val saksbehandlerOid = UUID.randomUUID()
        val annenSaksbehandlerOid = UUID.randomUUID()
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        tildelOppgave(saksbehandlerOid = saksbehandlerOid)
        ferdigstillOppgave(OPPGAVE_ID, ferdigstiltAvOid = saksbehandlerOid, ferdigstiltAv = SAKSBEHANDLER_NAVN)

        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        tildelOppgave(saksbehandlerOid = saksbehandlerOid)
        ferdigstillOppgave(OPPGAVE_ID, ferdigstiltAvOid = saksbehandlerOid, ferdigstiltAv = SAKSBEHANDLER_NAVN)

        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        tildelOppgave(saksbehandlerOid = saksbehandlerOid)
        avventerSystem(OPPGAVE_ID, ferdigstiltAvOid = saksbehandlerOid, ferdigstiltAv = SAKSBEHANDLER_NAVN)

        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        tildelOppgave(saksbehandlerOid = annenSaksbehandlerOid)
        ferdigstillOppgave(OPPGAVE_ID, ferdigstiltAvOid = annenSaksbehandlerOid, ferdigstiltAv = "ANNEN_SAKSBEHANDLER")

        val oppgaver = oppgaveDao.finnBehandledeOppgaver(saksbehandlerOid)
        assertEquals(3, oppgaver.size)
        assertEquals(3, oppgaver.first().filtrertAntall)
    }

    @Test
    fun `Både saksbehandler som sender til beslutter og saksbehandler som utbetaler ser oppgaven i behandlede oppgaver`() {
        val saksbehandlerOid = UUID.randomUUID()
        val beslutterOid = UUID.randomUUID()
        val annenSaksbehandlerOid = UUID.randomUUID()

        nyPerson(
            fødselsnummer = FNR,
            aktørId = AKTØR,
            organisasjonsnummer = ORGNUMMER,
            vedtaksperiodeId = VEDTAKSPERIODE
        )
        utbetalingsopplegg(1000, 0)
        opprettSaksbehandler(saksbehandlerOID = saksbehandlerOid)
        opprettSaksbehandler(beslutterOid, navn = "NAVN TIL BESLUTTER")
        opprettSaksbehandler(annenSaksbehandlerOid)
        opprettTotrinnsvurdering(vedtaksperiodeId = VEDTAKSPERIODE, saksbehandler = saksbehandlerOid, ferdigstill = true)
        ferdigstillOppgave(OPPGAVE_ID, ferdigstiltAvOid = beslutterOid, ferdigstiltAv = "NAVN TIL BESLUTTER")

        val behandletIDagForSaksbehandler = oppgaveDao.finnBehandledeOppgaver(saksbehandlerOid)
        val behandletIDagForBeslutter = oppgaveDao.finnBehandledeOppgaver(beslutterOid)
        val behandletIDagForAnnenSaksbehandler = oppgaveDao.finnBehandledeOppgaver(annenSaksbehandlerOid)

        assertEquals(1, behandletIDagForSaksbehandler.size)
        assertEquals("NAVN TIL BESLUTTER", behandletIDagForSaksbehandler.first().ferdigstiltAv)
        assertEquals(1, behandletIDagForBeslutter.size)
        assertEquals("NAVN TIL BESLUTTER", behandletIDagForBeslutter.first().ferdigstiltAv)
        assertEquals(0, behandletIDagForAnnenSaksbehandler.size)
    }

    @Test
    fun `Finn oppgaver for visning`() {
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaver = oppgaveDao.finnOppgaverForVisning(emptyList(), UUID.randomUUID())
        assertEquals(3, oppgaver.size)
    }

    @Test
    fun `Finn oppgaver med bestemte egenskaper`() {
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
            oppgaveEgenskaper = listOf(SØKNAD, RISK_QA, FORTROLIG_ADRESSE),
        )
        val oppgaveId2 = OPPGAVE_ID
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
            oppgaveEgenskaper = listOf(RISK_QA, SØKNAD),
        )
        val oppgaveId3 = OPPGAVE_ID
        val oppgaver =
            oppgaveDao.finnOppgaverForVisning(
                emptyList(),
                UUID.randomUUID(),
                grupperteFiltrerteEgenskaper =
                    mapOf(
                        Egenskap.Kategori.Ukategorisert to listOf(RISK_QA),
                        Egenskap.Kategori.Oppgavetype to listOf(SØKNAD),
                    ),
            )
        assertEquals(2, oppgaver.size)
        assertEquals(listOf(oppgaveId3, oppgaveId2), oppgaver.map { it.id })
    }

    @Test
    fun `Finner ikke oppgaver som ikke har alle de gitte egenskapene`() {
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
            oppgaveEgenskaper = listOf(SØKNAD),
        )
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
            oppgaveEgenskaper = listOf(RISK_QA),
        )
        val oppgaver =
            oppgaveDao.finnOppgaverForVisning(
                emptyList(),
                UUID.randomUUID(),
                grupperteFiltrerteEgenskaper =
                    mapOf(
                        Egenskap.Kategori.Ukategorisert to listOf(RISK_QA),
                        Egenskap.Kategori.Oppgavetype to listOf(SØKNAD),
                    ),
            )
        assertEquals(0, oppgaver.size)
    }

    @Test
    fun `Finn oppgaver for visning med offset og limit`() {
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId1 = OPPGAVE_ID
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId2 = OPPGAVE_ID
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaver = oppgaveDao.finnOppgaverForVisning(emptyList(), UUID.randomUUID(), 1, 2)
        assertEquals(2, oppgaver.size)
        assertEquals(listOf(oppgaveId2, oppgaveId1), oppgaver.map { it.id })
    }

    @Test
    fun `Finn behandlede oppgaver med offset og limit`() {
        val saksbehandlerOid = UUID.randomUUID()
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId1 = OPPGAVE_ID
        ferdigstillOppgave(oppgaveId1, ferdigstiltAvOid = saksbehandlerOid, ferdigstiltAv = SAKSBEHANDLER_NAVN)

        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId2 = OPPGAVE_ID
        ferdigstillOppgave(oppgaveId2, ferdigstiltAvOid = saksbehandlerOid, ferdigstiltAv = SAKSBEHANDLER_NAVN)

        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId3 = OPPGAVE_ID
        ferdigstillOppgave(oppgaveId3, ferdigstiltAvOid = saksbehandlerOid, ferdigstiltAv = SAKSBEHANDLER_NAVN)

        val oppgaver = oppgaveDao.finnBehandledeOppgaver(saksbehandlerOid, 1, 2)
        assertEquals(2, oppgaver.size)
        assertEquals(listOf(oppgaveId2, oppgaveId3), oppgaver.map { it.id })
    }

    @Test
    fun `Tar kun med oppgaver som avventer saksbehandler`() {
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId1 = OPPGAVE_ID
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId2 = OPPGAVE_ID
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId3 = OPPGAVE_ID
        avventerSystem(oppgaveId3, ferdigstiltAv = "navn", ferdigstiltAvOid = UUID.randomUUID())
        val oppgaver = oppgaveDao.finnOppgaverForVisning(emptyList(), UUID.randomUUID())
        assertEquals(2, oppgaver.size)
        assertEquals(listOf(oppgaveId2, oppgaveId1), oppgaver.map { it.id })
    }

    @Test
    fun `Sorterer oppgaver på opprettet stigende`() {
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId1 = OPPGAVE_ID
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId2 = OPPGAVE_ID
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId3 = OPPGAVE_ID
        val oppgaver =
            oppgaveDao.finnOppgaverForVisning(
                emptyList(),
                UUID.randomUUID(),
                sortering =
                    listOf(
                        OppgavesorteringForDatabase(SorteringsnøkkelForDatabase.OPPRETTET, true),
                    ),
            )
        assertEquals(3, oppgaver.size)
        assertEquals(listOf(oppgaveId1, oppgaveId2, oppgaveId3), oppgaver.map { it.id })
    }

    @Test
    fun `Sorterer oppgaver på opprettet fallende`() {
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId1 = OPPGAVE_ID
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId2 = OPPGAVE_ID
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId3 = OPPGAVE_ID
        val oppgaver =
            oppgaveDao.finnOppgaverForVisning(
                emptyList(),
                UUID.randomUUID(),
                sortering =
                    listOf(
                        OppgavesorteringForDatabase(SorteringsnøkkelForDatabase.OPPRETTET, false),
                    ),
            )
        assertEquals(3, oppgaver.size)
        assertEquals(listOf(oppgaveId3, oppgaveId2, oppgaveId1), oppgaver.map { it.id })
    }

    @Test
    fun `Sorterer oppgaver på opprinneligSøknadsdato stigende`() {
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId1 = OPPGAVE_ID
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId2 = OPPGAVE_ID
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId3 = OPPGAVE_ID
        val oppgaver =
            oppgaveDao.finnOppgaverForVisning(
                emptyList(),
                UUID.randomUUID(),
                sortering =
                    listOf(
                        OppgavesorteringForDatabase(SorteringsnøkkelForDatabase.SØKNAD_MOTTATT, true),
                    ),
            )
        assertEquals(3, oppgaver.size)
        assertEquals(listOf(oppgaveId1, oppgaveId2, oppgaveId3), oppgaver.map { it.id })
    }

    @Test
    fun `Sorterer oppgaver på opprinneligSøknadsdato fallende`() {
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId1 = OPPGAVE_ID
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId2 = OPPGAVE_ID
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId3 = OPPGAVE_ID
        val oppgaver =
            oppgaveDao.finnOppgaverForVisning(
                emptyList(),
                UUID.randomUUID(),
                sortering =
                    listOf(
                        OppgavesorteringForDatabase(SorteringsnøkkelForDatabase.SØKNAD_MOTTATT, false),
                    ),
            )
        assertEquals(3, oppgaver.size)
        assertEquals(listOf(oppgaveId3, oppgaveId2, oppgaveId1), oppgaver.map { it.id })
    }

    @Test
    fun `Sorterer oppgaver på tildeling stigende`() {
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId1 = OPPGAVE_ID
        tildelOppgave(oppgaveId1, UUID.randomUUID(), "Å")
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId2 = OPPGAVE_ID
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId3 = OPPGAVE_ID
        tildelOppgave(oppgaveId3, UUID.randomUUID(), "B")
        val oppgaver =
            oppgaveDao.finnOppgaverForVisning(
                emptyList(),
                UUID.randomUUID(),
                sortering =
                    listOf(
                        OppgavesorteringForDatabase(SorteringsnøkkelForDatabase.TILDELT_TIL, true),
                    ),
            )
        assertEquals(3, oppgaver.size)
        assertEquals(listOf(oppgaveId3, oppgaveId1, oppgaveId2), oppgaver.map { it.id })
    }

    @Test
    fun `Sorterer oppgaver på tildeling fallende`() {
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId1 = OPPGAVE_ID
        tildelOppgave(oppgaveId1, UUID.randomUUID(), "A")
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId2 = OPPGAVE_ID
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId3 = OPPGAVE_ID
        tildelOppgave(oppgaveId3, UUID.randomUUID(), "B")
        val oppgaver =
            oppgaveDao.finnOppgaverForVisning(
                emptyList(),
                UUID.randomUUID(),
                sortering =
                    listOf(
                        OppgavesorteringForDatabase(SorteringsnøkkelForDatabase.TILDELT_TIL, false),
                    ),
            )
        assertEquals(3, oppgaver.size)
        assertEquals(listOf(oppgaveId3, oppgaveId1, oppgaveId2), oppgaver.map { it.id })
    }

    @Test
    fun `Sorterer oppgaver på tildeling stigende først, deretter opprettet fallende`() {
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId1 = OPPGAVE_ID
        tildelOppgave(oppgaveId1, UUID.randomUUID(), "A")
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId2 = OPPGAVE_ID
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId3 = OPPGAVE_ID
        val saksbehandlerOid2 = UUID.randomUUID()
        tildelOppgave(oppgaveId3, saksbehandlerOid2, "B")
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId4 = OPPGAVE_ID
        tildelOppgave(oppgaveId4, saksbehandlerOid2, "B")
        val oppgaver =
            oppgaveDao.finnOppgaverForVisning(
                emptyList(),
                UUID.randomUUID(),
                sortering =
                    listOf(
                        OppgavesorteringForDatabase(SorteringsnøkkelForDatabase.TILDELT_TIL, true),
                        OppgavesorteringForDatabase(SorteringsnøkkelForDatabase.OPPRETTET, false),
                    ),
            )
        assertEquals(4, oppgaver.size)
        assertEquals(listOf(oppgaveId1, oppgaveId4, oppgaveId3, oppgaveId2), oppgaver.map { it.id })
    }

    @Test
    fun `Tar kun med oppgaver som saksbehandler har tilgang til`() {
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId1 = OPPGAVE_ID
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
            oppgaveEgenskaper =
                listOf(
                    BESLUTTER,
                ),
        )
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
            oppgaveEgenskaper =
                listOf(
                    RISK_QA,
                    FORTROLIG_ADRESSE,
                ),
        )
        val oppgaver =
            oppgaveDao.finnOppgaverForVisning(
                ekskluderEgenskaper = listOf("BESLUTTER", "RISK_QA"),
                UUID.randomUUID(),
            )
        assertEquals(1, oppgaver.size)
        assertEquals(listOf(oppgaveId1), oppgaver.map { it.id })
    }

    @Test
    fun `Ekskluderer ukategoriserte egenskaper`() {
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId1 = OPPGAVE_ID
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
            oppgaveEgenskaper =
                listOf(
                    BESLUTTER,
                ),
        )
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
            oppgaveEgenskaper =
                listOf(
                    RISK_QA,
                    FORTROLIG_ADRESSE,
                ),
        )
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
            oppgaveEgenskaper =
                listOf(
                    UTLAND,
                    HASTER,
                ),
        )
        val oppgaver =
            oppgaveDao.finnOppgaverForVisning(
                ekskluderEgenskaper = listOf("BESLUTTER", "RISK_QA") + Egenskap.alleUkategoriserteEgenskaper.map(Egenskap::toString),
                UUID.randomUUID(),
            )
        assertEquals(1, oppgaver.size)
        assertEquals(listOf(oppgaveId1), oppgaver.map { it.id })
    }

    @Test
    fun `Får kun oppgaver som er tildelt hvis tildelt er satt til true`() {
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId1 = OPPGAVE_ID
        tildelOppgave(oppgaveId1, saksbehandlerOid = UUID.randomUUID())
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )

        val oppgaver = oppgaveDao.finnOppgaverForVisning(emptyList(), UUID.randomUUID(), tildelt = true)
        assertEquals(1, oppgaver.size)
        assertEquals(1, oppgaver.first().filtrertAntall)
        assertEquals(listOf(oppgaveId1), oppgaver.map { it.id })
    }

    @Test
    fun `Får kun oppgaver som ikke er tildelt hvis tildelt er satt til false`() {
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId1 = OPPGAVE_ID
        tildelOppgave(oppgaveId1, saksbehandlerOid = UUID.randomUUID())
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId2 = OPPGAVE_ID
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId3 = OPPGAVE_ID

        val oppgaver = oppgaveDao.finnOppgaverForVisning(emptyList(), UUID.randomUUID(), tildelt = false)
        assertEquals(2, oppgaver.size)
        assertEquals(2, oppgaver.first().filtrertAntall)
        assertEquals(listOf(oppgaveId3, oppgaveId2), oppgaver.map { it.id })
    }

    @Test
    fun `Får både tildelte og ikke tildelte oppgaver hvis tildelt er satt til null`() {
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId1 = OPPGAVE_ID
        tildelOppgave(oppgaveId1, saksbehandlerOid = UUID.randomUUID())
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId2 = OPPGAVE_ID
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId3 = OPPGAVE_ID

        val oppgaver = oppgaveDao.finnOppgaverForVisning(emptyList(), UUID.randomUUID(), tildelt = null)
        assertEquals(3, oppgaver.size)
        assertEquals(3, oppgaver.first().filtrertAntall)
        assertEquals(listOf(oppgaveId3, oppgaveId2, oppgaveId1), oppgaver.map { it.id })
    }

    @Test
    fun `Tar med alle oppgaver som saksbehandler har tilgang til`() {
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId1 = OPPGAVE_ID
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId2 = OPPGAVE_ID
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
            oppgaveEgenskaper =
                listOf(
                    RISK_QA,
                    FORTROLIG_ADRESSE,
                ),
        )
        val oppgaveId3 = OPPGAVE_ID
        val oppgaver = oppgaveDao.finnOppgaverForVisning(emptyList(), UUID.randomUUID())
        assertEquals(3, oppgaver.size)
        assertEquals(3, oppgaver.first().filtrertAntall)
        assertEquals(listOf(oppgaveId3, oppgaveId2, oppgaveId1), oppgaver.map { it.id })
    }

    @Test
    fun `Tar kun med oppgaver som er tildelt saksbehandler når dette bes om`() {
        val saksbehandlerOid = UUID.randomUUID()
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId1 = OPPGAVE_ID
        tildelOppgave(oppgaveId1, saksbehandlerOid = saksbehandlerOid)
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
            oppgaveEgenskaper = listOf(PÅ_VENT),
        )
        val oppgaveId2 = OPPGAVE_ID
        tildelOppgave(oppgaveId2, saksbehandlerOid = saksbehandlerOid, egenskaper = listOf(SØKNAD, PÅ_VENT))
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId3 = OPPGAVE_ID

        val oppgaverSomErTildelt =
            oppgaveDao.finnOppgaverForVisning(
                emptyList(),
                saksbehandlerOid = saksbehandlerOid,
                egneSaker = true,
                egneSakerPåVent = false,
            )
        val oppgaverSomErTildeltOgPåvent =
            oppgaveDao.finnOppgaverForVisning(
                emptyList(),
                saksbehandlerOid = saksbehandlerOid,
                egneSaker = true,
                egneSakerPåVent = true,
            )
        val alleOppgaver =
            oppgaveDao.finnOppgaverForVisning(
                emptyList(),
                saksbehandlerOid = saksbehandlerOid,
                egneSaker = false,
                egneSakerPåVent = false,
            )
        assertEquals(1, oppgaverSomErTildelt.size)
        assertEquals(1, oppgaverSomErTildeltOgPåvent.size)
        assertEquals(3, alleOppgaver.size)
        assertEquals(listOf(oppgaveId1), oppgaverSomErTildelt.map { it.id })
        assertEquals(listOf(oppgaveId2), oppgaverSomErTildeltOgPåvent.map { it.id })
        assertEquals(setOf(oppgaveId1, oppgaveId2, oppgaveId3), alleOppgaver.map { it.id }.toSet())
    }

    @Test
    fun `Saksbehandler får ikke med oppgaver hen har sendt til beslutter selv om hen har beslutter-tilgang`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val saksbehandlerOid = UUID.randomUUID()
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = vedtaksperiodeId,
            oppgaveEgenskaper =
                listOf(
                    BESLUTTER,
                ),
        )
        opprettSaksbehandler(saksbehandlerOid)
        opprettTotrinnsvurdering(vedtaksperiodeId, saksbehandlerOid)
        val oppgaver = oppgaveDao.finnOppgaverForVisning(ekskluderEgenskaper = emptyList(), saksbehandlerOid = saksbehandlerOid)
        assertEquals(0, oppgaver.size)
    }

    @Test
    fun `Saksbehandler får ikke med oppgaver med egenskap STRENGT_FORTROLIG_ADRESSE`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val saksbehandlerOid = UUID.randomUUID()
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = vedtaksperiodeId,
            oppgaveEgenskaper =
                listOf(
                    STRENGT_FORTROLIG_ADRESSE,
                ),
        )
        opprettSaksbehandler(saksbehandlerOid)
        opprettTotrinnsvurdering(vedtaksperiodeId, saksbehandlerOid)
        val oppgaver =
            oppgaveDao.finnOppgaverForVisning(
                ekskluderEgenskaper = listOf("STRENGT_FORTROLIG_ADRESSE"),
                saksbehandlerOid = saksbehandlerOid,
            )
        assertEquals(0, oppgaver.size)
    }

    @Test
    fun `Oppgaver blir filtrert riktig`() {
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
            oppgaveEgenskaper =
                listOf(
                    SØKNAD,
                    HASTER,
                    UTLAND,
                    FORSTEGANGSBEHANDLING,
                    DELVIS_REFUSJON,
                    FLERE_ARBEIDSGIVERE,
                ),
        )
        val oppgaveId1 = OPPGAVE_ID
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
            oppgaveEgenskaper =
                listOf(
                    REVURDERING,
                    HASTER,
                ),
        )
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
            oppgaveEgenskaper =
                listOf(
                    SØKNAD,
                    UTLAND,
                ),
        )
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
            oppgaveEgenskaper =
                listOf(
                    UTBETALING_TIL_SYKMELDT,
                    EN_ARBEIDSGIVER,
                ),
        )
        val oppgaveId4 = OPPGAVE_ID

        val oppgaver =
            oppgaveDao.finnOppgaverForVisning(
                ekskluderEgenskaper = emptyList(),
                saksbehandlerOid = UUID.randomUUID(),
                grupperteFiltrerteEgenskaper = mapOf(Egenskap.Kategori.Oppgavetype to listOf(SØKNAD, REVURDERING)),
            )
        val oppgaver1 =
            oppgaveDao.finnOppgaverForVisning(
                ekskluderEgenskaper = emptyList(),
                saksbehandlerOid = UUID.randomUUID(),
                grupperteFiltrerteEgenskaper = mapOf(Egenskap.Kategori.Ukategorisert to listOf(HASTER, UTLAND)),
            )
        val oppgaver2 =
            oppgaveDao.finnOppgaverForVisning(
                ekskluderEgenskaper = emptyList(),
                saksbehandlerOid = UUID.randomUUID(),
                grupperteFiltrerteEgenskaper =
                    mapOf(
                        Egenskap.Kategori.Ukategorisert to listOf(HASTER, UTLAND),
                        Egenskap.Kategori.Oppgavetype to listOf(SØKNAD),
                        Egenskap.Kategori.Periodetype to listOf(FORSTEGANGSBEHANDLING),
                        Egenskap.Kategori.Mottaker to listOf(DELVIS_REFUSJON),
                        Egenskap.Kategori.Inntektskilde to listOf(FLERE_ARBEIDSGIVERE),
                    ),
            )
        val oppgaver3 =
            oppgaveDao.finnOppgaverForVisning(
                ekskluderEgenskaper = emptyList(),
                saksbehandlerOid = UUID.randomUUID(),
                grupperteFiltrerteEgenskaper =
                    mapOf(
                        Egenskap.Kategori.Mottaker to listOf(UTBETALING_TIL_SYKMELDT),
                        Egenskap.Kategori.Inntektskilde to listOf(EN_ARBEIDSGIVER),
                    ),
            )
        val oppgaver5 =
            oppgaveDao.finnOppgaverForVisning(
                ekskluderEgenskaper = emptyList(),
                saksbehandlerOid = UUID.randomUUID(),
                grupperteFiltrerteEgenskaper = emptyMap(),
            )

        assertEquals(3, oppgaver.size)
        assertEquals(1, oppgaver1.size)
        assertEquals(oppgaveId1, oppgaver1.first().id)
        assertEquals(1, oppgaver2.size)
        assertEquals(oppgaveId1, oppgaver2.first().id)
        assertEquals(1, oppgaver3.size)
        assertEquals(oppgaveId4, oppgaver3.first().id)
        assertEquals(4, oppgaver5.size)
    }

    @Test
    fun `Grupperte filtrerte egenskaper fungerer sammen med ekskluderte egenskaper`() {
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
            oppgaveEgenskaper =
                listOf(
                    SØKNAD,
                    FORSTEGANGSBEHANDLING,
                    DELVIS_REFUSJON,
                    FLERE_ARBEIDSGIVERE,
                    PÅ_VENT,
                ),
        )
        val oppgaveId1 = OPPGAVE_ID
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
            oppgaveEgenskaper =
                listOf(
                    SØKNAD,
                    HASTER,
                    UTLAND,
                    FORSTEGANGSBEHANDLING,
                    DELVIS_REFUSJON,
                    FLERE_ARBEIDSGIVERE,
                    PÅ_VENT,
                ),
        )

        val oppgaver =
            oppgaveDao.finnOppgaverForVisning(
                ekskluderEgenskaper = Egenskap.alleUkategoriserteEgenskaper.map(Egenskap::toString),
                saksbehandlerOid = UUID.randomUUID(),
                grupperteFiltrerteEgenskaper =
                    mapOf(
                        Egenskap.Kategori.Oppgavetype to listOf(SØKNAD),
                        Egenskap.Kategori.Periodetype to listOf(FORSTEGANGSBEHANDLING),
                        Egenskap.Kategori.Mottaker to listOf(DELVIS_REFUSJON),
                        Egenskap.Kategori.Inntektskilde to listOf(FLERE_ARBEIDSGIVERE),
                        Egenskap.Kategori.Status to listOf(PÅ_VENT),
                    ),
            )

        assertEquals(1, oppgaver.size)
        assertEquals(oppgaveId1, oppgaver.first().id)
    }

    @Test
    fun `finner vedtaksperiodeId`() {
        nyPerson()
        val actual = oppgaveDao.finnVedtaksperiodeId(oppgaveId)
        assertEquals(VEDTAKSPERIODE, actual)
    }

    @Test
    fun `oppdatere oppgave`() {
        val nyStatus = "Ferdigstilt"
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave(contextId = CONTEXT_ID)
        oppgaveDao.updateOppgave(oppgaveId, nyStatus, SAKSBEHANDLER_EPOST, SAKSBEHANDLER_OID, listOf(EGENSKAP, RISK_QA))
        assertEquals(1, oppgave().size)
        oppgave().first().assertEquals(
            LocalDate.now(),
            listOf(OPPGAVETYPE, "RISK_QA"),
            nyStatus,
            true,
            SAKSBEHANDLER_EPOST,
            SAKSBEHANDLER_OID,
            vedtakId,
            CONTEXT_ID,
        )
    }

    @Test
    fun `sjekker om det fins aktiv oppgave`() {
        nyPerson()
        oppgaveDao.updateOppgave(oppgaveId, "AvventerSaksbehandler", null, null, listOf(EGENSKAP))
        assertTrue(oppgaveDao.venterPåSaksbehandler(oppgaveId))

        oppgaveDao.updateOppgave(oppgaveId, "Ferdigstilt", null, null, listOf(EGENSKAP))
        assertFalse(oppgaveDao.venterPåSaksbehandler(oppgaveId))
    }

    @Test
    fun `sjekker om det fins aktiv oppgave med to oppgaver`() {
        nyPerson()
        oppgaveDao.updateOppgave(
            oppgaveId = oppgaveId,
            oppgavestatus = "Ferdigstilt",
            egenskaper =
                listOf(
                    EGENSKAP,
                ),
        )

        opprettOppgave(vedtaksperiodeId = VEDTAKSPERIODE)
        oppgaveDao.updateOppgave(
            oppgaveId = oppgaveId,
            oppgavestatus = "AvventerSaksbehandler",
            egenskaper =
                listOf(
                    EGENSKAP,
                ),
        )

        assertTrue(oppgaveDao.harGyldigOppgave(UTBETALING_ID))
    }

    @Test
    fun `sjekker at det ikke fins ferdigstilt oppgave`() {
        nyPerson()
        oppgaveDao.updateOppgave(
            oppgaveId = oppgaveId,
            oppgavestatus = "AvventerSaksbehandler",
            egenskaper =
                listOf(
                    EGENSKAP,
                ),
        )

        assertFalse(oppgaveDao.harFerdigstiltOppgave(VEDTAKSPERIODE))
    }

    @Test
    fun `sjekker at det fins ferdigstilt oppgave`() {
        nyPerson()
        oppgaveDao.updateOppgave(oppgaveId = oppgaveId, oppgavestatus = "Ferdigstilt", egenskaper = listOf(EGENSKAP))
        oppgaveDao.updateOppgave(oppgaveId = 2L, oppgavestatus = "AvventerSaksbehandler", egenskaper = listOf(EGENSKAP))

        assertTrue(oppgaveDao.harFerdigstiltOppgave(VEDTAKSPERIODE))
    }

    @Test
    fun `henter fødselsnummeret til personen en oppgave gjelder for`() {
        nyPerson()
        val fødselsnummer = oppgaveDao.finnFødselsnummer(oppgaveId)
        assertEquals(fødselsnummer, FNR)
    }

    @Test
    fun `Finner vedtaksperiodeId med oppgaveId`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave(contextId = CONTEXT_ID)

        assertEquals(VEDTAKSPERIODE, oppgaveDao.finnVedtaksperiodeId(oppgaveId))
    }

    @Test
    fun `invaliderer oppgaver`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave()
        val oppgaveId1 = oppgaveId

        val fnr2 = lagFødselsnummer()
        val aktørId2 = lagAktørId()
        val organisasjonsnummer2 = lagOrganisasjonsnummer()
        val vedtaksperiodeId2 = UUID.randomUUID()
        opprettPerson(fødselsnummer = fnr2, aktørId2)
        opprettArbeidsgiver(organisasjonsnummer2, "en annen bedrift")
        opprettVedtaksperiode(
            fødselsnummer = fnr2,
            organisasjonsnummer = organisasjonsnummer2,
            vedtaksperiodeId = vedtaksperiodeId2)
        opprettOppgave(vedtaksperiodeId = vedtaksperiodeId2, utbetalingId = UUID.randomUUID())
        val oppgaveId2 = oppgaveId

        oppgaveDao.invaliderOppgaveFor(fødselsnummer = FNR)

        assertOppgaveStatus(oppgaveId1, Oppgave.Invalidert)
        assertOppgaveStatus(oppgaveId2, Oppgave.AvventerSaksbehandler)
    }

    @Test
    fun `Finner egenskaper for gitt vedtaksperiode med gitt utbetalingId`() {
        opprettPerson()
        opprettArbeidsgiver()
        opprettVedtaksperiode()
        opprettOppgave()

        val fnr2 = lagFødselsnummer()
        val aktørId2 = lagAktørId()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val utbetalingId2 = UUID.randomUUID()
        opprettPerson(fødselsnummer = fnr2, aktørId2)
        val organisasjonsnummer2 = lagOrganisasjonsnummer()
        opprettArbeidsgiver(organisasjonsnummer2, "en annen bedrift")
        opprettVedtaksperiode(
            fødselsnummer = fnr2,
            vedtaksperiodeId = vedtaksperiodeId2,
            organisasjonsnummer = organisasjonsnummer2,
            utbetalingId = utbetalingId2
        )
        opprettOppgave(
            vedtaksperiodeId = vedtaksperiodeId2,
            egenskaper = listOf(SØKNAD, PÅ_VENT),
            utbetalingId = utbetalingId2,
        )

        oppgaveDao.invaliderOppgaveFor(fødselsnummer = FNR)

        val egenskaperOppgaveId1 = oppgaveDao.finnEgenskaper(VEDTAKSPERIODE, UTBETALING_ID)
        val egenskaperOppgaveId2 = oppgaveDao.finnEgenskaper(vedtaksperiodeId2, utbetalingId2)
        assertEquals(setOf(SØKNAD), egenskaperOppgaveId1)
        assertEquals(setOf(SØKNAD, PÅ_VENT), egenskaperOppgaveId2)
    }

    @Test
    fun `Teller mine saker og mine saker på vent riktig`() {
        val saksbehandlerOid = UUID.randomUUID()
        opprettSaksbehandler(saksbehandlerOid)

        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId1 = OPPGAVE_ID
        tildelOppgave(oppgaveId = oppgaveId1, saksbehandlerOid = saksbehandlerOid)

        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId2 = OPPGAVE_ID
        tildelOppgave(oppgaveId = oppgaveId2, saksbehandlerOid = saksbehandlerOid)

        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId3 = OPPGAVE_ID
        tildelOppgave(oppgaveId = oppgaveId3, saksbehandlerOid = saksbehandlerOid, egenskaper = listOf(SØKNAD, PÅ_VENT))

        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = UUID.randomUUID(),
        )
        val oppgaveId4 = OPPGAVE_ID
        tildelOppgave(oppgaveId = oppgaveId4, saksbehandlerOid = UUID.randomUUID(), egenskaper = listOf(SØKNAD, PÅ_VENT))

        val antallOppgaver = oppgaveDao.finnAntallOppgaver(saksbehandlerOid)

        assertEquals(2, antallOppgaver.antallMineSaker)
        assertEquals(1, antallOppgaver.antallMineSakerPåVent)
    }

    @Test
    fun `Antall mine saker og mine saker på vent er 0 hvis det ikke finnes tildeling for saksbehandler`() {
        val saksbehandlerOid = UUID.randomUUID()
        opprettSaksbehandler(saksbehandlerOid)

        val vedtaksperiodeId1 = UUID.randomUUID()
        nyPerson(
            fødselsnummer = lagFødselsnummer(),
            aktørId = lagAktørId(),
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = vedtaksperiodeId1,
        )

        val antallOppgaver = oppgaveDao.finnAntallOppgaver(saksbehandlerOid)

        assertEquals(0, antallOppgaver.antallMineSaker)
        assertEquals(0, antallOppgaver.antallMineSakerPåVent)
    }

    private fun assertOppgaveStatus(
        oppgaveId: Long,
        forventetStatus: Oppgave.Tilstand,
    ) {
        val status = query("SELECT * FROM oppgave where id = :id", "id" to oppgaveId).single { it.string("status") }
        assertEquals(forventetStatus.toString(), status)
    }

    private fun oppgave(vedtaksperiodeId: UUID = VEDTAKSPERIODE) =
        query(
            "SELECT o.* FROM oppgave o JOIN vedtak v on o.vedtak_ref = v.id WHERE v.vedtaksperiode_id = :vedtaksperiodeId ORDER BY id DESC",
            "vedtaksperiodeId" to vedtaksperiodeId,
        ).list { row ->
            OppgaveAssertions(
                oppdatert = row.localDate("oppdatert"),
                egenskaper = row.array<String>("egenskaper").toList(),
                status = row.string("status"),
                kanAvvises = row.boolean("kan_avvises"),
                ferdigstiltAv = row.stringOrNull("ferdigstilt_av"),
                ferdigstiltAvOid = row.stringOrNull("ferdigstilt_av_oid")?.let(UUID::fromString),
                vedtakRef = row.longOrNull("vedtak_ref"),
                commandContextId = row.stringOrNull("command_context_id")?.let(UUID::fromString),
            )
        }

    private class OppgaveAssertions(
        private val oppdatert: LocalDate,
        private val egenskaper: List<String>,
        private val status: String,
        private val kanAvvises: Boolean,
        private val ferdigstiltAv: String?,
        private val ferdigstiltAvOid: UUID?,
        private val vedtakRef: Long?,
        private val commandContextId: UUID?,
    ) {
        fun assertEquals(
            forventetOppdatert: LocalDate,
            forventetEgenskaper: List<String>,
            forventetStatus: String,
            forventetKanAvvises: Boolean,
            forventetFerdigstilAv: String?,
            forventetFerdigstilAvOid: UUID?,
            forventetVedtakRef: Long?,
            forventetCommandContextId: UUID,
        ) {
            assertEquals(forventetOppdatert, oppdatert)
            assertEquals(forventetEgenskaper, egenskaper)
            assertEquals(forventetStatus, status)
            assertEquals(forventetKanAvvises, kanAvvises)
            assertEquals(forventetFerdigstilAv, ferdigstiltAv)
            assertEquals(forventetFerdigstilAvOid, ferdigstiltAvOid)
            assertEquals(forventetVedtakRef, vedtakRef)
            assertEquals(forventetCommandContextId, commandContextId)
        }
    }
}
