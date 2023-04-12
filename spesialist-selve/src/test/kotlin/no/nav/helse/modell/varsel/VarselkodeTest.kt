package no.nav.helse.modell.varsel

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.modell.varsel.Varselkode.SB_EX_3
import no.nav.helse.modell.vedtaksperiode.IVedtaksperiodeObserver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class VarselkodeTest {
    @Test
    fun `opprett nytt varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val varsel = SB_EX_3.nyttVarsel(vedtaksperiodeId)
        varsel.registrer(observer)
        varsel.opprett(generasjonId)
        observer.assertOpprettelse(vedtaksperiodeId, generasjonId, SB_EX_3.name)
    }

    private val observer = object : IVedtaksperiodeObserver {
        val opprettedeVarsler = mutableMapOf<UUID, Opprettelse>()

        private inner class Opprettelse(
            val vedtaksperiodeId: UUID,
            val generasjonId: UUID,
            val varselkode: String,
        )

        override fun varselOpprettet(
            vedtaksperiodeId: UUID,
            generasjonId: UUID,
            varselId: UUID,
            varselkode: String,
            opprettet: LocalDateTime
        ) {
            opprettedeVarsler[generasjonId] = Opprettelse(vedtaksperiodeId, generasjonId, varselkode)
        }

        fun assertOpprettelse(
            forventetVedtaksperiodeId: UUID,
            forventetGenerasjonId: UUID,
            forventetVarselkode: String
        ) {
            val opprettelse = opprettedeVarsler[forventetGenerasjonId]
            assertEquals(forventetVedtaksperiodeId, opprettelse?.vedtaksperiodeId)
            assertEquals(forventetGenerasjonId, opprettelse?.generasjonId)
            assertEquals(forventetVarselkode, opprettelse?.varselkode)
        }
    }
}