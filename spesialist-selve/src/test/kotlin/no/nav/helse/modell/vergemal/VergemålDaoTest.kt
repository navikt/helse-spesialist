package no.nav.helse.modell.vergemal

import DatabaseIntegrationTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VergemålDaoTest: DatabaseIntegrationTest() {

    @BeforeEach
    fun setup() {
        opprettPerson()
    }

    @Test
    fun `lagre og les ut vergemål`() {
        vergemålDao.lagre(FNR, Vergemål(harVergemål = true, harFremtidsfullmakter = false, harFullmakter = false))
        assertEquals(true, vergemålDao.harVergemål(FNR))
    }

    @Test
    fun `ikke vergemål om vi ikke har gjort noe oppslag`() {
        assertNull(vergemålDao.harVergemål(FNR))
    }
}
