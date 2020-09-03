package no.nav.helse.modell.command.nyny

import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

internal class OpprettVedtakCommand(
    private val speilSnapshotRestClient: SpeilSnapshotRestClient,
    private val fødselsnummer: String,
    private val orgnummer: String,
    private val vedtaksperiodeId: UUID,
    private val periodeFom: LocalDate,
    private val periodeTom: LocalDate,
    private val personDao: PersonDao,
    private val arbeidsgiverDao: ArbeidsgiverDao,
    private val snapshotDao: SnapshotDao,
    private val vedtakDao: VedtakDao
) : Command {
    private companion object {
        private val log = LoggerFactory.getLogger(OpprettVedtakCommand::class.java)
    }

    override fun execute(context: CommandContext): Boolean {
        log.info("Henter snapshot for vedtaksperiode: $vedtaksperiodeId")
        val speilSnapshot = speilSnapshotRestClient.hentSpeilSpapshot(fødselsnummer)
        val snapshotId = snapshotDao.insertSpeilSnapshot(speilSnapshot)
        val personRef = requireNotNull(personDao.findPersonByFødselsnummer(fødselsnummer.toLong()))
        val arbeidsgiverRef = requireNotNull(arbeidsgiverDao.findArbeidsgiverByOrgnummer(orgnummer.toLong()))
        log.info("Oppretter vedtak for vedtaksperiode: $vedtaksperiodeId for person=$personRef, arbeidsgiver=$arbeidsgiverRef")
        vedtakDao.upsertVedtak(
            vedtaksperiodeId = vedtaksperiodeId,
            fom = periodeFom,
            tom = periodeTom,
            personRef = personRef,
            arbeidsgiverRef = arbeidsgiverRef,
            speilSnapshotRef = snapshotId
        )
        return true
    }

}
