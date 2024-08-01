package no.nav.helse.modell.kommando

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.db.AnnulleringDao
import no.nav.helse.db.SaksbehandlerDao
import no.nav.helse.modell.utbetaling.UtbetalingDao
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class LagreAnnulleringCommandTest {

    private companion object {
        private val UTBETALING_ID = UUID.randomUUID()
        private val ANNULLERING_ID = 7L
    }

    private val utbetalingDao = mockk<UtbetalingDao>(relaxed = true)
    private val annulleringDao = mockk<AnnulleringDao>(relaxed = true)
    private val saksbehandlerDao = mockk<SaksbehandlerDao>(relaxed = true)
    private val command = LagreAnnulleringCommand(
        utbetalingDao,
        annulleringDao,
        UTBETALING_ID
    )
    private lateinit var context: CommandContext

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        clearMocks(utbetalingDao)
        clearMocks(saksbehandlerDao)
    }

    @Test
    fun `Legg til annullert av saksbehandler`() {
        every { annulleringDao.finnAnnulleringId(UTBETALING_ID)} returns ANNULLERING_ID

        command.execute(context)
        verify(exactly = 1) { utbetalingDao.leggTilAnnullertAvSaksbehandler(UTBETALING_ID, ANNULLERING_ID) }
    }
}
