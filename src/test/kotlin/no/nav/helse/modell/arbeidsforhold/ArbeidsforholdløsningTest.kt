package no.nav.helse.modell.arbeidsforhold

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.IHendelseMediator
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class ArbeidsforholdløsningTest {
    private companion object {
        private const val ORGNR = "123456789"
        private const val FØDSELSNUMMER = "12345678910"
        private const val AKTØRID = "123456789"
        private const val STILLINGSTITTEL = "Stillingstittel1"
        private const val STILLINGSPROSENT = 100
        private val STARTDATO: LocalDate = LocalDate.now()
        private val SLUTTDATO = null
    }

    private val dao = mockk<ArbeidsforholdDao>(relaxed = true)
    private val mediator = mockk<IHendelseMediator>(relaxed = true)
    private val rapid = TestRapid()
    private val meldingsfabrikk = Testmeldingfabrikk(FØDSELSNUMMER, AKTØRID)

    init {
        Arbeidsforholdløsning.ArbeidsforholdRiver(rapid, mediator)
    }

    @BeforeEach
    internal fun resetTestSetup() {
        rapid.reset()
    }

    @Test
    fun `mottar arbeidsgiverløsning`() {
        rapid.sendTestMessage(
            meldingsfabrikk.lagArbeidsforholdløsning(
                stillingstittel = STILLINGSTITTEL,
                stillingsprosent = STILLINGSPROSENT,
                startdato = STARTDATO,
                sluttdato = SLUTTDATO
            )
        )
        verify(exactly = 1) { mediator.løsning(any(), any(), any(), any<Arbeidsforholdløsning>(), any()) }
    }

    @Test
    fun `oppretter løsning`() {
        val arbeidsforhold = Arbeidsforholdløsning(STARTDATO, SLUTTDATO, STILLINGSTITTEL, STILLINGSPROSENT)
        arbeidsforhold.opprett(dao, FØDSELSNUMMER, ORGNR)
        verify(exactly = 1) {
            dao.insertArbeidsforhold(
                fødselsnummer = FØDSELSNUMMER,
                organisasjonsnummer = ORGNR,
                startdato = STARTDATO,
                sluttdato = SLUTTDATO,
                stillingstittel = STILLINGSTITTEL,
                stillingsprosent = STILLINGSPROSENT
            )
        }
    }

    @Test
    fun `oppdaterer løsning`() {
        val arbeidsforhold = Arbeidsforholdløsning(STARTDATO, SLUTTDATO, STILLINGSTITTEL, STILLINGSPROSENT)
        arbeidsforhold.oppdater(dao, FØDSELSNUMMER, ORGNR)
        verify(exactly = 1) {
            dao.oppdaterArbeidsforhold(
                fødselsnummer = FØDSELSNUMMER,
                organisasjonsnummer = ORGNR,
                startdato = STARTDATO,
                sluttdato = SLUTTDATO,
                stillingstittel = STILLINGSTITTEL,
                stillingsprosent = STILLINGSPROSENT
            )
        }
    }
}
