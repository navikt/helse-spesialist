package no.nav.helse.modell.varsel

import java.util.UUID
import no.nav.helse.modell.varsel.Varselkode.SB_EX_4
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class VarselkodeTest {

    private val varselRepository = object : VarselRepository {

        val vedtaksperiodevarsler: MutableMap<UUID, MutableList<String>> = mutableMapOf()
        val deaktiverteVarsler: MutableMap<UUID, MutableList<String>> = mutableMapOf()

        override fun erAktivFor(vedtaksperiodeId: UUID, varselkode: String): Boolean {
            return vedtaksperiodevarsler[vedtaksperiodeId]?.contains(varselkode) == true
        }

        override fun nyttVarsel(vedtaksperiodeId: UUID, varselkode: String) {
            vedtaksperiodevarsler.getOrPut(vedtaksperiodeId) { mutableListOf() }.add(varselkode)
        }

        override fun deaktiverFor(vedtaksperiodeId: UUID, varselkode: String) {
            deaktiverteVarsler.getOrPut(vedtaksperiodeId) { mutableListOf() }.add(varselkode)
        }
    }

    @Test
    fun `opprett nytt varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        SB_EX_4.nyttVarsel(vedtaksperiodeId, varselRepository)

        assertEquals(1, varselRepository.vedtaksperiodevarsler[vedtaksperiodeId]?.size)
    }

    @Test
    fun `sjekker om varsel er aktivt`() {
        val vedtaksperiodeId = UUID.randomUUID()
        SB_EX_4.nyttVarsel(vedtaksperiodeId, varselRepository)
        SB_EX_4.nyttVarsel(vedtaksperiodeId, varselRepository)

        assertEquals(1, varselRepository.vedtaksperiodevarsler[vedtaksperiodeId]?.size)
    }

    @Test
    fun `deaktiverer varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        SB_EX_4.nyttVarsel(vedtaksperiodeId, varselRepository)
        SB_EX_4.deaktiverFor(vedtaksperiodeId, varselRepository)

        assertEquals(1, varselRepository.deaktiverteVarsler[vedtaksperiodeId]?.size)
    }
}