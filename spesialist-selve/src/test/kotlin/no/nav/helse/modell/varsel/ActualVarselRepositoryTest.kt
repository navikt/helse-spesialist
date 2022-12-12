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
import no.nav.helse.modell.vedtaksperiode.ActualGenerasjonRepository
import no.nav.helse.modell.vedtaksperiode.Generasjon
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class ActualVarselRepositoryTest : AbstractDatabaseTest() {

    private val generasjonRepository = ActualGenerasjonRepository(dataSource)
    private val varselRepository = ActualVarselRepository(dataSource)
    private val definisjonDao = DefinisjonDao(dataSource)

    private lateinit var definisjonId: UUID
    private lateinit var vedtaksperiodeId: UUID
    private lateinit var generasjonId: UUID
    private lateinit var generasjon: Generasjon

    @BeforeEach
    fun beforeEach() {
        definisjonId = UUID.randomUUID()
        vedtaksperiodeId = UUID.randomUUID()
        varselRepository.lagreDefinisjon(definisjonId, "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        generasjonId = UUID.randomUUID()

        generasjon = generasjonRepository.opprettFørste(vedtaksperiodeId, UUID.randomUUID(), generasjonId)!!
    }

    @Test
    fun `kan lagre varsel dersom det finnes en generasjon for perioden`() {
        varselRepository.lagreVarsel(UUID.randomUUID(), generasjonId, "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        assertAktiv(generasjonId, "EN_KODE")
    }

    @Test
    fun `kan ikke lagre varsel dersom det ikke finnes en generasjon for perioden`() {
        assertThrows<Exception> {
            varselRepository.lagreVarsel(UUID.randomUUID(), UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        }
    }

    @Test
    fun `kan godkjenne varsel`() {
        varselRepository.lagreVarsel(UUID.randomUUID(), generasjonId, "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        varselRepository.godkjennFor(vedtaksperiodeId, generasjonId, "EN_KODE", "EN_IDENT", null)
        assertEquals(GODKJENT, statusFor(generasjonId, "EN_KODE"))
    }

    @Test
    fun `godkjenning av varsel med definisjonId medfører at varselet lagres med referanse til denne definisjonen`() {
        varselRepository.lagreVarsel(UUID.randomUUID(), generasjonId, "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        varselRepository.godkjennFor(vedtaksperiodeId, generasjonId, "EN_KODE", "EN_IDENT", definisjonId)
        assertEquals(GODKJENT, statusFor(generasjonId, "EN_KODE"))
        assertDefinisjonFor(vedtaksperiodeId, "EN_KODE", definisjonId)
    }

    @Test
    fun `avvisning av varsel med definisjonId medfører at varselet lagres med referanse til denne definisjonen`() {
        generasjon.håndterNyttVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), varselRepository)
        generasjon.håndterAvvist("EN_KODE", varselRepository)
        assertEquals(AVVIST, statusFor(generasjonId, "EN_KODE"))
        assertDefinisjonFor(vedtaksperiodeId, "EN_KODE", definisjonId)
    }

    @Test
    fun `deaktivering av varsel med definisjonId medfører at varselet lagres med referanse til denne definisjonen`() {
        varselRepository.lagreVarsel(UUID.randomUUID(), generasjonId, "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        varselRepository.deaktiverFor(vedtaksperiodeId, generasjonId, "EN_KODE", definisjonId)
        assertEquals(INAKTIV, statusFor(generasjonId, "EN_KODE"))
        assertDefinisjonFor(vedtaksperiodeId, "EN_KODE", definisjonId)
    }

    @Test
    fun `kan avvise varsel`() {
        varselRepository.lagreVarsel(UUID.randomUUID(), generasjonId, "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        varselRepository.avvisFor(vedtaksperiodeId, generasjonId, "EN_KODE", "EN_IDENT", null)
        assertEquals(AVVIST, statusFor(generasjonId, "EN_KODE"))
    }

    @Test
    fun `kan deaktivere varsel`() {
        varselRepository.lagreVarsel(UUID.randomUUID(), generasjonId, "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        varselRepository.deaktiverFor(vedtaksperiodeId, generasjonId, "EN_KODE", null)
        assertEquals(INAKTIV, statusFor(generasjonId, "EN_KODE"))
    }

    @Test
    fun `nytt varsel er aktivt`() {
        varselRepository.lagreVarsel(UUID.randomUUID(), generasjonId, "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        assertAktiv(generasjonId, "EN_KODE")
    }

    @Test
    fun `varsel har ikke lenger aktiv-status når det er godkjent`() {
        varselRepository.lagreVarsel(UUID.randomUUID(), generasjonId, "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        varselRepository.godkjennFor(vedtaksperiodeId, generasjonId, "EN_KODE", "EN_IDENT", null)
        assertGodkjent(generasjonId, "EN_KODE")
    }

    @Test
    fun `varsel har ikke lenger aktiv-status når det er avvist`() {
        varselRepository.lagreVarsel(UUID.randomUUID(), generasjonId, "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        varselRepository.avvisFor(vedtaksperiodeId, generasjonId, "EN_KODE", "EN_IDENT", null)
        assertAvvist(generasjonId, "EN_KODE")
    }

    @Test
    fun `varsel har ikke lenger aktiv-status når det er deaktivert`() {
        varselRepository.lagreVarsel(UUID.randomUUID(), generasjonId, "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        varselRepository.deaktiverFor(vedtaksperiodeId, generasjonId, "EN_KODE", null)
        assertInaktiv(generasjonId, "EN_KODE")
    }

    @Test
    fun `oppdatering av varsel for én generasjon endrer ikke varsel for en annen generasjon på samme periode`() {
        generasjon.håndterNyttVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), varselRepository)
        generasjonRepository.låsFor(vedtaksperiodeId, UUID.randomUUID())
        generasjon = generasjonRepository.sisteFor(vedtaksperiodeId)
        val nesteGenerasjonId = UUID.randomUUID()
        val nesteGenerasjon = generasjon.håndterNyGenerasjon(UUID.randomUUID(), nesteGenerasjonId)
        nesteGenerasjon?.håndterNyttVarsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), varselRepository)
        nesteGenerasjon?.håndterDeaktivertVarsel("EN_KODE", varselRepository)
        assertAktiv(generasjonId, "EN_KODE")
        assertInaktiv(nesteGenerasjonId, "EN_KODE")
    }

    @Test
    fun `lagre definisjon`() {
        val definisjonId = UUID.randomUUID()
        varselRepository.lagreDefinisjon(definisjonId, "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        assertEquals(
            Varseldefinisjon(
                id = definisjonId,
                varselkode = "EN_KODE",
                tittel = "EN_TITTEL",
                forklaring = "EN_FORKLARING",
                handling = "EN_HANDLING",
                avviklet = false,
                opprettet = LocalDateTime.now()
            ),
            definisjonDao.definisjonFor(definisjonId)
        )
    }

    private fun statusFor(generasjonId: UUID, varselkode: String): Varsel.Status? {
        @Language("PostgreSQL")
        val query = "SELECT status FROM selve_varsel WHERE generasjon_ref = (SELECT id FROM selve_vedtaksperiode_generasjon WHERE unik_id = ? LIMIT 1) and kode = ?;"

        return sessionOf(dataSource).use { session ->
            session.run(queryOf(query, generasjonId, varselkode).map {
                enumValueOf<Varsel.Status>(it.string(1))
            }.asSingle)
        }
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

    private fun assertAktiv(generasjonId: UUID, varselkode: String) {
        val status = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT status FROM selve_varsel WHERE generasjon_ref = (SELECT id FROM selve_vedtaksperiode_generasjon WHERE unik_id = ? LIMIT 1) AND kode = ?;"
            session.run(
                queryOf(
                    query,
                    generasjonId,
                    varselkode
                ).map { it.string(1) }.asSingle
            )
        }
        assertEquals("AKTIV", status)
    }

    private fun assertInaktiv(generasjonId: UUID, varselkode: String) {
        val status = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT status FROM selve_varsel WHERE generasjon_ref = (SELECT id FROM selve_vedtaksperiode_generasjon WHERE unik_id = ? LIMIT 1) AND kode = ?;"
            session.run(
                queryOf(
                    query,
                    generasjonId,
                    varselkode
                ).map { it.string(1) }.asSingle
            )
        }
        assertEquals("INAKTIV", status)
    }

    private fun assertGodkjent(generasjonId: UUID, varselkode: String) {
        val status = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT status FROM selve_varsel WHERE generasjon_ref = (SELECT id FROM selve_vedtaksperiode_generasjon WHERE unik_id = ?) AND kode = ?;"
            session.run(
                queryOf(
                    query,
                    generasjonId,
                    varselkode
                ).map { it.string(1) }.asSingle
            )
        }
        assertEquals("GODKJENT", status)
    }

    private fun assertAvvist(generasjonId: UUID, varselkode: String) {
        val status = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT status FROM selve_varsel WHERE generasjon_ref = (SELECT id FROM selve_vedtaksperiode_generasjon WHERE unik_id = ?) AND kode = ?;"
            session.run(
                queryOf(
                    query,
                    generasjonId,
                    varselkode
                ).map { it.string(1) }.asSingle
            )
        }
        assertEquals("AVVIST", status)
    }
}