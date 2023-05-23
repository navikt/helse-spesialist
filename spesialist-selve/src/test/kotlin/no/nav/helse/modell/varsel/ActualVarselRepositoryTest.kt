package no.nav.helse.modell.varsel

import java.time.LocalDateTime
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.AbstractDatabaseTest
import no.nav.helse.januar
import no.nav.helse.mediator.meldinger.Varseldefinisjon
import no.nav.helse.modell.varsel.Varsel.Status.AVVIST
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

        generasjon = Generasjon(generasjonId, vedtaksperiodeId, 1.januar, 1.januar, 31.januar)
        generasjon.registrer(generasjonRepository, varselRepository)
        generasjon.håndterVedtaksperiodeOpprettet(UUID.randomUUID())
    }

    @Test
    fun `kan lagre varsel dersom det finnes en generasjon for perioden`() {
        varselRepository.varselOpprettet(UUID.randomUUID(), vedtaksperiodeId, generasjonId, "EN_KODE", LocalDateTime.now())
        assertAktiv(generasjonId, "EN_KODE")
    }

    @Test
    fun `kan ikke lagre varsel dersom det ikke finnes en generasjon for perioden`() {
        assertThrows<Exception> {
            varselRepository.varselOpprettet(UUID.randomUUID(), vedtaksperiodeId, UUID.randomUUID(), "EN_KODE", LocalDateTime.now())
        }
    }

    @Test
    fun `avvisning av varsel med definisjonId medfører at varselet lagres med referanse til denne definisjonen`() {
        generasjon.håndterNyttVarsel(Varsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId), UUID.randomUUID())
        generasjon.håndterAvvistAvSaksbehandler("EN_KODE")
        assertEquals(AVVIST, statusFor(generasjonId, "EN_KODE"))
        assertDefinisjonFor(vedtaksperiodeId, "EN_KODE", definisjonId)
    }

    @Test
    fun `kan deaktivere varsel`() {
        val varselId = UUID.randomUUID()
        varselRepository.varselOpprettet(varselId, vedtaksperiodeId, generasjonId, "EN_KODE", LocalDateTime.now())
        varselRepository.varselDeaktivert(varselId, "EN_KODE", generasjonId, vedtaksperiodeId)
        assertEquals(INAKTIV, statusFor(generasjonId, "EN_KODE"))
    }

    @Test
    fun `nytt varsel er aktivt`() {
        varselRepository.varselOpprettet(UUID.randomUUID(), vedtaksperiodeId, generasjonId, "EN_KODE", LocalDateTime.now())
        assertAktiv(generasjonId, "EN_KODE")
    }

    @Test
    fun `varsel har ikke lenger aktiv-status når det er deaktivert`() {
        val varselId = UUID.randomUUID()
        varselRepository.varselOpprettet(varselId, vedtaksperiodeId, generasjonId, "EN_KODE", LocalDateTime.now())
        varselRepository.varselDeaktivert(varselId, "EN_KODE", generasjonId, vedtaksperiodeId)
        assertInaktiv(generasjonId, "EN_KODE")
    }

    @Test
    fun `oppdatering av varsel for én generasjon endrer ikke varsel for en annen generasjon på samme periode`() {
        generasjon.håndterNyttVarsel(Varsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId), UUID.randomUUID())
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), UUID.randomUUID())
        generasjon.håndterGodkjentAvSaksbehandler("EN_IDENT", UUID.randomUUID())
        generasjon.håndterVedtakFattet(UUID.randomUUID())
        val nesteGenerasjonId = UUID.randomUUID()
        val nesteGenerasjon = generasjon.håndterVedtaksperiodeEndret(UUID.randomUUID(), nesteGenerasjonId)
        val varsel = Varsel(UUID.randomUUID(), "EN_KODE", LocalDateTime.now(), vedtaksperiodeId)
        nesteGenerasjon?.håndterNyttVarsel(varsel, UUID.randomUUID())
        nesteGenerasjon?.håndterDeaktivertVarsel(varsel)
        assertGodkjent(generasjonId, "EN_KODE")
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
}