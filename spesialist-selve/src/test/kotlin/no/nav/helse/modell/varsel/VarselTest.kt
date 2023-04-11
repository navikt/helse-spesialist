package no.nav.helse.modell.varsel

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.modell.varsel.Varsel.Companion.finnEksisterendeVarsel
import no.nav.helse.modell.varsel.Varsel.Status.INAKTIV
import no.nav.helse.modell.varsel.Varsel.Status.VURDERT
import no.nav.helse.modell.vedtaksperiode.IVedtaksperiodeObserver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class VarselTest {

    private val varsler = mutableListOf<String>()
    private val godkjenteVarsler = mutableListOf<String>()
    private val avvisteVarsler = mutableListOf<String>()
    private val deaktiverteVarsler = mutableListOf<String>()
    private val reaktiverteVarsler = mutableListOf<String>()

    @BeforeEach
    internal fun beforeEach() {
        varsler.clear()
        godkjenteVarsler.clear()
        avvisteVarsler.clear()
        deaktiverteVarsler.clear()
    }

    @Test
    fun lagre() {
        val varselId = UUID.randomUUID()
        val opprettet = LocalDateTime.now()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val varsel = Varsel(varselId, "EN_KODE", opprettet, vedtaksperiodeId)
        varsel.registrer(observer)
        varsel.opprett(generasjonId)
        observer.assertOpprettelse(vedtaksperiodeId, generasjonId, varselId, "EN_KODE", opprettet)
    }

    @Test
    fun `er relevant`() {
        val varselId = UUID.randomUUID()
        val opprettet = LocalDateTime.now()
        val vedtaksperiodeId = UUID.randomUUID()
        val varsel = Varsel(varselId, "EN_KODE", opprettet, vedtaksperiodeId)
        assertTrue(varsel.erRelevantFor(vedtaksperiodeId))
        assertFalse(varsel.erRelevantFor(UUID.randomUUID()))
    }

    @Test
    fun `kan godkjenne aktivt varsel`() {
        val varsel = Varsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), UUID.randomUUID())
        varsel.godkjennFor(UUID.randomUUID(), "EN_IDENT", varselRepository)
        assertEquals(1, godkjenteVarsler.size)
    }

    @Test
    fun `kan godkjenne vurdert varsel`() {
        val varsel = Varsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), UUID.randomUUID(), status = VURDERT)
        varsel.godkjennFor(UUID.randomUUID(), "EN_IDENT", varselRepository)
        assertEquals(1, godkjenteVarsler.size)
    }

    @Test
    fun `kan avvise aktivt varsel`() {
        val varsel = Varsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), UUID.randomUUID())
        varsel.avvisFor(UUID.randomUUID(), "EN_IDENT", varselRepository)
        assertEquals(1, avvisteVarsler.size)
    }

    @Test
    fun `kan deaktivere aktivt varsel`() {
        val varsel = Varsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), UUID.randomUUID())
        varsel.deaktiverFor(UUID.randomUUID(), varselRepository)
        assertEquals(1, deaktiverteVarsler.size)
    }

    @Test
    fun `kan ikke avvise godkjent varsel`() {
        val varsel = Varsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), UUID.randomUUID())
        val enGenerasjonId = UUID.randomUUID()
        varsel.godkjennFor(enGenerasjonId, "EN_IDENT", varselRepository)
        varsel.avvisFor(enGenerasjonId, "EN_IDENT", varselRepository)
        assertEquals(1, godkjenteVarsler.size)
        assertEquals(0, avvisteVarsler.size)
    }

    @Test
    fun `kan ikke deaktivere godkjent varsel`() {
        val varsel = Varsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), UUID.randomUUID())
        val enGenerasjonId = UUID.randomUUID()
        varsel.godkjennFor(enGenerasjonId, "EN_IDENT", varselRepository)
        varsel.deaktiverFor(enGenerasjonId, varselRepository)
        assertEquals(1, godkjenteVarsler.size)
        assertEquals(0, deaktiverteVarsler.size)
    }

    @Test
    fun `kan ikke godkjenne avvist varsel`() {
        val varsel = Varsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), UUID.randomUUID())
        val enGenerasjonId = UUID.randomUUID()
        varsel.avvisFor(enGenerasjonId, "EN_IDENT", varselRepository)
        varsel.godkjennFor(enGenerasjonId, "EN_IDENT", varselRepository)
        assertEquals(1, avvisteVarsler.size)
        assertEquals(0, godkjenteVarsler.size)
    }

    @Test
    fun `kan ikke deaktivere avvist varsel`() {
        val varsel = Varsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), UUID.randomUUID())
        val enGenerasjonId = UUID.randomUUID()
        varsel.avvisFor(enGenerasjonId, "EN_IDENT", varselRepository)
        varsel.deaktiverFor(enGenerasjonId, varselRepository)
        assertEquals(1, avvisteVarsler.size)
        assertEquals(0, deaktiverteVarsler.size)
    }

    @Test
    fun `kan ikke godkjenne deaktivert varsel`() {
        val varsel = Varsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), UUID.randomUUID())
        val enGenerasjonId = UUID.randomUUID()
        varsel.deaktiverFor(enGenerasjonId, varselRepository)
        varsel.godkjennFor(enGenerasjonId, "EN_IDENT", varselRepository)
        assertEquals(1, deaktiverteVarsler.size)
        assertEquals(0, godkjenteVarsler.size)
    }

    @Test
    fun `kan ikke avvise deaktivert varsel`() {
        val varsel = Varsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), UUID.randomUUID())
        val enGenerasjonId = UUID.randomUUID()
        varsel.deaktiverFor(enGenerasjonId, varselRepository)
        varsel.avvisFor(enGenerasjonId, "EN_IDENT", varselRepository)
        assertEquals(1, deaktiverteVarsler.size)
        assertEquals(0, avvisteVarsler.size)
    }

    @Test
    fun `kan reaktivere deaktivert varsel`() {
        val varsel = Varsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), UUID.randomUUID())
        val enGenerasjonId = UUID.randomUUID()
        varsel.deaktiverFor(enGenerasjonId, varselRepository)
        varsel.reaktiverFor(enGenerasjonId, varselRepository)
        assertEquals(1, deaktiverteVarsler.size)
        assertEquals(1, reaktiverteVarsler.size)
    }

    @Test
    fun `finner eksisterende varsel`() {
        val varsel = Varsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), UUID.randomUUID())
        val funnetVarsel = listOf(varsel).finnEksisterendeVarsel(varsel)
        assertEquals(varsel, funnetVarsel)
    }

    @Test
    fun `kan reaktivere varsel`() {
        val varselId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val varsel = Varsel(varselId, "EN_KODE", LocalDateTime.now(), vedtaksperiodeId, INAKTIV)
        varsel.registrer(observer)
        varsel.reaktiver(generasjonId)
        observer.assertReaktivering(vedtaksperiodeId, generasjonId, varselId, "EN_KODE")
    }
    @Test
    fun `reaktiverer ikke varsel som er aktivt`() {
        val varselId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val varsel = Varsel(varselId, "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        varsel.registrer(observer)
        varsel.reaktiver(generasjonId)
        assertEquals(0, observer.reaktiverteVarsler.size)
    }

    @Test
    fun `kan ikke reaktivere varsel som ikke er inaktivt`() {
        val varsel = Varsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), UUID.randomUUID())
        val enGenerasjonId = UUID.randomUUID()
        varsel.reaktiverFor(enGenerasjonId, varselRepository)
        assertEquals(0, deaktiverteVarsler.size)
        assertEquals(0, reaktiverteVarsler.size)
    }

    @Test
    fun equals() {
        val varselId = UUID.randomUUID()
        val opprettet = LocalDateTime.now()
        val vedtaksperiodeId = UUID.randomUUID()
        val varsel = Varsel(varselId, "EN_KODE", opprettet, vedtaksperiodeId)
        assertEquals(varsel, varsel)
        assertEquals(varsel.hashCode(), varsel.hashCode())
        assertEquals(Varsel(varselId, "EN_KODE", opprettet, vedtaksperiodeId), varsel)
        assertEquals(Varsel(varselId, "EN_KODE", opprettet, vedtaksperiodeId).hashCode(), varsel.hashCode())
    }

    @Test
    fun `not equals`() {
        val varsel = Varsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), UUID.randomUUID())
        assertNotEquals(Varsel(UUID.randomUUID(), "EN_ANNEN_KODE", LocalDateTime.now(), UUID.randomUUID()), varsel)
        assertNotEquals(
            Varsel(UUID.randomUUID(), "EN_ANNEN_KODE", LocalDateTime.now(), UUID.randomUUID()).hashCode(),
            varsel.hashCode()
        )
    }

    private val varselRepository = object : VarselRepository {
        override fun deaktiverFor(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String, definisjonId: UUID?) {
            deaktiverteVarsler.add(varselkode)
        }

        override fun reaktiverFor(vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String) {
            reaktiverteVarsler.add(varselkode)
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

        override fun lagreDefinisjon(id: UUID, varselkode: String, tittel: String, forklaring: String?, handling: String?, avviklet: Boolean, opprettet: LocalDateTime): Unit = TODO("Not yet implemented")
        override fun oppdaterGenerasjonFor(id: UUID, gammelGenerasjonId: UUID, nyGenerasjonId: UUID) {
            TODO("Not yet implemented")
        }
    }

    private val observer = object : IVedtaksperiodeObserver {

        val opprettedeVarsler = mutableMapOf<UUID, Opprettelse>()
        val reaktiverteVarsler = mutableMapOf<UUID, Reaktivering>()


        private inner class Opprettelse(
            val vedtaksperiodeId: UUID,
            val generasjonId: UUID,
            val varselId: UUID,
            val varselkode: String,
            val opprettet: LocalDateTime,
        )
        private inner class Reaktivering(
            val vedtaksperiodeId: UUID,
            val generasjonId: UUID,
            val varselId: UUID,
            val varselkode: String
        )

        override fun varselReaktivert(varselId: UUID, varselkode: String, generasjonId: UUID, vedtaksperiodeId: UUID) {
            reaktiverteVarsler[varselId] = Reaktivering(vedtaksperiodeId, generasjonId, varselId, varselkode)
        }

        override fun varselOpprettet(
            vedtaksperiodeId: UUID,
            generasjonId: UUID,
            varselId: UUID,
            varselkode: String,
            opprettet: LocalDateTime,
        ) {
            opprettedeVarsler[varselId] = Opprettelse(vedtaksperiodeId, generasjonId, varselId, varselkode, opprettet)
        }

        fun assertOpprettelse(
            forventetVedtaksperiodeId: UUID,
            forventetGenerasjonId: UUID,
            forventetVarselId: UUID,
            forventetVarselkode: String,
            forventetOpprettet: LocalDateTime
        ) {
            val opprettelse = opprettedeVarsler[forventetVarselId]
            assertEquals(forventetVedtaksperiodeId, opprettelse?.vedtaksperiodeId)
            assertEquals(forventetGenerasjonId, opprettelse?.generasjonId)
            assertEquals(forventetVarselkode, opprettelse?.varselkode)
            assertEquals(forventetVarselId, opprettelse?.varselId)
            assertEquals(forventetOpprettet, opprettelse?.opprettet)
        }

        fun assertReaktivering(
            forventetVedtaksperiodeId: UUID,
            forventetGenerasjonId: UUID,
            forventetVarselId: UUID,
            forventetVarselkode: String,
        ) {
            val reaktivering = reaktiverteVarsler[forventetVarselId]
            assertEquals(forventetVedtaksperiodeId, reaktivering?.vedtaksperiodeId)
            assertEquals(forventetGenerasjonId, reaktivering?.generasjonId)
            assertEquals(forventetVarselkode, reaktivering?.varselkode)
            assertEquals(forventetVarselId, reaktivering?.varselId)
        }
    }
}