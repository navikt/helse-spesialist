package no.nav.helse.modell.person

import no.nav.helse.modell.person.vedtaksperiode.Varsel
import no.nav.helse.modell.person.vedtaksperiode.Varsel.Companion.finnEksisterendeVarsel
import no.nav.helse.modell.person.vedtaksperiode.Varsel.Companion.forhindrerAutomatisering
import no.nav.helse.modell.person.vedtaksperiode.Varsel.Companion.inneholderMedlemskapsvarsel
import no.nav.helse.modell.person.vedtaksperiode.Varsel.Companion.inneholderSvartelistedeVarsler
import no.nav.helse.modell.person.vedtaksperiode.Varsel.Companion.inneholderVarselOmNegativtBeløp
import no.nav.helse.modell.person.vedtaksperiode.VarselDto
import no.nav.helse.modell.person.vedtaksperiode.VarselStatusDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDateTime
import java.util.UUID

internal class VarselTest {
    @Test
    fun lagre() {
        val varselId = UUID.randomUUID()
        val opprettet = LocalDateTime.now()
        val vedtaksperiodeId = UUID.randomUUID()
        val varsel = Varsel(varselId, "EN_KODE", opprettet, vedtaksperiodeId)
        varsel.assertOpprettelse(vedtaksperiodeId, varselId, "EN_KODE", opprettet)
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
    fun `finner eksisterende varsel basert på varselkode`() {
        val varsel = nyttVarsel()
        val funnetVarsel = listOf(varsel).finnEksisterendeVarsel("EN_KODE")
        assertEquals(varsel, funnetVarsel)
    }

    @Test
    fun `kan reaktivere varsel`() {
        val varselId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val varsel = Varsel(varselId, "EN_KODE", LocalDateTime.now(), vedtaksperiodeId, Varsel.Status.INAKTIV)
        varsel.reaktiver()
        varsel.assertAktivt()
    }

    @Test
    fun `kan reaktivere deaktivert varsel`() {
        val varsel = nyttVarsel()
        varsel.deaktiver()
        varsel.reaktiver()
        varsel.assertAktivt()
    }

    @Test
    fun `kan deaktivere varsel`() {
        val varselId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val varsel = Varsel(varselId, "EN_KODE", LocalDateTime.now(), vedtaksperiodeId, Varsel.Status.AKTIV)
        varsel.deaktiver()
        varsel.assertDeaktivert()
    }

    @ParameterizedTest
    @EnumSource(value = Varsel.Status::class, names = ["INAKTIV"], mode = EnumSource.Mode.EXCLUDE)
    fun `kan ikke reaktivere varsel som ikke er inaktivt`(status: Varsel.Status) {
        val varsel = nyttVarsel(status = status)
        varsel.reaktiver()
        varsel.assertStatus(status.toDto())
    }

    @ParameterizedTest
    @EnumSource(value = Varsel.Status::class, names = ["AKTIV"], mode = EnumSource.Mode.EXCLUDE)
    fun `kan ikke deaktivere varsel som ikke er aktivt`(status: Varsel.Status) {
        val varsel = nyttVarsel(status = status)
        varsel.deaktiver()
        varsel.assertStatus(status.toDto())
    }

    @ParameterizedTest
    @EnumSource(value = Varsel.Status::class, names = ["AKTIV", "VURDERT", "AVVIST"], mode = EnumSource.Mode.EXCLUDE)
    fun `forhindrer ikke automatisering`(status: Varsel.Status) {
        val varsel = nyttVarsel(status = status)
        assertFalse(listOf(varsel).forhindrerAutomatisering())
    }

    @ParameterizedTest
    @EnumSource(value = Varsel.Status::class, names = ["AKTIV", "VURDERT", "AVVIST"])
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
        val varsel = nyttVarsel(status = Varsel.Status.AKTIV, kode = "RV_MV_1")
        assertTrue(listOf(varsel).inneholderMedlemskapsvarsel())
    }

    @ParameterizedTest
    @EnumSource(value = Varsel.Status::class, names = ["AKTIV"], mode = EnumSource.Mode.EXCLUDE)
    fun `inneholder ikke varsel om negativt beløp`(status: Varsel.Status) {
        val varsel = nyttVarsel(status = status, kode = "RV_UT_23")
        assertFalse(listOf(varsel).inneholderVarselOmNegativtBeløp())
    }

    @ParameterizedTest
    @ValueSource(strings = ["RV_IT_3", "RV_SI_3", "RV_UT_23", "RV_VV_8", "SB_RV_2"])
    fun `inneholder svartelistet varsel`(varselkode: String) {
        val varsel = nyttVarsel(status = Varsel.Status.AKTIV, kode = varselkode)
        assertTrue(listOf(varsel).inneholderSvartelistedeVarsler())
    }

    @ParameterizedTest
    @EnumSource(value = Varsel.Status::class)
    fun `inneholder svartelistet varsel uavhengig av status`(status: Varsel.Status) {
        val varsel = nyttVarsel(status = status, kode = "RV_IT_3")
        assertTrue(listOf(varsel).inneholderSvartelistedeVarsler())
    }

    @Test
    fun `inneholder varsel om negativt beløp`() {
        val varsel = nyttVarsel(status = Varsel.Status.AKTIV, kode = "RV_UT_23")
        assertTrue(listOf(varsel).inneholderVarselOmNegativtBeløp())
    }

    @Test
    fun `godkjenn spesialsakvarsel`() {
        val varselId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val varsel = nyttVarsel(varselId = varselId, vedtaksperiodeId = vedtaksperiodeId, status = Varsel.Status.AKTIV, kode = "RV_UT_23")
        varsel.godkjennSpesialsakvarsel()
        val godkjentVarsel = varsel.toDto()
        assertEquals(varselId, godkjentVarsel.id)
        assertEquals("RV_UT_23", godkjentVarsel.varselkode)
        assertEquals(vedtaksperiodeId, godkjentVarsel.vedtaksperiodeId)
    }

    @Test
    fun `varsel toDto`() {
        val varselId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val opprettet = LocalDateTime.now()
        val varsel = Varsel(varselId, "SB_EX_1", opprettet, vedtaksperiodeId, Varsel.Status.AKTIV)
        val dto = varsel.toDto()
        assertEquals(VarselDto(varselId, "SB_EX_1", opprettet, vedtaksperiodeId, VarselStatusDto.AKTIV), dto)
    }

    @Test
    fun `varselStatus toDto`() {
        assertEquals(VarselStatusDto.AKTIV, Varsel.Status.AKTIV.toDto())
        assertEquals(VarselStatusDto.INAKTIV, Varsel.Status.INAKTIV.toDto())
        assertEquals(VarselStatusDto.VURDERT, Varsel.Status.VURDERT.toDto())
        assertEquals(VarselStatusDto.GODKJENT, Varsel.Status.GODKJENT.toDto())
        assertEquals(VarselStatusDto.AVVIST, Varsel.Status.AVVIST.toDto())
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
            varsel.hashCode(),
        )
    }

    private fun nyttVarsel(
        varselId: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        kode: String = "EN_KODE",
        status: Varsel.Status = Varsel.Status.AKTIV,
    ): Varsel {
        return Varsel(varselId, kode, LocalDateTime.now(), vedtaksperiodeId, status)
    }

    private fun Varsel.assertOpprettelse(
        forventetVedtaksperiodeId: UUID,
        forventetVarselId: UUID,
        forventetVarselkode: String,
        forventetOpprettet: LocalDateTime,
    ) {
        val varsel = this.toDto()
        assertEquals(forventetVedtaksperiodeId, varsel.vedtaksperiodeId)
        assertEquals(forventetVarselkode, varsel.varselkode)
        assertEquals(forventetVarselId, varsel.id)
        assertEquals(forventetOpprettet, varsel.opprettet)
    }

    private fun Varsel.assertStatus(statusDto: VarselStatusDto) {
        val varsel = this.toDto()
        assertEquals(statusDto, varsel.status)
    }

    private fun Varsel.assertAktivt() {
        val varsel = this.toDto()
        assertEquals(VarselStatusDto.AKTIV, varsel.status)
    }

    private fun Varsel.assertDeaktivert() {
        val varsel = this.toDto()
        assertEquals(VarselStatusDto.INAKTIV, varsel.status)
    }
}
