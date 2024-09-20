package no.nav.helse.modell.kommando

import no.nav.helse.db.ArbeidsforholdRepository
import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.modell.KomplettArbeidsforholdDto
import no.nav.helse.modell.arbeidsforhold.Arbeidsforholdløsning
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
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

    private val repository = object : ArbeidsforholdRepository {
        val arbeidsforholdSomHarBlittOppdatert = mutableListOf<KomplettArbeidsforholdDto>()
        val eksisterendeArbeidsforhold = mutableListOf<KomplettArbeidsforholdDto>()

        override fun findArbeidsforhold(fødselsnummer: String, organisasjonsnummer: String):
                List<KomplettArbeidsforholdDto> {
            return eksisterendeArbeidsforhold.filter {
                it.fødselsnummer.equals(fødselsnummer) &&
                it.organisasjonsnummer.equals(organisasjonsnummer)
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
        arbeidsforholdRepository = repository
    )

    private val observer = object : CommandContextObserver {
        val behov = mutableListOf<String>()

        override fun behov(behov: String, ekstraKontekst: Map<String, Any>, detaljer: Map<String, Any>) {
            this.behov.add(behov)
        }

        override fun hendelse(hendelse: String) {}
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
        assertEquals(enKomplettArbeidsforholdDto(), repository.arbeidsforholdSomHarBlittOppdatert.first())
    }

    @Test
    fun `oppdaterer arbeidsforhold`() {
        arbeidsforholdFinnes(enKomplettArbeidsforholdDto(oppdatert = LocalDate.now().minusYears(1)))
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
        assertEquals(enKomplettArbeidsforholdDto(), repository.arbeidsforholdSomHarBlittOppdatert.first())
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
        oppdatert: LocalDate = LocalDate.now()
    ) = KomplettArbeidsforholdDto(
        fødselsnummer = fødselsnummer,
        organisasjonsnummer = organisasjonsnummer,
        startdato = startdato,
        sluttdato = sluttdato,
        stillingstittel = stillingstittel,
        stillingsprosent = stillingsprosent,
        oppdatert = oppdatert
    )
}