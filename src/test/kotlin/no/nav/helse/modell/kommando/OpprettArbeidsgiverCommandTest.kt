package no.nav.helse.modell.kommando

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.meldinger.Arbeidsgiverinformasjonløsning
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class OpprettArbeidsgiverCommandTest {
    private companion object {
        private const val NAVN = "Et eller annet fint da"
        private const val ORGNR = "123456789"
        private val BRANSJER = listOf("Spaghettikoding")
    }

    private val dao = mockk<ArbeidsgiverDao>(relaxed = true)

    private lateinit var context: CommandContext
    private val command = OpprettArbeidsgiverCommand(listOf(ORGNR), dao)

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        clearMocks(dao)
    }

    @Test
    fun `opprett arbeidsgiver`() {
        arbeidsgiverFinnesIkke()
        context.add(
            Arbeidsgiverinformasjonløsning(
                listOf(
                    Arbeidsgiverinformasjonløsning.ArbeidsgiverDto(
                        ORGNR,
                        NAVN,
                        BRANSJER
                    )
                )
            )
        )
        assertTrue(command.execute(context))
        verify(exactly = 1) { dao.insertArbeidsgiver(ORGNR, NAVN, BRANSJER) }
    }

    @Test
    fun `oppretter ikke arbeidsgiver når den finnes`() {
        arbeidsgiverFinnes()
        assertTrue(command.execute(context))
        verify(exactly = 0) { dao.insertArbeidsgiver(any(), any(), any()) }
    }

    private fun arbeidsgiverFinnes() {
        every { dao.findArbeidsgiverByOrgnummer(ORGNR) } returns 1
    }

    private fun arbeidsgiverFinnesIkke() {
        every { dao.findArbeidsgiverByOrgnummer(ORGNR) } returns null
    }
}
