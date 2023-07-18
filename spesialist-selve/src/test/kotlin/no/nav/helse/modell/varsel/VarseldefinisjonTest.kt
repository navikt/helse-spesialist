package no.nav.helse.modell.varsel

import java.time.LocalDateTime
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

internal class VarseldefinisjonTest {

    @Test
    fun toDto() {
        val id = UUID.randomUUID()
        val opprettet = LocalDateTime.now()
        val definisjon = Varseldefinisjon(id, "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, opprettet)
        assertEquals(VarseldefinisjonDto(id, "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, opprettet), definisjon.toDto())
    }

    @Test
    fun `referential equals`() {
        val definisjon = Varseldefinisjon(UUID.randomUUID(), "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        assertEquals(definisjon, definisjon)
        assertEquals(definisjon.hashCode(), definisjon.hashCode())
    }

    @Test
    fun `structural equals`() {
        val definisjonId = UUID.randomUUID()
        val definisjon1 = Varseldefinisjon(definisjonId, "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val definisjon2 = Varseldefinisjon(definisjonId, "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        assertEquals(definisjon1, definisjon2)
        assertEquals(definisjon1.hashCode(), definisjon2.hashCode())
    }

    @Test
    fun `forskjellig definisjonIder`() {
        val definisjonId1 = UUID.randomUUID()
        val definisjonId2 = UUID.randomUUID()
        val definisjon1 = Varseldefinisjon(definisjonId1, "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val definisjon2 = Varseldefinisjon(definisjonId2, "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        assertNotEquals(definisjon1, definisjon2)
        assertNotEquals(definisjon1.hashCode(), definisjon2.hashCode())
    }

    @Test
    fun `forskjellige varselkode`() {
        val definisjonId = UUID.randomUUID()
        val definisjon1 = Varseldefinisjon(definisjonId, "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val definisjon2 = Varseldefinisjon(definisjonId, "EN_ANNEN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        assertNotEquals(definisjon1, definisjon2)
        assertNotEquals(definisjon1.hashCode(), definisjon2.hashCode())
    }

    @Test
    fun `forskjellig tittel`() {
        val definisjonId = UUID.randomUUID()
        val definisjon1 = Varseldefinisjon(definisjonId, "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val definisjon2 = Varseldefinisjon(definisjonId, "EN_KODE", "EN_ANNEN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        assertNotEquals(definisjon1, definisjon2)
        assertNotEquals(definisjon1.hashCode(), definisjon2.hashCode())
    }

    @Test
    fun `forskjellig forklaring`() {
        val definisjonId = UUID.randomUUID()
        val definisjon1 = Varseldefinisjon(definisjonId, "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val definisjon2 = Varseldefinisjon(definisjonId, "EN_KODE", "EN_TITTEL", "EN_ANNEN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        assertNotEquals(definisjon1, definisjon2)
        assertNotEquals(definisjon1.hashCode(), definisjon2.hashCode())
    }

    @Test
    fun `forskjellig handling`() {
        val definisjonId = UUID.randomUUID()
        val definisjon1 = Varseldefinisjon(definisjonId, "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val definisjon2 = Varseldefinisjon(definisjonId, "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_ANNEN_HANDLING", false, LocalDateTime.now())
        assertNotEquals(definisjon1, definisjon2)
        assertNotEquals(definisjon1.hashCode(), definisjon2.hashCode())
    }

    @Test
    fun `forskjellig avviklet`() {
        val definisjonId = UUID.randomUUID()
        val definisjon1 = Varseldefinisjon(definisjonId, "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val definisjon2 = Varseldefinisjon(definisjonId, "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", true, LocalDateTime.now())
        assertNotEquals(definisjon1, definisjon2)
        assertNotEquals(definisjon1.hashCode(), definisjon2.hashCode())
    }

}