package no.nav.helse.modell.egenansatt

import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.meldinger.løsninger.EgenAnsattløsning
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.melding.Behov
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.PersonRepository
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.domain.Identitetsnummer

internal class KontrollerEgenAnsattstatus(
    private val fødselsnummer: String,
    private val personRepository: PersonRepository,
) : Command {
    override fun execute(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ) = behandle(commandContext)

    override fun resume(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ) = behandle(commandContext)

    private fun behandle(commandContext: CommandContext): Boolean {
        if (viHarInformasjon()) return true
        val løsning = commandContext.get<EgenAnsattløsning>()
        if (løsning == null) {
            logg.info("Trenger informasjon om egen ansatt")
            commandContext.behov(Behov.EgenAnsatt)
            return false
        }

        løsning.lagre(personRepository)
        return true
    }

    // Hvis vi har informasjon i databasen er den "garantert" oppdatert, pga. at vi lytter på endringer på topic fra NOM.
    private fun viHarInformasjon() = personRepository.finn(Identitetsnummer.fraString(fødselsnummer))?.egenAnsattStatus != null
}
