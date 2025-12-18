package no.nav.helse.modell.kommando

import no.nav.helse.db.SessionContext
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.person.HentPersoninfoløsning
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Person
import java.time.LocalDate

internal class OppdaterPersoninfoCommand(
    fødselsnummer: String,
    private val force: Boolean,
) : Command {
    private companion object {
        private val BEHOV = Behov.Personinfo
        private val datoForUtdatert = LocalDate.now().minusDays(14)
    }

    private val identitetsnummer = Identitetsnummer.fraString(fødselsnummer)

    override fun execute(
        context: CommandContext,
        sessionContext: SessionContext,
    ): Boolean {
        val person =
            sessionContext.personRepository.finn(identitetsnummer)
                ?: error("Fant ikke person")
        val sistOppdatert = person.infoOppdatert
        if (sistOppdatert != null && sistOppdatert > datoForUtdatert && !force) return ignorer()
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
        logg.info("har ikke behov for ${BEHOV::class.simpleName}, informasjonen er ny nok")
        return true
    }

    private fun behandle(
        context: CommandContext,
        sessionContext: SessionContext,
        person: Person,
    ): Boolean {
        val løsning = context.get<HentPersoninfoløsning>() ?: return trengerMerInformasjon(context)
        logg.info("oppdaterer personinfo")
        person.oppdaterInfo(løsning.personinfo())
        sessionContext.personRepository.lagre(person)
        return true
    }

    private fun trengerMerInformasjon(context: CommandContext): Boolean {
        logg.info("trenger oppdatert ${BEHOV::class.simpleName}")
        context.behov(BEHOV)
        return false
    }
}
