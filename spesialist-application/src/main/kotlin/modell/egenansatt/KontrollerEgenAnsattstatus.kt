package no.nav.helse.modell.egenansatt

import no.nav.helse.mediator.meldinger.løsninger.EgenAnsattløsning
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.melding.Behov
import no.nav.helse.spesialist.application.PersonRepository
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.domain.Identitetsnummer

internal class KontrollerEgenAnsattstatus(
    private val fødselsnummer: String,
    private val personRepository: PersonRepository,
) : Command {
    override fun execute(context: CommandContext) = behandle(context)

    override fun resume(context: CommandContext) = behandle(context)

    private fun behandle(context: CommandContext): Boolean {
        if (viHarInformasjon()) return true
        val løsning = context.get<EgenAnsattløsning>()
        if (løsning == null) {
            logg.info("Trenger informasjon om egen ansatt")
            context.behov(Behov.EgenAnsatt)
            return false
        }

        løsning.lagre(personRepository)
        return true
    }

    // Hvis vi har informasjon i databasen er den "garantert" oppdatert, pga. at vi lytter på endringer på topic fra NOM.
    private fun viHarInformasjon() = personRepository.finn(Identitetsnummer.fraString(fødselsnummer))?.egenAnsattStatus != null
}
