package no.nav.helse.mediator.kafka

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.kafka.meldinger.GodkjenningMessage
import no.nav.helse.modell.Spleisbehov
import no.nav.helse.modell.dao.ArbeidsgiverDao
import no.nav.helse.modell.dao.OppgaveDao
import no.nav.helse.modell.dao.PersonDao
import no.nav.helse.modell.dao.SnapshotDao
import no.nav.helse.modell.dao.SpeilSnapshotRestDao
import no.nav.helse.modell.dao.SpleisbehovDao
import no.nav.helse.modell.dao.VedtakDao
import no.nav.helse.modell.løsning.ArbeidsgiverLøsning
import no.nav.helse.modell.løsning.HentEnhetLøsning
import no.nav.helse.modell.løsning.HentPersoninfoLøsning
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.util.UUID

internal class SpleisbehovMediator(
    private val spleisbehovDao: SpleisbehovDao,
    private val personDao: PersonDao,
    private val arbeidsgiverDao: ArbeidsgiverDao,
    private val vedtakDao: VedtakDao,
    private val snapshotDao: SnapshotDao,
    private val speilSnapshotRestDao: SpeilSnapshotRestDao,
    private val oppgaveDao: OppgaveDao,
    private val rapidsConnection: RapidsConnection
) {
    private val log = LoggerFactory.getLogger(SpleisbehovMediator::class.java)

    internal fun håndter(godkjenningMessage: GodkjenningMessage) {
        if (spleisbehovDao.findBehov(godkjenningMessage.id) != null) {
            log.warn(
                "Mottok duplikat godkjenning behov, {}, {}",
                keyValue("spleisBehovId", godkjenningMessage.id),
                keyValue("vedtaksperiodeId", godkjenningMessage.vedtaksperiodeId)
            )
            return
        }
        val spleisbehov = Spleisbehov(
            id = godkjenningMessage.id,
            fødselsnummer = godkjenningMessage.fødselsnummer,
            periodeFom = godkjenningMessage.periodeFom,
            periodeTom = godkjenningMessage.periodeTom,
            vedtaksperiodeId = godkjenningMessage.vedtaksperiodeId,
            aktørId = godkjenningMessage.aktørId,
            orgnummer = godkjenningMessage.organisasjonsnummer,
            vedtakRef = null,
            personDao = personDao,
            arbeidsgiverDao = arbeidsgiverDao,
            vedtakDao = vedtakDao,
            snapshotDao = snapshotDao,
            speilSnapshotRestDao = speilSnapshotRestDao,
            oppgaveDao = oppgaveDao,
            nåværendeOppgave = null
        )
        log.info(
            "Mottok Godkjenning behov med {}, {}",
            keyValue("vedtaksperiodeId", godkjenningMessage.vedtaksperiodeId),
            keyValue("spleisBehovId", godkjenningMessage.id)
        )
        spleisbehov.execute()
        publiserBehov(spleisbehov)
        spleisbehovDao.insertBehov(godkjenningMessage.id, spleisbehov.toJson())
    }

    private fun publiserBehov(spleisbehov: Spleisbehov) {
        spleisbehov.behov()?.also { behov ->
            log.info(
                "Sender ut behov for {}, {}, {}",
                keyValue("vedtaksperiodeId", behov.vedtaksperiodeId),
                keyValue("spleisBehovId", behov.spleisBehovId),
                keyValue("behov", behov.typer.toString())
            )
            rapidsConnection.publish(behov.fødselsnummer, behov.toJson())
        }
    }

    internal fun håndter(
        spleisbehovId: UUID,
        behandlendeEnhet: HentEnhetLøsning?,
        hentPersoninfoLøsning: HentPersoninfoLøsning?
    ) {
        log.info("Mottok personinfo løsning for spleis behov {}", keyValue("spleisBehovId", spleisbehovId))
        val spleisbehovJson = spleisbehovDao.findBehov(spleisbehovId)
        if (spleisbehovJson == null) {
            log.error("Fant ikke behov med id $spleisbehovId")
            return
        }
        if (oppgaveDao.findNåværendeOppgave(spleisbehovId) == null) {
            log.warn("TEMP: ignorerer svar på behov uten oppgave")
            return
        }
        val spleisbehov = spleisbehov(spleisbehovId, spleisbehovJson)
        behandlendeEnhet?.also(spleisbehov::fortsett)
        hentPersoninfoLøsning?.also(spleisbehov::fortsett)
        spleisbehov.execute()
        publiserBehov(spleisbehov)
        spleisbehovDao.updateBehov(spleisbehovId, spleisbehov.toJson())
    }

    fun håndter(spleisbehovId: UUID, løsning: ArbeidsgiverLøsning) {
        log.info("Mottok arbeidsgiver løsning for spleis behov {}", keyValue("spleisBehovId", spleisbehovId))
        val spleisbehovJson = spleisbehovDao.findBehov(spleisbehovId)
        if (spleisbehovJson == null) {
            log.error("Fant ikke behov med id $spleisbehovId")
            return
        }
        val spleisbehov = spleisbehov(spleisbehovId, spleisbehovJson)
        spleisbehov.fortsett(løsning)
        spleisbehov.execute()
        publiserBehov(spleisbehov)
        spleisbehovDao.updateBehov(spleisbehovId, spleisbehov.toJson())
    }

    private fun spleisbehov(id: UUID, spleisbehovJson: String) = Spleisbehov.restore(
        id, spleisbehovJson, personDao,
        arbeidsgiverDao,
        vedtakDao,
        snapshotDao,
        speilSnapshotRestDao,
        oppgaveDao,
        requireNotNull(oppgaveDao.findNåværendeOppgave(id)) { "Svar på behov krever at det er en nåværende oppgave" }
    )
}
