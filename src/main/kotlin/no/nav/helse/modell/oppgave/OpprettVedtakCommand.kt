package no.nav.helse.modell.oppgave

import no.nav.helse.Løsningstype
import no.nav.helse.modell.dao.*
import no.nav.helse.modell.dao.SpeilSnapshotRestDao
import no.nav.helse.modell.dao.VedtakDao
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

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
    ferdigstilt: LocalDateTime? = null
) : Command(behovId, ferdigstilt, Løsningstype.System) {

    override fun execute() {
        log.info("Henter snapshot for vedtaksperiode: $vedtaksperiodeId")
        val speilSnapshot = speilSnapshotRestDao.hentSpeilSpapshot(fødselsnummer)
        val snapshotId = snapshotDao.insertSpeilSnapshot(speilSnapshot)
        val personRef = requireNotNull(personDao.findPersonByFødselsnummer(fødselsnummer.toLong()))
        val arbeidsgiverRef = requireNotNull(arbeidsgiverDao.findArbeidsgiverByOrgnummer(orgnummer.toLong()))
        val id = vedtakDao.insertVedtak(
            vedtaksperiodeId = vedtaksperiodeId,
            fom = periodeFom,
            tom = periodeTom,
            personRef = personRef,
            arbeidsgiverRef = arbeidsgiverRef,
            speilSnapshotRef = snapshotId
        )
        ferdigstilt = LocalDateTime.now()
    }

}
