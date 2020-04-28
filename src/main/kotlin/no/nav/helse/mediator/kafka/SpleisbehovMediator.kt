package no.nav.helse.mediator.kafka

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.kafka.meldinger.GodkjenningMessage
import no.nav.helse.mediator.kafka.meldinger.PåminnelseMessage
import no.nav.helse.mediator.kafka.meldinger.TilInfotrygdMessage
import no.nav.helse.modell.Spleisbehov
import no.nav.helse.modell.dao.*
import no.nav.helse.modell.dto.OppgaveDto
import no.nav.helse.modell.løsning.ArbeidsgiverLøsning
import no.nav.helse.modell.løsning.HentEnhetLøsning
import no.nav.helse.modell.løsning.HentPersoninfoLøsning
import no.nav.helse.modell.løsning.SaksbehandlerLøsning
import no.nav.helse.objectMapper
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

internal class SpleisbehovMediator(
    private val spleisbehovDao: SpleisbehovDao,
    private val personDao: PersonDao,
    private val arbeidsgiverDao: ArbeidsgiverDao,
    private val vedtakDao: VedtakDao,
    private val snapshotDao: SnapshotDao,
    private val speilSnapshotRestDao: SpeilSnapshotRestDao,
    private val oppgaveDao: OppgaveDao,
    spesialistOID: UUID
) {
    private val log = LoggerFactory.getLogger(SpleisbehovMediator::class.java)
    private lateinit var rapidsConnection: RapidsConnection

    init {
        oid = spesialistOID
    }

    companion object {
        lateinit var oid: UUID
    }

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
            vedtaksperiodeReferanse = null,
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
        spleisbehovDao.insertBehov(
            godkjenningMessage.id,
            godkjenningMessage.vedtaksperiodeId,
            spleisbehov.toJson(),
            originalJson
        )
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
        restoreAndInvoke(spleisbehovId) { fortsett(løsning) }
    }

    fun håndter(spleisbehovId: UUID, løsning: SaksbehandlerLøsning) {
        log.info("Mottok godkjenningsløsning for spleis behov {}", keyValue("spleisbehovId", spleisbehovId))
        restoreAndInvoke(spleisbehovId) { fortsett(løsning) }
    }

    fun håndter(spleisbehovId: UUID, påminnelseMessage: PåminnelseMessage) {
        log.info("Mottok påminnelse for spleisbehov {}", keyValue("spleisbehovId", spleisbehovId))
        restoreAndInvoke(spleisbehovId) {}
    }

    fun håndter(vedtaksperiodeId: UUID, løsning: TilInfotrygdMessage) {
        spleisbehovDao.findBehovMedSpleisReferanse(vedtaksperiodeId)?.also { spleisbehovDBDto ->
            val nåværendeOppgave = oppgaveDao.findNåværendeOppgave(spleisbehovDBDto.id) ?: return
            log.info("Vedtaksperiode i spleis gikk TIL_INFOTRYGD")
            spleisbehov(spleisbehovDBDto.id, vedtaksperiodeId, spleisbehovDBDto.data, nåværendeOppgave)
                .invalider()
        }
    }

    fun restoreAndInvoke(spleisbehovId: UUID, invoke: Spleisbehov.() -> Unit) {
        val spleisbehovDBDto = requireNotNull(spleisbehovDao.findBehov(spleisbehovId)) {
            "Fant ikke behov med id $spleisbehovId"
        }
        val spleisbehov = spleisbehov(spleisbehovId, spleisbehovDBDto.spleisReferanse, spleisbehovDBDto.data)
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
                    "@id" to UUID.randomUUID(),
                    "@opprettet" to LocalDateTime.now(),
                    "timeout" to spleisbehov.currentTimeout().toSeconds(),
                    "spleisbehovId" to spleisbehovId,
                    "fødselsnummer" to spleisbehov.fødselsnummer,
                    "endringstidspunkt" to LocalDateTime.now(),
                    "ferdigstilt" to spleisbehov.ferdigstilt()
                )
            )
        )
    }

    private fun spleisbehov(
        id: UUID,
        spleisReferanse: UUID,
        spleisbehovJson: String,
        nåværendeOppgave: OppgaveDto = requireNotNull(oppgaveDao.findNåværendeOppgave(id)) { "Svar på behov krever at det er en nåværende oppgave" }
    ) = Spleisbehov.restore(
        id = id,
        vedtaksperiodeId = spleisReferanse,
        data = spleisbehovJson,
        personDao = personDao,
        arbeidsgiverDao = arbeidsgiverDao,
        vedtakDao = vedtakDao,
        snapshotDao = snapshotDao,
        speilSnapshotRestDao = speilSnapshotRestDao,
        oppgaveDao = oppgaveDao,
        nåværendeOppgave = nåværendeOppgave
    )
}
