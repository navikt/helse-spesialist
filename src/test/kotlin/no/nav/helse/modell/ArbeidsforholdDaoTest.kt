package no.nav.helse.modell

import DatabaseIntegrationTest
import no.nav.helse.april
import no.nav.helse.januar
import no.nav.helse.modell.arbeidsforhold.Arbeidsforholdløsning
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@Disabled
internal class ArbeidsforholdDaoTest : DatabaseIntegrationTest() {
    @BeforeEach
    fun setup() {
        resetDatabase()
        opprettPerson()
        opprettArbeidsgiver()
    }
    @Test
    fun `skriver ikke over alle arbeidsforhold med samme informasjon`() {
        val løsninger = Arbeidsforholdløsning(listOf(
            Arbeidsforholdløsning.Løsning(1.januar, null, "Nerd", 50),
            Arbeidsforholdløsning.Løsning(20.april, null, "Noe annet", 50)
        ))

        løsninger.opprett(arbeidsforholdDao, FNR, ORGNUMMER)
        val arbeidsforhold = arbeidsforholdDao.findArbeidsforhold(FNR, ORGNUMMER)

        løsninger.oppdater(arbeidsforholdDao, FNR, ORGNUMMER)
        assertEquals(arbeidsforhold, arbeidsforholdDao.findArbeidsforhold(FNR, ORGNUMMER))
    }

    @Test
    fun `fjerner arbeidsforhold som er borte fra aareg`() {
        Arbeidsforholdløsning(listOf(
            Arbeidsforholdløsning.Løsning(1.januar, null, "Nerd", 50),
            Arbeidsforholdløsning.Løsning(20.april, null, "Noe annet", 50)
        )).opprett(arbeidsforholdDao, FNR, ORGNUMMER)

        Arbeidsforholdløsning(listOf(
            Arbeidsforholdløsning.Løsning(1.januar, null, "Nerd", 50)
        )).oppdater(arbeidsforholdDao, FNR, ORGNUMMER)

        assertEquals(1, arbeidsforholdDao.findArbeidsforhold(FNR, ORGNUMMER).size)
    }
}
