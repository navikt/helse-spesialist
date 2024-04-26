package no.nav.helse.modell.varsel

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.AbstractDatabaseTest
import no.nav.helse.januar
import no.nav.helse.modell.person.vedtaksperiode.Varsel
import no.nav.helse.modell.person.vedtaksperiode.Varsel.Status.INAKTIV
import no.nav.helse.modell.vedtaksperiode.Generasjon
import no.nav.helse.modell.vedtaksperiode.GenerasjonDao
import no.nav.helse.modell.vedtaksperiode.GenerasjonRepository
import no.nav.helse.modell.vedtaksperiode.Periode
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.UUID

internal class VarselRepositoryTest : AbstractDatabaseTest() {
    private val generasjonRepository = GenerasjonRepository(dataSource)
    private val varselRepository = VarselRepository(dataSource)
    private val definisjonDao = DefinisjonDao(dataSource)
    private val generasjonDao = GenerasjonDao(dataSource)

    private lateinit var definisjonId: UUID
    private lateinit var vedtaksperiodeId: UUID
    private lateinit var generasjonId: UUID
    private lateinit var generasjon: Generasjon

    @BeforeEach
    fun beforeEach() {
        definisjonId = UUID.randomUUID()
        vedtaksperiodeId = UUID.randomUUID()
        val definisjonDto =
            VarseldefinisjonDto(definisjonId, "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        varselRepository.lagreDefinisjon(definisjonDto)
        generasjonId = UUID.randomUUID()

        generasjon =
            generasjonDao.opprettFor(
                generasjonId,
                vedtaksperiodeId,
                UUID.randomUUID(),
                1.januar,
                Periode(1.januar, 31.januar),
                Generasjon.VidereBehandlingAvklares,
            )
        generasjon.registrer(generasjonRepository, varselRepository)
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
    fun `varsel godkjent`() {
        val varselId = UUID.randomUUID()
        varselRepository.varselOpprettet(varselId, vedtaksperiodeId, generasjonId, "EN_KODE", LocalDateTime.now())
        varselRepository.varselGodkjent(varselId, "EN_KODE", generasjonId, vedtaksperiodeId, "Spesialsak")
        assertGodkjent(generasjonId, "EN_KODE")
    }

    @Test
    fun `varsel har ikke lenger aktiv-status når det er deaktivert`() {
        val varselId = UUID.randomUUID()
        varselRepository.varselOpprettet(varselId, vedtaksperiodeId, generasjonId, "EN_KODE", LocalDateTime.now())
        varselRepository.varselDeaktivert(varselId, "EN_KODE", generasjonId, vedtaksperiodeId)
        assertInaktiv(generasjonId, "EN_KODE")
    }

    @Test
    fun `lagre definisjon`() {
        val definisjonId = UUID.randomUUID()
        val definisjonDto =
            VarseldefinisjonDto(definisjonId, "EN_KODE", "EN_TITTEL", "EN_FORKLARING", "EN_HANDLING", false, LocalDateTime.now())
        varselRepository.lagreDefinisjon(definisjonDto)
        assertEquals(
            Varseldefinisjon(
                id = definisjonId,
                varselkode = "EN_KODE",
                tittel = "EN_TITTEL",
                forklaring = "EN_FORKLARING",
                handling = "EN_HANDLING",
                avviklet = false,
                opprettet = LocalDateTime.now(),
            ),
            definisjonDao.definisjonFor(definisjonId),
        )
    }

    private fun statusFor(
        generasjonId: UUID,
        varselkode: String,
    ): Varsel.Status? {
        @Language("PostgreSQL")
        val query = "SELECT status FROM selve_varsel WHERE generasjon_ref = (SELECT id FROM selve_vedtaksperiode_generasjon WHERE unik_id = ? LIMIT 1) and kode = ?;"

        return sessionOf(dataSource).use { session ->
            session.run(
                queryOf(query, generasjonId, varselkode).map {
                    enumValueOf<Varsel.Status>(it.string(1))
                }.asSingle,
            )
        }
    }

    private fun assertAktiv(
        generasjonId: UUID,
        varselkode: String,
    ) {
        val status =
            sessionOf(dataSource).use { session ->
                @Language("PostgreSQL")
                val query = "SELECT status FROM selve_varsel WHERE generasjon_ref = (SELECT id FROM selve_vedtaksperiode_generasjon WHERE unik_id = ? LIMIT 1) AND kode = ?;"
                session.run(
                    queryOf(
                        query,
                        generasjonId,
                        varselkode,
                    ).map { it.string(1) }.asSingle,
                )
            }
        assertEquals("AKTIV", status)
    }

    private fun assertGodkjent(
        generasjonId: UUID,
        varselkode: String,
    ) {
        val status =
            sessionOf(dataSource).use { session ->
                @Language("PostgreSQL")
                val query = "SELECT status FROM selve_varsel WHERE generasjon_ref = (SELECT id FROM selve_vedtaksperiode_generasjon WHERE unik_id = ? LIMIT 1) AND kode = ?;"
                session.run(
                    queryOf(
                        query,
                        generasjonId,
                        varselkode,
                    ).map { it.string(1) }.asSingle,
                )
            }
        assertEquals("GODKJENT", status)
    }

    private fun assertInaktiv(
        generasjonId: UUID,
        varselkode: String,
    ) {
        val status =
            sessionOf(dataSource).use { session ->
                @Language("PostgreSQL")
                val query = "SELECT status FROM selve_varsel WHERE generasjon_ref = (SELECT id FROM selve_vedtaksperiode_generasjon WHERE unik_id = ? LIMIT 1) AND kode = ?;"
                session.run(
                    queryOf(
                        query,
                        generasjonId,
                        varselkode,
                    ).map { it.string(1) }.asSingle,
                )
            }
        assertEquals("INAKTIV", status)
    }
}
