package no.nav.helse.modell.varsel

import DatabaseIntegrationTest
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.mediator.meldinger.NyeVarsler
import no.nav.helse.mediator.meldinger.NyeVarsler.Varsel.Companion.lagre
import no.nav.helse.mediator.meldinger.Varseldefinisjon
import no.nav.helse.mediator.meldinger.Varseldefinisjon.Companion.lagre
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class VarselDaoTest : DatabaseIntegrationTest() {

    internal companion object {
        val varselDao = VarselDao(dataSource)
    }

    @Test
    fun `lagrer unike varsler`() {
        val varsel1 = lagVarsel(VEDTAKSPERIODE)
        val varsel2 = lagVarsel(VEDTAKSPERIODE)

        listOf(varsel1, varsel2).lagre(varselDao)

        assertEquals(2, varselDao.alleVarslerForVedtaksperiode(VEDTAKSPERIODE).size)
    }

    @Test
    fun `lagrer varseldefinisjon`() {
        val varseldefinisjon1 = lagVarseldefinisjon()

        listOf(varseldefinisjon1).lagre(varselDao)

        assertEquals(1, varselDao.alleDefinisjoner().size)
    }


    private fun lagVarsel(vedtaksperiodeId: UUID): NyeVarsler.Varsel {
        return NyeVarsler.Varsel(
            id = UUID.randomUUID(),
            kode = "testKode",
            tidsstempel = LocalDateTime.now(),
            vedtaksperiodeId = vedtaksperiodeId
        )
    }

    private fun lagVarseldefinisjon(): Varseldefinisjon {
        return Varseldefinisjon(
            id = UUID.randomUUID(),
            kode = "${UUID.randomUUID()}",
            tittel = "En tittel",
            forklaring = "En forklaring",
            handling = "En handling",
            avviklet = false,
            opprettet = LocalDateTime.now()
        )
    }
}