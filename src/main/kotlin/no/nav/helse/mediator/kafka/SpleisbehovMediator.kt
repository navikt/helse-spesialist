package no.nav.helse.mediator.kafka

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.kafka.meldinger.GodkjenningMessage
import no.nav.helse.mediator.kafka.meldinger.PåminnelseMessage
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
import no.nav.helse.modell.løsning.SaksbehandlerLøsning
import no.nav.helse.objectMapper
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

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
    private lateinit var rapidsConnection: RapidsConnection

    internal fun init(rapidsConnection: RapidsConnection) {
        this.rapidsConnection = rapidsConnection
    }

    internal fun håndter(godkjenningMessage: GodkjenningMessage, originalJson: String) {
        if (spleisbehovDao.findBehov(godkjenningMessage.id) != null) {
            log.warn(
                "Mottok duplikat godkjenning behov, {}, {}",
                keyValue("spleisbehovId", godkjenningMessage.id),
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
            keyValue("spleisbehovId", godkjenningMessage.id)
        )
        spleisbehov.execute()
        spleisbehovDao.insertBehov(godkjenningMessage.id, spleisbehov.toJson(), originalJson)
        publiserBehov(godkjenningMessage.id, spleisbehov)
    }

    internal fun håndter(
        spleisbehovId: UUID,
        behandlendeEnhet: HentEnhetLøsning?,
        hentPersoninfoLøsning: HentPersoninfoLøsning?
    ) {
        log.info("Mottok personinfo løsning for spleis behov {}", keyValue("spleisbehovId", spleisbehovId))
        restoreAndInvoke(spleisbehovId) {
            behandlendeEnhet?.also(::fortsett)
            hentPersoninfoLøsning?.also(::fortsett)
        }
    }

    fun håndter(spleisbehovId: UUID, løsning: ArbeidsgiverLøsning) {
        log.info("Mottok arbeidsgiver løsning for spleis behov {}", keyValue("spleisbehovId", spleisbehovId))
        restoreAndInvoke(spleisbehovId) {
            fortsett(løsning)
        }
    }

    fun håndter(spleisbehovId: UUID, løsning: SaksbehandlerLøsning) {
        log.info("Mottok godkjenningsløsning for spleis behov {}", keyValue("spleisbehovId", spleisbehovId))
        restoreAndInvoke(spleisbehovId) {
            fortsett(løsning)
        }
    }

    fun håndter(spleisbehovId: UUID, påminnelseMessage: PåminnelseMessage) {
        log.info("Mottok påminnelse for spleisbehov", keyValue("spleisbehovId", spleisbehovId))
        restoreAndInvoke(spleisbehovId) {}
    }

    fun restoreAndInvoke(spleisbehovId: UUID, invoke: Spleisbehov.() -> Unit) {
        val spleisbehovJson = requireNotNull(spleisbehovDao.findBehov(spleisbehovId)) {
            "Fant ikke behov med id $spleisbehovId"
        }
        val spleisbehov = spleisbehov(spleisbehovId, spleisbehovJson)
        spleisbehov.invoke()
        spleisbehov.execute()
        spleisbehovDao.updateBehov(spleisbehovId, spleisbehov.toJson())
        publiserBehov(spleisbehovId, spleisbehov)
    }

    private fun publiserBehov(spleisbehovId: UUID, spleisbehov: Spleisbehov) {
        spleisbehov.behov()?.also { behov ->
            log.info(
                "Sender ut behov for {}, {}, {}",
                keyValue("vedtaksperiodeId", behov.vedtaksperiodeId),
                keyValue("spleisbehovId", behov.spleisBehovId),
                keyValue("behov", behov.typer.toString())
            )
            rapidsConnection.publish(spleisbehov.fødselsnummer, behov.toJson())
        }
        spleisbehov.løsning()?.also { løsning ->
            val originalJson = requireNotNull(spleisbehovDao.findOriginalBehov(spleisbehovId))
            val løsningJson = objectMapper.readValue<ObjectNode>(originalJson)
            løsningJson.set<ObjectNode>("@løsning", objectMapper.convertValue<JsonNode>(løsning))
            rapidsConnection.publish(spleisbehov.fødselsnummer, løsningJson.toString())
        }
        log.info(
            "Produserer oppgave endret event for {}, {}",
            keyValue("spleisbehovId", spleisbehovId),
            keyValue("vedtaksperiodeId", spleisbehov.vedtaksperiodeId)
        )
        rapidsConnection.publish(
            objectMapper.writeValueAsString(
                mapOf(
                    "@event_name" to "oppgave_oppdatert",
                    "timeout" to spleisbehov.currentTimeout().toSeconds(),
                    "spleisbehovId" to spleisbehovId,
                    "fødselsnummer" to spleisbehov.fødselsnummer,
                    "endringstidspunkt" to LocalDateTime.now(),
                    "ferdigstilt" to spleisbehov.ferdigstilt()
                )
            )
        )
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
