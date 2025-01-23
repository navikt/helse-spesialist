package no.nav.helse.modell.automatisering

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.db.PersonDao
import no.nav.helse.db.VergemålRepository
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.CommandContext.Companion.ferdigstill
import no.nav.helse.modell.person.HentEnhetløsning
import no.nav.helse.modell.person.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import org.slf4j.LoggerFactory

internal class VurderAutomatiskAvvisning(
    private val personDao: PersonDao,
    private val vergemålRepository: VergemålRepository,
    private val godkjenningMediator: GodkjenningMediator,
    private val utbetaling: Utbetaling,
    private val godkjenningsbehov: GodkjenningsbehovData,
    private val sykefraværstilfelle: Sykefraværstilfelle,
) : Command {
    override fun execute(context: CommandContext): Boolean {
        val fødselsnummer = godkjenningsbehov.fødselsnummer
        val vedtaksperiodeId = godkjenningsbehov.vedtaksperiodeId

        val stoppesForManglendeIM =
            sykefraværstilfelle.harVarselOmManglendeInntektsmelding(vedtaksperiodeId) &&
                !(fødselsnummer.length == 11 && (1..31).contains(fødselsnummer.take(2).toInt()))

        // Midlertid logging så lenge vi potensielt avviser disse.
        if (sykefraværstilfelle.harVarselOmManglendeInntektsmelding(vedtaksperiodeId)) {
            sikkerlogg.info("Mottatt godkjenningsbehov med varsel for manglende IM for fnr $fødselsnummer, avvises=$stoppesForManglendeIM")
        }

        val tilhørerEnhetUtland = HentEnhetløsning.erEnhetUtland(personDao.finnEnhetId(fødselsnummer))
        val underVergemål = vergemålRepository.harVergemål(fødselsnummer) ?: false

        if (!(tilhørerEnhetUtland || underVergemål || stoppesForManglendeIM)) return true

        val avvisningsårsaker = årsaker(tilhørerEnhetUtland, underVergemål, stoppesForManglendeIM)
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
        manglerIM: Boolean,
    ) = mutableListOf<String>().apply {
        if (tilhørerEnhetUtland) add("Utland")
        if (underVergemål) add("Vergemål")
        if (manglerIM) add("Mangler inntektsmelding")
    }

    private companion object {
        private val logg = LoggerFactory.getLogger(VurderAutomatiskAvvisning::class.java)
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}
