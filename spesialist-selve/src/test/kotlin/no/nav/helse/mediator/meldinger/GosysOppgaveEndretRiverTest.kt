package no.nav.helse.mediator.meldinger

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.mediator.IHendelseMediator
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.oppgave.GosysOppgaveEndretCommandData
import no.nav.helse.oppgave.OppgaveDao
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.tildeling.TildelingApiDto
import no.nav.helse.tildeling.TildelingDao
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

internal class GosysOppgaveEndretRiverTest {

    private val mediator = mockk<IHendelseMediator>(relaxed = true)
    private val testRapid = TestRapid()
    private val oppgaveDao = mockk<OppgaveDao>(relaxed = true)
    private val tildelingDao = mockk<TildelingDao>(relaxed = true)

    init {
        GosysOppgaveEndret.River(testRapid, mediator, oppgaveDao, tildelingDao)
    }

    @Test
    fun `Hvis vi får inn et event for en oppgave til_godkjenning som ikke er tildelt og som har commanddata, kall mediator`() {
        mocks()
        testRapid.sendTestMessage(event())
        verify(exactly = 1) { mediator.gosysOppgaveEndret(any(), any()) }
    }

    @Test
    fun `Kaller ikke mediator hvis oppgave er tildelt`() {
        mocks(tildeling = TildelingApiDto(navn="", epost="", oid=UUID.randomUUID(), påVent = false))
        testRapid.sendTestMessage(event())
        verify(exactly = 0) { mediator.gosysOppgaveEndret(any(), any()) }
    }

    @Test
    fun `Kaller ikke mediator hvis oppgave ikke er til_godkjenning`() {
        mocks(oppgaveId = null)
        testRapid.sendTestMessage(event())
        verify(exactly = 0) { mediator.gosysOppgaveEndret(any(), any()) }
    }

    @Test
    fun `Kaller ikke mediator hvis vi ikke har commanddata for oppgave`() {
        mocks(commandData = null)
        testRapid.sendTestMessage(event())
        verify(exactly = 0) { mediator.gosysOppgaveEndret(any(), any()) }
    }

    private fun mocks(
        oppgaveId: Long? = 1L,
        tildeling: TildelingApiDto? = null,
        commandData: GosysOppgaveEndretCommandData? = GosysOppgaveEndretCommandData(
            vedtaksperiodeId = UUID.randomUUID(),
            periodeFom = LocalDate.now(),
            periodeTom = LocalDate.now(),
            utbetalingId = UUID.randomUUID(),
            utbetalingType = Utbetalingtype.UTBETALING.name,
            hendelseId = UUID.randomUUID(),
            godkjenningsbehovJson = "{}",
        )
    ) {
        every { oppgaveDao.finnOppgaveId(any<String>()) }.returns(oppgaveId)
        every { tildelingDao.tildelingForOppgave(any()) }.returns(tildeling)
        every { oppgaveDao.gosysOppgaveEndretCommandData(any()) }.returns(commandData)
    }

    @Language("JSON")
    private fun event() = """
    {
      "@event_name": "gosys_oppgave_endret",
      "@id": "${UUID.randomUUID()}",
      "@opprettet": "${LocalDateTime.now()}",
      "aktørId": "1111100000000",
      "fødselsnummer": "11111100000"
    }"""

}