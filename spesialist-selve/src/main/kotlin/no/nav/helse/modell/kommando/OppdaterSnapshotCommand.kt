package no.nav.helse.modell.kommando

import java.util.UUID
import no.nav.helse.mediator.api.graphql.SnapshotClient
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.vedtak.Warning
import org.slf4j.LoggerFactory

internal class OppdaterSnapshotCommand(
    private val snapshotClient: SnapshotClient,
    private val snapshotDao: SnapshotDao,
    private val vedtaksperiodeId: UUID,
    private val fødselsnummer: String,
    private val warningDao: WarningDao,
    private val personDao: PersonDao,
) : Command {

    private companion object {
        private val log = LoggerFactory.getLogger(OppdaterSnapshotCommand::class.java)
        private val sikkerlogger = LoggerFactory.getLogger("tjenestekall")
    }

    override fun execute(context: CommandContext): Boolean {
        return if (personDao.findPersonByFødselsnummer(fødselsnummer) != null) oppdaterSnapshot() else ignorer()
    }

    private fun oppdaterSnapshot(): Boolean {
        log.info("oppdaterer snapshot for $vedtaksperiodeId")
        return snapshotClient.hentSnapshot(fnr = fødselsnummer).data?.person?.let { person ->
            snapshotDao.lagre(fødselsnummer = fødselsnummer, snapshot = person)
            log.info("oppdaterer warnings fra graphql-snapshot for $vedtaksperiodeId")
            warningDao.oppdaterSpleisWarnings(vedtaksperiodeId, Warning.graphQLWarnings(vedtaksperiodeId, person))
            true
        } ?: false
    }

    private fun ignorer(): Boolean {
        log.info("Kjenner ikke til person, henter ikke snapshot for vedtaksperiodeId=$vedtaksperiodeId (se sikkerlogg for detaljer.)")
        sikkerlogger.info("Kjenner ikke til person fnr=$fødselsnummer, henter ikke snapshot for vedtaksperiodeId=$vedtaksperiodeId.")
        return true
    }
}
