package no.nav.helse.modell.vergemal

import DatabaseIntegrationTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VergemålDaoTest: DatabaseIntegrationTest() {

    @BeforeEach
    fun setup() {
        opprettPerson()
    }

    @Test
    fun `lagre og les ut vergemål`() {
        vergemålDao.lagre(FNR, Vergemål(harVergemål = true, harFremtidsfullmakter = false, harFullmakter = false))
        val vergemål = requireNotNull(vergemålDao.hentVergemål(FNR))
        assertTrue(vergemål.harVergemål)
        assertFalse(vergemål.harFremtidsfullmakter)
        assertFalse(vergemål.harFullmakter)
    }

    @Test
    fun `ikke vergemål om vi ikke har gjort noe oppslag`() {
        assertNull(vergemålDao.hentVergemål(FNR))
    }
}
