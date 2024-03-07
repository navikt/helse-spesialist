package no.nav.helse.modell.vedtaksperiode

import java.util.UUID
import no.nav.helse.AbstractDatabaseTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ActualGenerasjonRepositoryTest: AbstractDatabaseTest() {
    private val repository = ActualGenerasjonRepository(dataSource)

    @Test
    fun `Exception om vedtaksperioden ikke finnes`() {
        assertThrows<IllegalStateException> {
            repository.brukVedtaksperiode(UUID.randomUUID()) {}
        }
    }
}