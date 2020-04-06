package no.nav.helse.modell.oppgave

import no.nav.helse.modell.Spleisbehov
import no.nav.helse.modell.dao.*
import no.nav.helse.modell.dao.SpeilSnapshotRestDao
import no.nav.helse.modell.dao.VedtakDao
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

internal class OpprettVedtakCommand(
    private val spleisbehov: Spleisbehov,
    private val personDao: PersonDao,
    private val arbeidsgiverDao: ArbeidsgiverDao,
    private val vedtakDao: VedtakDao,
    private val snapshotDao: SnapshotDao,
    private val speilSnapshotRestDao: SpeilSnapshotRestDao
) : Command() {
    override var ferdigstilt: LocalDateTime? = null
    private val log = LoggerFactory.getLogger(OpprettVedtakCommand::class.java)
    override fun execute() {
        log.info("Henter snapshot for vedtaksperiode: ${spleisbehov.vedtaksperiodeId}")
        val speilSnapshot = speilSnapshotRestDao.hentSpeilSpapshot(spleisbehov.fødselsnummer)
        val snapshotId = snapshotDao.insertSpeilSnapshot(speilSnapshot)
        val personRef = requireNotNull(personDao.findPerson(spleisbehov.fødselsnummer.toLong()))
        val arbeidsgiverRef = requireNotNull(arbeidsgiverDao.findArbeidsgiver(spleisbehov.orgnummer.toLong()))
        val id = vedtakDao.insertVedtak(
            vedtaksperiodeId = spleisbehov.vedtaksperiodeId,
            fom = spleisbehov.periodeFom,
            tom = spleisbehov.periodeTom,
            personRef = personRef,
            arbeidsgiverRef = arbeidsgiverRef,
            speilSnapshotRef = snapshotId
        )
        ferdigstilt = LocalDateTime.now()
    }

}
