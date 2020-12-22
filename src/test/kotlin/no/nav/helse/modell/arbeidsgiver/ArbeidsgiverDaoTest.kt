package no.nav.helse.modell.arbeidsgiver

import DatabaseIntegrationTest
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class ArbeidsgiverDaoTest : DatabaseIntegrationTest() {

    @Test
    fun `opprette arbeidsgiver`() {
        arbeidsgiverDao.insertArbeidsgiver(ORGNUMMER, ORGNAVN, BRANSJER)
        assertNotNull(arbeidsgiverDao.findArbeidsgiverByOrgnummer(ORGNUMMER))
        assertEquals(LocalDate.now(), arbeidsgiverDao.findNavnSistOppdatert(ORGNUMMER))
        assertEquals(1, arbeidsgiver().size)
        assertEquals(1, arbeidsgivernavn().size)
        assertEquals(1, bransjer().size)
        assertEquals(ORGNUMMER, arbeidsgiver().first().first)
        assertEquals(ORGNAVN, arbeidsgivernavn().first().second)
        assertEquals(objectMapper.writeValueAsString(BRANSJER), bransjer().first().second)
    }

    @Test
    fun `oppdatere arbeidsgivernavn`() {
        arbeidsgiverDao.insertArbeidsgiver(ORGNUMMER, ORGNAVN, BRANSJER)
        val nyttNavn = "Nærbutikken ASA"
        arbeidsgiverDao.updateNavn(ORGNUMMER, nyttNavn)
        assertEquals(1, arbeidsgivernavn().size)
        assertEquals(nyttNavn, arbeidsgivernavn().first().second)
    }

    @Test
    fun `oppdatere bransjer`() {
        arbeidsgiverDao.insertArbeidsgiver(ORGNUMMER, ORGNAVN, BRANSJER)
        val nyBransje = listOf("Ny bransje")
        arbeidsgiverDao.updateBransjer(ORGNUMMER, nyBransje)
        assertEquals(1, bransjer().size)
        assertEquals(objectMapper.writeValueAsString(nyBransje), bransjer().first().second)
    }

    @Test
    fun `insert bransjer`() {
        val arbeidsgiverRef = requireNotNull(arbeidsgiverDao.insertArbeidsgiver(ORGNUMMER, ORGNAVN, BRANSJER))
        fjernBransjerRef(arbeidsgiverRef)
        val nyBransje = listOf("Ny bransje")
        arbeidsgiverDao.insertBransjer(ORGNUMMER, nyBransje)
        assertEquals(2, bransjer().size)
        assertEquals(objectMapper.writeValueAsString(nyBransje), bransjer()[1].second)
    }

    @Test
    fun `kan hente arbeidsgivere uten bransje`() {
        val arbeidsgiverRef = requireNotNull(arbeidsgiverDao.insertArbeidsgiver(ORGNUMMER, ORGNAVN, BRANSJER))
        fjernBransjerRef(arbeidsgiverRef)
        assertNotNull(arbeidsgiverDao.findArbeidsgiver(arbeidsgiverRef))
    }

    @Test
    fun `kan hente arbeidsgivere med blank bransje`() {
        val arbeidsgiverRef = requireNotNull(arbeidsgiverDao.insertArbeidsgiver(ORGNUMMER, ORGNAVN, listOf("")))
        assertNotNull(arbeidsgiverDao.findArbeidsgiver(arbeidsgiverRef))
    }

    @Test
    fun `kan hente arbeidsgivere`() {
        val arbeidsgiverRef = requireNotNull(arbeidsgiverDao.insertArbeidsgiver(ORGNUMMER, ORGNAVN, BRANSJER))
        val arbeidsgiver = arbeidsgiverDao.findArbeidsgiver(arbeidsgiverRef)
        assertNotNull(arbeidsgiver)
        assertEquals(ORGNUMMER, arbeidsgiver?.organisasjonsnummer)
        assertEquals(ORGNAVN, arbeidsgiver?.navn)
        assertEquals(BRANSJER, arbeidsgiver?.bransjer)
    }

    private fun fjernBransjerRef(arbeidsgiverRef: Long) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val statement = "UPDATE arbeidsgiver SET bransjer_ref=NULL WHERE id=?"
        session.run(queryOf(statement, arbeidsgiverRef).asUpdate)
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
