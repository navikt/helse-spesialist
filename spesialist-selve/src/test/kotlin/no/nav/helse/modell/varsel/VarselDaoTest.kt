package no.nav.helse.modell.varsel

import DatabaseIntegrationTest
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.mediator.meldinger.NyeVarsler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class VarselDaoTest : DatabaseIntegrationTest() {

    @Test
    fun `lagrer unike varsler`() {
        val dao = VarselDao(dataSource)
        val vedtaksperiodeId = UUID.randomUUID()

        val varsel1 = lagVarsel(vedtaksperiodeId, UUID.randomUUID())
        val varsel2 = lagVarsel(vedtaksperiodeId, UUID.randomUUID())

        dao.lagre(listOf(varsel1, varsel2))
        assertEquals(2, dao.alleVarslerForVedtaksperiode(vedtaksperiodeId).size)
    }

    private fun lagVarsel(vedtaksperiodeId: UUID, id: UUID): NyeVarsler.Varsel {
        return NyeVarsler.Varsel(
            id = id,
            kode = "testKode",
            tittel = "Eksempelvarsel",
            forklaring = "Eksempelforklaring som forklarer",
            handling = "Handling som burde tas",
            avviklet = false,
            tidsstempel = LocalDateTime.now(),
            kontekster = listOf(
                NyeVarsler.Kontekst(
                    konteksttype = "Person",
                    kontekstmap = mapOf(
                        "fødselsnummer" to "12345678911",
                        "aktørId" to "2093088099680"
                    )
                ),
                NyeVarsler.Kontekst(
                    konteksttype = "Arbeidsgiver",
                    kontekstmap = mapOf(
                        "organisasjonsnummer" to "123456789"
                    )
                ),
                NyeVarsler.Kontekst(
                    konteksttype = "Vedtaksperiode",
                    kontekstmap = mapOf(
                        "vedtaksperiodeId" to vedtaksperiodeId.toString()
                    )
                )
            )
        )
    }
}