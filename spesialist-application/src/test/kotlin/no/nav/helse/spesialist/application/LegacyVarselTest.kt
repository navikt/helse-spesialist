package no.nav.helse.spesialist.application

import no.nav.helse.db.api.VarselDbDto
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

    private fun opprettVarsel(status: VarselDbDto.Varselstatus) =
        VarselDbDto(
            varselId = UUID.randomUUID(),
            behandlingId = UUID.randomUUID(),
            opprettet = LocalDateTime.now(),
            kode = "SB_EX_1",
            status = status,
            varseldefinisjon =
                VarselDbDto.VarseldefinisjonDbDto(
                    definisjonId = UUID.randomUUID(),
                    tittel = "EN_TITTEL",
                    forklaring = null,
                    handling = null,
                ),
            varselvurdering = null,
        )
}
