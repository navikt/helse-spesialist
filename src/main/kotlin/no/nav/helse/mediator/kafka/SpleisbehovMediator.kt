package no.nav.helse.mediator.kafka

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.kafka.meldinger.*
import no.nav.helse.modell.Behov
import no.nav.helse.modell.Godkjenningsbehov
import no.nav.helse.modell.dao.*
import no.nav.helse.modell.dto.OppgaveDto
import no.nav.helse.modell.løsning.ArbeidsgiverLøsning
import no.nav.helse.modell.løsning.HentEnhetLøsning
import no.nav.helse.modell.løsning.HentPersoninfoLøsning
import no.nav.helse.modell.løsning.SaksbehandlerLøsning
import no.nav.helse.modell.oppgave.Command
import no.nav.helse.modell.oppgave.CommandExecutor
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
    private val spesialistOID: UUID
) {
    private val log = LoggerFactory.getLogger(SpleisbehovMediator::class.java)
    private lateinit var rapidsConnection: RapidsConnection
    private var shutdown = false

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
        val spleisbehovExecutor = CommandExecutor(
            command = Godkjenningsbehov(
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
                speilSnapshotRestDao = speilSnapshotRestDao
            ),
            spesialistOid = spesialistOID,
            eventId = godkjenningMessage.id,
            nåværendeOppgave = null,
            oppgaveDao = oppgaveDao,
            vedtakDao = vedtakDao,
            loggingData = *arrayOf(
                keyValue("vedtaksperiodeId", godkjenningMessage.vedtaksperiodeId),
                keyValue("spleisbehovId", godkjenningMessage.id)
            )
        )
        log.info(
            "Mottok Godkjenning behov med {}, {}",
            keyValue("vedtaksperiodeId", godkjenningMessage.vedtaksperiodeId),
            keyValue("spleisbehovId", godkjenningMessage.id)
        )
        val resultater = spleisbehovExecutor.execute()
        spleisbehovDao.insertBehov(
            godkjenningMessage.id,
            godkjenningMessage.vedtaksperiodeId,
            spleisbehovExecutor.command.toJson(),
            originalJson
        )
        godkjenningMessage.warnings.forEach {
            spleisbehovDao.insertWarning(
                melding = it,
                spleisbehovRef = godkjenningMessage.id
            )
        }
        publiserBehov(
            spleisreferanse = godkjenningMessage.id,
            resultater = resultater,
            spleisbehovExecutor = spleisbehovExecutor
        )
    }

    internal fun håndter(vedtaksperiodeId: UUID, annullering: AnnulleringMessage) {
        log.info("Publiserer annullering på fagsystemId ${annullering.fagsystemId} for vedtaksperiode $vedtaksperiodeId")
        rapidsConnection.publish(annullering.fødselsnummer, annullering.toJson())
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
            spleisbehovExecutor(spleisbehovDBDto.id, vedtaksperiodeId, spleisbehovDBDto.data, nåværendeOppgave)
                .invalider()
        }
    }

    fun håndter(vedtaksperiodeId: UUID, vedtaksperiodeEndretMessage: VedtaksperiodeEndretMessage) {}

    private fun restoreAndInvoke(eventId: UUID, invoke: CommandExecutor.() -> Unit) {
        if (shutdown) {
            throw IllegalStateException("Stopper håndtering av behov når appen er i shutdown")
        }

        val spleisbehovDBDto = requireNotNull(spleisbehovDao.findBehov(eventId)) {
            "Fant ikke behov med id $eventId"
        }

        val nåværendeOppgave = requireNotNull(oppgaveDao.findNåværendeOppgave(eventId)) {
            "Svar på behov krever at det er en nåværende oppgave"
        }

        val spleisbehovExecutor = spleisbehovExecutor(
            id = eventId,
            spleisReferanse = spleisbehovDBDto.spleisReferanse,
            spleisbehovJson = spleisbehovDBDto.data,
            nåværendeOppgave = nåværendeOppgave
        )
        spleisbehovExecutor.invoke()


        val resultater = spleisbehovExecutor.execute()
        spleisbehovDao.updateBehov(eventId, spleisbehovExecutor.command.toJson())
        publiserBehov(
            spleisreferanse = eventId,
            resultater = resultater,
            spleisbehovExecutor = spleisbehovExecutor
        )
    }

    private fun publiserBehov(
        spleisreferanse: UUID,
        resultater: List<Command.Resultat>,
        spleisbehovExecutor: CommandExecutor
    ) {
        val behovstyper = resultater.filterIsInstance<Command.Resultat.HarBehov>().flatMap { it.behovstyper.toList() }
        if (behovstyper.isNotEmpty()) {
            val behov = Behov(
                typer = behovstyper,
                fødselsnummer = spleisbehovExecutor.command.fødselsnummer,
                orgnummer = spleisbehovExecutor.command.orgnummer,
                spleisBehovId = spleisreferanse,
                vedtaksperiodeId = spleisbehovExecutor.command.vedtaksperiodeId
            )
            log.info(
                "Sender ut behov for {}, {}, {}",
                keyValue("vedtaksperiodeId", behov.vedtaksperiodeId),
                keyValue("spleisbehovId", behov.spleisBehovId),
                keyValue("behov", behov.typer.toString())
            )
            rapidsConnection.publish(spleisbehovExecutor.command.fødselsnummer, behov.toJson())
        }

        resultater
            .filterIsInstance<Command.Resultat.Ok.Løst>()
            .forEach { løst ->
                val originalJson = requireNotNull(spleisbehovDao.findOriginalBehov(spleisreferanse))
                val løsningJson = objectMapper.readValue<ObjectNode>(originalJson)
                løsningJson.set<ObjectNode>("@løsning", objectMapper.convertValue<JsonNode>(løst.løsning))
                rapidsConnection.publish(spleisbehovExecutor.command.fødselsnummer, løsningJson.toString())
            }

        log.info(
            "Produserer oppgave endret event for {}, {}",
            keyValue("spleisbehovId", spleisreferanse),
            keyValue("vedtaksperiodeId", spleisbehovExecutor.command.vedtaksperiodeId)
        )

        rapidsConnection.publish(
            objectMapper.writeValueAsString(
                mapOf(
                    "@event_name" to "oppgave_oppdatert",
                    "@id" to UUID.randomUUID(),
                    "@opprettet" to LocalDateTime.now(),
                    "timeout" to spleisbehovExecutor.currentTimeout().toSeconds(),
                    "spleisbehovId" to spleisreferanse,
                    "fødselsnummer" to spleisbehovExecutor.command.fødselsnummer,
                    "endringstidspunkt" to LocalDateTime.now(),
                    "ferdigstilt" to (resultater.last() is Command.Resultat.Ok)
                )
            )
        )
    }

    fun shutdown() {
        shutdown = true
    }

    private fun spleisbehovExecutor(
        id: UUID,
        spleisReferanse: UUID,
        spleisbehovJson: String,
        nåværendeOppgave: OppgaveDto
    ) = CommandExecutor(
        command = Godkjenningsbehov.restore(
            id = id,
            vedtaksperiodeId = spleisReferanse,
            data = spleisbehovJson,
            personDao = personDao,
            arbeidsgiverDao = arbeidsgiverDao,
            vedtakDao = vedtakDao,
            snapshotDao = snapshotDao,
            speilSnapshotRestDao = speilSnapshotRestDao
        ),
        spesialistOid = spesialistOID,
        eventId = id,
        nåværendeOppgave = nåværendeOppgave,
        oppgaveDao = oppgaveDao,
        vedtakDao = vedtakDao,
        loggingData = *arrayOf(keyValue("vedtaksperiodeId", spleisReferanse), keyValue("spleisbehovId", id))
    )
}
