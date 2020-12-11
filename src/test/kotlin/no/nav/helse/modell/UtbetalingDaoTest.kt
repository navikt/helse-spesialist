package no.nav.helse.modell

import DatabaseIntegrationTest
import no.nav.helse.modell.utbetaling.UtbetalingDao
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals

class UtbetalingDaoTest : DatabaseIntegrationTest() {
    private val utbetalingDao = UtbetalingDao(dataSource)

    @Test
    fun `hent utbetalinger for en person`() {
        nyPerson()
        val fagsystemId = "JKASDH634ASD243DSAOJK"

        val arbeidsgiverOppdragId = utbetalingDao.nyttOppdrag(fagsystemId, ORGNUMMER, "SPREF", "NY", LocalDate.now().plusDays(169))!!
        val personOppdragId = utbetalingDao.nyttOppdrag("KLSDJAD1654123ASDZW", FNR, "SPREF", "NY", LocalDate.now().plusDays(169))!!
        utbetalingDao.nyLinje(arbeidsgiverOppdragId, "NY", "SPREFAG-IOP", null, null, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31), 1200, 30000, 100.0, 1, null, null)
        val utbetalingIdId = utbetalingDao.opprettUtbetalingId(UUID.randomUUID(), FNR, ORGNUMMER, "UTBETALING", LocalDateTime.now(), arbeidsgiverOppdragId, personOppdragId)
        utbetalingDao.nyUtbetalingStatus(utbetalingIdId, "GODKJENT", LocalDateTime.now(), "{}")
        utbetalingDao.nyUtbetalingStatus(utbetalingIdId, "SENDT", LocalDateTime.now(), "{}")
        utbetalingDao.nyUtbetalingStatus(utbetalingIdId, "OVERFØRT", LocalDateTime.now(), "{}")
        utbetalingDao.nyUtbetalingStatus(utbetalingIdId, "UTBETALT", LocalDateTime.now(), "{}")

        val utbetalinger = utbetalingDao.findUtbetalinger(FNR)
        assertEquals(1, utbetalinger.size)
        val utbetaling = utbetalinger.first()

        assertEquals("UTBETALT", utbetaling.status)
        assertEquals(fagsystemId, utbetaling.arbeidsgiverOppdrag.fagsystemId)
        assertEquals(1, utbetaling.arbeidsgiverOppdrag.linjer.size)
    }
}
