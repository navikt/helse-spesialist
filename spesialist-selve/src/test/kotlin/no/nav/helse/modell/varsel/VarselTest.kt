package no.nav.helse.modell.varsel

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.modell.varsel.Varsel.Companion.finnEksisterendeVarsel
import no.nav.helse.modell.varsel.Varsel.Companion.forhindrerAutomatisering
import no.nav.helse.modell.varsel.Varsel.Companion.inneholderMedlemskapsvarsel
import no.nav.helse.modell.varsel.Varsel.Status.AKTIV
import no.nav.helse.modell.varsel.Varsel.Status.INAKTIV
import no.nav.helse.modell.vedtaksperiode.IVedtaksperiodeObserver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class VarselTest {

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
        assertEquals(0, observer.reaktiverteVarsler.size)
    }

    @ParameterizedTest
    @EnumSource(value = Varsel.Status::class, names = ["AKTIV"], mode = EnumSource.Mode.EXCLUDE)
    fun `kan ikke deaktivere varsel som ikke er aktivt`(status: Varsel.Status) {
        val varsel = nyttVarsel(status = status)
        val enGenerasjonId = UUID.randomUUID()
        varsel.deaktiver(enGenerasjonId)
        assertEquals(0, observer.deaktiverteVarsler.size)
    }

    @ParameterizedTest
    @EnumSource(value = Varsel.Status::class, names = ["AKTIV", "VURDERT"], mode = EnumSource.Mode.EXCLUDE)
    fun `forhindrer ikke automatisering`(status: Varsel.Status) {
        val varsel = nyttVarsel(status = status)
        assertFalse(listOf(varsel).forhindrerAutomatisering())
    }

    @ParameterizedTest
    @EnumSource(value = Varsel.Status::class, names = ["AKTIV", "VURDERT"])
    fun `forhindrer automatisering`(status: Varsel.Status) {
        val varsel = nyttVarsel(status = status)
        assertTrue(listOf(varsel).forhindrerAutomatisering())
    }

    @ParameterizedTest
    @EnumSource(value = Varsel.Status::class, names = ["AKTIV"], mode = EnumSource.Mode.EXCLUDE)
    fun `inneholder ikke medlemskapsvarsel`(status: Varsel.Status) {
        val varsel = nyttVarsel(status = status, kode = "RV_MV_1")
        assertFalse(listOf(varsel).inneholderMedlemskapsvarsel())
    }

    @Test
    fun `inneholder medlemskapsvarsel`() {
        val varsel = nyttVarsel(status = AKTIV, kode = "RV_MV_1")
        assertTrue(listOf(varsel).inneholderMedlemskapsvarsel())
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

        override fun varselOpprettet(varselId: UUID, vedtaksperiodeId: UUID, generasjonId: UUID, varselkode: String, opprettet: LocalDateTime) {
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