package no.nav.helse.modell.utbetaling

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.modell.kommando.CommandContext
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class UtbetalingsfilterCommandTest {
    private lateinit var context: CommandContext
    private val utbetalingsfilter = mockk<Utbetalingsfilter>(relaxed = true)
    private val godkjenningMediator = mockk<GodkjenningMediator>(relaxed = true)

    private val command = UtbetalingsfilterCommand(
        vedtaksperiodeId = UUID.randomUUID(),
        fødselsnummer = "11111111111",
        hendelseId = UUID.randomUUID(),
        godkjenningsbehovJson = "{}",
        godkjenningMediator = godkjenningMediator,
        utbetalingsfilter = { utbetalingsfilter },
        utbetaling = Utbetaling(UUID.randomUUID(), 1000, 1000)
    )

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        clearMocks(utbetalingsfilter)
    }

    @Test
    fun `vedtaksperioden kan utbetales`() {
        every { utbetalingsfilter.kanUtbetales }.returns(true)
        assertTrue(command.execute(context))
        verify(exactly = 1) { utbetalingsfilter.kanUtbetales }
        verify(exactly = 0) { utbetalingsfilter.årsaker() }
        verify(exactly = 0) { godkjenningMediator.automatiskAvvisning(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `vedtaksperioden kan ikke utbetales`() {
        val årsaker = listOf("test")
        every { utbetalingsfilter.kanUtbetales }.returns(false)
        every { utbetalingsfilter.årsaker() }.returns(årsaker)
        assertTrue(command.execute(context))
        verify(exactly = 1) { utbetalingsfilter.kanUtbetales }
        verify(exactly = 1) { utbetalingsfilter.årsaker() }
        verify(exactly = 1) { godkjenningMediator.automatiskAvvisning(any(), any(), any(), any(), eq(årsaker), any()) }
    }
}
