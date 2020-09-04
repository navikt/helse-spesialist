package no.nav.helse.modell.arbeidsgiver

import AbstractEndToEndTest
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class ArbeidsgiverDaoTest : AbstractEndToEndTest() {
    private companion object {
        private const val ORGNR = "123456789"
        private const val NAVN = "Bedrift AS"
    }

    private lateinit var dao: ArbeidsgiverDao

    @BeforeEach
    fun setup() {
        dao = ArbeidsgiverDao(dataSource)
    }

    @Test
    fun `opprette arbeidsgiver`() {
        dao.insertArbeidsgiver(ORGNR, NAVN)
        assertNotNull(dao.findArbeidsgiverByOrgnummer(ORGNR))
        assertEquals(LocalDate.now(), dao.findNavnSistOppdatert(ORGNR))
        assertEquals(1, arbeidsgiver().size)
        assertEquals(1, arbeidsgivernavn().size)
        assertEquals(ORGNR, arbeidsgiver().first().first)
        assertEquals(NAVN, arbeidsgivernavn().first().second)
    }

    @Test
    fun `oppdatere arbeidsgiver`() {
        dao.insertArbeidsgiver(ORGNR, NAVN)
        val nyttNavn = "Nærbutikken ASA"
        dao.updateNavn(ORGNR, nyttNavn)
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
