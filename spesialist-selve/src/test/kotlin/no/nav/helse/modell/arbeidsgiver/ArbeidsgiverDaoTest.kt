package no.nav.helse.modell.arbeidsgiver

import DatabaseIntegrationTest
import no.nav.helse.db.InntektskilderDao
import no.nav.helse.modell.InntektskildetypeDto.ORDINÆR
import no.nav.helse.modell.KomplettInntektskildeDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class ArbeidsgiverDaoTest : DatabaseIntegrationTest() {
    private val inntektskildeDao = InntektskilderDao(dataSource)

    @Test
    fun `kan hente bransjer`() {
        inntektskildeDao.lagreInntektskilder(listOf(
            KomplettInntektskildeDto(ORGNUMMER, ORDINÆR, ORGNAVN, BRANSJER, LocalDate.now())
        ))
        assertEquals(BRANSJER, arbeidsgiverApiDao.finnBransjer(ORGNUMMER))
    }

    @Test
    fun `kan hente blanke bransjer`() {
        inntektskildeDao.lagreInntektskilder(listOf(
            KomplettInntektskildeDto(ORGNUMMER, ORDINÆR, ORGNAVN, listOf(""), LocalDate.now())
        ))
        assertTrue(arbeidsgiverApiDao.finnBransjer(ORGNUMMER).isEmpty())
    }

    @Test
    fun `kan hente tomme bransjer`() {
        inntektskildeDao.lagreInntektskilder(listOf(
            KomplettInntektskildeDto(ORGNUMMER, ORDINÆR, ORGNAVN, emptyList(), LocalDate.now())
        ))
        assertTrue(arbeidsgiverApiDao.finnBransjer(ORGNUMMER).isEmpty())
    }

    @Test
    fun `kan hente navn`() {
        inntektskildeDao.lagreInntektskilder(listOf(
            KomplettInntektskildeDto(ORGNUMMER, ORDINÆR, ORGNAVN, BRANSJER, LocalDate.now())
        ))
        assertEquals(ORGNAVN, arbeidsgiverApiDao.finnNavn(ORGNUMMER))
    }
}
