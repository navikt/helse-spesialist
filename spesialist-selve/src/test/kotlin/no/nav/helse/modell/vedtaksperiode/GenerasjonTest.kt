package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.modell.varsel.Varsel
import no.nav.helse.modell.varsel.VarselRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class GenerasjonTest {
    private val varsler = mutableListOf<String>()
    private val godkjenteVarsler = mutableListOf<String>()
    private val avvisteVarsler = mutableListOf<String>()
    private val deaktiverteVarsler = mutableListOf<String>()

    @BeforeEach
    internal fun beforeEach() {
        varsler.clear()
        godkjenteVarsler.clear()
        avvisteVarsler.clear()
        deaktiverteVarsler.clear()
    }

    @Test
    fun `godkjenner enkelt varsel`() {
        val generasjon = Generasjon(UUID.randomUUID(), UUID.randomUUID(), false, setOf(nyttVarsel("SB_EX_1"), nyttVarsel("SB_EX_2")))
        generasjon.håndterGodkjentVarsel("SB_EX_1", "EN_IDENT", varselRepository)
        assertEquals(1, godkjenteVarsler.size)
        assertEquals("SB_EX_1", godkjenteVarsler[0])
    }

    @Test
    fun `deaktiverer enkelt varsel`() {
        val generasjon = Generasjon(UUID.randomUUID(), UUID.randomUUID(), false, setOf(nyttVarsel("SB_EX_1"), nyttVarsel("SB_EX_2")))
        generasjon.håndterDeaktivertVarsel("SB_EX_1", varselRepository)
        assertEquals(1, deaktiverteVarsler.size)
        assertEquals("SB_EX_1", deaktiverteVarsler[0])
    }
    @Test
    fun `godkjenner alle varsler når generasjonen blir godkjent`() {
        val generasjon = Generasjon(UUID.randomUUID(), UUID.randomUUID(), false, setOf(nyttVarsel("SB_EX_1"), nyttVarsel("SB_EX_2")))
        generasjon.håndterGodkjent("EN_IDENT", varselRepository)
        assertEquals(2, godkjenteVarsler.size)
        assertEquals("SB_EX_1", godkjenteVarsler[0])
        assertEquals("SB_EX_2", godkjenteVarsler[1])
    }

    @Test
    fun `avviser alle varsler når generasjonen blir avvist`() {
        val generasjon = Generasjon(UUID.randomUUID(), UUID.randomUUID(), false, setOf(nyttVarsel("SB_EX_1"), nyttVarsel("SB_EX_2")))
        generasjon.håndterAvvist("EN_IDENT", varselRepository)
        assertEquals(2, avvisteVarsler.size)
        assertEquals("SB_EX_1", avvisteVarsler[0])
        assertEquals("SB_EX_2", avvisteVarsler[1])
    }

    @Test
    fun `Lagrer kun én utgave av et aktivt varsel`() {
        val generasjon = Generasjon(UUID.randomUUID(), UUID.randomUUID(), false, setOf(nyttVarsel("SB_EX_1"), nyttVarsel("SB_EX_2")))
        generasjon.håndterNyttVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), varselRepository)

        assertEquals(1, varsler.size)
    }

    @Test
    fun `referential equals`() {
        val generasjon = Generasjon(UUID.randomUUID(), UUID.randomUUID(), false)
        assertEquals(generasjon, generasjon)
        assertEquals(generasjon.hashCode(), generasjon.hashCode())
    }

    @Test
    fun `structural equals`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon1 = Generasjon(generasjonId, vedtaksperiodeId, false)
        val generasjon2 = Generasjon(generasjonId, vedtaksperiodeId, false)
        assertEquals(generasjon1, generasjon2)
        assertEquals(generasjon1.hashCode(), generasjon2.hashCode())
    }

    @Test
    fun `forskjellig generasjonIder`() {
        val generasjonId1 = UUID.randomUUID()
        val generasjonId2 = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon1 = Generasjon(generasjonId1, vedtaksperiodeId, false)
        val generasjon2 = Generasjon(generasjonId2, vedtaksperiodeId, false)
        assertNotEquals(generasjon1, generasjon2)
        assertNotEquals(generasjon1.hashCode(), generasjon2.hashCode())
    }

    @Test
    fun `forskjellige vedtaksperiodeIder`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val generasjon1 = Generasjon(generasjonId, vedtaksperiodeId1, false)
        val generasjon2 = Generasjon(generasjonId, vedtaksperiodeId2, false)
        assertNotEquals(generasjon1, generasjon2)
        assertNotEquals(generasjon1.hashCode(), generasjon2.hashCode())
    }

    @Test
    fun `forskjellig låst`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon1 = Generasjon(generasjonId, vedtaksperiodeId, false)
        val generasjon2 = Generasjon(generasjonId, vedtaksperiodeId, true)
        assertNotEquals(generasjon1, generasjon2)
        assertNotEquals(generasjon1.hashCode(), generasjon2.hashCode())
    }

    private fun nyttVarsel(varselkode: String): Varsel =
        Varsel(UUID.randomUUID(), varselkode, LocalDateTime.now(), UUID.randomUUID())

    private val varselRepository = object : VarselRepository {
        override fun finnVarslerFor(vedtaksperiodeId: UUID): List<Varsel> {
            TODO("Not yet implemented")
        }

        override fun deaktiverFor(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String, definisjonId: UUID?) {
            deaktiverteVarsler.add(varselkode)
        }

        override fun godkjennFor(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String, ident: String, definisjonId: UUID?) {
            godkjenteVarsler.add(varselkode)
        }

        override fun avvisFor(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String, ident: String, definisjonId: UUID?) {
            avvisteVarsler.add(varselkode)
        }

        override fun lagreVarsel(id: UUID, generasjonId: UUID, varselkode: String, opprettet: LocalDateTime, vedtaksperiodeId: UUID) {
            varsler.add(varselkode)
        }

        override fun lagreDefinisjon(id: UUID, varselkode: String, tittel: String, forklaring: String?, handling: String?, avviklet: Boolean, opprettet: LocalDateTime) {
            TODO("Not yet implemented")
        }
    }
}