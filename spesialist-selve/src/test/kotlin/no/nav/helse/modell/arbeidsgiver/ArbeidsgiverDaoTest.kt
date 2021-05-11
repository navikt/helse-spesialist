package no.nav.helse.modell.arbeidsgiver

import DatabaseIntegrationTest
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class ArbeidsgiverDaoTest : DatabaseIntegrationTest() {

    @Test
    fun `opprette arbeidsgiver`() {
        arbeidsgiverDao.insertArbeidsgiver(ORGNUMMER, ORGNAVN, BRANSJER)
        assertNotNull(arbeidsgiverDao.findArbeidsgiverByOrgnummer(ORGNUMMER))
        assertEquals(LocalDate.now(), arbeidsgiverDao.findNavnSistOppdatert(ORGNUMMER))
        assertEquals(ORGNAVN, arbeidsgiverDao.finnNavn(ORGNUMMER))
        assertEquals(BRANSJER, arbeidsgiverDao.finnBransjer(ORGNUMMER))
    }

    @Test
    fun `oppdatere arbeidsgivernavn`() {
        arbeidsgiverDao.insertArbeidsgiver(ORGNUMMER, ORGNAVN, BRANSJER)
        val nyttNavn = "Nærbutikken ASA"
        arbeidsgiverDao.updateNavn(ORGNUMMER, nyttNavn)
        assertEquals(nyttNavn, arbeidsgiverDao.finnNavn(ORGNUMMER))
    }

    @Test
    fun `oppdatere bransjer`() {
        arbeidsgiverDao.insertArbeidsgiver(ORGNUMMER, ORGNAVN, BRANSJER)
        val nyBransje = listOf("Ny bransje")
        arbeidsgiverDao.updateBransjer(ORGNUMMER, nyBransje)
        assertEquals(nyBransje, arbeidsgiverDao.finnBransjer(ORGNUMMER))
    }

    @Test
    fun `kan hente bransjer`() {
        assertNotNull(arbeidsgiverDao.insertArbeidsgiver(ORGNUMMER, ORGNAVN, BRANSJER))
        assertEquals(BRANSJER, arbeidsgiverDao.finnBransjer(ORGNUMMER))
    }

    @Test
    fun `kan hente blanke bransjer`() {
        assertNotNull(arbeidsgiverDao.insertArbeidsgiver(ORGNUMMER, ORGNAVN, listOf("")))
        assertTrue(arbeidsgiverDao.finnBransjer(ORGNUMMER).isEmpty())
    }

    @Test
    fun `kan hente tomme bransjer`() {
        assertNotNull(arbeidsgiverDao.insertArbeidsgiver(ORGNUMMER, ORGNAVN, emptyList()))
        assertTrue(arbeidsgiverDao.finnBransjer(ORGNUMMER).isEmpty())
    }

    @Test
    fun `kan hente navn`() {
        assertNotNull(arbeidsgiverDao.insertArbeidsgiver(ORGNUMMER, ORGNAVN, listOf("")))
        assertEquals(ORGNAVN, arbeidsgiverDao.finnNavn(ORGNUMMER))
    }
}
