package no.nav.helse.modell

import DatabaseIntegrationTest
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.modell.utbetaling.UtbetalingDao
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals

class UtbetalingDaoTest : DatabaseIntegrationTest() {

    @Test
    fun `hent utbetalinger for en person`() {
        nyPerson()
        val fagsystemId = fagsystemId()

        val arbeidsgiverOppdragId = lagOppdrag(fagsystemId)

        lagLinje(arbeidsgiverOppdragId, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31))
        val utbetalingIdId = lagUtbetalingId(arbeidsgiverOppdragId)
        utbetalingDao.nyUtbetalingStatus(utbetalingIdId, "GODKJENT", LocalDateTime.now(), "{}")
        utbetalingDao.nyUtbetalingStatus(utbetalingIdId, "SENDT", LocalDateTime.now(), "{}")
        utbetalingDao.nyUtbetalingStatus(utbetalingIdId, "OVERFØRT", LocalDateTime.now(), "{}")
        utbetalingDao.nyUtbetalingStatus(utbetalingIdId, "UTBETALT", LocalDateTime.now(), "{}")

        val utbetalinger = utbetalingDao.findUtbetalinger(FNR)
        assertEquals(1, utbetalinger.size)
        val utbetaling = utbetalinger.first()

        assertEquals("UTBETALT", utbetaling.status)
        assertEquals("UTBETALING", utbetaling.type)
        assertEquals(fagsystemId, utbetaling.arbeidsgiverOppdrag.fagsystemId)
        assertEquals(ORGNUMMER, utbetaling.arbeidsgiverOppdrag.organisasjonsnummer)
        assertEquals(1, utbetaling.arbeidsgiverOppdrag.linjer.size)
    }

    @Test
    fun `henter riktige linjer for person`() {
        nyPerson()
        val oppdrag1 = lagOppdrag()
        lagLinje(oppdrag1, 1.januar(), 31.januar())
        lagLinje(oppdrag1, 1.februar(), 28.februar())

        val arbeidsgiverOppdragId = lagOppdrag()
        lagLinje(arbeidsgiverOppdragId, 1.juli(), 31.juli())
        val utbetalingIdId = lagUtbetalingId(arbeidsgiverOppdragId)
        utbetalingDao.nyUtbetalingStatus(utbetalingIdId, "UTBETALT", LocalDateTime.now(), "{}")

        val utbetaling = utbetalingDao.findUtbetalinger(FNR).first()

        assertEquals(listOf(UtbetalingDao.UtbetalingDto.OppdragDto.UtbetalingLinje(
            fom = 1.juli(),
            tom = 31.juli(),
            totalbeløp = null
        )), utbetaling.arbeidsgiverOppdrag.linjer)
    }

    private fun lagOppdrag(fagsystemId: String = fagsystemId()) =
        utbetalingDao.nyttOppdrag(fagsystemId, ORGNUMMER, "SPREF", "NY", LocalDate.now().plusDays(169))!!

    private fun lagUtbetalingId(arbeidsgiverOppdragId: Long): Long {
        val personOppdragId = utbetalingDao.nyttOppdrag(fagsystemId(), FNR, "SPREF", "NY", LocalDate.now().plusDays(169))!!
        val utbetalingIdId = utbetalingDao.opprettUtbetalingId(
            utbetalingId = UUID.randomUUID(),
            fødselsnummer = FNR,
            orgnummer = ORGNUMMER,
            type = "UTBETALING",
            opprettet = LocalDateTime.now(),
            arbeidsgiverFagsystemIdRef = arbeidsgiverOppdragId,
            personFagsystemIdRef = personOppdragId
        )
        return utbetalingIdId
    }

    private fun lagLinje(oppdrag: Long, fom: LocalDate, tom: LocalDate) {
        utbetalingDao.nyLinje(
            oppdragId = oppdrag,
            endringskode = "NY",
            klassekode = "SPREFAG-IOP",
            statuskode = null,
            datoStatusFom = null,
            fom = fom,
            tom = tom,
            dagsats = 1200,
            totalbeløp = null,
            lønn = 3000,
            grad = 100.0,
            delytelseId = 1,
            refDelytelseId = null,
            refFagsystemId = null
        )
    }

    fun fagsystemId() = (0..31).map { 'A' + Random().nextInt('Z' - 'A') }.joinToString("")
}
