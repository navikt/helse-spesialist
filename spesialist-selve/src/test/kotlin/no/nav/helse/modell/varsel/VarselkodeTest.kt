package no.nav.helse.modell.varsel

import no.nav.helse.modell.person.vedtaksperiode.VarselStatusDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.util.UUID

internal class VarselkodeTest {
    @ParameterizedTest
    @EnumSource(value = Varselkode::class)
    fun `kan ikke deaktivere varsel som ikke er aktivt`(varselkode: Varselkode) {
        val vedtaksperiodeId = UUID.randomUUID()
        val varsel = varselkode.nyttVarsel(vedtaksperiodeId)
        assertEquals(varselkode.name, varsel.toDto().varselkode)
        assertEquals(vedtaksperiodeId, varsel.toDto().vedtaksperiodeId)
        assertEquals(VarselStatusDto.AKTIV, varsel.toDto().status)
    }
}
