package no.nav.helse.modell.varsel

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.modell.varsel.Varsel.Companion.finnEksisterendeVarsel
import no.nav.helse.modell.varsel.Varsel.Companion.forhindrerAutomatisering
import no.nav.helse.modell.varsel.Varsel.Companion.inneholderMedlemskapsvarsel
import no.nav.helse.modell.varsel.Varsel.Companion.inneholderSvartelistedeVarsler
import no.nav.helse.modell.varsel.Varsel.Companion.inneholderVarselOmNegativtBeløp
import no.nav.helse.modell.varsel.Varsel.Status
import no.nav.helse.modell.varsel.Varsel.Status.AKTIV
import no.nav.helse.modell.varsel.Varsel.Status.AVVIST
import no.nav.helse.modell.varsel.Varsel.Status.GODKJENT
import no.nav.helse.modell.varsel.Varsel.Status.INAKTIV
import no.nav.helse.modell.varsel.Varsel.Status.VURDERT
import no.nav.helse.modell.vedtaksperiode.IVedtaksperiodeObserver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource

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
    @EnumSource(value = Status::class, names = ["INAKTIV"], mode = EnumSource.Mode.EXCLUDE)
    fun `kan ikke reaktivere varsel som ikke er inaktivt`(status: Status) {
        val varsel = nyttVarsel(status = status)
        val enGenerasjonId = UUID.randomUUID()
        varsel.reaktiver(enGenerasjonId)
        assertEquals(0, observer.reaktiverteVarsler.size)
    }

    @ParameterizedTest
    @EnumSource(value = Status::class, names = ["AKTIV"], mode = EnumSource.Mode.EXCLUDE)
    fun `kan ikke deaktivere varsel som ikke er aktivt`(status: Status) {
        val varsel = nyttVarsel(status = status)
        val enGenerasjonId = UUID.randomUUID()
        varsel.deaktiver(enGenerasjonId)
        assertEquals(0, observer.deaktiverteVarsler.size)
    }

    @ParameterizedTest
    @EnumSource(value = Status::class, names = ["AKTIV", "VURDERT", "AVVIST"], mode = EnumSource.Mode.EXCLUDE)
    fun `forhindrer ikke automatisering`(status: Status) {
        val varsel = nyttVarsel(status = status)
        assertFalse(listOf(varsel).forhindrerAutomatisering())
    }

    @ParameterizedTest
    @EnumSource(value = Status::class, names = ["AKTIV", "VURDERT", "AVVIST"])
    fun `forhindrer automatisering`(status: Status) {
        val varsel = nyttVarsel(status = status)
        assertTrue(listOf(varsel).forhindrerAutomatisering())
    }

    @ParameterizedTest
    @EnumSource(value = Status::class, names = ["AKTIV"], mode = EnumSource.Mode.EXCLUDE)
    fun `inneholder ikke medlemskapsvarsel`(status: Status) {
        val varsel = nyttVarsel(status = status, kode = "RV_MV_1")
        assertFalse(listOf(varsel).inneholderMedlemskapsvarsel())
    }

    @Test
    fun `inneholder medlemskapsvarsel`() {
        val varsel = nyttVarsel(status = AKTIV, kode = "RV_MV_1")
        assertTrue(listOf(varsel).inneholderMedlemskapsvarsel())
    }

    @ParameterizedTest
    @EnumSource(value = Status::class, names = ["AKTIV"], mode = EnumSource.Mode.EXCLUDE)
    fun `inneholder ikke varsel om negativt beløp`(status: Status) {
        val varsel = nyttVarsel(status = status, kode = "RV_UT_23")
        assertFalse(listOf(varsel).inneholderVarselOmNegativtBeløp())
    }

    @ParameterizedTest
    @ValueSource(strings = ["RV_IT_3", "RV_OS_2", "RV_OS_3", "RV_SI_3", "RV_UT_21", "RV_UT_23", "RV_VV_8", "SB_RV_2"])
    fun `inneholder svartelistet varsel`(varselkode: String) {
        val varsel = nyttVarsel(status = AKTIV, kode = varselkode)
        assertTrue(listOf(varsel).inneholderSvartelistedeVarsler())
    }

    @ParameterizedTest
    @EnumSource(value = Status::class)
    fun `inneholder svartelistet varsel uavhengig av status`(status: Status) {
        val varsel = nyttVarsel(status = status, kode = "RV_IT_3")
        assertTrue(listOf(varsel).inneholderSvartelistedeVarsler())
    }

    @Test
    fun `inneholder varsel om negativt beløp`() {
        val varsel = nyttVarsel(status = AKTIV, kode = "RV_UT_23")
        assertTrue(listOf(varsel).inneholderVarselOmNegativtBeløp())
    }

    @Test
    fun `godkjenn spesialsakvarsel`() {
        val varselId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val varsel = nyttVarsel(varselId = varselId, vedtaksperiodeId = vedtaksperiodeId, status = AKTIV, kode = "RV_UT_23")
        varsel.godkjennSpesialsakvarsel(generasjonId)
        val godkjentVarsel = observer.godkjenteVarsler[varselId]
        assertEquals(varselId, godkjentVarsel?.varselId)
        assertEquals(generasjonId, godkjentVarsel?.generasjonId)
        assertEquals("Automatisk godkjent - spesialsak", godkjentVarsel?.statusEndretAv)
        assertEquals("RV_UT_23", godkjentVarsel?.varselkode)
        assertEquals(vedtaksperiodeId, godkjentVarsel?.vedtaksperiodeId)
    }

    @Test
    fun `varsel toDto`() {
        val varselId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val opprettet = LocalDateTime.now()
        val varsel = Varsel(varselId, "SB_EX_1", opprettet, vedtaksperiodeId, AKTIV)
        val dto = varsel.toDto()
        assertEquals(VarselDto(varselId, "SB_EX_1", opprettet, vedtaksperiodeId, VarselStatusDto.AKTIV), dto)
    }

    @Test
    fun `varselStatus toDto`() {
        assertEquals(VarselStatusDto.AKTIV, AKTIV.toDto())
        assertEquals(VarselStatusDto.INAKTIV, INAKTIV.toDto())
        assertEquals(VarselStatusDto.VURDERT, VURDERT.toDto())
        assertEquals(VarselStatusDto.GODKJENT, GODKJENT.toDto())
        assertEquals(VarselStatusDto.AVVIST, AVVIST.toDto())
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

    private fun nyttVarsel(varselId: UUID = UUID.randomUUID(), vedtaksperiodeId: UUID = UUID.randomUUID(), kode: String = "EN_KODE", status: Status = AKTIV): Varsel {
        return Varsel(varselId, kode, LocalDateTime.now(), vedtaksperiodeId, status).also {
            it.registrer(observer)
        }
    }

    private val observer = object : IVedtaksperiodeObserver {

        val opprettedeVarsler = mutableMapOf<UUID, Opprettelse>()
        val reaktiverteVarsler = mutableMapOf<UUID, Reaktivering>()
        val deaktiverteVarsler = mutableMapOf<UUID, Deaktivering>()
        val godkjenteVarsler = mutableMapOf<UUID, Godkjent>()

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
            val varselkode: String,
        )

        private inner class Deaktivering(
            val vedtaksperiodeId: UUID,
            val generasjonId: UUID,
            val varselId: UUID,
            val varselkode: String,
        )

        private inner class Godkjent(
            val vedtaksperiodeId: UUID,
            val generasjonId: UUID,
            val varselId: UUID,
            val varselkode: String,
            val statusEndretAv: String,
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

        override fun varselGodkjent(
            varselId: UUID,
            varselkode: String,
            generasjonId: UUID,
            vedtaksperiodeId: UUID,
            statusEndretAv: String
        ) {
            godkjenteVarsler[varselId] = Godkjent(vedtaksperiodeId, generasjonId, varselId, varselkode, statusEndretAv)
        }

        fun assertOpprettelse(
            forventetVedtaksperiodeId: UUID,
            forventetGenerasjonId: UUID,
            forventetVarselId: UUID,
            forventetVarselkode: String,
            forventetOpprettet: LocalDateTime,
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