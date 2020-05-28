package no.nav.helse.modell.oppgave

import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.modell.command.Command
import no.nav.helse.modell.command.CommandExecutor
import no.nav.helse.modell.command.RootCommand
import no.nav.helse.setupDataSourceMedFlyway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.*

class CommandExecutorTest {
    private val dataSource = setupDataSourceMedFlyway()
    private val spesialistOID: UUID = UUID.randomUUID()
    private val saksbehandlerOID = UUID.randomUUID()

    @Test
    internal fun `persisterer hvem som ferdigstilte den siste oppgaven`() {
        val behovId = UUID.randomUUID()
        val testCommand = TestRootCommand(behovId)
        val executor = CommandExecutor(
            command = testCommand,
            spesialistOid = spesialistOID,
            eventId = behovId,
            nåværendeOppgave = null,
            dataSource = dataSource
        )
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

    @Test
    fun `command executor vil kjøre alle nestede subcommands`() {
        val behovId = UUID.randomUUID()
        val command = NestedCommand(behovId, "12356543")
        val executor = CommandExecutor(
            command = command,
            spesialistOid = spesialistOID,
            eventId = behovId,
            nåværendeOppgave = null,
            dataSource = dataSource
        )

        executor.execute()
        assertTrue(command.innerExecuted)
        assertTrue(command.innerInnerExecuted)
    }

    inner class TestRootCommand(behovId: UUID, override val fødselsnummer: String = "12345") :
        RootCommand(behovId, Duration.ZERO) {
        override fun execute(session: Session) = Resultat.Ok.System
        override val orgnummer: String? = null
        override val vedtaksperiodeId: UUID? = null
        override fun toJson(): String = "{}"
        override val oppgaver: Set<Command> = setOf(TestCommand(behovId, this))
    }

    inner class TestCommand(behovId: UUID, parent: Command) : Command(behovId, parent, Duration.ZERO) {
        override fun execute(session: Session) = Resultat.Ok.Løst("saksbehandler@nav.no", saksbehandlerOID, mapOf())
    }

    data class FerdigstiltAv(val epost: String?, val oid: UUID?)


    private class NestedCommand(
        behovId: UUID,
        override val fødselsnummer: String
    ) : RootCommand(behovId = behovId, timeout = Duration.ZERO) {
        override val orgnummer: String? = null
        override val vedtaksperiodeId: UUID? = null

        override fun toJson() = "{}"

        override fun execute(session: Session): Resultat {
            return Resultat.Ok.System
        }

        override val oppgaver: Set<Command> = setOf(InnerCommand())
        internal var innerExecuted = false
        internal var innerInnerExecuted = false

        inner class InnerCommand : Command(behovId = behovId, parent = this, timeout = Duration.ZERO) {
            override val oppgaver: Set<Command> = setOf(InnerInnerCommand())

            inner class InnerInnerCommand : Command(behovId = behovId, parent = this, timeout = Duration.ZERO) {
                override fun execute(session: Session): Resultat {
                    innerInnerExecuted = true
                    return Resultat.Ok.System
                }
            }

            override fun execute(session: Session): Resultat {
                innerExecuted = true
                return Resultat.Ok.System
            }
        }
    }
}
