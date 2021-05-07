package no.nav.helse.modell.kommando

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.mediator.meldinger.Arbeidsgiverinformasjonløsning
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class KlargjørArbeidsgiverCommandTest {
    private companion object {
        private const val ORGNR = "123456789"
        private const val NAVN = "Et arbeidsgivernavn"
        private val BRANSJER = listOf("Spaghettikoding")
    }

    private val dao = mockk<ArbeidsgiverDao>(relaxed = true)

    private lateinit var context: CommandContext
    private val command = KlargjørArbeidsgiverCommand(listOf(ORGNR), dao)

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
                    Arbeidsgiverinformasjonløsning.ArbeidsgiverDto(ORGNR, NAVN, BRANSJER)
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
        every { dao.findBransjerSistOppdatert(ORGNR) } returns LocalDate.now()
        every { dao.findNavnSistOppdatert(ORGNR) } returns LocalDate.now()
    }

    private fun arbeidsgiverFinnesIkke() {
        every { dao.findArbeidsgiverByOrgnummer(ORGNR) } returns null
    }
}
