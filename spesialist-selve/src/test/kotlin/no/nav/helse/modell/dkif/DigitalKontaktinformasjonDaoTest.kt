package no.nav.helse.modell.dkif

import DatabaseIntegrationTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue

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

        assertNotNull(erDigitalSvar)
        if (erDigitalSvar != null){
            assertFalse(erDigitalSvar)
        }
    }

    @Test
    fun `lagre og lese digital bruker`() {
        digitalKontaktinformasjonDao.lagre(FNR, true, LocalDateTime.now())
        val erDigitalSvar = digitalKontaktinformasjonDao.erDigital(FNR)

        assertNotNull(erDigitalSvar)
        if (erDigitalSvar != null){
            assertTrue(erDigitalSvar)
        }
    }

    @Test
    fun `oppdaterer digital bruker til å bli ikke-digital`() {
        digitalKontaktinformasjonDao.lagre(FNR, true, LocalDateTime.now())
        val erDigitalSvar = digitalKontaktinformasjonDao.erDigital(FNR)

        assertNotNull(erDigitalSvar)
        if (erDigitalSvar != null){
            assertTrue(erDigitalSvar)
        }

        digitalKontaktinformasjonDao.lagre(FNR, false, LocalDateTime.now())
        val erDigitalSvar2 = digitalKontaktinformasjonDao.erDigital(FNR)

        assertNotNull(erDigitalSvar2)
        if (erDigitalSvar2 != null){
            assertFalse(erDigitalSvar2)
        }
    }

    @Test
    fun `mangler dkif-sjekk`() {
        opprettPerson("12345678910")
        digitalKontaktinformasjonDao.lagre("12345678910", true, LocalDateTime.now())
        assertNull(digitalKontaktinformasjonDao.erDigital(FNR))
    }
}
