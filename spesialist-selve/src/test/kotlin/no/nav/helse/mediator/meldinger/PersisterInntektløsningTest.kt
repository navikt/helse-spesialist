package no.nav.helse.mediator.meldinger

import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.time.YearMonth
import no.nav.helse.mediator.meldinger.løsninger.Inntekter
import no.nav.helse.mediator.meldinger.løsninger.Inntektløsning
import no.nav.helse.modell.person.PersonDao
import org.junit.jupiter.api.Test

internal class PersisterInntektløsningTest {
    private companion object {
        private const val FØDSELSNUMMER = "12345678910"
        private val SKJÆRINGSTIDSPUNKT = LocalDate.of(2022,9, 1)
        private val INNTEKTER: List<Inntekter> = listOf(
            Inntekter(
                årMåned = YearMonth.parse("2022-08"),
                inntektsliste = listOf(
                    Inntekter.Inntekt(
                        beløp = 20000.0,
                        orgnummer = "123456789"
                    )
                )

            )
        )
    }

    private val personDao = mockk<PersonDao>(relaxed = true)

    @Test
    fun `lagre inntekt`() {
        val inntektløsning = Inntektløsning(INNTEKTER)
        inntektløsning.lagre(personDao, FØDSELSNUMMER, SKJÆRINGSTIDSPUNKT)
        verify(exactly = 1) { personDao.lagreInntekter(FØDSELSNUMMER, SKJÆRINGSTIDSPUNKT, INNTEKTER) }
    }
}
