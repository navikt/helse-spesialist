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
        val generasjon = generasjonDao.opprettFor(VEDTAKSPERIODE, UUID.randomUUID(), generasjonId)
        generasjon.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), varselDao::lagreVarsel)
        assertEquals(1, varselDao.alleVarslerFor(VEDTAKSPERIODE).size)
    }

    @Test
    fun `lagre flere varsler`() {
        definisjonDao.lagreDefinisjon(UUID.randomUUID(), "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        definisjonDao.lagreDefinisjon(UUID.randomUUID(), "EN_ANNEN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val generasjonId = UUID.randomUUID()
        val generasjon = generasjonDao.opprettFor(VEDTAKSPERIODE, UUID.randomUUID(), generasjonId)
        generasjon.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), varselDao::lagreVarsel)
        generasjon.lagreVarsel(UUID.randomUUID(), "EN_ANNEN_KODE", LocalDateTime.now(), varselDao::lagreVarsel)
        assertEquals(2, varselDao.alleVarslerFor(VEDTAKSPERIODE).size)
    }

    @Test
    fun `lagrer ikke varsler dobbelt`() {
        definisjonDao.lagreDefinisjon(UUID.randomUUID(), "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val unikId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val generasjon = generasjonDao.opprettFor(VEDTAKSPERIODE, UUID.randomUUID(), generasjonId)
        generasjon.lagreVarsel(unikId, "EN_KODE", LocalDateTime.now(), varselDao::lagreVarsel)
        generasjon.lagreVarsel(unikId, "EN_KODE", LocalDateTime.now(), varselDao::lagreVarsel)
        assertEquals(1, varselDao.alleVarslerFor(VEDTAKSPERIODE).size)
    }

    @Test
    fun `sjekk for aktivt varsel`() {
        definisjonDao.lagreDefinisjon(UUID.randomUUID(), "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val generasjonId = UUID.randomUUID()
        val generasjon = generasjonDao.opprettFor(VEDTAKSPERIODE, UUID.randomUUID(), generasjonId)
        generasjon.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), varselDao::lagreVarsel)
        assertEquals(AKTIV, varselDao.finnVarselstatus(VEDTAKSPERIODE, "EN_KODE"))
    }

    @Test
    fun `sjekk for inaktivt varsel`() {
        definisjonDao.lagreDefinisjon(UUID.randomUUID(), "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val generasjonId = UUID.randomUUID()
        val generasjon = generasjonDao.opprettFor(VEDTAKSPERIODE, UUID.randomUUID(), generasjonId)
        generasjon.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), varselDao::lagreVarsel)
        val definisjon = definisjonDao.sisteDefinisjonFor("EN_KODE")
        definisjon.oppdaterVarsel(VEDTAKSPERIODE, generasjonId, INAKTIV, "EN_IDENT", varselDao::oppdaterVarsel)
        assertEquals(INAKTIV, varselDao.finnVarselstatus(VEDTAKSPERIODE, "EN_KODE"))
    }

    @Test
    fun `endring av en varselstatus for en vedtaksperiode endrer ikke status for en annen`() {
        definisjonDao.lagreDefinisjon(UUID.randomUUID(), "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val v1 = UUID.randomUUID()
        val v2 = UUID.randomUUID()
        val generasjonIdv1 = UUID.randomUUID()
        val generasjonIdv2 = UUID.randomUUID()
        val generasjonv1 = generasjonDao.opprettFor(v1, UUID.randomUUID(), generasjonIdv1)
        val generasjonv2 = generasjonDao.opprettFor(v2, UUID.randomUUID(), generasjonIdv2)
        generasjonv1.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), varselDao::lagreVarsel)
        generasjonv2.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), varselDao::lagreVarsel)
        val definisjon = definisjonDao.sisteDefinisjonFor("EN_KODE")
        definisjon.oppdaterVarsel(v1, generasjonIdv1, INAKTIV, "EN_IDENT", varselDao::oppdaterVarsel)
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
        val generasjon = generasjonDao.opprettFor(v1, UUID.randomUUID(), generasjonId)
        generasjon.lagreVarsel(varselId, "EN_KODE", opprettet, varselDao::lagreVarsel)
        assertEquals(listOf(Varsel(varselId, "EN_KODE", opprettet, v1)), varselDao.alleVarslerFor(v1))
    }
}