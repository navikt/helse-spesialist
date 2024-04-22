package no.nav.helse.modell.automatisering

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
import java.util.UUID

internal class VurderAutomatiskAvvisning(
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val spleisBehandlingId: UUID?,
    private val personDao: PersonDao,
    private val vergemålDao: VergemålDao,
    private val godkjenningMediator: GodkjenningMediator,
    private val hendelseId: UUID,
    private val utbetaling: Utbetaling,
    private val kanAvvises: Boolean,
) : Command {
    override fun execute(context: CommandContext): Boolean {
        val tilhørerEnhetUtland = HentEnhetløsning.erEnhetUtland(personDao.finnEnhetId(fødselsnummer))
        val avvisGrunnetEnhetUtland = tilhørerEnhetUtland && kanAvvises
        val underVergemål = vergemålDao.harVergemål(fødselsnummer) ?: false
        val avvisGrunnetVergemål = underVergemål && kanAvvises

        val avvisningsårsaker = mutableListOf<String>()
        if (tilhørerEnhetUtland) avvisningsårsaker.add("Utland")
        if (underVergemål) avvisningsårsaker.add("Vergemål")
        if (!avvisGrunnetEnhetUtland && !avvisGrunnetVergemål) {
            if (avvisningsårsaker.size > 0) {
                logg.info(
                    "Avviser ikke {} som har $avvisningsårsaker, fordi: {}",
                    kv("vedtaksperiodeId", vedtaksperiodeId),
                    kv("kanAvvises", kanAvvises),
                )
            }
            return true
        }

        godkjenningMediator.automatiskAvvisning(
            publiserer = context::publiser,
            vedtaksperiodeId = vedtaksperiodeId,
            begrunnelser = avvisningsårsaker.toList(),
            utbetaling = utbetaling,
            hendelseId = hendelseId,
            spleisBehandlingId = spleisBehandlingId,
        )
        logg.info("Automatisk avvisning av vedtaksperiode $vedtaksperiodeId pga:$avvisningsårsaker")
        return ferdigstill(context)
    }

    private companion object {
        private val logg = LoggerFactory.getLogger(VurderAutomatiskAvvisning::class.java)
    }
}
