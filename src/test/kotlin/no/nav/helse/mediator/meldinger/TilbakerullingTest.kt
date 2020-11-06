package no.nav.helse.mediator.meldinger

import io.mockk.Ordering
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.OppgaveMediator
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.kommando.CommandContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class TilbakerullingTest {
    private companion object {
        private const val FNR = "fnr"
    }
    private lateinit var tilbakerulling: Tilbakerulling
    private lateinit var context: CommandContext
    private val CONTEXTID = UUID.randomUUID()

    private val vedtaksperiodeIder = listOf(UUID.randomUUID(), UUID.randomUUID())
    private val commandContextDao = mockk<CommandContextDao>(relaxed = true)
    private val oppgaveMediator = mockk<OppgaveMediator>(relaxed = true)

    private val vedtakDao = mockk<VedtakDao>(relaxed = true)

    @BeforeEach
    fun setupEach() {
        tilbakerulling = Tilbakerulling(
            UUID.randomUUID(),
            FNR,
            "{}",
            vedtaksperiodeIder,
            commandContextDao,
            oppgaveMediator,
            vedtakDao
        )

        context = CommandContext(CONTEXTID)
    }

    @Test
    fun `Sletter alt relatert til vedtaksperioder`() {
        tilbakerulling.execute(context)

        verify(ordering = Ordering.SEQUENCE) {
            oppgaveMediator.avbrytOppgaver(FNR)
            commandContextDao.avbryt(FNR, CONTEXTID)
            vedtakDao.fjernVedtaksperioder(vedtaksperiodeIder)
        }
    }
}

