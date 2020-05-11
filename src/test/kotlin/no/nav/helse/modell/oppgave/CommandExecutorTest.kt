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

    @Test
    internal fun `persisterer hvem som ferdigstilte den siste oppgaven`() {
        val behovId = UUID.randomUUID()
        val testCommand = TestCommand(behovId)
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
}

class TestCommand(behovId: UUID, override val fødselsnummer: String = "12345") : RootCommand(behovId, Duration.ZERO) {
    override fun execute() = Resultat.Ok.System
    override val orgnummer: String? = null
    override val vedtaksperiodeId: UUID? = null
    override fun toJson(): String = "{}"
}

data class FerdigstiltAv(val epost: String?, val oid: UUID?)
