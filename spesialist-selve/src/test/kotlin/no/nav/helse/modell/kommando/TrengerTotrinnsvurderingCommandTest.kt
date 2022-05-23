package no.nav.helse.modell.kommando


import io.mockk.every
import io.mockk.mockk
import java.util.UUID
import no.nav.helse.modell.WarningDao

import no.nav.helse.oppgave.OppgaveDao
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


internal class TrengerTotrinnsvurderingCommandTest {

    private companion object {
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
    }

    private val warningDao = mockk<WarningDao>(relaxed = true)
    private val oppgaveDao = mockk<OppgaveDao>(relaxed = true)
    private lateinit var context: CommandContext

    private val command = TrengerTotrinnsvurderingCommand(
        vedtaksperiodeId = VEDTAKSPERIODE_ID,
        warningDao = warningDao,
        oppgaveDao = oppgaveDao
    )

    @BeforeEach
    fun setUp() {
        context = CommandContext(UUID.randomUUID())
    }

    @Test
    fun `Setter trenger totrinnsvudering dersom oppgaven har endring`() {
        every { oppgaveDao.harOppgaveMedEndring(VEDTAKSPERIODE_ID) } returns true
        assertTrue(command.execute(context))


    }

}