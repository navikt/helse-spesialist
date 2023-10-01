package no.nav.helse.modell.arbeidsforhold

import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import no.nav.helse.Testdata.VEDTAKSPERIODE_ID
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.mediator.meldinger.Testmeldingfabrikk
import no.nav.helse.mediator.meldinger.løsninger.ArbeidsforholdRiver
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
    private val mediator = mockk<HendelseMediator>(relaxed = true)
    private val rapid = TestRapid()

    init {
        ArbeidsforholdRiver(rapid, mediator)
    }

    @BeforeEach
    internal fun resetTestSetup() {
        rapid.reset()
    }

    @Test
    fun `mottar arbeidsgiverløsning`() {
        rapid.sendTestMessage(
            Testmeldingfabrikk.lagArbeidsforholdløsning(
                aktørId = AKTØRID,
                fødselsnummer = FØDSELSNUMMER,
                organisasjonsnummer = ORGNR,
                løsning = listOf(
                    Arbeidsforholdløsning.Løsning(
                        stillingstittel = STILLINGSTITTEL,
                        stillingsprosent = STILLINGSPROSENT,
                        startdato = STARTDATO,
                        sluttdato = SLUTTDATO
                    )
                ),
                vedtaksperiodeId = VEDTAKSPERIODE_ID
            )
        )
        verify(exactly = 1) { mediator.løsning(any(), any(), any(), any<Arbeidsforholdløsning>(), any()) }
    }

    @Test
    fun `mottar tom arbeidsgiverløsning`() {
        rapid.sendTestMessage(
            Testmeldingfabrikk.lagArbeidsforholdløsning(
                aktørId = AKTØRID,
                fødselsnummer = FØDSELSNUMMER,
                organisasjonsnummer = ORGNR,
                løsning = listOf(),
                vedtaksperiodeId = VEDTAKSPERIODE_ID
            )
        )

        verify(exactly = 1) { mediator.løsning(any(), any(), any(), any<Arbeidsforholdløsning>(), any()) }
    }

    @Test
    fun `oppretter løsning`() {
        val arbeidsforhold = Arbeidsforholdløsning(
            listOf(
                Arbeidsforholdløsning.Løsning(
                    STARTDATO,
                    SLUTTDATO,
                    STILLINGSTITTEL,
                    STILLINGSPROSENT
                )
            )
        )
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
        val arbeidsforhold = listOf(
            Arbeidsforholdløsning.Løsning(
                STARTDATO,
                SLUTTDATO,
                STILLINGSTITTEL,
                STILLINGSPROSENT
            )
        )
        val arbeidsforholdløsning = Arbeidsforholdløsning(arbeidsforhold)
        arbeidsforholdløsning.oppdater(dao, FØDSELSNUMMER, ORGNR)
        verify(exactly = 1) {
            dao.oppdaterArbeidsforhold(
                fødselsnummer = FØDSELSNUMMER,
                organisasjonsnummer = ORGNR,
                arbeidsforhold = arbeidsforhold
            )
        }
    }
}
