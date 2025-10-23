package no.nav.helse.modell.person

import no.nav.helse.modell.person.vedtaksperiode.LegacyVarsel
import no.nav.helse.modell.person.vedtaksperiode.LegacyVarsel.Companion.finnEksisterendeVarsel
import no.nav.helse.modell.person.vedtaksperiode.LegacyVarsel.Companion.forhindrerAutomatisering
import no.nav.helse.modell.person.vedtaksperiode.LegacyVarsel.Companion.inneholderMedlemskapsvarsel
import no.nav.helse.modell.person.vedtaksperiode.LegacyVarsel.Companion.inneholderVarselOmNegativtBeløp
import no.nav.helse.modell.person.vedtaksperiode.VarselDto
import no.nav.helse.modell.person.vedtaksperiode.VarselStatusDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDateTime
import java.util.UUID

internal class LegacyVarselTest {
    @Test
    fun lagre() {
        val varselId = UUID.randomUUID()
        val opprettet = LocalDateTime.now()
        val vedtaksperiodeId = UUID.randomUUID()
        val varsel = LegacyVarsel(varselId, "EN_KODE", opprettet, vedtaksperiodeId)
        varsel.assertOpprettelse(vedtaksperiodeId, varselId, "EN_KODE", opprettet)
    }

    @Test
    fun `er relevant`() {
        val varselId = UUID.randomUUID()
        val opprettet = LocalDateTime.now()
        val vedtaksperiodeId = UUID.randomUUID()
        val varsel = LegacyVarsel(varselId, "EN_KODE", opprettet, vedtaksperiodeId)
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
        val varsel = LegacyVarsel(varselId, "EN_KODE", LocalDateTime.now(), vedtaksperiodeId, LegacyVarsel.Status.INAKTIV)
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
        val varsel = LegacyVarsel(varselId, "EN_KODE", LocalDateTime.now(), vedtaksperiodeId, LegacyVarsel.Status.AKTIV)
        varsel.deaktiver()
        varsel.assertDeaktivert()
    }

    @ParameterizedTest
    @EnumSource(value = LegacyVarsel.Status::class, names = ["INAKTIV"], mode = EnumSource.Mode.EXCLUDE)
    fun `kan ikke reaktivere varsel som ikke er inaktivt`(status: LegacyVarsel.Status) {
        val varsel = nyttVarsel(status = status)
        varsel.reaktiver()
        varsel.assertStatus(status.toDto())
    }

    @ParameterizedTest
    @EnumSource(value = LegacyVarsel.Status::class, names = ["AKTIV"], mode = EnumSource.Mode.EXCLUDE)
    fun `kan ikke deaktivere varsel som ikke er aktivt`(status: LegacyVarsel.Status) {
        val varsel = nyttVarsel(status = status)
        varsel.deaktiver()
        varsel.assertStatus(status.toDto())
    }

    @ParameterizedTest
    @EnumSource(value = LegacyVarsel.Status::class, names = ["AKTIV", "VURDERT", "AVVIST"], mode = EnumSource.Mode.EXCLUDE)
    fun `forhindrer ikke automatisering`(status: LegacyVarsel.Status) {
        val varsel = nyttVarsel(status = status)
        assertFalse(listOf(varsel).forhindrerAutomatisering())
    }

    @ParameterizedTest
    @EnumSource(value = LegacyVarsel.Status::class, names = ["AKTIV", "VURDERT", "AVVIST"])
    fun `forhindrer automatisering`(status: LegacyVarsel.Status) {
        val varsel = nyttVarsel(status = status)
        assertTrue(listOf(varsel).forhindrerAutomatisering())
    }

    @ParameterizedTest
    @EnumSource(value = LegacyVarsel.Status::class, names = ["AKTIV"], mode = EnumSource.Mode.EXCLUDE)
    fun `inneholder ikke medlemskapsvarsel`(status: LegacyVarsel.Status) {
        val varsel = nyttVarsel(status = status, kode = "RV_MV_1")
        assertFalse(listOf(varsel).inneholderMedlemskapsvarsel())
    }

    @Test
    fun `inneholder medlemskapsvarsel`() {
        val varsel = nyttVarsel(status = LegacyVarsel.Status.AKTIV, kode = "RV_MV_1")
        assertTrue(listOf(varsel).inneholderMedlemskapsvarsel())
    }

    @ParameterizedTest
    @EnumSource(value = LegacyVarsel.Status::class, names = ["AKTIV"], mode = EnumSource.Mode.EXCLUDE)
    fun `inneholder ikke varsel om negativt beløp`(status: LegacyVarsel.Status) {
        val varsel = nyttVarsel(status = status, kode = "RV_UT_23")
        assertFalse(listOf(varsel).inneholderVarselOmNegativtBeløp())
    }

    @Test
    fun `inneholder varsel om negativt beløp`() {
        val varsel = nyttVarsel(status = LegacyVarsel.Status.AKTIV, kode = "RV_UT_23")
        assertTrue(listOf(varsel).inneholderVarselOmNegativtBeløp())
    }

    @Test
    fun `varsel toDto`() {
        val varselId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val opprettet = LocalDateTime.now()
        val varsel = LegacyVarsel(varselId, "SB_EX_1", opprettet, vedtaksperiodeId, LegacyVarsel.Status.AKTIV)
        val dto = varsel.toDto()
        assertEquals(VarselDto(varselId, "SB_EX_1", opprettet, vedtaksperiodeId, VarselStatusDto.AKTIV), dto)
    }

    @Test
    fun `varselStatus toDto`() {
        assertEquals(VarselStatusDto.AKTIV, LegacyVarsel.Status.AKTIV.toDto())
        assertEquals(VarselStatusDto.INAKTIV, LegacyVarsel.Status.INAKTIV.toDto())
        assertEquals(VarselStatusDto.VURDERT, LegacyVarsel.Status.VURDERT.toDto())
        assertEquals(VarselStatusDto.GODKJENT, LegacyVarsel.Status.GODKJENT.toDto())
        assertEquals(VarselStatusDto.AVVIST, LegacyVarsel.Status.AVVIST.toDto())
    }

    @Test
    fun equals() {
        val varselId = UUID.randomUUID()
        val opprettet = LocalDateTime.now()
        val vedtaksperiodeId = UUID.randomUUID()
        val varsel = LegacyVarsel(varselId, "EN_KODE", opprettet, vedtaksperiodeId)
        assertEquals(varsel, varsel)
        assertEquals(varsel.hashCode(), varsel.hashCode())
        assertEquals(LegacyVarsel(varselId, "EN_KODE", opprettet, vedtaksperiodeId), varsel)
        assertEquals(LegacyVarsel(varselId, "EN_KODE", opprettet, vedtaksperiodeId).hashCode(), varsel.hashCode())
    }

    @Test
    fun `not equals`() {
        val varsel = nyttVarsel()
        assertNotEquals(nyttVarsel(kode = "EN_ANNEN_KODE"), varsel)
        assertNotEquals(
            LegacyVarsel(UUID.randomUUID(), "EN_ANNEN_KODE", LocalDateTime.now(), UUID.randomUUID()).hashCode(),
            varsel.hashCode(),
        )
    }

    private fun nyttVarsel(
        varselId: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        kode: String = "EN_KODE",
        status: LegacyVarsel.Status = LegacyVarsel.Status.AKTIV,
    ): LegacyVarsel {
        return LegacyVarsel(varselId, kode, LocalDateTime.now(), vedtaksperiodeId, status)
    }

    private fun LegacyVarsel.assertOpprettelse(
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

    private fun LegacyVarsel.assertStatus(statusDto: VarselStatusDto) {
        val varsel = this.toDto()
        assertEquals(statusDto, varsel.status)
    }

    private fun LegacyVarsel.assertAktivt() {
        val varsel = this.toDto()
        assertEquals(VarselStatusDto.AKTIV, varsel.status)
    }

    private fun LegacyVarsel.assertDeaktivert() {
        val varsel = this.toDto()
        assertEquals(VarselStatusDto.INAKTIV, varsel.status)
    }
}
