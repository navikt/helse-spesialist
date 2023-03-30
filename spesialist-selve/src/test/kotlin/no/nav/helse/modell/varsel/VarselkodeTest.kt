package no.nav.helse.modell.varsel

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.januar
import no.nav.helse.modell.varsel.Varselkode.SB_EX_3
import no.nav.helse.modell.vedtaksperiode.Generasjon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class VarselkodeTest {
    private val vedtaksperiodevarsler: MutableMap<UUID, MutableList<String>> = mutableMapOf()
    private val deaktiverteVarsler: MutableMap<UUID, MutableList<String>> = mutableMapOf()

    @BeforeEach
    internal fun beforeEach() {
        vedtaksperiodevarsler.clear()
        deaktiverteVarsler.clear()
    }

    @Test
    fun `opprett nytt varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjon(vedtaksperiodeId)
        SB_EX_3.nyttVarsel(generasjon, varselRepository)

        assertEquals(1, vedtaksperiodevarsler[vedtaksperiodeId]?.size)
    }

    @Test
    fun `deaktiverer varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjon(vedtaksperiodeId)
        SB_EX_3.nyttVarsel(generasjon, varselRepository)
        SB_EX_3.deaktiverFor(generasjon, varselRepository)

        assertEquals(1, deaktiverteVarsler[vedtaksperiodeId]?.size)
    }

    private fun generasjon(vedtaksperiodeId: UUID = UUID.randomUUID()) = Generasjon(
        id = UUID.randomUUID(),
        vedtaksperiodeId = vedtaksperiodeId,
        fom = 1.januar,
        tom = 31.januar,
        skjæringstidspunkt = 1.januar
    )

    private val varselRepository = object : VarselRepository {

        override fun lagreVarsel(id: UUID, generasjonId: UUID, varselkode: String, opprettet: LocalDateTime, vedtaksperiodeId: UUID) {
            vedtaksperiodevarsler.getOrPut(vedtaksperiodeId) { mutableListOf() }.add(varselkode)
        }

        override fun deaktiverFor(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String, definisjonId: UUID?) {
            deaktiverteVarsler.getOrPut(vedtaksperiodeId) { mutableListOf() }.add(varselkode)
        }

        override fun reaktiverFor(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String): Unit = TODO("Not yet implemented")
        override fun godkjennFor(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String, ident: String, definisjonId: UUID?): Unit = TODO("Not yet implemented")
        override fun avvisFor(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String, ident: String, definisjonId: UUID?): Unit = TODO("Not yet implemented")
        override fun lagreDefinisjon(id: UUID, varselkode: String, tittel: String, forklaring: String?, handling: String?, avviklet: Boolean, opprettet: LocalDateTime): Unit = TODO("Not yet implemented")
        override fun oppdaterGenerasjonFor(id: UUID, gammelGenerasjonId: UUID, nyGenerasjonId: UUID) {
            TODO("Not yet implemented")
        }
    }

}