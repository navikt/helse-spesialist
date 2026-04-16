package no.nav.helse.modell.kommando

import no.nav.helse.db.SessionContext
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.person.HentEnhetløsning
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.PersonRepository
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Person
import java.time.LocalDate

internal class OppdaterEnhetCommand(
    fødselsnummer: String,
    private val personRepository: PersonRepository,
) : Command {
    private companion object {
        private val datoForUtdatert = LocalDate.now().minusDays(5)
    }

    private val identitetsnummer = Identitetsnummer.fraString(fødselsnummer)

    override fun execute(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        val person =
            personRepository.finn(identitetsnummer)
                ?: error("Fant ikke person")
        val sistOppdatert = person.enhetRefOppdatert
        if (sistOppdatert != null && sistOppdatert > datoForUtdatert) return ignorer()
        return behandle(commandContext, person)
    }

    override fun resume(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        val person =
            personRepository.finn(identitetsnummer)
                ?: error("Fant ikke person")
        return behandle(commandContext, person)
    }

    private fun ignorer(): Boolean {
        loggInfo("har ikke behov for Enhet, informasjonen er ny nok")
        return true
    }

    private fun trengerMerInformasjon(commandContext: CommandContext): Boolean {
        val behov = Behov.Enhet
        loggInfo("trenger oppdatert $behov")
        commandContext.behov(behov)
        return false
    }

    private fun behandle(
        commandContext: CommandContext,
        person: Person,
    ): Boolean {
        val løsning = commandContext.get<HentEnhetløsning>() ?: return trengerMerInformasjon(commandContext)
        loggInfo("oppdaterer enhetsnr")
        person.oppdaterEnhet(løsning.enhet())
        personRepository.lagre(person)
        return true
    }
}
