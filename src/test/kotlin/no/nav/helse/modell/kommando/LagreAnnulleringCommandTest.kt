package no.nav.helse.modell.kommando

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.januar
import no.nav.helse.modell.saksbehandler.SaksbehandlerDao
import no.nav.helse.modell.saksbehandler.SaksbehandlerDto
import no.nav.helse.modell.utbetaling.UtbetalingDao
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class LagreAnnulleringCommandTest {

    private companion object {
        private val ANNULLERT_TIDSPUNKT = 1.januar.atTime(12, 0)
        private const val SAKSBEHANDLER_EPOST = "kevders.chilleby@nav.no"
        private const val SAKSBEHANDLER_NAVN = "Kevders Chilleby"
        private val UTBETALING_ID = UUID.randomUUID()
        private val ANNULLERING_ID = 7L
    }

    private val utbetalingDao = mockk<UtbetalingDao>(relaxed = true)
    private val saksbehandlerDao = mockk<SaksbehandlerDao>(relaxed = true)
    private val command = LagreAnnulleringCommand(
        utbetalingDao,
        saksbehandlerDao,
        ANNULLERT_TIDSPUNKT,
        SAKSBEHANDLER_EPOST,
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
        every { saksbehandlerDao.finnSaksbehandler(SAKSBEHANDLER_EPOST) } returns listOf(
            SaksbehandlerDto(
                oid = UUID.randomUUID(),
                navn = SAKSBEHANDLER_NAVN,
                epost = SAKSBEHANDLER_EPOST
            )
        )
        every { utbetalingDao.nyAnnullering(ANNULLERT_TIDSPUNKT, any()) } returns ANNULLERING_ID

        command.execute(context)
        verify(exactly = 1) { utbetalingDao.leggTilAnnullertAvSaksbehandler(UTBETALING_ID, ANNULLERING_ID) }
    }
}
