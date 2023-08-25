package no.nav.helse.mediator.meldinger

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.gosysoppgaver.GosysOppgaveEndretCommandData
import no.nav.helse.modell.oppgave.OppgaveDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

internal class GosysOppgaveEndretRiverTest {

    private val mediator = mockk<HendelseMediator>(relaxed = true)
    private val testRapid = TestRapid()
    private val personDao = mockk<PersonDao>(relaxed = true)
    private val oppgaveDao = mockk<OppgaveDao>(relaxed = true)

    init {
        GosysOppgaveEndretRiver(testRapid, mediator, oppgaveDao, personDao)
    }

    @Test
    fun `Hvis vi får inn et event for en oppgave til_godkjenning som ikke er tildelt og som har commanddata, kall mediator`() {
        mocks()
        testRapid.sendTestMessage(event())
        verify(exactly = 1) { mediator.gosysOppgaveEndret(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `Kaller ikke mediator hvis oppgave ikke er til_godkjenning`() {
        mocks(oppgaveId = null)
        testRapid.sendTestMessage(event())
        verify(exactly = 0) { mediator.gosysOppgaveEndret(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `Kaller ikke mediator hvis vi ikke har commanddata for oppgave`() {
        mocks(commandData = null)
        testRapid.sendTestMessage(event())
        verify(exactly = 0) { mediator.gosysOppgaveEndret(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `Kaller ikke mediator hvis vi ikke finner aktørid`() {
        mocks()
        every { personDao.finnAktørId(any()) } returns null
        testRapid.sendTestMessage(event())
        verify(exactly = 0) { mediator.gosysOppgaveEndret(any(), any(), any(), any(), any()) }
    }

    private fun mocks(
        oppgaveId: Long? = 1L,
        commandData: GosysOppgaveEndretCommandData? = GosysOppgaveEndretCommandData(
            vedtaksperiodeId = UUID.randomUUID(),
            periodeFom = LocalDate.now(),
            periodeTom = LocalDate.now(),
            utbetalingId = UUID.randomUUID(),
            hendelseId = UUID.randomUUID(),
            godkjenningsbehovJson = "{}",
            periodetype = Periodetype.FØRSTEGANGSBEHANDLING,
            skjæringstidspunkt = LocalDate.now()
        )
    ) {
        every { oppgaveDao.finnOppgaveId(any<String>()) }.returns(oppgaveId)
        every { oppgaveDao.gosysOppgaveEndretCommandData(any()) }.returns(commandData)
    }

    @Language("JSON")
    private fun event() = """
    {
      "@event_name": "gosys_oppgave_endret",
      "@id": "${UUID.randomUUID()}",
      "@opprettet": "${LocalDateTime.now()}",
      "fødselsnummer": "11111100000"
    }"""

}
