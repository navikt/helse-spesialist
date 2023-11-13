package no.nav.helse.mediator

import io.mockk.mockk
import java.time.LocalDateTime
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.AbstractDatabaseTest
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.mediator.oppgave.OppgaveMediator
import no.nav.helse.modell.HendelseDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.varsel.Varseldefinisjon
import no.nav.helse.modell.varsel.Varselkode
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spesialist.api.snapshot.SnapshotApiDao
import no.nav.helse.spesialist.api.snapshot.SnapshotClient
import no.nav.helse.spesialist.api.snapshot.SnapshotMediator
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import no.nav.helse.spesialist.api.abonnement.OpptegnelseDao as OpptegnelseApiDao

internal class HendelseMediatorTest : AbstractDatabaseTest() {

    private val snapshotClient = mockk<SnapshotClient>(relaxed = true)
    private val oppgaveMediator = mockk<OppgaveMediator>()
    private val automatisering = mockk<Automatisering>()

    private val testRapid = TestRapid()

    private val oppgaveDao = OppgaveDao(dataSource)
    private val vedtakDao = VedtakDao(dataSource)
    private val opptegnelseDao = OpptegnelseApiDao(dataSource)
    private val hendelseDao = HendelseDao(dataSource)
    private val utbetalingDao = UtbetalingDao(dataSource)
    private val snapshotApiDao = SnapshotApiDao(dataSource)
    private val snapshotMediator = SnapshotMediator(snapshotApiDao, snapshotClient)

    private val godkjenningMediator =
        GodkjenningMediator(vedtakDao, opptegnelseDao, oppgaveDao, utbetalingDao, hendelseDao)

    private val hendelsefabrikk = Hendelsefabrikk(
        dataSource = dataSource,
        snapshotClient = snapshotClient,
        oppgaveMediator = { oppgaveMediator },
        godkjenningMediator = godkjenningMediator,
        automatisering = automatisering,
        overstyringMediator = OverstyringMediator(testRapid),
        snapshotMediator = snapshotMediator,
        versjonAvKode = "versjonAvKode",
    )

    private val hendelseMediator = HendelseMediator(
        dataSource = dataSource,
        rapidsConnection = testRapid,
        godkjenningMediator = godkjenningMediator,
        hendelsefabrikk = hendelsefabrikk
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
        hendelseMediator.håndter(varseldefinisjon)
        assertVarseldefinisjon(id)
    }

    private fun assertVarseldefinisjon(id: UUID) {
        @Language("PostgreSQL") val query = " SELECT COUNT(1) FROM api_varseldefinisjon WHERE unik_id = ? "
        val antall = sessionOf(dataSource).use { session ->
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
        @Language("PostgreSQL") val query = """
            INSERT INTO api_varseldefinisjon (unik_id, kode, tittel, forklaring, handling, avviklet, opprettet)
            VALUES (:unik_id, :kode, :tittel, :forklaring, :handling, :avviklet, :opprettet)
            ON CONFLICT (unik_id) DO NOTHING
        """.trimIndent()
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    query, mapOf(
                        "unik_id" to UUID.nameUUIDFromBytes(varselkode.toByteArray()),
                        "kode" to varselkode,
                        "tittel" to "En tittel for varselkode=${varselkode}",
                        "forklaring" to "En forklaring for varselkode=${varselkode}",
                        "handling" to "En handling for varselkode=${varselkode}",
                        "avviklet" to false,
                        "opprettet" to LocalDateTime.now()
                    )
                ).asUpdate
            )
        }
    }
}
