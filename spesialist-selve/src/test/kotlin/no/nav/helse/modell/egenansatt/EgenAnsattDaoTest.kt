package no.nav.helse.modell.egenansatt

import no.nav.helse.DatabaseIntegrationTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

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
        assertNotNull(egenAnsattSvar)
        if (egenAnsattSvar != null) {
            assertFalse(egenAnsattSvar)
        }
    }
}
