package no.nav.helse.modell.automatisering

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.db.PersonRepository
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.CommandContext.Companion.ferdigstill
import no.nav.helse.modell.person.HentEnhetløsning
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import no.nav.helse.modell.vergemal.VergemålDao
import org.slf4j.LoggerFactory

internal class VurderAutomatiskAvvisning(
    private val personRepository: PersonRepository,
    private val vergemålDao: VergemålDao,
    private val godkjenningMediator: GodkjenningMediator,
    private val utbetaling: Utbetaling,
    private val godkjenningsbehov: GodkjenningsbehovData,
) : Command {
    override fun execute(context: CommandContext): Boolean {
        val fødselsnummer = godkjenningsbehov.fødselsnummer
        val vedtaksperiodeId = godkjenningsbehov.vedtaksperiodeId
        val kanAvvises = godkjenningsbehov.kanAvvises

        val tilhørerEnhetUtland = HentEnhetløsning.erEnhetUtland(personRepository.finnEnhetId(fødselsnummer))
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
            begrunnelser = avvisningsårsaker.toList(),
            utbetaling = utbetaling,
            godkjenningsbehov = godkjenningsbehov,
        )
        logg.info("Automatisk avvisning av vedtaksperiode $vedtaksperiodeId pga:$avvisningsårsaker")
        return ferdigstill(context)
    }

    private companion object {
        private val logg = LoggerFactory.getLogger(VurderAutomatiskAvvisning::class.java)
    }
}
