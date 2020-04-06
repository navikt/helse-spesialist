package no.nav.helse.mediator.kafka

import no.nav.helse.mediator.kafka.meldinger.GodkjenningMessage
import no.nav.helse.modell.Spleisbehov
import no.nav.helse.modell.dao.*
import no.nav.helse.modell.dao.SpeilSnapshotRestDao
import no.nav.helse.modell.dao.SpleisbehovDao
import no.nav.helse.modell.dao.VedtakDao
import no.nav.helse.modell.løsning.ArbeidsgiverLøsning
import no.nav.helse.modell.løsning.HentEnhetLøsning
import no.nav.helse.modell.løsning.HentPersoninfoLøsning
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.util.*

internal class SpleisbehovMediator(
    private val spleisbehovDao: SpleisbehovDao,
    private val personDao: PersonDao,
    private val arbeidsgiverDao: ArbeidsgiverDao,
    private val vedtakDao: VedtakDao,
    private val snapshotDao: SnapshotDao,
    private val speilSnapshotRestDao: SpeilSnapshotRestDao,
    private val oppgaveDao: OppgaveDao
) {
    private val log = LoggerFactory.getLogger(SpleisbehovMediator::class.java)

    internal fun håndter(godkjenningMessage: GodkjenningMessage) {
        val spleisbehov = Spleisbehov(
            id = godkjenningMessage.id,
            fødselsnummer = godkjenningMessage.fødselsnummer,
            periodeFom = godkjenningMessage.periodeFom,
            periodeTom = godkjenningMessage.periodeTom,
            vedtaksperiodeId = godkjenningMessage.vedtaksperiodeId,
            aktørId = godkjenningMessage.aktørId,
            orgnummer = godkjenningMessage.organisasjonsnummer,
            personDao = personDao,
            arbeidsgiverDao = arbeidsgiverDao,
            vedtakDao = vedtakDao,
            snapshotDao = snapshotDao,
            speilSnapshotRestDao = speilSnapshotRestDao,
            oppgaveDao = oppgaveDao
        )
        spleisbehov.execute()
//        spleisbehov.behov()?.also { behov ->
//            context.send(behov.fødselsnummer, behov.toJson())
//        }
        spleisbehovDao.insertBehov(spleisbehov.id, spleisbehov.toJson())
    }

    internal fun håndter(
        spleisbehovId: UUID,
        behandlendeEnhet: HentEnhetLøsning?,
        hentPersoninfoLøsning: HentPersoninfoLøsning?
    ) {
        val spleisbehovJson = spleisbehovDao.findBehov(spleisbehovId)
        if (spleisbehovJson == null) {
            log.error("Fant ikke behov med id $spleisbehovId")
            return
        }
        val spleisbehov = spleisbehov(spleisbehovId, spleisbehovJson)
        behandlendeEnhet?.also(spleisbehov::fortsett)
        hentPersoninfoLøsning?.also(spleisbehov::fortsett)
        spleisbehov.execute()
        spleisbehovDao.updateBehov(spleisbehovId, spleisbehov.toJson())
    }

    fun håndter(spleisbehovId: UUID, løsning: ArbeidsgiverLøsning) {
        val spleisbehovJson = spleisbehovDao.findBehov(spleisbehovId)
        if (spleisbehovJson == null) {
            log.error("Fant ikke behov med id $spleisbehovId")
            return
        }
        val spleisbehov = spleisbehov(spleisbehovId, spleisbehovJson)
        spleisbehov.fortsett(løsning)
        spleisbehov.execute()
        spleisbehovDao.updateBehov(spleisbehovId, spleisbehov.toJson())
    }

    private fun spleisbehov(id: UUID, spleisbehovJson: String) = Spleisbehov.restore(
        id, spleisbehovJson, personDao,
        arbeidsgiverDao,
        vedtakDao,
        snapshotDao,
        speilSnapshotRestDao,
        oppgaveDao
    )
}
