package no.nav.helse.modell.vergemal

import DatabaseIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class VergemålDaoTest : DatabaseIntegrationTest() {

    @BeforeEach
    fun setup() {
        opprettPerson()
    }

    @Test
    fun `lagre og les ut vergemål`() {
        vergemålDao.lagre(FNR, VergemålOgFremtidsfullmakt(harVergemål = true, harFremtidsfullmakter = false), false)
        assertEquals(true, vergemålDao.harVergemål(FNR))
        vergemålDao.lagre(FNR, VergemålOgFremtidsfullmakt(harVergemål = false, harFremtidsfullmakter = false), false)
        assertEquals(false, vergemålDao.harVergemål(FNR))
    }

    @Test
    fun `lagre og les ut fullmakter`() {
        vergemålDao.lagre(FNR, VergemålOgFremtidsfullmakt(harVergemål = false, harFremtidsfullmakter = false), false)
        assertEquals(false, vergemålDao.harFullmakt(FNR))
        vergemålDao.lagre(FNR, VergemålOgFremtidsfullmakt(harVergemål = false, harFremtidsfullmakter = false), true)
        assertEquals(true, vergemålDao.harFullmakt(FNR))
        vergemålDao.lagre(FNR, VergemålOgFremtidsfullmakt(harVergemål = false, harFremtidsfullmakter = true), false)
        assertEquals(true, vergemålDao.harFullmakt(FNR))
        vergemålDao.lagre(FNR, VergemålOgFremtidsfullmakt(harVergemål = false, harFremtidsfullmakter = true), true)
        assertEquals(true, vergemålDao.harFullmakt(FNR))
    }

    @Test
    fun `ikke vergemål om vi ikke har gjort noe oppslag`() {
        assertNull(vergemålDao.harVergemål(FNR))
    }

    @Test
    fun `ikke fullmakt om vi ikke har gjort noe oppslag`() {
        assertNull(vergemålDao.harFullmakt(FNR))
    }
}
