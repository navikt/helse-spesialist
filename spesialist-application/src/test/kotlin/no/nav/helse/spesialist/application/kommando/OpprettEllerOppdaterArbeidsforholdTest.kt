package no.nav.helse.spesialist.application.kommando

import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.modell.KomplettArbeidsforholdDto
import no.nav.helse.modell.arbeidsforhold.Arbeidsforholdløsning
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.OpprettEllerOppdaterArbeidsforhold
import no.nav.helse.modell.melding.Behov
import no.nav.helse.spesialist.application.testing.assertIFortiden
import no.nav.helse.spesialist.application.testing.assertMindreEnnNSekunderSiden
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class OpprettEllerOppdaterArbeidsforholdTest : ApplicationTest() {
    private companion object {
        const val FØDSELSNUMMER = "12345678910"
        const val ORGANISASJONSNUMMER = "987654321"
        const val STILLINGSPROSENT = 100
        const val STILLINGSTITTEL = "Slabberasansvarlig"
        val STARTDATO: LocalDate = LocalDate.now()
        val SLUTTDATO = null
    }

    private val observer =
        object : CommandContextObserver {
            val behov = mutableListOf<Behov>()

            override fun behov(
                behov: Behov,
                commandContextId: UUID,
                sti: List<Int>,
            ) {
                this.behov.add(behov)
            }
        }
    private val commandContext: CommandContext = CommandContext(UUID.randomUUID()).also { it.nyObserver(observer) }

    private fun enCommand() =
        OpprettEllerOppdaterArbeidsforhold(
            fødselsnummer = FØDSELSNUMMER,
            organisasjonsnummer = ORGANISASJONSNUMMER,
        )

    @Test
    fun `oppretter arbeidsforhold`() {
        arbeidsforholdFinnesIkke()
        val command = enCommand()
        assertFalse(command.execute(commandContext, sessionContext, outbox))
        assertTrue(observer.behov.isNotEmpty())
        commandContext.add(
            Arbeidsforholdløsning(
                listOf(
                    Arbeidsforholdløsning.Løsning(
                        STARTDATO,
                        SLUTTDATO,
                        STILLINGSTITTEL,
                        STILLINGSPROSENT,
                    ),
                ),
            ),
        )
        assertTrue(command.resume(commandContext, sessionContext, outbox))

        assertEquals(1, sessionContext.arbeidsforholdDao.oppdaterteArbeidsforhold.size)
        assertExpectedKomplettArbeidsforholdDto(sessionContext.arbeidsforholdDao.oppdaterteArbeidsforhold.single())
    }

    @Test
    fun `oppdaterer arbeidsforhold`() {
        arbeidsforholdFinnes(enKomplettArbeidsforholdDto(oppdatert = LocalDateTime.now().minusYears(1)))
        val command = enCommand()
        assertFalse(command.execute(commandContext, sessionContext, outbox))
        assertTrue(observer.behov.isNotEmpty())
        commandContext.add(
            Arbeidsforholdløsning(
                listOf(
                    Arbeidsforholdløsning.Løsning(
                        STARTDATO,
                        SLUTTDATO,
                        STILLINGSTITTEL,
                        STILLINGSPROSENT,
                    ),
                ),
            ),
        )
        assertTrue(command.resume(commandContext, sessionContext, outbox))

        assertEquals(1, sessionContext.arbeidsforholdDao.oppdaterteArbeidsforhold.size)
        assertExpectedKomplettArbeidsforholdDto(sessionContext.arbeidsforholdDao.oppdaterteArbeidsforhold.single())
    }

    @Test
    fun `oppretter ikke arbeidsforhold når den finnes`() {
        arbeidsforholdFinnes(enKomplettArbeidsforholdDto())
        assertTrue(enCommand().execute(commandContext, sessionContext, outbox))
        assertEquals(0, sessionContext.arbeidsforholdDao.oppdaterteArbeidsforhold.size)
    }

    @Test
    fun `oppdaterer ikke arbeidsforhold når den er oppdatert`() {
        arbeidsforholdFinnes(enKomplettArbeidsforholdDto())
        assertTrue(enCommand().execute(commandContext, sessionContext, outbox))
        assertEquals(0, sessionContext.arbeidsforholdDao.oppdaterteArbeidsforhold.size)
    }

    @Test
    fun `oppretter arbeidsforhold når det ikke finnes for orgnummeret`() {
        arbeidsforholdFinnes(enKomplettArbeidsforholdDto(organisasjonsnummer = ORGANISASJONSNUMMER.reversed()))
        val command = enCommand()
        assertFalse(command.execute(commandContext, sessionContext, outbox))
        assertTrue(observer.behov.isNotEmpty())
        commandContext.add(
            Arbeidsforholdløsning(
                listOf(
                    Arbeidsforholdløsning.Løsning(
                        STARTDATO,
                        SLUTTDATO,
                        STILLINGSTITTEL,
                        STILLINGSPROSENT,
                    ),
                ),
            ),
        )
        assertTrue(command.resume(commandContext, sessionContext, outbox))
        assertEquals(1, sessionContext.arbeidsforholdDao.oppdaterteArbeidsforhold.size)
    }

    private fun arbeidsforholdFinnes(komplettArbeidsforholdDto: KomplettArbeidsforholdDto) {
        sessionContext.arbeidsforholdDao.eksisterendeArbeidsforhold.add(komplettArbeidsforholdDto)
    }

    private fun arbeidsforholdFinnesIkke() {
        sessionContext.arbeidsforholdDao.eksisterendeArbeidsforhold.clear()
    }

    private fun enKomplettArbeidsforholdDto(
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGANISASJONSNUMMER,
        startdato: LocalDate = STARTDATO,
        sluttdato: LocalDate? = SLUTTDATO,
        stillingstittel: String = STILLINGSTITTEL,
        stillingsprosent: Int = STILLINGSPROSENT,
        oppdatert: LocalDateTime = LocalDateTime.now(),
    ) = KomplettArbeidsforholdDto(
        fødselsnummer = fødselsnummer,
        organisasjonsnummer = organisasjonsnummer,
        startdato = startdato,
        sluttdato = sluttdato,
        stillingstittel = stillingstittel,
        stillingsprosent = stillingsprosent,
        oppdatert = oppdatert,
    )

    private fun assertExpectedKomplettArbeidsforholdDto(actual: KomplettArbeidsforholdDto) {
        assertEquals(FØDSELSNUMMER, actual.fødselsnummer)
        assertEquals(ORGANISASJONSNUMMER, actual.organisasjonsnummer)
        assertEquals(STARTDATO, actual.startdato)
        assertEquals(SLUTTDATO, actual.sluttdato)
        assertEquals(STILLINGSTITTEL, actual.stillingstittel)
        assertEquals(STILLINGSPROSENT, actual.stillingsprosent)
        assertIFortiden(actual.oppdatert)
        assertMindreEnnNSekunderSiden(5, actual.oppdatert)
    }
}
