package no.nav.helse.spesialist.api.varsel

import java.util.UUID
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


    private fun opprettVarsel(status: Varsel.Varselstatus): Varsel {
        return Varsel(UUID.randomUUID(), UUID.randomUUID(), "SB_EX_1", status, "EN_TITTEL", null, null, null)
    }
}