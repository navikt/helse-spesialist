package no.nav.helse.modell.kommando

import no.nav.helse.db.SessionContext
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.person.HentPersoninfoløsning
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Person
import java.time.LocalDate

internal class OppdaterPersoninfoCommand(
    private val identitetsnummer: Identitetsnummer,
    private val force: Boolean,
) : Command {
    private companion object {
        private val BEHOV = Behov.Personinfo
        private val datoForUtdatert = LocalDate.now().minusDays(14)
    }

    override fun execute(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        val person =
            sessionContext.personRepository.finn(this@OppdaterPersoninfoCommand.identitetsnummer)
                ?: error("Fant ikke person")
        val sistOppdatert = person.infoOppdatert
        if (sistOppdatert != null && sistOppdatert > datoForUtdatert && !force) return ignorer()
        return behandle(commandContext, sessionContext, person)
    }

    override fun resume(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        outbox: Outbox,
    ): Boolean {
        val person =
            sessionContext.personRepository.finn(this@OppdaterPersoninfoCommand.identitetsnummer)
                ?: error("Fant ikke person")
        return behandle(commandContext, sessionContext, person)
    }

    private fun ignorer(): Boolean {
        logg.info("har ikke behov for ${BEHOV::class.simpleName}, informasjonen er ny nok")
        return true
    }

    private fun behandle(
        commandContext: CommandContext,
        sessionContext: SessionContext,
        person: Person,
    ): Boolean {
        val løsning = commandContext.get<HentPersoninfoløsning>() ?: return trengerMerInformasjon(commandContext)
        logg.info("oppdaterer personinfo")
        person.oppdaterInfo(løsning.personinfo())
        sessionContext.personRepository.lagre(person)
        return true
    }

    private fun trengerMerInformasjon(commandContext: CommandContext): Boolean {
        logg.info("trenger oppdatert ${BEHOV::class.simpleName}")
        commandContext.behov(BEHOV)
        return false
    }
}
