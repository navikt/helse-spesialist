package no.nav.helse.modell.varsel

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.AbstractDatabaseTest
import no.nav.helse.modell.varsel.Varsel.Status.GODKJENT
import no.nav.helse.modell.varsel.Varsel.Status.INAKTIV
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ActualVarselRepositoryTest : AbstractDatabaseTest() {

    private val repository = ActualVarselRepository(dataSource)
    private val dao = VarselDao(dataSource)

    @Test
    fun `kan godkjenne varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        repository.godkjennFor(vedtaksperiodeId, "EN_KODE", "EN_IDENT")
        assertEquals(GODKJENT, dao.finnVarselstatus(vedtaksperiodeId, "EN_KODE"))
    }

    @Test
    fun `kan deaktivere varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        repository.deaktiverFor(vedtaksperiodeId, "EN_KODE")
        assertEquals(INAKTIV, dao.finnVarselstatus(vedtaksperiodeId, "EN_KODE"))
    }

    @Test
    fun `kan ikke deaktivere godkjent varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        repository.godkjennFor(vedtaksperiodeId, "EN_KODE", "EN_IDENT")
        repository.deaktiverFor(vedtaksperiodeId, "EN_KODE")
        assertEquals(GODKJENT, dao.finnVarselstatus(vedtaksperiodeId, "EN_KODE"))
    }

    @Test
    fun `kan ikke godkjenne deaktivert varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        repository.deaktiverFor(vedtaksperiodeId, "EN_KODE")
        repository.godkjennFor(vedtaksperiodeId, "EN_KODE", "EN_IDENT")
        assertEquals(INAKTIV, dao.finnVarselstatus(vedtaksperiodeId, "EN_KODE"))
    }

    @Test
    fun `nytt varsel er aktivt`() {
        val vedtaksperiodeId = UUID.randomUUID()
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        assertTrue(repository.erAktivFor(vedtaksperiodeId, "EN_KODE"))
    }

    @Test
    fun `varsel er ikke aktivt når det er godkjent`() {
        val vedtaksperiodeId = UUID.randomUUID()
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        repository.godkjennFor(vedtaksperiodeId, "EN_KODE", "EN_IDENT")
        assertFalse(repository.erAktivFor(vedtaksperiodeId, "EN_KODE"))
    }

    @Test
    fun `varsel er ikke aktivt når det er deaktivert`() {
        val vedtaksperiodeId = UUID.randomUUID()
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        repository.deaktiverFor(vedtaksperiodeId, "EN_KODE")
        assertFalse(repository.erAktivFor(vedtaksperiodeId, "EN_KODE"))
    }

    @Test
    fun `finner varsler for vedtaksperiode`() {
        val vedtaksperiodeId = UUID.randomUUID()
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)

        assertEquals(2, repository.finnVarslerFor(vedtaksperiodeId).size)
    }

    @Test
    fun `finner varsler for ulike vedtaksperioder`() {
        val v1 = UUID.randomUUID()
        val v2 = UUID.randomUUID()
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), v1)
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), v2)

        assertEquals(1, repository.finnVarslerFor(v1).size)
        assertEquals(1, repository.finnVarslerFor(v2).size)
    }

    @Test
    fun `lagre definisjon`() {
        val definisjonsId = UUID.randomUUID()
        repository.lagreDefinisjon(definisjonsId, "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        assertNotNull(dao.definisjonFor(definisjonsId))
    }
}