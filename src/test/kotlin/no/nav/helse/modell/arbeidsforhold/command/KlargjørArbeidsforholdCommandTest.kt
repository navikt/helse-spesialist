package no.nav.helse.modell.arbeidsforhold.command

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.FeatureToggle.ARBEIDSFORHOLD_TOGGLE
import no.nav.helse.modell.arbeidsforhold.ArbeidsforholdDao
import no.nav.helse.modell.arbeidsforhold.ArbeidsforholdDto
import no.nav.helse.modell.arbeidsforhold.Arbeidsforholdløsning
import no.nav.helse.modell.kommando.CommandContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class KlargjørArbeidsforholdCommandTest {
    private companion object {
        const val AKTØR_ID = "00000123123123"
        const val FØDSELSNUMMER = "12345678910"
        const val ORGANISASJONSNUMMER = "987654321"
        const val PERSONID = 1L
        const val ARBEIDSGIVERID = 1L
        const val STILLINGSPROSENT = 100
        const val STILLINGSTITTEL = "Slabberasansvarlig"
        val STARTDATO: LocalDate = LocalDate.now()
        val SLUTTDATO = null
    }

    private val arbeidsforholdDao = mockk<ArbeidsforholdDao>(relaxed = true)

    private lateinit var context: CommandContext
    private val command = KlargjørArbeidsforholdCommand(
        aktørId = AKTØR_ID,
        fødselsnummer = FØDSELSNUMMER,
        organisasjonsnummer = ORGANISASJONSNUMMER,
        arbeidsforholdDao = arbeidsforholdDao
    )

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        clearMocks(arbeidsforholdDao)
        ARBEIDSFORHOLD_TOGGLE.enable()
    }

    @AfterEach
    fun tearDown() {
        ARBEIDSFORHOLD_TOGGLE.disable()
    }

    @Test
    fun `oppretter arbeidsforhold`() {
        arbeidsforholdFinnesIkke()
        assertFalse(command.execute(context))
        assertTrue(context.harBehov())
        context.add(
            Arbeidsforholdløsning(
                listOf(
                    Arbeidsforholdløsning.Løsning(
                        STARTDATO,
                        SLUTTDATO,
                        STILLINGSTITTEL,
                        STILLINGSPROSENT
                    )
                )
            )
        )
        assertTrue(command.resume(context))
        verify(exactly = 1) {
            arbeidsforholdDao.insertArbeidsforhold(
                FØDSELSNUMMER,
                ORGANISASJONSNUMMER,
                STARTDATO,
                SLUTTDATO,
                STILLINGSTITTEL,
                STILLINGSPROSENT
            )
        }
    }

    @Test
    fun `oppdaterer arbeidsforhold`() {
        arbeidsforholdErUtdatert()
        assertFalse(command.execute(context))
        assertTrue(context.harBehov())
        context.add(
            Arbeidsforholdløsning(
                listOf(
                    Arbeidsforholdløsning.Løsning(
                        STARTDATO,
                        SLUTTDATO,
                        STILLINGSTITTEL,
                        STILLINGSPROSENT
                    )
                )
            )
        )
        assertTrue(command.resume(context))
        verify(exactly = 1) {
            arbeidsforholdDao.oppdaterArbeidsforhold(
                FØDSELSNUMMER,
                ORGANISASJONSNUMMER,
                STARTDATO,
                SLUTTDATO,
                STILLINGSTITTEL,
                STILLINGSPROSENT
            )
        }
    }

    @Test
    fun `oppretter ikke arbeidsforhold når den finnes`() {
        arbeidsforholdFinnes()
        assertTrue(command.execute(context))
        verify(exactly = 0) { arbeidsforholdDao.insertArbeidsforhold(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `oppdaterer ikke arbeidsforhold når den er oppdatert`() {
        arbeidsforholdFinnes()
        assertTrue(command.execute(context))
        verify(exactly = 0) { arbeidsforholdDao.oppdaterArbeidsforhold(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `sender ikke behov om feature toggle er skrudd av`() {
        ARBEIDSFORHOLD_TOGGLE.disable()
        arbeidsforholdFinnesIkke()
        assertTrue(command.execute(context))
        assertFalse(context.harBehov())
        verify(exactly = 0) { arbeidsforholdDao.oppdaterArbeidsforhold(any(), any(), any(), any(), any(), any()) }
    }

    private fun arbeidsforholdFinnes() {
        every {
            arbeidsforholdDao.findArbeidsforhold(FØDSELSNUMMER, ORGANISASJONSNUMMER)
        } returns listOf(
            ArbeidsforholdDto(
                PERSONID,
                ARBEIDSGIVERID,
                STARTDATO,
                SLUTTDATO,
                STILLINGSPROSENT,
                STILLINGSTITTEL
            )
        )
        every {
            arbeidsforholdDao.findArbeidsforholdSistOppdatert(
                FØDSELSNUMMER,
                ORGANISASJONSNUMMER
            )
        } returns LocalDate.now()
    }

    private fun arbeidsforholdErUtdatert() {
        every {
            arbeidsforholdDao.findArbeidsforhold(FØDSELSNUMMER, ORGANISASJONSNUMMER)
        } returns listOf(
            ArbeidsforholdDto(
                PERSONID,
                ARBEIDSGIVERID,
                STARTDATO,
                SLUTTDATO,
                STILLINGSPROSENT,
                STILLINGSTITTEL
            )
        )
        every {
            arbeidsforholdDao.findArbeidsforholdSistOppdatert(
                FØDSELSNUMMER,
                ORGANISASJONSNUMMER
            )
        } returns LocalDate.now().minusYears(1)
    }

    private fun arbeidsforholdFinnesIkke() {
        every { arbeidsforholdDao.findArbeidsforhold(FØDSELSNUMMER, ORGANISASJONSNUMMER) } returns emptyList()
    }
}
