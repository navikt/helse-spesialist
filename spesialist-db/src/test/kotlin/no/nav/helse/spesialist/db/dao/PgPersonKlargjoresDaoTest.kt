package no.nav.helse.spesialist.db.dao

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class PgPersonKlargjoresDaoTest : AbstractDBIntegrationTest() {
    private val personKlargjoresDao = sessionContext.personKlargjoresDao
    private val fødselsnummer = lagFødselsnummer()

    @Test
    fun `kan markere at en person er under klargjøring`() {
        assertFalse(personKlargjoresDao.klargjøringPågår(fødselsnummer))
        personKlargjoresDao.personKlargjøres(fødselsnummer)
        assertTrue(personKlargjoresDao.klargjøringPågår(fødselsnummer))
    }

    @Test
    fun `kan fjerne en person fra klargjøringstabellen`() {
        personKlargjoresDao.personKlargjøres(fødselsnummer)
        assertTrue(personKlargjoresDao.klargjøringPågår(fødselsnummer))
        personKlargjoresDao.personKlargjort(fødselsnummer)
        assertFalse(personKlargjoresDao.klargjøringPågår(fødselsnummer))
    }
}
