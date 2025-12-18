package no.nav.helse.modell.kommando

import no.nav.helse.db.SessionContext
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.person.HentEnhetløsning
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Person
import java.time.LocalDate

internal class OppdaterEnhetCommand(
    fødselsnummer: String,
) : Command {
    private companion object {
        private val datoForUtdatert = LocalDate.now().minusDays(5)
    }

    private val identitetsnummer = Identitetsnummer.fraString(fødselsnummer)

    override fun execute(
        context: CommandContext,
        sessionContext: SessionContext,
    ): Boolean {
        val person =
            sessionContext.personRepository.finn(identitetsnummer)
                ?: error("Fant ikke person")
        val sistOppdatert = person.enhetRefOppdatert
        if (sistOppdatert != null && sistOppdatert > datoForUtdatert) return ignorer()
        return behandle(context, sessionContext, person)
    }

    override fun resume(
        context: CommandContext,
        sessionContext: SessionContext,
    ): Boolean {
        val person =
            sessionContext.personRepository.finn(identitetsnummer)
                ?: error("Fant ikke person")
        return behandle(context, sessionContext, person)
    }

    private fun ignorer(): Boolean {
        loggInfo("har ikke behov for Enhet, informasjonen er ny nok")
        return true
    }

    private fun trengerMerInformasjon(context: CommandContext): Boolean {
        val behov = Behov.Enhet
        loggInfo("trenger oppdatert $behov")
        context.behov(behov)
        return false
    }

    private fun behandle(
        context: CommandContext,
        sessionContext: SessionContext,
        person: Person,
    ): Boolean {
        val løsning = context.get<HentEnhetløsning>() ?: return trengerMerInformasjon(context)
        loggInfo("oppdaterer enhetsnr")
        person.oppdaterEnhet(løsning.enhet())
        sessionContext.personRepository.lagre(person)
        return true
    }
}
