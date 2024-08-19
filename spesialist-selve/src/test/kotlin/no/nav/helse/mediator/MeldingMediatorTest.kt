package no.nav.helse.mediator

import io.mockk.mockk
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.AbstractDatabaseTest
import no.nav.helse.db.AvslagDao
import no.nav.helse.db.AvviksvurderingDao
import no.nav.helse.modell.stoppautomatiskbehandling.StansAutomatiskBehandlingMediator
import no.nav.helse.modell.varsel.Varseldefinisjon
import no.nav.helse.modell.varsel.Varselkode
import no.nav.helse.modell.vedtaksperiode.GenerasjonDao
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class MeldingMediatorTest : AbstractDatabaseTest() {
    private val testRapid = TestRapid()

    private val avviksvurderingDao = mockk<AvviksvurderingDao>()
    private val generasjonDao = mockk<GenerasjonDao>()
    private val avslagDao = mockk<AvslagDao>()
    private val kommandofabrikk = mockk<Kommandofabrikk>(relaxed = true)
    private val stansAutomatiskBehandlingMediator = mockk<StansAutomatiskBehandlingMediator>(relaxed = true)

    private val meldingMediator =
        MeldingMediator(
            dataSource = dataSource,
            rapidsConnection = testRapid,
            kommandofabrikk = kommandofabrikk,
            avviksvurderingDao = avviksvurderingDao,
            stansAutomatiskBehandlingMediator = stansAutomatiskBehandlingMediator,
            generasjonDao = generasjonDao,
            avslagDao = avslagDao,
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
