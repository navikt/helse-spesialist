package no.nav.helse.spesialist.db.dao

import no.nav.helse.db.VergemålOgFremtidsfullmakt
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PgVergemålDaoTest : AbstractDBIntegrationTest() {
    private val person = opprettPerson()

    @Test
    fun `lagre og les ut vergemål`() {
        vergemålDao.lagre(person.id.value, VergemålOgFremtidsfullmakt(harVergemål = true, harFremtidsfullmakter = false), false)
        assertEquals(true, vergemålDao.harVergemål(person.id.value))
        vergemålDao.lagre(person.id.value, VergemålOgFremtidsfullmakt(harVergemål = false, harFremtidsfullmakter = false), false)
        assertEquals(false, vergemålDao.harVergemål(person.id.value))
    }

    @Test
    fun `lagre og les ut fullmakter`() {
        vergemålDao.lagre(person.id.value, VergemålOgFremtidsfullmakt(harVergemål = false, harFremtidsfullmakter = false), false)
        assertEquals(false, vergemålDao.harFullmakt(person.id.value))
        vergemålDao.lagre(person.id.value, VergemålOgFremtidsfullmakt(harVergemål = false, harFremtidsfullmakter = false), true)
        assertEquals(true, vergemålDao.harFullmakt(person.id.value))
        vergemålDao.lagre(person.id.value, VergemålOgFremtidsfullmakt(harVergemål = false, harFremtidsfullmakter = true), false)
        assertEquals(true, vergemålDao.harFullmakt(person.id.value))
        vergemålDao.lagre(person.id.value, VergemålOgFremtidsfullmakt(harVergemål = false, harFremtidsfullmakter = true), true)
        assertEquals(true, vergemålDao.harFullmakt(person.id.value))
    }

    @Test
    fun `ikke vergemål om vi ikke har gjort noe oppslag`() {
        assertNull(vergemålDao.harVergemål(person.id.value))
    }

    @Test
    fun `ikke fullmakt om vi ikke har gjort noe oppslag`() {
        assertNull(vergemålDao.harFullmakt(person.id.value))
    }
}
