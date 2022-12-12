package no.nav.helse.modell.varsel

import DatabaseIntegrationTest
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.modell.varsel.Varsel.Status.AKTIV
import no.nav.helse.modell.varsel.Varsel.Status.INAKTIV
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class VarselDaoTest : DatabaseIntegrationTest() {

    private val varselDao = VarselDao(dataSource)
    private val definisjonDao = DefinisjonDao(dataSource)

    @Test
    fun `lagre varsel`() {
        definisjonDao.lagreDefinisjon(UUID.randomUUID(), "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val generasjonId = UUID.randomUUID()
        generasjonDao.opprettFor(generasjonId, VEDTAKSPERIODE, UUID.randomUUID())
        varselDao.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), VEDTAKSPERIODE, generasjonId)
        assertEquals(1, varselDao.alleVarslerFor(VEDTAKSPERIODE).size)
    }

    @Test
    fun `lagre flere varsler`() {
        definisjonDao.lagreDefinisjon(UUID.randomUUID(), "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        definisjonDao.lagreDefinisjon(UUID.randomUUID(), "EN_ANNEN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val generasjonId = UUID.randomUUID()
        generasjonDao.opprettFor(generasjonId, VEDTAKSPERIODE, UUID.randomUUID())
        varselDao.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), VEDTAKSPERIODE, generasjonId)
        varselDao.lagreVarsel(UUID.randomUUID(), "EN_ANNEN_KODE", LocalDateTime.now(), VEDTAKSPERIODE, generasjonId)
        assertEquals(2, varselDao.alleVarslerFor(VEDTAKSPERIODE).size)
    }

    @Test
    fun `sjekk for aktivt varsel`() {
        definisjonDao.lagreDefinisjon(UUID.randomUUID(), "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val generasjonId = UUID.randomUUID()
        generasjonDao.opprettFor(generasjonId, VEDTAKSPERIODE, UUID.randomUUID())
        varselDao.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), VEDTAKSPERIODE, generasjonId)
        assertEquals(AKTIV, varselDao.finnVarselstatus(VEDTAKSPERIODE, "EN_KODE"))
    }

    @Test
    fun `sjekk for inaktivt varsel`() {
        val definisjonId = UUID.randomUUID()
        definisjonDao.lagreDefinisjon(definisjonId, "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val generasjonId = UUID.randomUUID()
        generasjonDao.opprettFor(generasjonId, VEDTAKSPERIODE, UUID.randomUUID())
        varselDao.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), VEDTAKSPERIODE, generasjonId)
        varselDao.oppdaterVarsel(VEDTAKSPERIODE, generasjonId, "EN_KODE", INAKTIV, "EN_IDENT", definisjonId)
        assertEquals(INAKTIV, varselDao.finnVarselstatus(VEDTAKSPERIODE, "EN_KODE"))
    }

    @Test
    fun `endring av en varselstatus for en vedtaksperiode endrer ikke status for en annen`() {
        val definisjonId = UUID.randomUUID()
        definisjonDao.lagreDefinisjon(definisjonId, "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val v1 = UUID.randomUUID()
        val v2 = UUID.randomUUID()
        val generasjonIdv1 = UUID.randomUUID()
        val generasjonIdv2 = UUID.randomUUID()
        generasjonDao.opprettFor(generasjonIdv1, v1, UUID.randomUUID())
        generasjonDao.opprettFor(generasjonIdv2, v2, UUID.randomUUID())
        varselDao.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), v1, generasjonIdv1)
        varselDao.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), v2, generasjonIdv2)
        varselDao.oppdaterVarsel(v1, generasjonIdv1, "EN_KODE", INAKTIV, "EN_IDENT", definisjonId)
        assertEquals(INAKTIV, varselDao.finnVarselstatus(v1, "EN_KODE"))
        assertEquals(AKTIV, varselDao.finnVarselstatus(v2, "EN_KODE"))
    }

    @Test
    fun `status gir null dersom vi ikke finner varsel`() {
        assertNull(varselDao.finnVarselstatus(UUID.randomUUID(), "EN_KODE"))
    }

    @Test
    fun `finner varsler for vedtaksperiode`() {
        definisjonDao.lagreDefinisjon(UUID.randomUUID(), "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val v1 = UUID.randomUUID()
        val varselId = UUID.randomUUID()
        val opprettet = LocalDateTime.now()
        val generasjonId = UUID.randomUUID()
        generasjonDao.opprettFor(generasjonId, v1, UUID.randomUUID())
        varselDao.lagreVarsel(varselId, "EN_KODE", LocalDateTime.now(), v1, generasjonId)
        assertEquals(listOf(Varsel(varselId, "EN_KODE", opprettet, v1)), varselDao.alleVarslerFor(v1))
    }
}