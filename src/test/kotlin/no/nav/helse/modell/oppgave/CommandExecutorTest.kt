package no.nav.helse.modell.oppgave

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.modell.dao.OppgaveDao
import no.nav.helse.modell.dao.VedtakDao
import no.nav.helse.setupDataSourceMedFlyway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.*

class CommandExecutorTest {
    private val dataSource = setupDataSourceMedFlyway()
    private val vedtakDao = VedtakDao(dataSource)
    private val oppgaveDao = OppgaveDao(dataSource)
    private val spesialistOID: UUID = UUID.randomUUID()
    private val saksbehandlerOID = UUID.randomUUID()

    @Test
    internal fun `persisterer hvem som ferdigstilte den siste oppgaven`() {
        val behovId = UUID.randomUUID()
        val testCommand = TestRootCommand(behovId)
        val executor = CommandExecutor(testCommand, spesialistOID, behovId, null, oppgaveDao, vedtakDao)
        executor.execute()
        val oppgaverForBehov = using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "SELECT * FROM oppgave where behov_id=?;",
                    behovId
                ).map {
                    FerdigstiltAv(
                        it.stringOrNull("ferdigstilt_av"),
                        it.stringOrNull("ferdigstilt_av_oid")?.let(UUID::fromString)
                    )
                }
                    .asList
            )
        }
        assertEquals(listOf(FerdigstiltAv("tbd@nav.no", spesialistOID)), oppgaverForBehov)
    }

    inner class TestRootCommand(behovId: UUID, override val fødselsnummer: String = "12345") : RootCommand(behovId, Duration.ZERO) {
        override fun execute() = Resultat.Ok.System
        override val orgnummer: String? = null
        override val vedtaksperiodeId: UUID? = null
        override fun toJson(): String = "{}"
        override val oppgaver: Set<Command> = setOf(TestCommand(behovId, this))
    }

    inner class TestCommand(behovId: UUID, parent: Command) : Command(behovId, parent, Duration.ZERO) {
        override fun execute() = Resultat.Ok.Løst("saksbehandler@nav.no", saksbehandlerOID, mapOf())
    }

    data class FerdigstiltAv(val epost: String?, val oid: UUID?)
}
