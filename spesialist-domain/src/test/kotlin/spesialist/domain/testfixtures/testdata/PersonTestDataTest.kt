package no.nav.helse.spesialist.domain.testfixtures.testdata

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PersonTestDataTest {
    @Test
    fun `alle genererte fødselsnummer er 11 numeriske tegn`() {
        (1..100).map { lagFødselsnummer() }
            .onEach { assertEquals(11, it.length, "Uventet verdi: $it") }
            .onEach { it.toLong() }
    }

    @Test
    fun `alle genererte D-nummer er 11 numeriske tegn`() {
        (1..100).map { lagDNummer() }
            .onEach { assertEquals(11, it.length, "Uventet verdi: $it") }
            .onEach { it.toLong() }
    }

    @Test
    fun `fødselsnummer starter med syntetisert fødselsdato`() {
        assertEquals("038271", lagFødselsnummer(fødselsdato = LocalDate.parse("1971-02-03"), mann = false).take(6))
    }

    @Test
    fun `tredje siffer i individnummer i fødselsnummer er et partall for kvinner`() {
        (1..100).map { lagFødselsnummer(fødselsdato = lagFødselsdato(), mann = false) }
            .forEach {
                assertTrue(it[8].digitToInt() in setOf(0, 2, 4, 6, 8), "Uventet verdi: $it")
            }
    }

    @Test
    fun `tredje siffer i individnummer i fødselsnummer er et oddetall for menn`() {
        (1..100).map { lagFødselsnummer(fødselsdato = lagFødselsdato(), mann = true) }
            .forEach {
                assertTrue(it[8].digitToInt() in setOf(1, 3, 5, 7, 9), "Uventet verdi: $it")
            }
    }

    @Test
    fun `D-nummer starter med syntetisert fødselsdato`() {
        assertEquals("438271", lagDNummer(fødselsdato = LocalDate.parse("1971-02-03"), mann = false).take(6))
    }

    @Test
    fun `tredje siffer i individnummer i D-nummer er et partall for kvinner`() {
        (1..100).map { lagDNummer(fødselsdato = lagFødselsdato(), mann = false) }
            .forEach {
                assertTrue(it[8].digitToInt() in setOf(0, 2, 4, 6, 8), "Uventet verdi: $it")
            }
    }

    @Test
    fun `tredje siffer i individnummer i D-nummer er et oddetall for menn`() {
        (1..100).map { lagDNummer(fødselsdato = lagFødselsdato(), mann = true) }
            .forEach {
                assertTrue(it[8].digitToInt() in setOf(1, 3, 5, 7, 9), "Uventet verdi: $it")
            }
    }
}
