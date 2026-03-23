package no.nav.helse.spesialist.application

import no.nav.helse.db.api.VarselDbDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDateTime
import java.util.UUID

internal class LegacyVarselTest {

    @ParameterizedTest
    @EnumSource(value = VarselDbDto.Varselstatus::class, names = ["AKTIV"], mode = EnumSource.Mode.EXCLUDE)
    fun `er ikke aktiv`(status: VarselDbDto.Varselstatus) {
        val varsel = opprettVarsel(status)
        assertFalse(varsel.erAktiv())
    }

    @Test
    fun `er aktiv`() {
        val varsel = opprettVarsel(VarselDbDto.Varselstatus.AKTIV)
        assertTrue(varsel.erAktiv())
    }

    @ParameterizedTest
    @EnumSource(value = VarselDbDto.Varselstatus::class, names = ["VURDERT", "AKTIV"], mode = EnumSource.Mode.EXCLUDE)
    fun `varsel blir ikke godkjent hvis det ikke er vurdert eller aktivt`(status: VarselDbDto.Varselstatus) {
        var nyStatus: VarselDbDto.Varselstatus = status
        val varsel = opprettVarsel(nyStatus)
        var godkjent = false
        varsel.vurder(
            godkjent = true,
            "FNR",
            UUID.randomUUID(),
            UUID.randomUUID(),
            "ident"
        ) { _: String, _: UUID, _: UUID, _: UUID, _: String, _: String, _: VarselDbDto.Varselstatus, gjeldendeStatus: VarselDbDto.Varselstatus, _: String ->
            godkjent = true
            nyStatus = gjeldendeStatus
        }

        assertEquals(false, godkjent)
        assertEquals(status, nyStatus)
    }

    @Test
    fun `godkjenn varsel som er vurdert`() {
        var status: VarselDbDto.Varselstatus = VarselDbDto.Varselstatus.VURDERT
        val varsel = opprettVarsel(status)
        var vurdert = false

        varsel.vurder(
            godkjent = true,
            "FNR",
            UUID.randomUUID(),
            UUID.randomUUID(),
            "ident"
        ) { _: String, _: UUID, _: UUID, _: UUID, _: String, _: String, _: VarselDbDto.Varselstatus, gjeldendeStatus: VarselDbDto.Varselstatus, _: String ->
            vurdert = true
            status = gjeldendeStatus
        }

        assertEquals(true, vurdert)
        assertEquals(VarselDbDto.Varselstatus.GODKJENT, status)
    }

    @Test
    fun `avviser varsel som er aktivt eller vurdert dersom perioden ikke er godkjent`() {
        var status: VarselDbDto.Varselstatus = VarselDbDto.Varselstatus.VURDERT
        val varsel = opprettVarsel(status)
        var vurdert = false
        varsel.vurder(
            godkjent = false,
            "FNR",
            UUID.randomUUID(),
            UUID.randomUUID(),
            "ident"
        ) { _: String, _: UUID, _: UUID, _: UUID, _: String, _: String, _: VarselDbDto.Varselstatus, gjeldendeStatus: VarselDbDto.Varselstatus, _: String ->
            vurdert = true
            status = gjeldendeStatus
        }

        assertEquals(true, vurdert)
        assertEquals(VarselDbDto.Varselstatus.AVVIST, status)
    }

    private fun opprettVarsel(status: VarselDbDto.Varselstatus) = VarselDbDto(
        varselId = UUID.randomUUID(),
        behandlingId = UUID.randomUUID(),
        opprettet = LocalDateTime.now(),
        kode = "SB_EX_1",
        status = status,
        varseldefinisjon = VarselDbDto.VarseldefinisjonDbDto(
            definisjonId = UUID.randomUUID(),
            tittel = "EN_TITTEL",
            forklaring = null,
            handling = null,
        ),
        varselvurdering = null,
    )
}
