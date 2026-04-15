package no.nav.helse.spesialist.db.repository

import no.nav.helse.db.overstyring.venting.MeldingId
import no.nav.helse.db.overstyring.venting.VenterPåKvitteringForOverstyring
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagIdentitetsnummer
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PgVenterPåKvitteringForOverstyringRepositoryTest: AbstractDBIntegrationTest() {

    private val repository = sessionContext.venterPåKvitteringForOverstyringRepository

    @Test
    fun `lagrer og finner`() {
        // Given
        val meldingId = MeldingId(UUID.randomUUID())
        val identitetsnummer = lagIdentitetsnummer()
        val venterPåKvitteringForOverstyring = VenterPåKvitteringForOverstyring.ny(meldingId, identitetsnummer)

        // When
        repository.lagre(venterPåKvitteringForOverstyring)

        // Then
        val lagret = repository.finn(meldingId)
        assertNotNull(lagret)
        assertEquals(identitetsnummer, lagret.identitetsnummer)
    }

    @Test
    fun `kan slette`() {
        // Given
        val meldingId = MeldingId(UUID.randomUUID())
        val identitetsnummer = lagIdentitetsnummer()
        val venterPåKvitteringForOverstyring = VenterPåKvitteringForOverstyring.ny(meldingId, identitetsnummer)
        repository.lagre(venterPåKvitteringForOverstyring)
        assertNotNull(repository.finn(meldingId))

        // When
        repository.slett(meldingId)

        // Then
        assertNull(repository.finn(meldingId))
    }
}
