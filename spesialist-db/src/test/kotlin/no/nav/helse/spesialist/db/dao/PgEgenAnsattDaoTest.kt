package no.nav.helse.spesialist.db.dao

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

internal class PgEgenAnsattDaoTest : AbstractDBIntegrationTest() {
    @Test
    fun `setter og henter egen ansatt`() {
        val person = opprettPerson()
        egenAnsattDao.lagre(person.id.value, false, LocalDateTime.now())
        val egenAnsattSvar = egenAnsattDao.erEgenAnsatt(person.id.value)
        assertNotNull(egenAnsattSvar)
        assertFalse(egenAnsattSvar)
    }
}
