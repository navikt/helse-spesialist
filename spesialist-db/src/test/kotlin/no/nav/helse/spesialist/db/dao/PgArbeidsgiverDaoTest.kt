package no.nav.helse.spesialist.db.dao

import kotliquery.sessionOf
import no.nav.helse.db.PgArbeidsgiverDao
import no.nav.helse.modell.InntektskildetypeDto
import no.nav.helse.modell.KomplettInntektskildeDto
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.LocalDate


internal class PgArbeidsgiverDaoTest : AbstractDBIntegrationTest() {

    @Test
    fun `Oppretter minimal arbeidsgiver`() {
        sessionOf(dataSource).use { session ->
            session.transaction { transaction ->
                val dao = PgArbeidsgiverDao(transaction)
                dao.insertMinimalArbeidsgiver(ORGNUMMER)
                assertNotNull(dao.findArbeidsgiverByOrgnummer(ORGNUMMER))
            }
        }
    }

    @Test
    fun `kan hente bransjer`() {
        inntektskilderRepository.lagreInntektskilder(listOf(
            KomplettInntektskildeDto(ORGNUMMER, InntektskildetypeDto.ORDINÆR, ORGNAVN, BRANSJER, LocalDate.now())
        ))
        Assertions.assertEquals(BRANSJER, arbeidsgiverApiDao.finnBransjer(ORGNUMMER))
    }

    @Test
    fun `kan hente blanke bransjer`() {
        inntektskilderRepository.lagreInntektskilder(listOf(
            KomplettInntektskildeDto(ORGNUMMER, InntektskildetypeDto.ORDINÆR, ORGNAVN, listOf(""), LocalDate.now())
        ))
        Assertions.assertTrue(arbeidsgiverApiDao.finnBransjer(ORGNUMMER).isEmpty())
    }

    @Test
    fun `kan hente tomme bransjer`() {
        inntektskilderRepository.lagreInntektskilder(listOf(
            KomplettInntektskildeDto(ORGNUMMER, InntektskildetypeDto.ORDINÆR, ORGNAVN, emptyList(), LocalDate.now())
        ))
        Assertions.assertTrue(arbeidsgiverApiDao.finnBransjer(ORGNUMMER).isEmpty())
    }

    @Test
    fun `kan hente navn`() {
        inntektskilderRepository.lagreInntektskilder(listOf(
            KomplettInntektskildeDto(ORGNUMMER, InntektskildetypeDto.ORDINÆR, ORGNAVN, BRANSJER, LocalDate.now())
        ))
        Assertions.assertEquals(ORGNAVN, arbeidsgiverApiDao.finnNavn(ORGNUMMER))
    }
}
