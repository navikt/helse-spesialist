package no.nav.helse.spesialist.db.repository

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.UtbetalingId
import no.nav.helse.spesialist.domain.oppgave.Egenskap
import no.nav.helse.spesialist.domain.oppgave.Egenskap.SØKNAD
import no.nav.helse.spesialist.domain.oppgave.Oppgave
import no.nav.helse.spesialist.domain.testfixtures.testdata.finnInntektsforhold
import no.nav.helse.spesialist.domain.testfixtures.testdata.finnInntektskilde
import no.nav.helse.spesialist.domain.testfixtures.testdata.finnMottaker
import no.nav.helse.spesialist.domain.testfixtures.testdata.finnOppgavetype
import no.nav.helse.spesialist.domain.testfixtures.testdata.finnPeriodetype
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import java.util.UUID
import java.util.UUID.randomUUID
import kotlin.random.Random.Default.nextLong
import kotlin.test.assertEquals

@Isolated
class PgOppgaveRepositoryVarselOppslagTest : AbstractDBIntegrationTest() {
    @BeforeEach
    fun tømTabeller() {
        dbQuery.execute("TRUNCATE oppgave CASCADE")
    }

    private val person = opprettPerson()
    private val arbeidsgiver = opprettArbeidsgiver()
    private val vedtaksperiode = opprettVedtaksperiode(person, arbeidsgiver)
    private val utbetalingId: UUID = randomUUID()
    private val behandling = opprettBehandling(vedtaksperiode, utbetalingId = UtbetalingId(utbetalingId))
    private val repository = PgOppgaveRepository(session)
    private val behandlingId = behandling.spleisBehandlingId!!

    @Test
    fun `finnFødselsnumreForÅpneOppgaverMedAktivtVarsel - returnerer fødselsnummer for oppgave med aktivt varsel`() {
        repository.lagre(lagOppgave())
        opprettVarsel(behandling, "SB_EX_3")

        val resultat = repository.finnFødselsnumreForÅpneOppgaverMedAktivtVarsel("SB_EX_3")

        assertEquals(setOf(person.id.value), resultat)
    }

    @Test
    fun `finnFødselsnumreForÅpneOppgaverMedAktivtVarsel - returnerer tom liste når varselet er inaktivt`() {
        repository.lagre(lagOppgave())
        val varsel = opprettVarsel(behandling, "SB_EX_3")
        varsel.deaktiver()
        sessionContext.varselRepository.lagre(varsel)

        val resultat = repository.finnFødselsnumreForÅpneOppgaverMedAktivtVarsel("SB_EX_3")

        assertEquals(emptySet(), resultat)
    }

    @Test
    fun `finnFødselsnumreForÅpneOppgaverMedAktivtVarsel - returnerer tom liste når varselkode ikke matcher`() {
        repository.lagre(lagOppgave())
        opprettVarsel(behandling, "EN_ANNEN_KODE")

        val resultat = repository.finnFødselsnumreForÅpneOppgaverMedAktivtVarsel("SB_EX_3")

        assertEquals(emptySet(), resultat)
    }

    @Test
    fun `finnFødselsnumreForÅpneOppgaverMedAktivtVarsel - returnerer ikke fødselsnummer når oppgave ikke er AvventerSaksbehandler`() {
        val oppgave = lagOppgave()
        val (ident, oid) = lagSaksbehandler().let { it.ident to it.id }
        oppgave.avventerSystem(ident, oid.value)
        repository.lagre(oppgave)
        opprettVarsel(behandling, "SB_EX_3")

        val resultat = repository.finnFødselsnumreForÅpneOppgaverMedAktivtVarsel("SB_EX_3")

        assertEquals(emptySet(), resultat)
    }

    private fun lagOppgave(
        oppgaveId: Long = nextLong(),
        egenskaper: Set<Egenskap> = setOf(SØKNAD),
    ) = Oppgave.ny(
        id = oppgaveId,
        førsteOpprettet = repository.førsteOpprettetForBehandlingId(behandlingId.value),
        vedtaksperiodeId = vedtaksperiode.id,
        behandlingId = behandlingId,
        utbetalingId = utbetalingId,
        hendelseId = randomUUID(),
        kanAvvises = true,
        egenskaper = egenskaper,
        mottaker = egenskaper.finnMottaker(),
        oppgavetype = egenskaper.finnOppgavetype(),
        inntektskilde = egenskaper.finnInntektskilde(),
        inntektsforhold = egenskaper.finnInntektsforhold(),
        periodetype = egenskaper.finnPeriodetype(),
    )
}
