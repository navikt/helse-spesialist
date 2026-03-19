package no.nav.helse.spesialist.db.repository

import no.nav.helse.spesialist.application.testing.assertEqualsByMicrosecond
import no.nav.helse.spesialist.application.testing.assertNotEqualsByMicrosecond
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.UtbetalingId
import no.nav.helse.spesialist.domain.oppgave.Egenskap
import no.nav.helse.spesialist.domain.oppgave.Egenskap.PÅ_VENT
import no.nav.helse.spesialist.domain.oppgave.Egenskap.SØKNAD
import no.nav.helse.spesialist.domain.oppgave.Oppgave
import no.nav.helse.spesialist.domain.testfixtures.testdata.finnInntektsforhold
import no.nav.helse.spesialist.domain.testfixtures.testdata.finnInntektskilde
import no.nav.helse.spesialist.domain.testfixtures.testdata.finnMottaker
import no.nav.helse.spesialist.domain.testfixtures.testdata.finnOppgavetype
import no.nav.helse.spesialist.domain.testfixtures.testdata.finnPeriodetype
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.random.Random.Default.nextLong
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PgOppgaveRepositoryTest : AbstractDBIntegrationTest() {
    private val person = opprettPerson()
    private val arbeidsgiver = opprettArbeidsgiver()
    private val vedtaksperiode = opprettVedtaksperiode(person, arbeidsgiver)
    private val utbetalingId: UUID = UUID.randomUUID()
    private val behandling = opprettBehandling(vedtaksperiode, utbetalingId = UtbetalingId(utbetalingId))
    private val repository = PgOppgaveRepository(session)
    private val vedtaksperiodeId = vedtaksperiode.id.value
    private val behandlingId = behandling.spleisBehandlingId!!
    private val godkjenningsbehovId: UUID = UUID.randomUUID()

    private val saksbehandler =
        lagSaksbehandler().also {
            sessionContext.saksbehandlerRepository.lagre(it)
        }

    @Test
    fun `lagre og finn oppgave`() {
        val oppgave = lagOppgave()

        repository.lagre(oppgave)
        val funnetOppgave = repository.finn(oppgave.id.value)
        assertNotNull(funnetOppgave)
        assertEquals(oppgave, funnetOppgave)
    }

    @Test
    fun `lagre og finn aktiv oppgave for person`() {
        val oppgave = lagOppgave()

        repository.lagre(oppgave)
        val funnetOppgave = repository.finnAktivForPerson(person.id)
        assertNotNull(funnetOppgave)
        assertEquals(oppgave, funnetOppgave)
    }

    @Test
    fun `lagre og finn oppgave for spleisBehandlingId`() {
        val oppgave = lagOppgave()

        repository.lagre(oppgave)
        val funnetOppgave = repository.finn(behandlingId)
        assertNotNull(funnetOppgave)
        assertEquals(oppgave, funnetOppgave)
    }

    @Test
    fun `tildel oppgave`() {
        val oppgave = lagOppgave()
        oppgave.forsøkTildeling(saksbehandler, emptySet())

        repository.lagre(oppgave)
        val funnetOppgave = repository.finn(oppgave.id.value)
        assertNotNull(funnetOppgave)
        assertEquals(saksbehandler.id, funnetOppgave.tildeltTil)
    }

    @Test
    fun `avmeld oppgave`() {
        val oppgave = lagOppgave()
        oppgave.forsøkTildeling(saksbehandler, emptySet())
        repository.lagre(oppgave)

        oppgave.forsøkAvmelding(saksbehandler)
        repository.lagre(oppgave)

        val funnetOppgave = repository.finn(oppgave.id.value)
        assertNotNull(funnetOppgave)
        assertEquals(null, funnetOppgave.tildeltTil)
    }

    @Test
    fun `endre egenskaper`() {
        val oppgave = lagOppgave(egenskaper = setOf(SØKNAD, PÅ_VENT))
        repository.lagre(oppgave)
        oppgave.leggTilEgenAnsatt()
        oppgave.fjernFraPåVent()
        repository.lagre(oppgave)
        val funnetOppgave = repository.finn(oppgave.id.value)
        assertNotNull(funnetOppgave)
        assertEquals(setOf(Egenskap.EGEN_ANSATT, SØKNAD), funnetOppgave.egenskaper)
    }

    @Test
    fun `endre tilstand`() {
        val saksbehandler = opprettSaksbehandler()
        val oppgave = lagOppgave()
        repository.lagre(oppgave)
        oppgave.avventerSystem(saksbehandler.ident, saksbehandler.id.value)
        repository.lagre(oppgave)
        val funnetOppgave = repository.finn(oppgave.id.value)
        assertNotNull(funnetOppgave)
        assertEquals(Oppgave.AvventerSystem, funnetOppgave.tilstand)
    }

    @Test
    fun ferdigstill() {
        val saksbehandler = opprettSaksbehandler()
        val oppgave = lagOppgave()
        repository.lagre(oppgave)
        oppgave.avventerSystem(saksbehandler.ident, saksbehandler.id.value)
        oppgave.ferdigstill()
        repository.lagre(oppgave)
        val funnetOppgave = repository.finn(oppgave.id.value)
        assertNotNull(funnetOppgave)
        assertEquals(Oppgave.Ferdigstilt, funnetOppgave.tilstand)
        assertEquals(saksbehandler.ident, funnetOppgave.ferdigstiltAvIdent)
        assertEquals(saksbehandler.id.value, funnetOppgave.ferdigstiltAvOid)
    }

    @Test
    fun `exception om det finnes en annen aktiv oppgave for personen ved lagring`() {
        val oppgave1 =
            lagOppgave()
        val oppgave2 =
            lagOppgave()
        repository.lagre(oppgave1)
        assertThrows<IllegalStateException> {
            repository.lagre(oppgave2)
        }
    }

    @Test
    fun `skal lagre ny oppgave dersom eksisterende oppgave på samme person ikke avventer saksbehandler`() {
        val saksbehandler = opprettSaksbehandler()
        val oppgave1 =
            lagOppgave()
        oppgave1.avventerSystem(saksbehandler.ident, saksbehandler.id.value)
        oppgave1.ferdigstill()
        val oppgave2 =
            lagOppgave()
        repository.lagre(oppgave1)
        assertDoesNotThrow {
            repository.lagre(oppgave2)
        }
    }

    @Test
    fun `lagrer første opprettet-kolonnen på en oppgave når det ligger to fra før på behandlingen`() {
        val oppgaveId = nextLong()
        val førsteOppgave = lagOppgave(oppgaveId)
        førsteOppgave.avventerSystem(saksbehandler.ident, saksbehandler.id.value)
        førsteOppgave.avbryt()
        repository.lagre(førsteOppgave)
        val lagretFørsteOppgave = repository.finn(førsteOppgave.id.value)
        assertNotNull(lagretFørsteOppgave)
        assertEqualsByMicrosecond(lagretFørsteOppgave.opprettet, lagretFørsteOppgave.førsteOpprettet)

        Thread.sleep(100L)

        val andreOppgave = lagOppgave(oppgaveId + 1)
        andreOppgave.avventerSystem(saksbehandler.ident, saksbehandler.id.value)
        andreOppgave.avbryt()
        repository.lagre(andreOppgave)
        val lagretAndreOppgave = repository.finn(andreOppgave.id.value)
        assertNotNull(lagretAndreOppgave)
        assertNotEqualsByMicrosecond(lagretAndreOppgave.opprettet, lagretAndreOppgave.førsteOpprettet)
        assertEqualsByMicrosecond(lagretFørsteOppgave.opprettet, lagretAndreOppgave.førsteOpprettet)

        Thread.sleep(100L)

        val tredjeOppgave = lagOppgave(oppgaveId + 2)
        repository.lagre(tredjeOppgave)
        val lagretTredjeOppgave = repository.finn(tredjeOppgave.id.value)
        assertNotNull(lagretTredjeOppgave)
        assertNotEqualsByMicrosecond(lagretTredjeOppgave.opprettet, lagretTredjeOppgave.førsteOpprettet)
        assertEqualsByMicrosecond(lagretFørsteOppgave.opprettet, lagretTredjeOppgave.førsteOpprettet)
    }

    private fun lagOppgave(
        oppgaveId: Long = nextLong(),
        egenskaper: Set<Egenskap> = setOf(SØKNAD),
    ) = Oppgave.ny(
        id = oppgaveId,
        førsteOpprettet = repository.førsteOpprettetForBehandlingId(behandlingId.value),
        vedtaksperiodeId = vedtaksperiodeId,
        behandlingId = behandlingId,
        utbetalingId = utbetalingId,
        hendelseId = godkjenningsbehovId,
        kanAvvises = true,
        egenskaper = egenskaper,
        mottaker = egenskaper.finnMottaker(),
        oppgavetype = egenskaper.finnOppgavetype(),
        inntektskilde = egenskaper.finnInntektskilde(),
        inntektsforhold = egenskaper.finnInntektsforhold(),
        periodetype = egenskaper.finnPeriodetype(),
    )

    private fun assertEquals(
        expected: Oppgave,
        actual: Oppgave,
    ) {
        assertEquals(expected.id.value, actual.id.value)
        assertEqualsByMicrosecond(expected.opprettet, actual.opprettet)
        assertEqualsByMicrosecond(expected.førsteOpprettet, actual.førsteOpprettet)
        assertEquals(expected.tilstand, actual.tilstand)
        assertEquals(expected.vedtaksperiodeId, actual.vedtaksperiodeId)
        assertEquals(expected.behandlingId, actual.behandlingId)
        assertEquals(expected.utbetalingId, actual.utbetalingId)
        assertEquals(expected.godkjenningsbehovId, actual.godkjenningsbehovId)
        assertEquals(expected.kanAvvises, actual.kanAvvises)
        assertEquals(expected.ferdigstiltAvIdent, actual.ferdigstiltAvIdent)
        assertEquals(expected.ferdigstiltAvOid, actual.ferdigstiltAvOid)
        assertEquals(expected.egenskaper, actual.egenskaper)
        assertEquals(expected.tildeltTil, actual.tildeltTil)
    }
}
