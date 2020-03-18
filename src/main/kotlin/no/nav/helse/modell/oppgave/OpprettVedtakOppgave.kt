package no.nav.helse.modell.oppgave

import no.nav.helse.modell.SpleisBehov
import no.nav.helse.modell.dao.ArbeidsgiverDao
import no.nav.helse.modell.dao.PersonDao
import no.nav.helse.modell.dao.VedtakDao
import java.time.LocalDateTime

internal class OpprettVedtakOppgave(
    private val spleisBehov: SpleisBehov,
    private val personDao: PersonDao,
    private val arbeidsgiverDao: ArbeidsgiverDao,
    private val vedtakDao: VedtakDao
) : Oppgave() {
    override var ferdigstilt: LocalDateTime? = null

    override fun execute() {
        // TODO: Hent speil_snapshot
        // Oppdater speil_snapshot
        val speilSnapshotRef = 123
        val personRef = requireNotNull(personDao.finnPerson(spleisBehov.fødselsnummer.toLong()))
        val arbeidsgiverRef = requireNotNull(arbeidsgiverDao.finnArbeidsgiver(spleisBehov.orgnummer.toLong()))
        val id = vedtakDao.insertVedtak(
            vedtaksperiodeId = spleisBehov.vedtaksperiodeId,
            fom = spleisBehov.periodeFom,
            tom = spleisBehov.periodeTom,
            personRef = personRef,
            arbeidsgiverRef = arbeidsgiverRef,
            speilSnapshotRef = speilSnapshotRef
        )
        ferdigstilt = LocalDateTime.now()
    }

}
