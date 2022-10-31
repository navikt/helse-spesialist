package no.nav.helse.modell.varsel

import DatabaseIntegrationTest
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.mediator.meldinger.NyeVarsler
import no.nav.helse.mediator.meldinger.NyeVarsler.Varsel.Companion.lagre
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class VarselDaoTest : DatabaseIntegrationTest() {

    @Test
    fun `lagrer unike varsler`() {
        val dao = VarselDao(dataSource)
        val vedtaksperiodeId = UUID.randomUUID()

        val varsel1 = lagVarsel(vedtaksperiodeId, UUID.randomUUID())
        val varsel2 = lagVarsel(vedtaksperiodeId, UUID.randomUUID())

        listOf(varsel1, varsel2).lagre(dao)

        assertEquals(2, dao.alleVarslerForVedtaksperiode(vedtaksperiodeId).size)
    }

    private fun lagVarsel(vedtaksperiodeId: UUID, id: UUID): NyeVarsler.Varsel {
        return NyeVarsler.Varsel(
            id = id,
            kode = "testKode",
            tidsstempel = LocalDateTime.now(),
            vedtaksperiodeId = vedtaksperiodeId,
        )
    }
}