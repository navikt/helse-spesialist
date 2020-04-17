package no.nav.helse.modell.oppgave

import no.nav.helse.Oppgavestatus
import no.nav.helse.modell.dao.ArbeidsgiverDao
import no.nav.helse.modell.dao.PersonDao
import no.nav.helse.modell.dao.SnapshotDao
import no.nav.helse.modell.dao.SpeilSnapshotRestDao
import no.nav.helse.modell.dao.VedtakDao
import java.time.Duration
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
    parent: Command,
    ferdigstilt: LocalDateTime? = null
) : Command(
    behovId = behovId,
    initiellStatus = Oppgavestatus.AvventerSystem,
    parent = parent,
    timeout = Duration.ofHours(1)
) {

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

        oppdaterVedtakRef(id)

        ferdigstillSystem()
    }

}
