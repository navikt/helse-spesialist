package no.nav.helse.modell.command

import no.nav.helse.Oppgavestatus
import no.nav.helse.modell.Behovtype
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverLøsning
import no.nav.helse.modell.person.HentEnhetLøsning
import no.nav.helse.modell.person.HentInfotrygdutbetalingerLøsning
import no.nav.helse.modell.person.HentPersoninfoLøsning
import no.nav.helse.modell.vedtak.SaksbehandlerLøsning
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

abstract class Command(
    protected val behovId: UUID,
    private val parent: Command?,
    internal val timeout: Duration
) {
    protected val log: Logger = LoggerFactory.getLogger("command")
    internal open val oppgaver: Set<Command> = setOf()
    internal val oppgavetype: String = requireNotNull(this::class.simpleName)

    internal abstract fun execute(): Resultat
    internal open fun fortsett(løsning: HentEnhetLøsning) {}
    internal open fun fortsett(løsning: HentPersoninfoLøsning) {}
    internal open fun fortsett(løsning: ArbeidsgiverLøsning) {}
    internal open fun fortsett(løsning: SaksbehandlerLøsning) {}
    internal open fun fortsett(løsning: HentInfotrygdutbetalingerLøsning) {}

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

