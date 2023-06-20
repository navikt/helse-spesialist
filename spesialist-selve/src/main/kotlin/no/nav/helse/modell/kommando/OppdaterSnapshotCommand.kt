package no.nav.helse.modell.kommando

import java.util.UUID
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.spesialist.api.snapshot.SnapshotClient
import org.slf4j.LoggerFactory

internal class OppdaterSnapshotCommand(
    private val snapshotClient: SnapshotClient,
    private val snapshotDao: SnapshotDao,
    vedtaksperiodeId: UUID? = null,
    private val fødselsnummer: String,
    private val personDao: PersonDao,
) : Command {
    val vedtaksperiodeId: String = vedtaksperiodeId?.toString() ?: "ikke_spesifisert"

    private companion object {
        private val log = LoggerFactory.getLogger(OppdaterSnapshotCommand::class.java)
        private val sikkerlogger = LoggerFactory.getLogger("tjenestekall")
    }

    override fun execute(context: CommandContext): Boolean {
        // findPersoninfoRef for å se om vi kun har minimal person
        return if (
            personDao.findPersonByFødselsnummer(fødselsnummer) != null &&
            personDao.findPersoninfoRef(fødselsnummer) != null
        ) {
            oppdaterSnapshot()
        } else ignorer()
    }

    private fun oppdaterSnapshot(): Boolean {
        log.info("oppdaterer snapshot for vedtaksperiodeId=$vedtaksperiodeId")
        sikkerlogger.info("Oppdaterer snapshot for vedtaksperiodeId=$vedtaksperiodeId, fødselsnummer=$fødselsnummer")
        return snapshotClient.hentSnapshot(fnr = fødselsnummer).data?.person?.let { person ->
            snapshotDao.lagre(fødselsnummer = fødselsnummer, snapshot = person)
            true
        } ?: false
    }

    private fun ignorer(): Boolean {
        log.info("Kjenner ikke til person, henter ikke snapshot for vedtaksperiodeId=$vedtaksperiodeId (se sikkerlogg for detaljer.)")
        sikkerlogger.info("Kjenner ikke til person fnr=$fødselsnummer, henter ikke snapshot for vedtaksperiodeId=$vedtaksperiodeId.")
        return true
    }
}
