package no.nav.helse.modell.automatisering

import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.CommandContext.Companion.ferdigstill
import no.nav.helse.modell.person.HentEnhetløsning
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.vergemal.VergemålDao
import org.slf4j.LoggerFactory

internal class AutomatiskAvvisningCommand(
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val personDao: PersonDao,
    private val vergemålDao: VergemålDao,
    private val godkjenningMediator: GodkjenningMediator,
    private val hendelseId: UUID,
    private val utbetaling: Utbetaling,
    private val kanAvvises: Boolean,
) : Command {

    override fun execute(context: CommandContext): Boolean {
        val tilhørerEnhetUtland = HentEnhetløsning.erEnhetUtland(personDao.finnEnhetId(fødselsnummer))
        val avvisGrunnetEnhetUtland = tilhørerEnhetUtland && !utbetaling.erRevurdering()
        val underVergemål = vergemålDao.harVergemål(fødselsnummer) ?: false
        val avvisGrunnetVergemål = underVergemål && !utbetaling.erRevurdering()

        if (!avvisGrunnetEnhetUtland && !avvisGrunnetVergemål) return true

        val årsaker = mutableListOf<String>()
        if (tilhørerEnhetUtland) årsaker.add("Utland")
        if (underVergemål) årsaker.add("Vergemål")
        if (!kanAvvises) {
            logg.info(
                "Ville ha avvist {} pga. $årsaker, men behovet er markert som ikke avvisbar",
                kv("vedtaksperiodeId", vedtaksperiodeId)
            )
            return true
        }

        godkjenningMediator.automatiskAvvisning(
            context::publiser,
            vedtaksperiodeId,
            årsaker.toList(),
            utbetaling,
            hendelseId,
        )
        logg.info("Automatisk avvisning av vedtaksperiode $vedtaksperiodeId pga:$årsaker")
        return ferdigstill(context)
    }

    private companion object {
        private val logg = LoggerFactory.getLogger(AutomatiskAvvisningCommand::class.java)
    }
}
