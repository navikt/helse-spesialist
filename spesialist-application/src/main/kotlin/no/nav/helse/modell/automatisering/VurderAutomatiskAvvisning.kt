package no.nav.helse.modell.automatisering

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.CommandContext.Companion.ferdigstill
import no.nav.helse.modell.person.HentEnhetløsning
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.logg.logg

internal class VurderAutomatiskAvvisning(
    private val godkjenningsbehov: GodkjenningsbehovData,
) : Command {
    override fun execute(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        val fødselsnummer = godkjenningsbehov.fødselsnummer
        val vedtaksperiodeId = godkjenningsbehov.vedtaksperiodeId

        val tilhørerEnhetUtland = HentEnhetløsning.erEnhetUtland(sessionContext.personDao.finnEnhetId(fødselsnummer))
        val underVergemål = sessionContext.vergemålDao.harVergemål(fødselsnummer) ?: false

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

        GodkjenningMediator(sessionContext.opptegnelseRepository).automatiskAvvisning(
            outbox = outbox,
            begrunnelser = avvisningsårsaker.toList(),
            behov = godkjenningsbehov,
        )
        logg.info("Automatisk avvisning av vedtaksperiode $vedtaksperiodeId pga:$avvisningsårsaker")
        return ferdigstill(commandContext)
    }

    private fun årsaker(
        tilhørerEnhetUtland: Boolean,
        underVergemål: Boolean,
    ) = mutableListOf<String>().apply {
        if (tilhørerEnhetUtland) add("Utland")
        if (underVergemål) add("Vergemål")
    }
}
