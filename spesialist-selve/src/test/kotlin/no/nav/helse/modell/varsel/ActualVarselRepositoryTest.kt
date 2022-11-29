package no.nav.helse.modell.varsel

import java.time.LocalDateTime
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.AbstractDatabaseTest
import no.nav.helse.mediator.meldinger.Varseldefinisjon
import no.nav.helse.modell.varsel.Varsel.Status.AVVIST
import no.nav.helse.modell.varsel.Varsel.Status.GODKJENT
import no.nav.helse.modell.varsel.Varsel.Status.INAKTIV
import no.nav.helse.modell.vedtaksperiode.GenerasjonDao
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ActualVarselRepositoryTest : AbstractDatabaseTest() {

    private val repository = ActualVarselRepository(dataSource)
    private val varselDao = VarselDao(dataSource)
    private val generasjonDao = GenerasjonDao(dataSource)
    private val definisjonDao = DefinisjonDao(dataSource)

    @Test
    fun `kan kun lagre varsel dersom det finnes en generasjon for perioden`() {
        definisjonDao.lagreDefinisjon(UUID.randomUUID(), "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val vedtaksperiodeId = UUID.randomUUID()
        generasjonDao.opprettFor(vedtaksperiodeId, UUID.randomUUID())
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        assertEquals(1, repository.finnVarslerFor(vedtaksperiodeId).size)
    }

    @Test
    fun `kan ikke lagre varsel dersom det ikke finnes en generasjon for perioden`() {
        definisjonDao.lagreDefinisjon(UUID.randomUUID(), "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val vedtaksperiodeId = UUID.randomUUID()
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        assertEquals(0, repository.finnVarslerFor(vedtaksperiodeId).size)
    }

    @Test
    fun `kan godkjenne varsel`() {
        definisjonDao.lagreDefinisjon(UUID.randomUUID(), "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val vedtaksperiodeId = UUID.randomUUID()
        generasjonDao.opprettFor(vedtaksperiodeId, UUID.randomUUID())
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        repository.godkjennFor(vedtaksperiodeId, "EN_KODE", "EN_IDENT", null)
        assertEquals(GODKJENT, varselDao.finnVarselstatus(vedtaksperiodeId, "EN_KODE"))
    }

    @Test
    fun `godkjenning av varsel med definisjonId medfører at varselet lagres med referanse til denne definisjonen`() {
        val definisjonId = UUID.randomUUID()
        definisjonDao.lagreDefinisjon(definisjonId, "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val vedtaksperiodeId = UUID.randomUUID()
        generasjonDao.opprettFor(vedtaksperiodeId, UUID.randomUUID())
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        repository.godkjennFor(vedtaksperiodeId, "EN_KODE", "EN_IDENT", definisjonId)
        assertEquals(GODKJENT, varselDao.finnVarselstatus(vedtaksperiodeId, "EN_KODE"))
        assertDefinisjonFor(vedtaksperiodeId, "EN_KODE", definisjonId)
    }

    @Test
    fun `avvisning av varsel med definisjonId medfører at varselet lagres med referanse til denne definisjonen`() {
        val definisjonId = UUID.randomUUID()
        definisjonDao.lagreDefinisjon(definisjonId, "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val vedtaksperiodeId = UUID.randomUUID()
        generasjonDao.opprettFor(vedtaksperiodeId, UUID.randomUUID())
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        repository.avvisFor(vedtaksperiodeId, "EN_KODE", "EN_IDENT", definisjonId)
        assertEquals(AVVIST, varselDao.finnVarselstatus(vedtaksperiodeId, "EN_KODE"))
        assertDefinisjonFor(vedtaksperiodeId, "EN_KODE", definisjonId)
    }

    @Test
    fun `deaktivering av varsel med definisjonId medfører at varselet lagres med referanse til denne definisjonen`() {
        val definisjonId = UUID.randomUUID()
        definisjonDao.lagreDefinisjon(definisjonId, "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val vedtaksperiodeId = UUID.randomUUID()
        generasjonDao.opprettFor(vedtaksperiodeId, UUID.randomUUID())
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        repository.deaktiverFor(vedtaksperiodeId, "EN_KODE", definisjonId)
        assertEquals(INAKTIV, varselDao.finnVarselstatus(vedtaksperiodeId, "EN_KODE"))
        assertDefinisjonFor(vedtaksperiodeId, "EN_KODE", definisjonId)
    }

    @Test
    fun `kan godkjenne alle varsler`() {
        definisjonDao.lagreDefinisjon(UUID.randomUUID(), "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        definisjonDao.lagreDefinisjon(UUID.randomUUID(), "EN_ANNEN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
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
        definisjonDao.lagreDefinisjon(UUID.randomUUID(), "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val vedtaksperiodeId = UUID.randomUUID()
        generasjonDao.opprettFor(vedtaksperiodeId, UUID.randomUUID())
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        repository.avvisFor(vedtaksperiodeId, "EN_KODE", "EN_IDENT", null)
        assertEquals(AVVIST, varselDao.finnVarselstatus(vedtaksperiodeId, "EN_KODE"))
    }

    @Test
    fun `kan avvise alle varsler`() {
        definisjonDao.lagreDefinisjon(UUID.randomUUID(), "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        definisjonDao.lagreDefinisjon(UUID.randomUUID(), "EN_ANNEN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
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
        definisjonDao.lagreDefinisjon(UUID.randomUUID(), "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val vedtaksperiodeId = UUID.randomUUID()
        generasjonDao.opprettFor(vedtaksperiodeId, UUID.randomUUID())
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        repository.deaktiverFor(vedtaksperiodeId, "EN_KODE", null)
        assertEquals(INAKTIV, varselDao.finnVarselstatus(vedtaksperiodeId, "EN_KODE"))
    }

    @Test
    fun `kan ikke deaktivere godkjent varsel`() {
        definisjonDao.lagreDefinisjon(UUID.randomUUID(), "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val vedtaksperiodeId = UUID.randomUUID()
        generasjonDao.opprettFor(vedtaksperiodeId, UUID.randomUUID())
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        repository.godkjennFor(vedtaksperiodeId, "EN_KODE", "EN_IDENT", null)
        repository.deaktiverFor(vedtaksperiodeId, "EN_KODE", null)
        assertEquals(GODKJENT, varselDao.finnVarselstatus(vedtaksperiodeId, "EN_KODE"))
    }

    @Test
    fun `kan ikke godkjenne deaktivert varsel`() {
        definisjonDao.lagreDefinisjon(UUID.randomUUID(), "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val vedtaksperiodeId = UUID.randomUUID()
        generasjonDao.opprettFor(vedtaksperiodeId, UUID.randomUUID())
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        repository.deaktiverFor(vedtaksperiodeId, "EN_KODE", null)
        repository.godkjennFor(vedtaksperiodeId, "EN_KODE", "EN_IDENT", null)
        assertEquals(INAKTIV, varselDao.finnVarselstatus(vedtaksperiodeId, "EN_KODE"))
    }

    @Test
    fun `nytt varsel er aktivt`() {
        definisjonDao.lagreDefinisjon(UUID.randomUUID(), "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val vedtaksperiodeId = UUID.randomUUID()
        generasjonDao.opprettFor(vedtaksperiodeId, UUID.randomUUID())
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        assertAktiv(vedtaksperiodeId, "EN_KODE")
    }

    @Test
    fun `varsel har ikke aktiv-status lenger når det er godkjent`() {
        definisjonDao.lagreDefinisjon(UUID.randomUUID(), "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val vedtaksperiodeId = UUID.randomUUID()
        generasjonDao.opprettFor(vedtaksperiodeId, UUID.randomUUID())
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        repository.godkjennFor(vedtaksperiodeId, "EN_KODE", "EN_IDENT", null)
        assertGodkjent(vedtaksperiodeId, "EN_KODE")
    }

    @Test
    fun `varsel er ikke aktivt når det er deaktivert`() {
        definisjonDao.lagreDefinisjon(UUID.randomUUID(), "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val vedtaksperiodeId = UUID.randomUUID()
        generasjonDao.opprettFor(vedtaksperiodeId, UUID.randomUUID())
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        repository.deaktiverFor(vedtaksperiodeId, "EN_KODE", null)
        assertInaktiv(vedtaksperiodeId, "EN_KODE")
    }

    @Test
    fun `finner varsler for vedtaksperiode`() {
        definisjonDao.lagreDefinisjon(UUID.randomUUID(), "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        definisjonDao.lagreDefinisjon(UUID.randomUUID(), "EN_ANNEN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val vedtaksperiodeId = UUID.randomUUID()
        generasjonDao.opprettFor(vedtaksperiodeId, UUID.randomUUID())
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        repository.lagreVarsel(UUID.randomUUID(), "EN_ANNEN_KODE", LocalDateTime.now(), vedtaksperiodeId)

        assertEquals(2, repository.finnVarslerFor(vedtaksperiodeId).size)
    }

    @Test
    fun `Lagrer kun én utgave av et aktivt varsel`() {
        definisjonDao.lagreDefinisjon(UUID.randomUUID(), "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        val vedtaksperiodeId = UUID.randomUUID()
        generasjonDao.opprettFor(vedtaksperiodeId, UUID.randomUUID())
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        repository.lagreVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)

        assertEquals(1, repository.finnVarslerFor(vedtaksperiodeId).size)
    }

    @Test
    fun `finner varsler for ulike vedtaksperioder`() {
        definisjonDao.lagreDefinisjon(UUID.randomUUID(), "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
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
        assertEquals(
            Varseldefinisjon(
                id = definisjonsId,
                varselkode = "EN_KODE",
                tittel = "EN_TITTEL",
                forklaring = "EN_FORKLARING",
                handling = "EN_HANDLING",
                avviklet = false,
                opprettet = LocalDateTime.now()
            ),
            definisjonDao.definisjonFor(definisjonsId)
        )
    }

    private fun assertDefinisjonFor(vedtaksperiodeId: UUID, varselkode: String, definisjonId: UUID) {
        @Language("PostgreSQL")
        val query =
            "SELECT 1 FROM selve_varsel WHERE vedtaksperiode_id = ? AND kode = ? AND definisjon_ref = (SELECT id FROM api_varseldefinisjon WHERE unik_id = ?)"
        val antall = sessionOf(dataSource).use { session ->
            session.run(queryOf(query, vedtaksperiodeId, varselkode, definisjonId).map { it.int(1) }.asSingle)
        }
        assertEquals(1, antall)
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