package no.nav.helse.modell.kommando

import no.nav.helse.modell.tildeling.ReservasjonDao
import no.nav.helse.tildeling.TildelingDao
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class ReserverPersonHvisTildeltCommand(
    private val fødselsnummer: String,
    private val reservasjonDao: ReservasjonDao,
    private val tildelingDao: TildelingDao
) : Command {
    private companion object {
        private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")
    }

    override fun execute(context: CommandContext): Boolean {
        val tildelingDto = tildelingDao.tildelingForPerson(fødselsnummer) ?: return true
        sikkerLogg.info("Oppretter reservasjon for $fødselsnummer til ${tildelingDto.navn} pga eksisterende tildeling")
        reservasjonDao.reserverPerson(tildelingDto.oid, fødselsnummer)
        return true
    }
}
