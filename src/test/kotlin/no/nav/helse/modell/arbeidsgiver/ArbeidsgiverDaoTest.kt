package no.nav.helse.modell.arbeidsgiver

import DatabaseIntegrationTest
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class ArbeidsgiverDaoTest : DatabaseIntegrationTest() {
    private companion object {
        private const val ORGNR = "123456789"
        private const val NAVN = "Bedrift AS"
        private const val BRANSJER = "BEDRIFTSGREIER OG STÆSJ"
    }

    @Test
    fun `opprette arbeidsgiver`() {
        arbeidsgiverDao.insertArbeidsgiver(ORGNR, NAVN, BRANSJER)
        assertNotNull(arbeidsgiverDao.findArbeidsgiverByOrgnummer(ORGNR))
        assertEquals(LocalDate.now(), arbeidsgiverDao.findNavnSistOppdatert(ORGNR))
        assertEquals(1, arbeidsgiver().size)
        assertEquals(1, arbeidsgivernavn().size)
        assertEquals(1, bransjer().size)
        assertEquals(ORGNR, arbeidsgiver().first().first)
        assertEquals(NAVN, arbeidsgivernavn().first().second)
        assertEquals(BRANSJER, bransjer().first().second)
    }

    @Test
    fun `oppdatere arbeidsgivernavn`() {
        arbeidsgiverDao.insertArbeidsgiver(ORGNR, NAVN, BRANSJER)
        val nyttNavn = "Nærbutikken ASA"
        arbeidsgiverDao.updateNavn(ORGNR, nyttNavn)
        assertEquals(1, arbeidsgivernavn().size)
        assertEquals(nyttNavn, arbeidsgivernavn().first().second)
    }

    @Test
    fun `oppdatere bransjer`() {
        arbeidsgiverDao.insertArbeidsgiver(ORGNR, NAVN, BRANSJER)
        val nyBransje = "Ny bransje"
        arbeidsgiverDao.updateBransjer(ORGNR, nyBransje)
        assertEquals(1, bransjer().size)
        assertEquals(nyBransje, bransjer().first().second)
    }

    private fun arbeidsgiver() =
        using(sessionOf(dataSource)) { session ->
            session.run(queryOf("SELECT orgnummer, navn_ref FROM arbeidsgiver").map {
                it.string("orgnummer") to it.int("navn_ref")
            }.asList)
        }

    private fun arbeidsgivernavn() =
        using(sessionOf(dataSource)) { session ->
            session.run(queryOf("SELECT id, navn, navn_oppdatert FROM arbeidsgiver_navn").map {
                Triple(it.int("id"), it.string("navn"), it.localDate("navn_oppdatert"))
            }.asList)
        }

    private fun bransjer() =
        using(sessionOf(dataSource)) { session ->
            session.run(queryOf("SELECT id, bransjer, oppdatert FROM arbeidsgiver_bransjer").map {
                Triple(it.long("id"), it.string("bransjer"), it.localDate("oppdatert"))
            }.asList)
        }
}
