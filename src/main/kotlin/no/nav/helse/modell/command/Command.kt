package no.nav.helse.modell.command

import kotliquery.Session
import no.nav.helse.Oppgavestatus
import no.nav.helse.modell.Behovtype
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

abstract class Command(
    protected val eventId: UUID,
    private val parent: Command?,
    internal val timeout: Duration
) {
    protected val log: Logger = LoggerFactory.getLogger("command")
    internal open val oppgaver: Set<Command> = setOf()
    internal val oppgavetype: String = requireNotNull(this::class.simpleName)

    internal abstract fun execute(session: Session): Resultat
    internal open fun resume(session: Session, løsninger: Løsninger) {}

    sealed class Resultat(private val oppgavestatus: Oppgavestatus) {
        internal fun tilOppgavestatus() = oppgavestatus
        sealed class Ok : Resultat(Oppgavestatus.Ferdigstilt) {
            object System : Ok()
            class Løst(
                internal val ferdigstiltAv: String,
                internal val oid: UUID,
                internal val løsning: Map<String, Any?>
            ) : Ok()
        }

        internal class HarBehov(internal vararg val behovstyper: Behovtype) : Resultat(Oppgavestatus.AvventerSystem)
        object TrengerSaksbehandlerInput : Resultat(Oppgavestatus.AvventerSaksbehandler)
        object Invalidert : Resultat(Oppgavestatus.Invalidert)
    }
}

