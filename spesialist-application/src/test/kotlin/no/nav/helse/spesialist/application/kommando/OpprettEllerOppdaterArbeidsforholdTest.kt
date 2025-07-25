package no.nav.helse.spesialist.application.kommando

import no.nav.helse.db.ArbeidsforholdDao
import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.modell.KomplettArbeidsforholdDto
import no.nav.helse.modell.arbeidsforhold.Arbeidsforholdløsning
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.OpprettEllerOppdaterArbeidsforhold
import no.nav.helse.modell.melding.Behov
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class OpprettEllerOppdaterArbeidsforholdTest {
    private companion object {
        const val FØDSELSNUMMER = "12345678910"
        const val ORGANISASJONSNUMMER = "987654321"
        const val STILLINGSPROSENT = 100
        const val STILLINGSTITTEL = "Slabberasansvarlig"
        val STARTDATO: LocalDate = LocalDate.now()
        val SLUTTDATO = null
    }

    private val repository = object : ArbeidsforholdDao {
        val arbeidsforholdSomHarBlittOppdatert = mutableListOf<KomplettArbeidsforholdDto>()
        val eksisterendeArbeidsforhold = mutableListOf<KomplettArbeidsforholdDto>()

        override fun findArbeidsforhold(fødselsnummer: String, arbeidsgiverIdentifikator: String):
                List<KomplettArbeidsforholdDto> {
            return eksisterendeArbeidsforhold.filter {
                it.fødselsnummer.equals(fødselsnummer) &&
                        it.organisasjonsnummer.equals(arbeidsgiverIdentifikator)
            }
        }

        override fun upsertArbeidsforhold(
            fødselsnummer: String,
            organisasjonsnummer: String,
            arbeidsforhold: List<KomplettArbeidsforholdDto>
        ) {
            arbeidsforholdSomHarBlittOppdatert.addAll(arbeidsforhold)
        }
    }

    private lateinit var context: CommandContext

    private fun enCommand() = OpprettEllerOppdaterArbeidsforhold(
        fødselsnummer = FØDSELSNUMMER,
        organisasjonsnummer = ORGANISASJONSNUMMER,
        arbeidsforholdDao = repository
    )

    private val observer = object : CommandContextObserver {
        val behov = mutableListOf<Behov>()

        override fun behov(behov: Behov, commandContextId: UUID) {
            this.behov.add(behov)
        }
    }

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        context.nyObserver(observer)
    }

    @Test
    fun `oppretter arbeidsforhold`() {
        arbeidsforholdFinnesIkke()
        val command = enCommand()
        assertFalse(command.execute(context))
        assertTrue(observer.behov.isNotEmpty())
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

        assertEquals(1, repository.arbeidsforholdSomHarBlittOppdatert.size)
        assertExpectedKomplettArbeidsforholdDto(repository.arbeidsforholdSomHarBlittOppdatert.single())
    }

    @Test
    fun `oppdaterer arbeidsforhold`() {
        arbeidsforholdFinnes(enKomplettArbeidsforholdDto(oppdatert = LocalDateTime.now().minusYears(1)))
        val command = enCommand()
        assertFalse(command.execute(context))
        assertTrue(observer.behov.isNotEmpty())
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

        assertEquals(1, repository.arbeidsforholdSomHarBlittOppdatert.size)
        assertExpectedKomplettArbeidsforholdDto(repository.arbeidsforholdSomHarBlittOppdatert.single())
    }

    @Test
    fun `oppretter ikke arbeidsforhold når den finnes`() {
        arbeidsforholdFinnes(enKomplettArbeidsforholdDto())
        assertTrue(enCommand().execute(context))
        assertEquals(0, repository.arbeidsforholdSomHarBlittOppdatert.size)
    }

    @Test
    fun `oppdaterer ikke arbeidsforhold når den er oppdatert`() {
        arbeidsforholdFinnes(enKomplettArbeidsforholdDto())
        assertTrue(enCommand().execute(context))
        assertEquals(0, repository.arbeidsforholdSomHarBlittOppdatert.size)
    }

    @Test
    fun `oppretter arbeidsforhold når det ikke finnes for orgnummeret`() {
        arbeidsforholdFinnes(enKomplettArbeidsforholdDto(organisasjonsnummer = ORGANISASJONSNUMMER.reversed()))
        val command = enCommand()
        assertFalse(command.execute(context))
        assertTrue(observer.behov.isNotEmpty())
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
        assertEquals(1, repository.arbeidsforholdSomHarBlittOppdatert.size)
    }

    private fun arbeidsforholdFinnes(komplettArbeidsforholdDto: KomplettArbeidsforholdDto) {
        repository.eksisterendeArbeidsforhold.add(komplettArbeidsforholdDto)
    }

    private fun arbeidsforholdFinnesIkke() {
        repository.eksisterendeArbeidsforhold.clear()
    }

    private fun enKomplettArbeidsforholdDto(
        fødselsnummer: String = FØDSELSNUMMER,
        organisasjonsnummer: String = ORGANISASJONSNUMMER,
        startdato: LocalDate = STARTDATO,
        sluttdato: LocalDate? = SLUTTDATO,
        stillingstittel: String = STILLINGSTITTEL,
        stillingsprosent: Int = STILLINGSPROSENT,
        oppdatert: LocalDateTime = LocalDateTime.now()
    ) = KomplettArbeidsforholdDto(
        fødselsnummer = fødselsnummer,
        organisasjonsnummer = organisasjonsnummer,
        startdato = startdato,
        sluttdato = sluttdato,
        stillingstittel = stillingstittel,
        stillingsprosent = stillingsprosent,
        oppdatert = oppdatert
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

    private fun assertIFortiden(actual: LocalDateTime) {
        val now = LocalDateTime.now()
        assertTrue(actual.isBefore(now)) {
            "Forventet at tidspunktet var i fortiden (i forhold til nå: $now), men det var $actual"
        }
    }

    private fun assertMindreEnnNSekunderSiden(@Suppress("SameParameterValue") sekunder: Int, actual: LocalDateTime) {
        val now = LocalDateTime.now()
        assertTrue(actual.isAfter(now.minusSeconds(sekunder.toLong()))) {
            "Forventet at tidspunktet var innenfor $sekunder sekunder tilbake i tid (i forhold til nå: $now), men det var $actual"
        }
    }
}
