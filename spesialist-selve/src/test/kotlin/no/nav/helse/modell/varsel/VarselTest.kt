package no.nav.helse.modell.varsel

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.modell.varsel.Varsel.Companion.finnEksisterendeVarsel
import no.nav.helse.modell.varsel.Varsel.Status.AKTIV
import no.nav.helse.modell.varsel.Varsel.Status.INAKTIV
import no.nav.helse.modell.varsel.Varsel.Status.VURDERT
import no.nav.helse.modell.vedtaksperiode.IVedtaksperiodeObserver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

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
        val varsel = nyttVarsel()
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
        val varsel = nyttVarsel()
        varsel.avvisFor(UUID.randomUUID(), "EN_IDENT", varselRepository)
        assertEquals(1, avvisteVarsler.size)
    }

    @Test
    fun `kan ikke avvise godkjent varsel`() {
        val varsel = nyttVarsel()
        val enGenerasjonId = UUID.randomUUID()
        varsel.godkjennFor(enGenerasjonId, "EN_IDENT", varselRepository)
        varsel.avvisFor(enGenerasjonId, "EN_IDENT", varselRepository)
        assertEquals(1, godkjenteVarsler.size)
        assertEquals(0, avvisteVarsler.size)
    }

    @Test
    fun `kan ikke godkjenne avvist varsel`() {
        val varsel = nyttVarsel()
        val enGenerasjonId = UUID.randomUUID()
        varsel.avvisFor(enGenerasjonId, "EN_IDENT", varselRepository)
        varsel.godkjennFor(enGenerasjonId, "EN_IDENT", varselRepository)
        assertEquals(1, avvisteVarsler.size)
        assertEquals(0, godkjenteVarsler.size)
    }

    @Test
    fun `kan ikke godkjenne deaktivert varsel`() {
        val varsel = nyttVarsel()
        varsel.registrer(observer)
        val enGenerasjonId = UUID.randomUUID()
        varsel.deaktiver(enGenerasjonId)
        varsel.godkjennFor(enGenerasjonId, "EN_IDENT", varselRepository)
        assertEquals(1, observer.deaktiverteVarsler.size)
        assertEquals(0, godkjenteVarsler.size)
    }

    @Test
    fun `kan ikke avvise deaktivert varsel`() {
        val varsel = nyttVarsel()
        varsel.registrer(observer)
        val enGenerasjonId = UUID.randomUUID()
        varsel.deaktiver(enGenerasjonId)
        varsel.avvisFor(enGenerasjonId, "EN_IDENT", varselRepository)
        assertEquals(1, observer.deaktiverteVarsler.size)
        assertEquals(0, avvisteVarsler.size)
    }

    @Test
    fun `finner eksisterende varsel`() {
        val varsel = nyttVarsel()
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
    fun `kan reaktivere deaktivert varsel`() {
        val varsel = nyttVarsel()
        varsel.registrer(observer)
        val enGenerasjonId = UUID.randomUUID()
        varsel.deaktiver(enGenerasjonId)
        varsel.reaktiver(enGenerasjonId)
        assertEquals(1, observer.deaktiverteVarsler.size)
        assertEquals(1, observer.reaktiverteVarsler.size)
    }

    @Test
    fun `kan deaktivere varsel`() {
        val varselId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val varsel = Varsel(varselId, "EN_KODE", LocalDateTime.now(), vedtaksperiodeId, AKTIV)
        varsel.registrer(observer)
        varsel.deaktiver(generasjonId)
        observer.assertDeaktivering(vedtaksperiodeId, generasjonId, varselId, "EN_KODE")
    }

    @ParameterizedTest
    @EnumSource(value = Varsel.Status::class, names = ["INAKTIV"], mode = EnumSource.Mode.EXCLUDE)
    fun `kan ikke reaktivere varsel som ikke er inaktivt`(status: Varsel.Status) {
        val varsel = nyttVarsel(status = status)
        val enGenerasjonId = UUID.randomUUID()
        varsel.reaktiver(enGenerasjonId)
        assertEquals(0, reaktiverteVarsler.size)
    }

    @ParameterizedTest
    @EnumSource(value = Varsel.Status::class, names = ["AKTIV"], mode = EnumSource.Mode.EXCLUDE)
    fun `kan ikke deaktivere varsel som ikke er aktivt`(status: Varsel.Status) {
        val varsel = nyttVarsel(status = status)
        val enGenerasjonId = UUID.randomUUID()
        varsel.deaktiver(enGenerasjonId)
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
        val varsel = nyttVarsel()
        assertNotEquals(nyttVarsel(kode = "EN_ANNEN_KODE"), varsel)
        assertNotEquals(
            Varsel(UUID.randomUUID(), "EN_ANNEN_KODE", LocalDateTime.now(), UUID.randomUUID()).hashCode(),
            varsel.hashCode()
        )
    }

    private fun nyttVarsel(kode: String = "EN_KODE", status: Varsel.Status = AKTIV): Varsel {
        return Varsel(UUID.randomUUID(), kode, LocalDateTime.now(), UUID.randomUUID(), status).also {
            it.registrer(observer)
        }
    }

    private val varselRepository = object : VarselRepository {

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
        val deaktiverteVarsler = mutableMapOf<UUID, Deaktivering>()

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
        private inner class Deaktivering(
            val vedtaksperiodeId: UUID,
            val generasjonId: UUID,
            val varselId: UUID,
            val varselkode: String
        )

        override fun varselReaktivert(varselId: UUID, varselkode: String, generasjonId: UUID, vedtaksperiodeId: UUID) {
            reaktiverteVarsler[varselId] = Reaktivering(vedtaksperiodeId, generasjonId, varselId, varselkode)
        }

        override fun varselDeaktivert(varselId: UUID, varselkode: String, generasjonId: UUID, vedtaksperiodeId: UUID) {
            deaktiverteVarsler[varselId] = Deaktivering(vedtaksperiodeId, generasjonId, varselId, varselkode)
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

        fun assertDeaktivering(
            forventetVedtaksperiodeId: UUID,
            forventetGenerasjonId: UUID,
            forventetVarselId: UUID,
            forventetVarselkode: String,
        ) {
            val deaktivering = deaktiverteVarsler[forventetVarselId]
            assertEquals(forventetVedtaksperiodeId, deaktivering?.vedtaksperiodeId)
            assertEquals(forventetGenerasjonId, deaktivering?.generasjonId)
            assertEquals(forventetVarselkode, deaktivering?.varselkode)
            assertEquals(forventetVarselId, deaktivering?.varselId)
        }
    }
}