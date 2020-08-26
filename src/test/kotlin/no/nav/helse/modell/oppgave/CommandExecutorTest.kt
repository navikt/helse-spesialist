package no.nav.helse.modell.oppgave

import AbstractEndToEndTest
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.modell.command.Command
import no.nav.helse.modell.command.CommandExecutor
import no.nav.helse.modell.command.MacroCommand
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.*

class CommandExecutorTest : AbstractEndToEndTest() {
    private lateinit var session:Session
    private val spesialistOID: UUID = UUID.randomUUID()
    private val saksbehandlerOID = UUID.randomUUID()

    @BeforeAll
    fun setup() {
        session = sessionOf(dataSource)
    }

    @AfterAll
    fun cleanup() {
        session.close()
    }

    @Test
    internal fun `persisterer hvem som ferdigstilte den siste oppgaven`() {
        val eventId = UUID.randomUUID()
        val testCommand = TestMacroCommand(eventId)
        val executor = CommandExecutor(
            command = testCommand,
            session = session,
            spesialistOid = spesialistOID,
            eventId = eventId,
            nåværendeOppgave = null
        )
        executor.execute()
        val oppgaverForBehov = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            session.run(
                queryOf(
                    "SELECT * FROM oppgave where event_id=?;",
                    eventId
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
        val eventId = UUID.randomUUID()
        val command = NestedCommand(eventId, "12356543")
        val executor = CommandExecutor(
            session = session,
            command = command,
            spesialistOid = spesialistOID,
            eventId = eventId,
            nåværendeOppgave = null
        )

        executor.execute()
        assertTrue(command.innerExecuted)
        assertTrue(command.innerInnerExecuted)
    }

    inner class TestMacroCommand(behovId: UUID, override val fødselsnummer: String = "12345") :
        MacroCommand(behovId, Duration.ZERO) {
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
        eventId: UUID,
        override val fødselsnummer: String
    ) : MacroCommand(eventId = eventId, timeout = Duration.ZERO) {
        override val orgnummer: String? = null
        override val vedtaksperiodeId: UUID? = null

        override fun toJson() = "{}"

        override fun execute(session: Session): Resultat {
            return Resultat.Ok.System
        }

        override val oppgaver: Set<Command> = setOf(InnerCommand())
        internal var innerExecuted = false
        internal var innerInnerExecuted = false

        inner class InnerCommand : Command(eventId = eventId, parent = this, timeout = Duration.ZERO) {
            override val oppgaver: Set<Command> = setOf(InnerInnerCommand())

            inner class InnerInnerCommand : Command(eventId = eventId, parent = this, timeout = Duration.ZERO) {
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
