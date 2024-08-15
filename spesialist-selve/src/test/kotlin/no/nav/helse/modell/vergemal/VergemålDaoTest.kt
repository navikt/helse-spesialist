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
        vergemålDao.lagre(FNR, Vergemål(harVergemål = true, harFremtidsfullmakter = false, harFullmakter = false))
        assertEquals(true, vergemålDao.harVergemål(FNR))
        vergemålDao.lagre(FNR, Vergemål(harVergemål = false, harFremtidsfullmakter = false, harFullmakter = false))
        assertEquals(false, vergemålDao.harVergemål(FNR))
    }

    @Test
    fun `lagre og les ut fullmakter`() {
        vergemålDao.lagre(FNR, Vergemål(harVergemål = false, harFremtidsfullmakter = false, harFullmakter = false))
        assertEquals(false, vergemålDao.harFullmakt(FNR))
        vergemålDao.lagre(FNR, Vergemål(harVergemål = false, harFremtidsfullmakter = false, harFullmakter = true))
        assertEquals(true, vergemålDao.harFullmakt(FNR))
        vergemålDao.lagre(FNR, Vergemål(harVergemål = false, harFremtidsfullmakter = true, harFullmakter = false))
        assertEquals(true, vergemålDao.harFullmakt(FNR))
        vergemålDao.lagre(FNR, Vergemål(harVergemål = false, harFremtidsfullmakter = true, harFullmakter = true))
        assertEquals(true, vergemålDao.harFullmakt(FNR))
    }

    @Test
    fun `ikke vergemål om vi ikke har gjort noe oppslag`() {
        assertNull(vergemålDao.harVergemål(FNR))
    }
}
