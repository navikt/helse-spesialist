package no.nav.helse.modell.gosysoppgaver

import DatabaseIntegrationTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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
        val harÅpneOppgaver = åpneGosysOppgaverDao.harÅpneOppgaver(FNR)

        assertNotNull(harÅpneOppgaver) {
            assertEquals(1, it)
        }
    }

    @Test
    fun `lagre og lese uten åpne oppgaver`() {
        åpneGosysOppgaverDao.persisterÅpneGosysOppgaver(
            ÅpneGosysOppgaverDto(FNR, 0, false, LocalDateTime.now())
        )
        val harÅpneOppgaver = åpneGosysOppgaverDao.harÅpneOppgaver(FNR)

        assertNotNull(harÅpneOppgaver) {
            assertEquals(0, it)
        }
    }

    @Test
    fun `oppdaterer bruker uten åpne oppgaver til å ha åpne oppgaver`() {
        åpneGosysOppgaverDao.persisterÅpneGosysOppgaver(
            ÅpneGosysOppgaverDto(FNR, 0, false, LocalDateTime.now())
        )
        val harÅpneOppgaver = åpneGosysOppgaverDao.harÅpneOppgaver(FNR)

        assertNotNull(harÅpneOppgaver) {
            assertEquals(0, it)
        }

        åpneGosysOppgaverDao.persisterÅpneGosysOppgaver(
            ÅpneGosysOppgaverDto(FNR, 1, false, LocalDateTime.now())
        )
        val erDigitalSvar2 = åpneGosysOppgaverDao.harÅpneOppgaver(FNR)

        assertNotNull(erDigitalSvar2) {
            assertEquals(1, it)
        }
    }

    @Test
    fun `lagre og lese ved feil i oppslag mot oppgave(gosys)`() {
        åpneGosysOppgaverDao.persisterÅpneGosysOppgaver(
            ÅpneGosysOppgaverDto(FNR, null, true, LocalDateTime.now())
        )

        assertNull(åpneGosysOppgaverDao.harÅpneOppgaver(FNR))
    }

    @Test
    fun `mangler åpne oppgaver-sjekk`() {
        opprettPerson("12345678910")
        åpneGosysOppgaverDao.persisterÅpneGosysOppgaver(
            ÅpneGosysOppgaverDto("12345678910", 1, false, LocalDateTime.now())
        )
        assertNull(åpneGosysOppgaverDao.harÅpneOppgaver(FNR))
    }
}
