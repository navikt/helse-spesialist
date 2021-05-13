package no.nav.helse.modell.egenansatt

import DatabaseIntegrationTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

internal class EgenAnsattDaoTest : DatabaseIntegrationTest() {
    @BeforeEach
    fun setup() {
        testhendelse()
        opprettPerson()
    }

    @Test
    fun `setter og henter egen ansatt`() {
        egenAnsattDao.lagre(FNR, false, LocalDateTime.now())
        val egenAnsattSvar = egenAnsattDao.erEgenAnsatt(FNR)
        assertNotNull(egenAnsattSvar) {
            assertFalse(it)
        }
    }
}
