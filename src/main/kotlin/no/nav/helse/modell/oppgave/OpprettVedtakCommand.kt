package no.nav.helse.modell.oppgave

import no.nav.helse.modell.dao.*
import java.time.Duration
import java.time.LocalDate
import java.util.*

internal class OpprettVedtakCommand(
    private val personDao: PersonDao,
    private val arbeidsgiverDao: ArbeidsgiverDao,
    private val vedtakDao: VedtakDao,
    private val snapshotDao: SnapshotDao,
    private val speilSnapshotRestDao: SpeilSnapshotRestDao,
    private val fødselsnummer: String,
    private val orgnummer: String,
    private val vedtaksperiodeId: UUID,
    private val periodeFom: LocalDate,
    private val periodeTom: LocalDate,
    behovId: UUID,
    parent: Command
) : Command(
    behovId = behovId,
    parent = parent,
    timeout = Duration.ofHours(1)
) {
    override fun execute(): Resultat {
        log.info("Henter snapshot for vedtaksperiode: $vedtaksperiodeId")
        val speilSnapshot = speilSnapshotRestDao.hentSpeilSpapshot(fødselsnummer)
        val snapshotId = snapshotDao.insertSpeilSnapshot(speilSnapshot)
        val personRef = requireNotNull(personDao.findPersonByFødselsnummer(fødselsnummer.toLong()))
        val arbeidsgiverRef = requireNotNull(arbeidsgiverDao.findArbeidsgiverByOrgnummer(orgnummer.toLong()))
        vedtakDao.insertVedtak(
            vedtaksperiodeId = vedtaksperiodeId,
            fom = periodeFom,
            tom = periodeTom,
            personRef = personRef,
            arbeidsgiverRef = arbeidsgiverRef,
            speilSnapshotRef = snapshotId
        )

        return Resultat.Ok.System
    }

}
