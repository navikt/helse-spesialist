package no.nav.helse.spesialist.api.varsel

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.spesialist.api.varsel.Varsel.Varselstatus.AVVIST
import no.nav.helse.spesialist.api.varsel.Varsel.Varselstatus.GODKJENT
import no.nav.helse.spesialist.api.varsel.Varsel.Varselstatus.VURDERT
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class VarselTest {

    @ParameterizedTest
    @EnumSource(value = Varsel.Varselstatus::class, names = ["AKTIV"], mode = EnumSource.Mode.EXCLUDE)
    fun `er ikke aktiv`(status: Varsel.Varselstatus) {
        val varsel = opprettVarsel(status)
        assertFalse(varsel.erAktiv())
    }

    @Test
    fun `er aktiv`() {
        val varsel = opprettVarsel(Varsel.Varselstatus.AKTIV)
        assertTrue(varsel.erAktiv())
    }

    @ParameterizedTest
    @EnumSource(value = Varsel.Varselstatus::class, names = ["VURDERT", "AKTIV"], mode = EnumSource.Mode.EXCLUDE)
    fun `varsel blir ikke godkjent hvis det ikke er vurdert eller aktivt`(status: Varsel.Varselstatus) {
        var nyStatus: Varsel.Varselstatus = status
        val varsel = opprettVarsel(nyStatus)
        var godkjent = false
        varsel.vurder(
            godkjent = true,
            "FNR",
            UUID.randomUUID(),
            UUID.randomUUID(),
            "ident"
        ) { _: String, _: UUID, _: UUID, _: UUID, _: String, _: String, _: Varsel.Varselstatus, gjeldendeStatus: Varsel.Varselstatus, _: String ->
            godkjent = true
            nyStatus = gjeldendeStatus
        }

        assertEquals(false, godkjent)
        assertEquals(status, nyStatus)
    }

    @Test
    fun `godkjenn varsel som er vurdert`() {
        var status: Varsel.Varselstatus = VURDERT
        val varsel = opprettVarsel(status)
        var vurdert = false

        varsel.vurder(
            godkjent = true,
            "FNR",
            UUID.randomUUID(),
            UUID.randomUUID(),
            "ident"
        ) { _: String, _: UUID, _: UUID, _: UUID, _: String, _: String, _: Varsel.Varselstatus, gjeldendeStatus: Varsel.Varselstatus, _: String ->
            vurdert = true
            status = gjeldendeStatus
        }

        assertEquals(true, vurdert)
        assertEquals(GODKJENT, status)
    }

    @Test
    fun `avviser varsel som er aktivt eller vurdert dersom perioden ikke er godkjent`() {
        var status: Varsel.Varselstatus = VURDERT
        val varsel = opprettVarsel(status)
        var vurdert = false
        varsel.vurder(
            godkjent = false,
            "FNR",
            UUID.randomUUID(),
            UUID.randomUUID(),
            "ident"
        ) { _: String, _: UUID, _: UUID, _: UUID, _: String, _: String, _: Varsel.Varselstatus, gjeldendeStatus: Varsel.Varselstatus, _: String ->
            vurdert = true
            status = gjeldendeStatus
        }

        assertEquals(true, vurdert)
        assertEquals(AVVIST, status)
    }

    private fun opprettVarsel(status: Varsel.Varselstatus): Varsel {
        return Varsel(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            LocalDateTime.now(),
            "SB_EX_1",
            status,
            "EN_TITTEL",
            null,
            null,
            null,
        )
    }
}
