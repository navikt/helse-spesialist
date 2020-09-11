package no.nav.helse.modell.arbeidsgiver

import AbstractEndToEndTest
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class ArbeidsgiverDaoTest : AbstractEndToEndTest() {
    private companion object {
        private const val ORGNR = "123456789"
        private const val NAVN = "Bedrift AS"
    }

    @Test
    fun `opprette arbeidsgiver`() {
        arbeidsgiverDao.insertArbeidsgiver(ORGNR, NAVN)
        assertNotNull(arbeidsgiverDao.findArbeidsgiverByOrgnummer(ORGNR))
        assertEquals(LocalDate.now(), arbeidsgiverDao.findNavnSistOppdatert(ORGNR))
        assertEquals(1, arbeidsgiver().size)
        assertEquals(1, arbeidsgivernavn().size)
        assertEquals(ORGNR, arbeidsgiver().first().first)
        assertEquals(NAVN, arbeidsgivernavn().first().second)
    }

    @Test
    fun `oppdatere arbeidsgiver`() {
        arbeidsgiverDao.insertArbeidsgiver(ORGNR, NAVN)
        val nyttNavn = "Nærbutikken ASA"
        arbeidsgiverDao.updateNavn(ORGNR, nyttNavn)
        assertEquals(1, arbeidsgivernavn().size)
        assertEquals(nyttNavn, arbeidsgivernavn().first().second)
    }

    private fun arbeidsgiver() =
        using(sessionOf(dataSource)) {
            it.run(queryOf("SELECT orgnummer, navn_ref FROM arbeidsgiver").map {
                it.string("orgnummer") to it.int("navn_ref")
            }.asList)
        }
    private fun arbeidsgivernavn() =
        using(sessionOf(dataSource)) {
            it.run(queryOf("SELECT id, navn, navn_oppdatert FROM arbeidsgiver_navn").map {
                Triple(it.int("id"), it.string("navn"), it.localDate("navn_oppdatert"))
            }.asList)
        }
}
