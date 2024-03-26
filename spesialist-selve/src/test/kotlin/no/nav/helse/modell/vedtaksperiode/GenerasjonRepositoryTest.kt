package no.nav.helse.modell.vedtaksperiode

import java.util.UUID
import no.nav.helse.AbstractDatabaseTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class GenerasjonRepositoryTest : AbstractDatabaseTest() {

    private val repository = GenerasjonRepository(dataSource)

    @Test
    fun `Exception om vedtaksperioden ikke finnes`() {
        assertThrows<IllegalStateException> {
            repository.brukVedtaksperiode("1234567891011", UUID.randomUUID()) {}
        }
    }
}