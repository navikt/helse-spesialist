package no.nav.helse.modell.kommando

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.modell.SpeilSnapshotDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.vedtak.Warning
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.objectMapper
import org.slf4j.LoggerFactory
import java.util.*
import no.nav.helse.modell.person.PersonDao

internal class OppdaterSpeilSnapshotCommand(
    private val speilSnapshotRestClient: SpeilSnapshotRestClient,
    private val warningDao: WarningDao,
    private val personDao: PersonDao,
    private val speilSnapshotDao: SpeilSnapshotDao,
    private val vedtaksperiodeId: UUID,
    private val fødselsnummer: String
) : Command {

    private companion object {
        private val log = LoggerFactory.getLogger(OppdaterSpeilSnapshotCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        if (personDao.findPersonByFødselsnummer(fødselsnummer) == null) return ignorer()
        return oppdaterSnapshot()
    }

    private fun ignorer(): Boolean {
        log.info("kjenner ikke til vedtaksperiode $vedtaksperiodeId")
        return true
    }

    private fun oppdaterSnapshot(): Boolean {
        log.info("oppdaterer snapshot for $vedtaksperiodeId")
        return speilSnapshotRestClient.hentSpeilSnapshot(fødselsnummer).let { snapshot ->
            speilSnapshotDao.lagre(fødselsnummer, snapshot)
            log.info("oppdaterer warnings for $vedtaksperiodeId")
            warningDao.oppdaterSpleisWarnings(vedtaksperiodeId, Warning.warnings(vedtaksperiodeId, objectMapper.readValue(snapshot)))
            true
        }
    }
}
