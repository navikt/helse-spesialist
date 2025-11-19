package no.nav.helse.spesialist.db.dao

import no.nav.helse.db.BehandletOppgaveFraDatabaseForVisning
import no.nav.helse.db.EgenskapForDatabase.PÅ_VENT
import no.nav.helse.db.EgenskapForDatabase.SØKNAD
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.db.TestMelding
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagAktørId
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagEtternavn
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFornavn
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.Isolated
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Isolated
@Execution(ExecutionMode.SAME_THREAD)
class PgOppgaveDaoTest : AbstractDBIntegrationTest() {
    private val CONTEXT_ID = UUID.randomUUID()
    private val TESTHENDELSE = TestMelding(HENDELSE_ID, UUID.randomUUID(), FNR)
    private val legacySaksbehandler = nyLegacySaksbehandler()

    @BeforeTest
    fun setupDaoTest() {
        godkjenningsbehov(TESTHENDELSE.id)
        CommandContext(CONTEXT_ID).opprett(commandContextDao, TESTHENDELSE.id)
        dbQuery.execute("truncate oppgave restart identity cascade")
    }

    @Test
    fun `Finn neste ledige id`() {
        assertDoesNotThrow {
            oppgaveDao.reserverNesteId()
            oppgaveDao.reserverNesteId()
        }
    }

    @Test
    fun `finn SpleisBehandlingId`() {
        val oppgave = nyOppgaveForNyPerson()
        assertEquals(oppgave.behandlingId, oppgaveDao.finnSpleisBehandlingId(oppgave.id))
    }

    @Test
    fun `finn generasjonId`() {
        val oppgave = nyOppgaveForNyPerson()
        assertDoesNotThrow {
            oppgaveDao.finnGenerasjonId(oppgave.id)
        }
    }

    @Test
    fun `finner oppgaveId ved hjelp av fødselsnummer`() {
        val fødselsnummer = lagFødselsnummer()
        val oppgave = nyOppgaveForNyPerson(fødselsnummer = fødselsnummer)

        assertEquals(oppgave.id, oppgaveDao.finnOppgaveId(fødselsnummer))
    }

    @Test
    fun `finner oppgaveId for ikke-avsluttet oppgave ved hjelp av vedtaksperiodeId`() {
        val oppgave = nyOppgaveForNyPerson()

        assertEquals(oppgave.id, oppgaveDao.finnIdForAktivOppgave(oppgave.vedtaksperiodeId))

        oppgave.invaliderOgLagre()
        assertNull(oppgaveDao.finnIdForAktivOppgave(oppgave.vedtaksperiodeId))
    }

    @Test
    fun `Finner behandlet oppgave for visning`() {
        val aktørId = lagAktørId()
        val fornavn = lagFornavn()
        val mellomnavn = lagEtternavn()
        val etternavn = lagEtternavn()
        val oppgave = nyOppgaveForNyPerson(
            aktørId = aktørId,
            fornavn = fornavn,
            mellomnavn = mellomnavn,
            etternavn = etternavn
        )
            .tildelOgLagre(legacySaksbehandler)
            .avventSystemOgLagre(legacySaksbehandler)
            .ferdigstillOgLagre()

        val oppgaver = oppgaveDao.finnBehandledeOppgaver(legacySaksbehandler.saksbehandler.id.value)
        assertEquals(1, oppgaver.size)
        val førsteOppgave = oppgaver.first()
        assertEquals(oppgave.id, førsteOppgave.id)
        assertEquals(aktørId, førsteOppgave.aktørId)
        assertEquals(oppgave.egenskaper.map { it.name }, førsteOppgave.egenskaper.map { it.name })
        assertEquals(legacySaksbehandler.saksbehandler.ident, førsteOppgave.ferdigstiltAv)
        assertEquals(legacySaksbehandler.saksbehandler.ident, førsteOppgave.saksbehandler)
        assertNull(førsteOppgave.beslutter)
        assertEquals(fornavn, førsteOppgave.navn.fornavn)
        assertEquals(mellomnavn, førsteOppgave.navn.mellomnavn)
        assertEquals(etternavn, førsteOppgave.navn.etternavn)
    }

    @Test
    fun `Finn behandlede oppgaver for visning`() {
        nyOppgaveForNyPerson()
            .tildelOgLagre(legacySaksbehandler)
            .avventSystemOgLagre(legacySaksbehandler)
            .ferdigstillOgLagre()

        nyOppgaveForNyPerson()
            .tildelOgLagre(legacySaksbehandler)
            .avventSystemOgLagre(legacySaksbehandler)
            .ferdigstillOgLagre()

        nyOppgaveForNyPerson()
            .tildelOgLagre(legacySaksbehandler)
            .avventSystemOgLagre(legacySaksbehandler)
            .avventSystemOgLagre(legacySaksbehandler)

        val annenSaksbehandler = nyLegacySaksbehandler()
        nyOppgaveForNyPerson()
            .tildelOgLagre(annenSaksbehandler)
            .avventSystemOgLagre(annenSaksbehandler)
            .ferdigstillOgLagre()

        val oppgaver = oppgaveDao.finnBehandledeOppgaver(legacySaksbehandler.saksbehandler.id.value)
        assertEquals(3, oppgaver.size)
        assertEquals(3, oppgaver.first().filtrertAntall)
    }

    @Test
    fun `Både saksbehandler som sender til beslutter og saksbehandler som utbetaler ser oppgaven i behandlede oppgaver`() {
        val fødselsnummer = lagFødselsnummer()

        val saksbehandler = nyLegacySaksbehandler()
        val annenSaksbehandler = nyLegacySaksbehandler()
        val beslutter = nyLegacySaksbehandler()
        val oppgave = nyOppgaveForNyPerson(fødselsnummer = fødselsnummer)
        nyTotrinnsvurdering(fødselsnummer, oppgave)
            .sendTilBeslutterOgLagre(saksbehandler)
            .ferdigstillOgLagre(beslutter)
        oppgave
            .sendTilBeslutterOgLagre(null)
            .avventSystemOgLagre(beslutter)
            .ferdigstillOgLagre()

        val behandletIDagForSaksbehandler = oppgaveDao.finnBehandledeOppgaver(saksbehandler.saksbehandler.id.value)
        val behandletIDagForBeslutter = oppgaveDao.finnBehandledeOppgaver(beslutter.saksbehandler.id.value)
        val behandletIDagForAnnenSaksbehandler = oppgaveDao.finnBehandledeOppgaver(annenSaksbehandler.saksbehandler.id.value)

        assertEquals(1, behandletIDagForSaksbehandler.size)
        assertEquals(beslutter.saksbehandler.ident, behandletIDagForSaksbehandler.first().ferdigstiltAv)
        assertEquals(beslutter.saksbehandler.ident, behandletIDagForSaksbehandler.first().beslutter)
        assertEquals(saksbehandler.saksbehandler.ident, behandletIDagForSaksbehandler.first().saksbehandler)
        assertEquals(1, behandletIDagForBeslutter.size)
        assertEquals(beslutter.saksbehandler.ident, behandletIDagForBeslutter.first().ferdigstiltAv)
        assertEquals(beslutter.saksbehandler.ident, behandletIDagForBeslutter.first().beslutter)
        assertEquals(saksbehandler.saksbehandler.ident, behandletIDagForBeslutter.first().saksbehandler)
        assertEquals(0, behandletIDagForAnnenSaksbehandler.size)
    }

    @Test
    fun `Finn behandlede oppgaver med offset og limit`() {
        nyOppgaveForNyPerson()
            .avventSystemOgLagre(legacySaksbehandler)
            .ferdigstillOgLagre()
        val oppgave2 = nyOppgaveForNyPerson()
            .avventSystemOgLagre(legacySaksbehandler)
            .ferdigstillOgLagre()
        val oppgave3 = nyOppgaveForNyPerson()
            .avventSystemOgLagre(legacySaksbehandler)
            .ferdigstillOgLagre()

        val oppgaver = oppgaveDao.finnBehandledeOppgaver(legacySaksbehandler.saksbehandler.id.value, 1, 2)
        assertEquals(2, oppgaver.size)
        oppgaver.assertIderSamsvarerMed(oppgave2, oppgave3)
    }

    @Test
    fun `finner vedtaksperiodeId`() {
        val oppgave = nyOppgaveForNyPerson()
        val actual = oppgaveDao.finnVedtaksperiodeId(oppgave.id)
        assertEquals(oppgave.vedtaksperiodeId, actual)
    }

    @Test
    fun `sjekker at det ikke fins ferdigstilt oppgave`() {
        val oppgave = nyOppgaveForNyPerson()

        assertFalse(oppgaveDao.harFerdigstiltOppgave(oppgave.vedtaksperiodeId))
    }

    @Test
    fun `sjekker at det fins ferdigstilt oppgave`() {
        val oppgave = nyOppgaveForNyPerson()
            .avventSystemOgLagre(legacySaksbehandler)
            .ferdigstillOgLagre()

        opprettOppgave(vedtaksperiodeId = oppgave.vedtaksperiodeId)

        assertTrue(oppgaveDao.harFerdigstiltOppgave(oppgave.vedtaksperiodeId))
    }

    @Test
    fun `henter fødselsnummeret til personen en oppgave gjelder for`() {
        val fødselsnummer = lagFødselsnummer()
        val oppgave = nyOppgaveForNyPerson(fødselsnummer = fødselsnummer)
        val funnetFødselsnummer = oppgaveDao.finnFødselsnummer(oppgave.id)
        assertEquals(funnetFødselsnummer, fødselsnummer)
    }

    @Test
    fun `Finner vedtaksperiodeId med oppgaveId`() {
        val oppgave = nyOppgaveForNyPerson()

        assertEquals(oppgave.vedtaksperiodeId, oppgaveDao.finnVedtaksperiodeId(oppgave.id))
    }

    @Test
    fun `invaliderer oppgaver`() {
        val fødselsnummer1 = lagFødselsnummer()
        val oppgave1 = nyOppgaveForNyPerson(fødselsnummer = fødselsnummer1)
        val oppgave2 = nyOppgaveForNyPerson()

        oppgaveDao.invaliderOppgaveFor(fødselsnummer = fødselsnummer1)

        assertOppgaveStatus(oppgave1.id, Oppgave.Invalidert)
        assertOppgaveStatus(oppgave2.id, Oppgave.AvventerSaksbehandler)
    }

    @Test
    fun `Finner egenskaper for gitt vedtaksperiode med gitt utbetalingId`() {
        val oppgave1 = nyOppgaveForNyPerson()
        val oppgave2 = nyOppgaveForNyPerson(oppgaveegenskaper = setOf(Egenskap.SØKNAD, Egenskap.PÅ_VENT))

        val egenskaperOppgaveId1 = oppgaveDao.finnEgenskaper(oppgave1.vedtaksperiodeId, oppgave1.utbetalingId)
        val egenskaperOppgaveId2 = oppgaveDao.finnEgenskaper(oppgave2.vedtaksperiodeId, oppgave2.utbetalingId)
        assertEquals(setOf(SØKNAD), egenskaperOppgaveId1)
        assertEquals(setOf(SØKNAD, PÅ_VENT), egenskaperOppgaveId2)
    }

    @Test
    fun `Teller mine saker og mine saker på vent riktig`() {
        val annenSaksbehandler = nyLegacySaksbehandler()
        nyOppgaveForNyPerson()
            .tildelOgLagre(legacySaksbehandler)

        nyOppgaveForNyPerson()
            .tildelOgLagre(legacySaksbehandler)

        nyOppgaveForNyPerson()
            .tildelOgLagre(legacySaksbehandler)
            .leggPåVentOgLagre(legacySaksbehandler)

        nyOppgaveForNyPerson()
            .tildelOgLagre(annenSaksbehandler)
            .leggPåVentOgLagre(annenSaksbehandler)

        val antallOppgaver = oppgaveDao.finnAntallOppgaver(legacySaksbehandler.saksbehandler.id.value)

        assertEquals(2, antallOppgaver.antallMineSaker)
        assertEquals(1, antallOppgaver.antallMineSakerPåVent)
    }

    @Test
    fun `Antall mine saker og mine saker på vent er 0 hvis det ikke finnes tildeling for saksbehandler`() {
        nyOppgaveForNyPerson()

        val antallOppgaver = oppgaveDao.finnAntallOppgaver(legacySaksbehandler.saksbehandler.id.value)

        assertEquals(0, antallOppgaver.antallMineSaker)
        assertEquals(0, antallOppgaver.antallMineSakerPåVent)
    }

    private fun assertOppgaveStatus(
        oppgaveId: Long,
        forventetStatus: Oppgave.Tilstand,
    ) {
        val status = dbQuery.single("SELECT * FROM oppgave where id = :id", "id" to oppgaveId) { it.string("status") }
        assertEquals(forventetStatus.toString(), status)
    }

    private fun List<BehandletOppgaveFraDatabaseForVisning>.assertIderSamsvarerMed(vararg oppgaver: Oppgave) {
        assertEquals(oppgaver.map { it.id }.toSet(), map { it.id }.toSet())
    }
}
