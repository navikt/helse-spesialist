package no.nav.helse.modell.varsel

import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

internal class VarselTest {

    private val varselRepositoryMock = mockk<VarselRepository>(relaxed = true)

    @Test
    fun lagre() {
        val varselId = UUID.randomUUID()
        val opprettet = LocalDateTime.now()
        val vedtaksperiodeId = UUID.randomUUID()
        val varsel = Varsel(varselId, "EN_KODE", opprettet, vedtaksperiodeId)
        varsel.lagre(varselRepositoryMock)
        verify(exactly = 1) { varselRepositoryMock.lagreVarsel(varselId, "EN_KODE", opprettet, vedtaksperiodeId) }
    }

    @Test
    fun equals() {
        val varselId = UUID.randomUUID()
        val opprettet = LocalDateTime.now()
        val vedtaksperiodeId = UUID.randomUUID()
        val varsel = Varsel(varselId, "EN_KODE", opprettet, vedtaksperiodeId)
        assertEquals(varsel, varsel)
        assertEquals(varsel.hashCode(), varsel.hashCode())
        assertEquals(Varsel(varselId,"EN_KODE", opprettet, vedtaksperiodeId), varsel)
        assertEquals(Varsel(varselId,"EN_KODE", opprettet, vedtaksperiodeId).hashCode(), varsel.hashCode())
    }

    @Test
    fun `not equals`() {
        val varsel = Varsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), UUID.randomUUID())
        assertNotEquals(Varsel(UUID.randomUUID(), "EN_ANNEN_KODE", LocalDateTime.now(), UUID.randomUUID()), varsel)
        assertNotEquals(Varsel(UUID.randomUUID(), "EN_ANNEN_KODE", LocalDateTime.now(), UUID.randomUUID()).hashCode(), varsel.hashCode())
    }
}