package no.nav.helse.modell.varsel

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.modell.varsel.Varselkode.SB_EX_3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class VarselkodeTest {

    private val varselRepository = object : VarselRepository {

        val vedtaksperiodevarsler: MutableMap<UUID, MutableList<String>> = mutableMapOf()
        val deaktiverteVarsler: MutableMap<UUID, MutableList<String>> = mutableMapOf()

        override fun finnVarslerFor(vedtaksperiodeId: UUID): List<Varsel> {
            TODO("Not yet implemented")
        }

        override fun deaktiverFor(vedtaksperiodeId: UUID, varselkode: String, definisjonId: UUID?) {
            deaktiverteVarsler.getOrPut(vedtaksperiodeId) { mutableListOf() }.add(varselkode)
        }

        override fun godkjennFor(vedtaksperiodeId: UUID, varselkode: String, ident: String, definisjonId: UUID?) {
            TODO("Not yet implemented")
        }

        override fun avvisFor(vedtaksperiodeId: UUID, varselkode: String, ident: String, definisjonId: UUID?) {
            TODO("Not yet implemented")
        }

        override fun godkjennAlleFor(vedtaksperiodeId: UUID, ident: String) {
            TODO("Not yet implemented")
        }

        override fun avvisAlleFor(vedtaksperiodeId: UUID, ident: String) {
            TODO("Not yet implemented")
        }

        override fun lagreVarsel(id: UUID, varselkode: String, opprettet: LocalDateTime, vedtaksperiodeId: UUID) {
            vedtaksperiodevarsler.getOrPut(vedtaksperiodeId) { mutableListOf() }.add(varselkode)
        }

        override fun lagreDefinisjon(
            id: UUID,
            varselkode: String,
            tittel: String,
            forklaring: String?,
            handling: String?,
            avviklet: Boolean,
            opprettet: LocalDateTime,
        ) {
            TODO("Not yet implemented")
        }
    }

    @Test
    fun `opprett nytt varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        SB_EX_3.nyttVarsel(vedtaksperiodeId, varselRepository)

        assertEquals(1, varselRepository.vedtaksperiodevarsler[vedtaksperiodeId]?.size)
    }

    @Test
    fun `deaktiverer varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        SB_EX_3.nyttVarsel(vedtaksperiodeId, varselRepository)
        SB_EX_3.deaktiverFor(vedtaksperiodeId, varselRepository)

        assertEquals(1, varselRepository.deaktiverteVarsler[vedtaksperiodeId]?.size)
    }
}