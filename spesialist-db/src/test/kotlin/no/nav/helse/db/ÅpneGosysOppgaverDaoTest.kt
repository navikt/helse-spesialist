package no.nav.helse.db

import no.nav.helse.DatabaseIntegrationTest
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime


internal class ÅpneGosysOppgaverDaoTest : DatabaseIntegrationTest() {

    @BeforeEach
    fun setup() {
        testhendelse()
        opprettPerson()
    }

    @Test
    fun `lagre og lese med åpne oppgaver`() {
        åpneGosysOppgaverDao.persisterÅpneGosysOppgaver(
            ÅpneGosysOppgaverDto(FNR, 1, false, LocalDateTime.now())
        )
        val antallÅpneOppgaver = åpneGosysOppgaverDao.antallÅpneOppgaver(FNR)

        assertNotNull(antallÅpneOppgaver)
        if (antallÅpneOppgaver != null) {
            assertEquals(1, antallÅpneOppgaver)
        }
    }

    @Test
    fun `lagre og lese uten åpne oppgaver`() {
        åpneGosysOppgaverDao.persisterÅpneGosysOppgaver(
            ÅpneGosysOppgaverDto(FNR, 0, false, LocalDateTime.now())
        )
        val antallÅpneOppgaver = åpneGosysOppgaverDao.antallÅpneOppgaver(FNR)

        assertNotNull(antallÅpneOppgaver)
        if (antallÅpneOppgaver != null) {
            assertEquals(0, antallÅpneOppgaver)
        }
    }

    @Test
    fun `oppdaterer bruker uten åpne oppgaver til å ha åpne oppgaver`() {
        åpneGosysOppgaverDao.persisterÅpneGosysOppgaver(
            ÅpneGosysOppgaverDto(FNR, 0, false, LocalDateTime.now())
        )
        val antallÅpneOppgaver = åpneGosysOppgaverDao.antallÅpneOppgaver(FNR)

        assertNotNull(antallÅpneOppgaver)
        if (antallÅpneOppgaver != null) {
            assertEquals(0, antallÅpneOppgaver)
        }

        åpneGosysOppgaverDao.persisterÅpneGosysOppgaver(
            ÅpneGosysOppgaverDto(FNR, 1, false, LocalDateTime.now())
        )
        val erDigitalSvar2 = åpneGosysOppgaverDao.antallÅpneOppgaver(FNR)

        assertNotNull(erDigitalSvar2)
        if (erDigitalSvar2 != null) {
            assertEquals(1, erDigitalSvar2)
        }

    }

    @Test
    fun `lagre og lese ved feil i oppslag mot oppgave(gosys)`() {
        åpneGosysOppgaverDao.persisterÅpneGosysOppgaver(
            ÅpneGosysOppgaverDto(FNR, null, true, LocalDateTime.now())
        )

        assertNull(åpneGosysOppgaverDao.antallÅpneOppgaver(FNR))
    }

    @Test
    fun `mangler åpne oppgaver-sjekk`() {
        opprettPerson("12345678910")
        åpneGosysOppgaverDao.persisterÅpneGosysOppgaver(
            ÅpneGosysOppgaverDto("12345678910", 1, false, LocalDateTime.now())
        )
        assertNull(åpneGosysOppgaverDao.antallÅpneOppgaver(FNR))
    }
}
