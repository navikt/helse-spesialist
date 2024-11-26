package no.nav.helse.modell.automatisering

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.db.PersonRepository
import no.nav.helse.db.VergemålRepository
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.CommandContext.Companion.ferdigstill
import no.nav.helse.modell.person.HentEnhetløsning
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import org.slf4j.LoggerFactory

internal class VurderAutomatiskAvvisning(
    private val personRepository: PersonRepository,
    private val vergemålRepository: VergemålRepository,
    private val godkjenningMediator: GodkjenningMediator,
    private val utbetaling: Utbetaling,
    private val godkjenningsbehov: GodkjenningsbehovData,
) : Command {
    override fun execute(context: CommandContext): Boolean {
        val fødselsnummer = godkjenningsbehov.fødselsnummer
        val vedtaksperiodeId = godkjenningsbehov.vedtaksperiodeId

        val tilhørerEnhetUtland = HentEnhetløsning.erEnhetUtland(personRepository.finnEnhetId(fødselsnummer))
        val underVergemål = vergemålRepository.harVergemål(fødselsnummer) ?: false

        if (!(tilhørerEnhetUtland || underVergemål)) return true

        val avvisningsårsaker = årsaker(tilhørerEnhetUtland, underVergemål)
        if (!godkjenningsbehov.kanAvvises) {
            logg.info(
                "Avviser ikke {} som har $avvisningsårsaker, fordi: {}",
                kv("vedtaksperiodeId", vedtaksperiodeId),
                kv("kanAvvises", false),
            )
            return true
        }

        godkjenningMediator.automatiskAvvisning(
            context,
            begrunnelser = avvisningsårsaker.toList(),
            utbetaling = utbetaling,
            behov = godkjenningsbehov,
        )
        logg.info("Automatisk avvisning av vedtaksperiode $vedtaksperiodeId pga:$avvisningsårsaker")
        return ferdigstill(context)
    }

    private fun årsaker(
        tilhørerEnhetUtland: Boolean,
        underVergemål: Boolean,
    ) = mutableListOf<String>().apply {
        if (tilhørerEnhetUtland) add("Utland")
        if (underVergemål) add("Vergemål")
    }

    private companion object {
        private val logg = LoggerFactory.getLogger(VurderAutomatiskAvvisning::class.java)
    }
}
