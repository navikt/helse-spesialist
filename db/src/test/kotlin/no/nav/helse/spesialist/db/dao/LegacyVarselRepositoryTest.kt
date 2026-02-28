package no.nav.helse.spesialist.db.dao

import no.nav.helse.modell.varsel.LegacyVarselRepository
import no.nav.helse.modell.varsel.Varseldefinisjon
import no.nav.helse.modell.varsel.VarseldefinisjonDto
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class LegacyVarselRepositoryTest : AbstractDBIntegrationTest() {
    private val legacyVarselRepository  = LegacyVarselRepository(
        legacyVarselDao = daos.legacyVarselDao,
        definisjonDao = daos.definisjonDao
    )
    private val definisjonDao = daos.definisjonDao

    @Test
    fun `lagre definisjon`() {
        val definisjonId = UUID.randomUUID()
        val definisjonDto =
            VarseldefinisjonDto(definisjonId, "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        legacyVarselRepository.lagreDefinisjon(definisjonDto)
        assertEquals(
            Varseldefinisjon(
                id = definisjonId,
                varselkode = "EN_KODE",
                tittel = "EN_TITTEL",
                forklaring = "EN_FORKLARING",
                handling = "EN_HANDLING",
                avviklet = false,
                opprettet = LocalDateTime.now(),
            ),
            definisjonDao.definisjonFor(definisjonId),
        )
    }
}
