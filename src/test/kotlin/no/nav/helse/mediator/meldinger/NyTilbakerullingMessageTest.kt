package no.nav.helse.mediator.meldinger

import io.mockk.Ordering
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.command.OppgaveDao
import no.nav.helse.modell.command.nyny.CommandContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class NyTilbakerullingMessageTest {

    private lateinit var nyTilbakerullingMessage: NyTilbakerullingMessage
    private lateinit var context: CommandContext
    private val CONTEXTID = UUID.randomUUID()
    private val vedtaksperiodeIder = listOf(UUID.randomUUID(), UUID.randomUUID())

    private val commandContextDao = mockk<CommandContextDao>(relaxed = true)
    private val oppgaveDao = mockk<OppgaveDao>(relaxed = true)
    private val vedtakDao = mockk<VedtakDao>(relaxed = true)

    @BeforeEach
    fun setupEach() {
        nyTilbakerullingMessage = NyTilbakerullingMessage(
            UUID.randomUUID(),
            FNR,
            "{}",
            vedtaksperiodeIder,
            commandContextDao,
            oppgaveDao,
            vedtakDao
        )

        context = CommandContext(CONTEXTID)
    }

    @Test
    fun `Sletter alt relatert til vedtaksperioder`() {
        nyTilbakerullingMessage.execute(context)

        verify(ordering = Ordering.SEQUENCE) {
            oppgaveDao.invaliderOppgaver(FNR)
            commandContextDao.avbryt(FNR, CONTEXTID)
            vedtakDao.fjernVedtaksperioder(vedtaksperiodeIder)
        }
    }

    companion object {
        private const val FNR = "fnr"
    }
}

