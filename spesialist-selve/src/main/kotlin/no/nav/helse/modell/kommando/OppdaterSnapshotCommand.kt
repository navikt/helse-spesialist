package no.nav.helse.modell.kommando

import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.spesialist.api.snapshot.SnapshotClient
import org.slf4j.LoggerFactory

internal class OppdaterSnapshotCommand(
    private val snapshotClient: SnapshotClient,
    private val snapshotDao: SnapshotDao,
    private val fødselsnummer: String,
    private val personDao: PersonDao,
) : Command {
    private companion object {
        private val logg = LoggerFactory.getLogger(OppdaterSnapshotCommand::class.java)
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    override fun execute(context: CommandContext): Boolean {
        // findPersoninfoRef for å se om vi kun har minimal person
        return if (
            personDao.findPersonByFødselsnummer(fødselsnummer) != null &&
            personDao.findPersoninfoRef(fødselsnummer) != null
        ) {
            oppdaterSnapshot()
        } else {
            ignorer()
        }
    }

    private fun oppdaterSnapshot(): Boolean {
        logg.info("Oppdaterer snapshot")
        sikkerlogg.info("Oppdaterer snapshot fødselsnummer=$fødselsnummer")
        return snapshotClient.hentSnapshot(fnr = fødselsnummer).data?.person?.let { person ->
            snapshotDao.lagre(fødselsnummer = fødselsnummer, snapshot = person)
            true
        } ?: run {
            logg.warn("Kunne ikke hente snapshot - dette betyr at kommandokjeden stopper opp")
            false
        }
    }

    private fun ignorer(): Boolean {
        logg.info("Kjenner ikke til person, henter ikke snapshot (se sikkerlogg for detaljer)")
        sikkerlogg.info("Kjenner ikke til person fødselsnummer=$fødselsnummer, henter ikke snapshot")
        return true
    }
}
