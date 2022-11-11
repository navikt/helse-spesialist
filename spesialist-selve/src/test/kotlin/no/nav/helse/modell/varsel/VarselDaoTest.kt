package no.nav.helse.modell.varsel

import DatabaseIntegrationTest
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.mediator.meldinger.NyeVarsler.Varsel.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class VarselDaoTest : DatabaseIntegrationTest() {
    private val dao = VarselDao(dataSource)

    @Test
    fun `lagre varsel`() {
        dao.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), VEDTAKSPERIODE)
        assertEquals(1, dao.alleVarslerFor(VEDTAKSPERIODE).size)
    }

    @Test
    fun `lagre flere varsler`() {
        dao.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), VEDTAKSPERIODE)
        dao.lagreVarsel(UUID.randomUUID(), "EN_ANNEN_KODE", LocalDateTime.now(), VEDTAKSPERIODE)
        assertEquals(2, dao.alleVarslerFor(VEDTAKSPERIODE).size)
    }

    @Test
    fun `lagre varseldefinisjon`() {
        val definisjonId = UUID.randomUUID()
        dao.lagreDefinisjon(definisjonId, "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        assertNotNull(dao.definisjonFor(definisjonId))
    }

    @Test
    fun `lagre flere varseldefinisjoner`() {
        val definisjonId1 = UUID.randomUUID()
        val definisjonId2 = UUID.randomUUID()
        dao.lagreDefinisjon(definisjonId1, "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        dao.lagreDefinisjon(definisjonId2, "EN_ANNEN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        assertNotNull(dao.definisjonFor(definisjonId1))
        assertNotNull(dao.definisjonFor(definisjonId2))
    }

    @Test
    fun `sjekk for aktivt varsel`() {
        dao.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), VEDTAKSPERIODE)
        assertTrue(dao.erAktivFor(VEDTAKSPERIODE, "EN_KODE"))
    }

    @Test
    fun `sjekk for inaktivt varsel`() {
        dao.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), VEDTAKSPERIODE)
        dao.oppdaterStatus(VEDTAKSPERIODE, "EN_KODE", Status.INAKTIV, "EN_IDENT")
        assertFalse(dao.erAktivFor(VEDTAKSPERIODE, "EN_KODE"))
    }

    @Test
    fun `endring av en varselstatus for en vedtaksperiode endrer ikke status for en annen`() {
        val v1 = UUID.randomUUID()
        val v2 = UUID.randomUUID()
        dao.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), v1)
        dao.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), v2)
        dao.oppdaterStatus(v1, "EN_KODE", Status.INAKTIV, "EN_IDENT")
        assertFalse(dao.erAktivFor(v1, "EN_KODE"))
        assertTrue(dao.erAktivFor(v2, "EN_KODE"))
    }
}