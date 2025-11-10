package no.nav.helse.spesialist.db.dao

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.Fødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class PgPersonPseudoIdDaoTest : AbstractDBIntegrationTest() {

    @Test
    fun `generer og få tilbake id`() {
        val dao = PgPersonPseudoIdDao(session)
        val identitetsnummer = lagFødselsnummer()
        val personPseudoId = dao.nyPersonPseudoId(Fødselsnummer(identitetsnummer))
        assertEquals(identitetsnummer, dao.hentIdentitetsnummer(personPseudoId)?.value)
    }

    @Test
    fun `slett utdaterte pseudo-ider`() {
        val fødselsnummer = Fødselsnummer(lagFødselsnummer())
        runAndRollback {
            val pseudoId1 = it.nyPersonPseudoId(fødselsnummer)
            Thread.sleep(2000)
            val pseudoId2 = it.nyPersonPseudoId(fødselsnummer)
            val antallSlettet = it.slettPseudoIderEldreEnn(Duration.of(1, ChronoUnit.SECONDS))
            assertEquals(null, it.hentIdentitetsnummer(pseudoId1))
            assertEquals(fødselsnummer, it.hentIdentitetsnummer(pseudoId2))
            assertAtLeast(1, antallSlettet)
        }
    }

    private fun runAndRollback(test: (dao: PgPersonPseudoIdDao) -> Unit) {
        val rollbackMessage = "Rollback transaction"
        val exception = assertThrows<IllegalStateException> {
            session.transaction {
                test(PgPersonPseudoIdDao(it))
                error(rollbackMessage)
            }
        }
        assertEquals(rollbackMessage, exception.message)
    }

    private fun assertAtLeast(expectedMinimum: Int, actual: Int) {
        assertTrue(actual >= expectedMinimum) { "Forventet minst $expectedMinimum, men var $actual" }
    }
}
