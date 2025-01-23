package no.nav.helse.mediator

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.AbstractDatabaseTest
import no.nav.helse.TestRapidHelpers.meldinger
import no.nav.helse.db.RepositoryFactoryImpl
import no.nav.helse.kafka.MessageContextMeldingPubliserer
import no.nav.helse.mediator.meldinger.PoisonPills
import no.nav.helse.modell.person.vedtaksperiode.Varselkode
import no.nav.helse.modell.varsel.Varseldefinisjon
import no.nav.helse.spesialist.test.lagFødselsnummer
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class MeldingMediatorTest : AbstractDatabaseTest() {
    private val testRapid = TestRapid()

    private val kommandofabrikk = mockk<Kommandofabrikk>(relaxed = true)

    private val meldingMediator =
        MeldingMediator(
            dataSource = dataSource,
            repositoryFactory = RepositoryFactoryImpl(dataSource),
            publiserer = MessageContextMeldingPubliserer(testRapid),
            kommandofabrikk = kommandofabrikk,
            poisonPills = PoisonPills(emptyMap())
        )

    @BeforeEach
    internal fun resetTestSetup() {
        testRapid.reset()
        lagVarseldefinisjoner()
    }

    @Test
    fun `lagre varseldefinisjon`() {
        val id = UUID.randomUUID()
        val varseldefinisjon = Varseldefinisjon(id, "SB_EX_1", "En tittel", null, null, false, LocalDateTime.now())
        meldingMediator.håndter(varseldefinisjon)
        assertVarseldefinisjon(id)
    }

    @Test
    fun `oppdater persondata`() {
        val fødselsnummer = lagFødselsnummer()
        meldingMediator.oppdaterSnapshot(fødselsnummer)
        val sisteMelding = testRapid.inspektør.meldinger().last()
        assertEquals("oppdater_persondata", sisteMelding["@event_name"].asText())
        assertEquals(fødselsnummer, sisteMelding["fødselsnummer"].asText())
    }

    @Test
    fun `klargjør person for visning`() {
        val fødselsnummer = lagFødselsnummer()
        meldingMediator.klargjørPersonForVisning(fødselsnummer)
        val sisteMelding = testRapid.inspektør.meldinger().last()
        assertEquals("klargjør_person_for_visning", sisteMelding["@event_name"].asText())
        assertEquals(fødselsnummer, sisteMelding["fødselsnummer"].asText())
    }

    private fun assertVarseldefinisjon(id: UUID) {
        @Language("PostgreSQL")
        val query = " SELECT COUNT(1) FROM api_varseldefinisjon WHERE unik_id = ? "
        val antall =
            sessionOf(dataSource).use { session ->
                session.run(queryOf(query, id).map { it.int(1) }.asSingle)
            }
        assertEquals(1, antall)
    }

    private fun lagVarseldefinisjoner() {
        Varselkode.entries.forEach { varselkode ->
            lagVarseldefinisjon(varselkode.name)
        }
    }

    private fun lagVarseldefinisjon(varselkode: String) {
        @Language("PostgreSQL")
        val query =
            """
            INSERT INTO api_varseldefinisjon (unik_id, kode, tittel, forklaring, handling, avviklet, opprettet)
            VALUES (:unik_id, :kode, :tittel, :forklaring, :handling, :avviklet, :opprettet)
            ON CONFLICT (unik_id) DO NOTHING
            """.trimIndent()
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    query,
                    mapOf(
                        "unik_id" to UUID.nameUUIDFromBytes(varselkode.toByteArray()),
                        "kode" to varselkode,
                        "tittel" to "En tittel for varselkode=$varselkode",
                        "forklaring" to "En forklaring for varselkode=$varselkode",
                        "handling" to "En handling for varselkode=$varselkode",
                        "avviklet" to false,
                        "opprettet" to LocalDateTime.now(),
                    ),
                ).asUpdate,
            )
        }
    }
}
