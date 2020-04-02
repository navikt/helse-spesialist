package no.nav.helse.modell.oppgave

import no.nav.helse.modell.SpleisBehov
import no.nav.helse.modell.dao.*
import no.nav.helse.modell.dao.SpeilSnapshotRestDao
import no.nav.helse.modell.dao.VedtakDao
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

internal class OpprettVedtakCommand(
    private val spleisBehov: SpleisBehov,
    private val personDao: PersonDao,
    private val arbeidsgiverDao: ArbeidsgiverDao,
    private val vedtakDao: VedtakDao,
    private val snapshotDao: SnapshotDao,
    private val speilSnapshotRestDao: SpeilSnapshotRestDao
) : Command() {
    override var ferdigstilt: LocalDateTime? = null
    private val log = LoggerFactory.getLogger(OpprettVedtakCommand::class.java)
    override fun execute() {
        log.info("Henter snapshot for vedtaksperiode: ${spleisBehov.vedtaksperiodeId}")
        val speilSnapshot = speilSnapshotRestDao.hentSpeilSpapshot(spleisBehov.fødselsnummer)
        val snapshotId = snapshotDao.insertSpeilSnapshot(speilSnapshot)
        val personRef = requireNotNull(personDao.finnPerson(spleisBehov.fødselsnummer.toLong()))
        val arbeidsgiverRef = requireNotNull(arbeidsgiverDao.finnArbeidsgiver(spleisBehov.orgnummer.toLong()))
        val id = vedtakDao.insertVedtak(
            vedtaksperiodeId = spleisBehov.vedtaksperiodeId,
            fom = spleisBehov.periodeFom,
            tom = spleisBehov.periodeTom,
            personRef = personRef,
            arbeidsgiverRef = arbeidsgiverRef,
            speilSnapshotRef = snapshotId
        )
        ferdigstilt = LocalDateTime.now()
    }

}
