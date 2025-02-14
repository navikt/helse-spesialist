package no.nav.helse.mediator.meldinger

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.db.CommandContextDao
import no.nav.helse.db.PersonDao
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeForkastetCommand
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

internal class VedtaksperiodeForkastetCommandTest {
    private companion object {
        private val HENDELSE = UUID.randomUUID()
        private val VEDTAKSPERIODE = UUID.randomUUID()
        private val CONTEXT = UUID.randomUUID()
        private const val FNR = "fnr"
    }

    private val commandContextDao = mockk<CommandContextDao>(relaxed = true)
    private val personDao = mockk<PersonDao>(relaxed = true)
    private val oppgaveService = mockk<OppgaveService>(relaxed = true)
    private val context = CommandContext(CONTEXT)
    private val vedtaksperiodeForkastetCommand =
        VedtaksperiodeForkastetCommand(
            fødselsnummer = FNR,
            vedtaksperiodeId = VEDTAKSPERIODE,
            id = HENDELSE,
            commandContextDao = commandContextDao,
            oppgaveService = oppgaveService,
            reservasjonDao = mockk(relaxed = true),
            tildelingDao = mockk(relaxed = true),
            oppgaveDao = mockk(relaxed = true),
            totrinnsvurderingRepository = mockk(relaxed = true),
        )

    @Test
    fun `avbryter kommandoer og markerer vedtaksperiode som forkastet`() {
        every { personDao.finnPersonMedFødselsnummer(FNR) } returns 1
        assertTrue(vedtaksperiodeForkastetCommand.execute(context))
        verify(exactly = 1) { commandContextDao.avbryt(VEDTAKSPERIODE, CONTEXT) }
    }
}
