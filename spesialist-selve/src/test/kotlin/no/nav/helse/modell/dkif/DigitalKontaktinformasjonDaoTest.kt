package no.nav.helse.modell.dkif

import DatabaseIntegrationTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class DigitalKontaktinformasjonDaoTest : DatabaseIntegrationTest() {

    @BeforeEach
    fun setup() {
        testhendelse()
        opprettPerson()
    }

    @Test
    fun `lagre og lese ikke-digital bruker`() {
        digitalKontaktinformasjonDao.lagre(FNR, false, LocalDateTime.now())
        val erDigitalSvar = digitalKontaktinformasjonDao.erDigital(FNR)

        assertNotNull(erDigitalSvar) {
            assertFalse(it)
        }
    }

    @Test
    fun `lagre og lese digital bruker`() {
        digitalKontaktinformasjonDao.lagre(FNR, true, LocalDateTime.now())
        val erDigitalSvar = digitalKontaktinformasjonDao.erDigital(FNR)

        assertNotNull(erDigitalSvar) {
            assertTrue(it)
        }
    }

    @Test
    fun `oppdaterer digital bruker til å bli ikke-digital`() {
        digitalKontaktinformasjonDao.lagre(FNR, true, LocalDateTime.now())
        val erDigitalSvar = digitalKontaktinformasjonDao.erDigital(FNR)

        assertNotNull(erDigitalSvar) {
            assertTrue(it)
        }

        digitalKontaktinformasjonDao.lagre(FNR, false, LocalDateTime.now())
        val erDigitalSvar2 = digitalKontaktinformasjonDao.erDigital(FNR)

        assertNotNull(erDigitalSvar2) {
            assertFalse(it)
        }
    }

    @Test
    fun `mangler dkif-sjekk`() {
        opprettPerson("12345678910")
        digitalKontaktinformasjonDao.lagre("12345678910", true, LocalDateTime.now())
        assertNull(digitalKontaktinformasjonDao.erDigital(FNR))
    }
}
