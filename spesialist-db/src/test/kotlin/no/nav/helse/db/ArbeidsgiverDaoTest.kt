package no.nav.helse.db

import no.nav.helse.DatabaseIntegrationTest
import kotliquery.sessionOf
import no.nav.helse.modell.InntektskildetypeDto
import no.nav.helse.modell.KomplettInntektskildeDto
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.LocalDate


internal class ArbeidsgiverDaoTest : DatabaseIntegrationTest() {
    private val inntektskildeDao = InntektskilderDao(sessionOf(dataSource, returnGeneratedKey = true))

    @Test
    fun `Oppretter minimal arbeidsgiver`() {
        sessionOf(dataSource).use { session ->
            session.transaction { transaction ->
                val dao = ArbeidsgiverDao(transaction)
                dao.insertMinimalArbeidsgiver(ORGNUMMER)
                assertNotNull(dao.findArbeidsgiverByOrgnummer(ORGNUMMER))
            }
        }
    }

    @Test
    fun `kan hente bransjer`() {
        inntektskildeDao.lagreInntektskilder(listOf(
            KomplettInntektskildeDto(ORGNUMMER, InntektskildetypeDto.ORDINÆR, ORGNAVN, BRANSJER, LocalDate.now())
        ))
        Assertions.assertEquals(BRANSJER, arbeidsgiverApiDao.finnBransjer(ORGNUMMER))
    }

    @Test
    fun `kan hente blanke bransjer`() {
        inntektskildeDao.lagreInntektskilder(listOf(
            KomplettInntektskildeDto(ORGNUMMER, InntektskildetypeDto.ORDINÆR, ORGNAVN, listOf(""), LocalDate.now())
        ))
        Assertions.assertTrue(arbeidsgiverApiDao.finnBransjer(ORGNUMMER).isEmpty())
    }

    @Test
    fun `kan hente tomme bransjer`() {
        inntektskildeDao.lagreInntektskilder(listOf(
            KomplettInntektskildeDto(ORGNUMMER, InntektskildetypeDto.ORDINÆR, ORGNAVN, emptyList(), LocalDate.now())
        ))
        Assertions.assertTrue(arbeidsgiverApiDao.finnBransjer(ORGNUMMER).isEmpty())
    }

    @Test
    fun `kan hente navn`() {
        inntektskildeDao.lagreInntektskilder(listOf(
            KomplettInntektskildeDto(ORGNUMMER, InntektskildetypeDto.ORDINÆR, ORGNAVN, BRANSJER, LocalDate.now())
        ))
        Assertions.assertEquals(ORGNAVN, arbeidsgiverApiDao.finnNavn(ORGNUMMER))
    }
}