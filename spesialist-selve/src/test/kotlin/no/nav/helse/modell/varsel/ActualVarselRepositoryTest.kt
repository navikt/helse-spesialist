package no.nav.helse.modell.varsel

import java.time.LocalDateTime
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.AbstractDatabaseTest
import no.nav.helse.modell.varsel.Varsel.Status.AVVIST
import no.nav.helse.modell.varsel.Varsel.Status.GODKJENT
import no.nav.helse.modell.varsel.Varsel.Status.INAKTIV
import no.nav.helse.modell.vedtaksperiode.GenerasjonDao
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class ActualVarselRepositoryTest : AbstractDatabaseTest() {

    private val repository = ActualVarselRepository(dataSource)
    private val varselDao = VarselDao(dataSource)
    private val generasjonDao = GenerasjonDao(dataSource)

    @Test
    fun `kan kun lagre varsel dersom det finnes en generasjon for perioden`() {
        val vedtaksperiodeId = UUID.randomUUID()
        generasjonDao.opprettFor(vedtaksperiodeId, UUID.randomUUID())
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        assertEquals(1, repository.finnVarslerFor(vedtaksperiodeId).size)
    }

    @Test
    fun `kan ikke lagre varsel dersom det ikke finnes en generasjon for perioden`() {
        val vedtaksperiodeId = UUID.randomUUID()
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        assertEquals(0, repository.finnVarslerFor(vedtaksperiodeId).size)
    }

    @Test
    fun `kan godkjenne varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        generasjonDao.opprettFor(vedtaksperiodeId, UUID.randomUUID())
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        repository.godkjennFor(vedtaksperiodeId, "EN_KODE", "EN_IDENT")
        assertEquals(GODKJENT, varselDao.finnVarselstatus(vedtaksperiodeId, "EN_KODE"))
    }

    @Test
    fun `kan godkjenne alle varsler`() {
        val vedtaksperiodeId = UUID.randomUUID()
        generasjonDao.opprettFor(vedtaksperiodeId, UUID.randomUUID())
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        repository.lagreVarsel(UUID.randomUUID(), "EN_ANNEN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        repository.godkjennAlleFor(vedtaksperiodeId, "EN_IDENT")
        assertEquals(GODKJENT, varselDao.finnVarselstatus(vedtaksperiodeId, "EN_KODE"))
        assertEquals(GODKJENT, varselDao.finnVarselstatus(vedtaksperiodeId, "EN_ANNEN_KODE"))
    }

    @Test
    fun `kan avvise varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        generasjonDao.opprettFor(vedtaksperiodeId, UUID.randomUUID())
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        repository.avvisFor(vedtaksperiodeId, "EN_KODE", "EN_IDENT")
        assertEquals(AVVIST, varselDao.finnVarselstatus(vedtaksperiodeId, "EN_KODE"))
    }

    @Test
    fun `kan avvise alle varsler`() {
        val vedtaksperiodeId = UUID.randomUUID()
        generasjonDao.opprettFor(vedtaksperiodeId, UUID.randomUUID())
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        repository.lagreVarsel(UUID.randomUUID(), "EN_ANNEN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        repository.avvisAlleFor(vedtaksperiodeId, "EN_IDENT")
        assertEquals(AVVIST, varselDao.finnVarselstatus(vedtaksperiodeId, "EN_KODE"))
        assertEquals(AVVIST, varselDao.finnVarselstatus(vedtaksperiodeId, "EN_ANNEN_KODE"))
    }

    @Test
    fun `kan deaktivere varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        generasjonDao.opprettFor(vedtaksperiodeId, UUID.randomUUID())
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        repository.deaktiverFor(vedtaksperiodeId, "EN_KODE")
        assertEquals(INAKTIV, varselDao.finnVarselstatus(vedtaksperiodeId, "EN_KODE"))
    }

    @Test
    fun `kan ikke deaktivere godkjent varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        generasjonDao.opprettFor(vedtaksperiodeId, UUID.randomUUID())
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        repository.godkjennFor(vedtaksperiodeId, "EN_KODE", "EN_IDENT")
        repository.deaktiverFor(vedtaksperiodeId, "EN_KODE")
        assertEquals(GODKJENT, varselDao.finnVarselstatus(vedtaksperiodeId, "EN_KODE"))
    }

    @Test
    fun `kan ikke godkjenne deaktivert varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        generasjonDao.opprettFor(vedtaksperiodeId, UUID.randomUUID())
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        repository.deaktiverFor(vedtaksperiodeId, "EN_KODE")
        repository.godkjennFor(vedtaksperiodeId, "EN_KODE", "EN_IDENT")
        assertEquals(INAKTIV, varselDao.finnVarselstatus(vedtaksperiodeId, "EN_KODE"))
    }

    @Test
    fun `nytt varsel er aktivt`() {
        val vedtaksperiodeId = UUID.randomUUID()
        generasjonDao.opprettFor(vedtaksperiodeId, UUID.randomUUID())
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        assertAktiv(vedtaksperiodeId, "EN_KODE")
    }

    @Test
    fun `varsel har ikke aktiv-status lenger når det er godkjent`() {
        val vedtaksperiodeId = UUID.randomUUID()
        generasjonDao.opprettFor(vedtaksperiodeId, UUID.randomUUID())
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        repository.godkjennFor(vedtaksperiodeId, "EN_KODE", "EN_IDENT")
        assertGodkjent(vedtaksperiodeId, "EN_KODE")
    }

    @Test
    fun `varsel er ikke aktivt når det er deaktivert`() {
        val vedtaksperiodeId = UUID.randomUUID()
        generasjonDao.opprettFor(vedtaksperiodeId, UUID.randomUUID())
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        repository.deaktiverFor(vedtaksperiodeId, "EN_KODE")
        assertInaktiv(vedtaksperiodeId, "EN_KODE")
    }

    @Test
    fun `finner varsler for vedtaksperiode`() {
        val vedtaksperiodeId = UUID.randomUUID()
        generasjonDao.opprettFor(vedtaksperiodeId, UUID.randomUUID())
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        repository.lagreVarsel(UUID.randomUUID(), "EN_ANNEN_KODE", LocalDateTime.now(), vedtaksperiodeId)

        assertEquals(2, repository.finnVarslerFor(vedtaksperiodeId).size)
    }

    @Test
    fun `Lagrer kun én utgave av et aktivt varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        generasjonDao.opprettFor(vedtaksperiodeId, UUID.randomUUID())
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)

        assertEquals(1, repository.finnVarslerFor(vedtaksperiodeId).size)
    }

    @Test
    fun `finner varsler for ulike vedtaksperioder`() {
        val v1 = UUID.randomUUID()
        val v2 = UUID.randomUUID()
        generasjonDao.opprettFor(v1, UUID.randomUUID())
        generasjonDao.opprettFor(v2, UUID.randomUUID())
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), v1)
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), v2)

        assertEquals(1, repository.finnVarslerFor(v1).size)
        assertEquals(1, repository.finnVarslerFor(v2).size)
    }

    @Test
    fun `lagre definisjon`() {
        val definisjonsId = UUID.randomUUID()
        repository.lagreDefinisjon(definisjonsId, "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        assertNotNull(varselDao.definisjonFor(definisjonsId))
    }

    private fun assertAktiv(vedtaksperiodeId: UUID, varselkode: String) {
        val status = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT status FROM selve_varsel WHERE vedtaksperiode_id = ? AND kode = ?;"
            session.run(
                queryOf(
                    query,
                    vedtaksperiodeId,
                    varselkode
                ).map { it.string(1) }.asSingle
            )
        }
        assertEquals("AKTIV", status)
    }

    private fun assertInaktiv(vedtaksperiodeId: UUID, varselkode: String) {
        val status = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT status FROM selve_varsel WHERE vedtaksperiode_id = ? AND kode = ?;"
            session.run(
                queryOf(
                    query,
                    vedtaksperiodeId,
                    varselkode
                ).map { it.string(1) }.asSingle
            )
        }
        assertEquals("INAKTIV", status)
    }

    private fun assertGodkjent(vedtaksperiodeId: UUID, varselkode: String) {
        val status = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT status FROM selve_varsel WHERE vedtaksperiode_id = ? AND kode = ?;"
            session.run(
                queryOf(
                    query,
                    vedtaksperiodeId,
                    varselkode
                ).map { it.string(1) }.asSingle
            )
        }
        assertEquals("GODKJENT", status)
    }
}