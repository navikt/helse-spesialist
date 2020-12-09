package no.nav.helse.modell.kommando

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.MiljøstyrtFeatureToggle
import no.nav.helse.mediator.meldinger.Arbeidsgiverinformasjonløsning
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class KlargjørArbeidsgiverCommandTest {
    private companion object {
        private const val ORGNR = "123456789"
        private const val NAVN = "Et arbeidsgivernavn"
        private const val BRANSJER = "Spaghettikoding"
    }

    private val dao = mockk<ArbeidsgiverDao>(relaxed = true)
    private val miljøstyrtFeatureToggle = mockk<MiljøstyrtFeatureToggle>(relaxed = true)

    private lateinit var context: CommandContext
    private val command = KlargjørArbeidsgiverCommand(ORGNR, dao, miljøstyrtFeatureToggle)

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        clearMocks(dao)
        every { miljøstyrtFeatureToggle.arbeidsgiverinformasjon() } returns true
    }

    @Test
    fun `opprett arbeidsgiver`() {
        arbeidsgiverFinnesIkke()
        context.add(Arbeidsgiverinformasjonløsning(NAVN, BRANSJER))
        assertTrue(command.execute(context))
        verify(exactly = 1) { dao.insertArbeidsgiver(ORGNR, NAVN, BRANSJER) }
    }

    @Test
    fun `oppretter ikke arbeidsgiver når den finnes`() {
        arbeidsgiverFinnes()
        assertTrue(command.execute(context))
        verify(exactly = 0) { dao.insertArbeidsgiver(any(), any(), any()) }
    }

    @Test
    fun `sender ikke behov om feature toggle er skrudd av`() {
        every { miljøstyrtFeatureToggle.arbeidsgiverinformasjon() } returns false
        assertTrue(command.execute(context))
        assertFalse(context.harBehov())
        verify(exactly = 0) { dao.updateNavn(any(), any()) }
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
