package no.nav.helse.spesialist.db.dao

import no.nav.helse.modell.varsel.VarselRepository
import no.nav.helse.modell.varsel.Varseldefinisjon
import no.nav.helse.modell.varsel.VarseldefinisjonDto
import no.nav.helse.spesialist.db.AbstractDatabaseTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class VarselRepositoryTest : AbstractDatabaseTest() {
    private val varselRepository  = VarselRepository(
        varselDao = repositories.varselDao,
        definisjonDao = repositories.definisjonDao
    )
    private val definisjonDao = repositories.definisjonDao

    @Test
    fun `lagre definisjon`() {
        val definisjonId = UUID.randomUUID()
        val definisjonDto =
            VarseldefinisjonDto(definisjonId, "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        varselRepository.lagreDefinisjon(definisjonDto)
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
