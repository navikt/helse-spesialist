package no.nav.helse.mediator.kafka

import kotliquery.sessionOf
import kotliquery.using
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.api.Rollback
import no.nav.helse.api.RollbackDelete
import no.nav.helse.measureAsHistogram
import no.nav.helse.mediator.kafka.meldinger.*
import no.nav.helse.modell.Behov
import no.nav.helse.modell.Godkjenningsbehov
import no.nav.helse.modell.OppdaterVedtaksperiode
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverLøsning
import no.nav.helse.modell.command.*
import no.nav.helse.modell.person.HentEnhetLøsning
import no.nav.helse.modell.person.HentInfotrygdutbetalingerLøsning
import no.nav.helse.modell.person.HentPersoninfoLøsning
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikoDao
import no.nav.helse.modell.vedtak.SaksbehandlerLøsning
import no.nav.helse.modell.vedtak.deleteVedtak
import no.nav.helse.modell.vedtak.snapshot.SnapshotDao
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestDao
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

internal class SpleisbehovMediator(
    private val spleisbehovDao: SpleisbehovDao,
    private val personDao: PersonDao,
    private val arbeidsgiverDao: ArbeidsgiverDao,
    private val snapshotDao: SnapshotDao,
    private val speilSnapshotRestDao: SpeilSnapshotRestDao,
    private val risikoDao: RisikoDao,
    private val dataSource: DataSource,
    private val spesialistOID: UUID
) {
    private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    private val log = LoggerFactory.getLogger(SpleisbehovMediator::class.java)
    private lateinit var rapidsConnection: RapidsConnection
    private var shutdown = false

    internal fun init(rapidsConnection: RapidsConnection) {
        this.rapidsConnection = rapidsConnection
    }

    internal fun håndter(godkjenningMessage: GodkjenningMessage, originalJson: String) {
        if (spleisbehovDao.findBehov(godkjenningMessage.id) != null) {
            log.warn(
                "Mottok duplikat godkjenningsbehov, {}, {}",
                keyValue("eventId", godkjenningMessage.id),
                keyValue("vedtaksperiodeId", godkjenningMessage.vedtaksperiodeId)
            )
            return
        }
        val spleisbehovExecutor = CommandExecutor(
            dataSource = dataSource,
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
                snapshotDao = snapshotDao,
                speilSnapshotRestDao = speilSnapshotRestDao
            ),
            spesialistOid = spesialistOID,
            eventId = godkjenningMessage.id,
            nåværendeOppgave = null,
            loggingData = *arrayOf(
                keyValue("vedtaksperiodeId", godkjenningMessage.vedtaksperiodeId),
                keyValue("eventId", godkjenningMessage.id)
            )
        )
        log.info(
            "Mottok godkjenningsbehov med {}, {}",
            keyValue("vedtaksperiodeId", godkjenningMessage.vedtaksperiodeId),
            keyValue("eventId", godkjenningMessage.id)
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
        godkjenningMessage.periodetype?.let {
            spleisbehovDao.insertSaksbehandleroppgavetype(type = it, spleisbehovRef = godkjenningMessage.id)
        }
        publiserBehov(
            spleisreferanse = godkjenningMessage.id,
            resultater = resultater,
            commandExecutor = spleisbehovExecutor
        )
    }

    internal fun håndter(vedtaksperiodeId: UUID, annullering: AnnulleringMessage) {
        log.info("Publiserer annullering på fagsystemId ${annullering.fagsystemId} for vedtaksperiode $vedtaksperiodeId")
        rapidsConnection.publish(annullering.fødselsnummer, annullering.toJson().also {
            sikkerLogg.info("sender annullering for fagsystemId=${annullering.fagsystemId} for vedtaksperiodeId=$vedtaksperiodeId:\n\t$it")
        })
    }

    internal fun håndter(
        eventId: UUID,
        behandlendeEnhet: HentEnhetLøsning?,
        hentPersoninfoLøsning: HentPersoninfoLøsning?,
        hentInfotrygdutbetalingerLøsning: HentInfotrygdutbetalingerLøsning?
    ) {
        val behovSomHarLøsninger = mapOf(
            "HentEnhet" to behandlendeEnhet,
            "HentPersoninfo" to hentPersoninfoLøsning,
            "HentInfotrygdutbetalinger" to hentInfotrygdutbetalingerLøsning
        ).filter { it.value != null }.keys.toList()

        log.info("Mottok løsninger for $behovSomHarLøsninger for Spleis-behov {}", keyValue("eventId", eventId))
        restoreAndInvoke(eventId) {
            behandlendeEnhet?.also(::fortsett)
            hentPersoninfoLøsning?.also(::fortsett)
            hentInfotrygdutbetalingerLøsning?.also(::fortsett)
        }
    }

    fun håndter(eventId: UUID, løsning: ArbeidsgiverLøsning) {
        log.info("Mottok arbeidsgiverløsning for Spleis-behov {}", keyValue("eventId", eventId))
        restoreAndInvoke(eventId) { fortsett(løsning) }
    }

    fun håndter(eventId: UUID, løsning: SaksbehandlerLøsning) {
        log.info("Mottok godkjenningsløsning for Spleis-behov {}", keyValue("eventId", eventId))
        restoreAndInvoke(eventId) { fortsett(løsning) }
    }

    fun håndter(eventId: UUID, påminnelseMessage: PåminnelseMessage) {
        log.info("Mottok påminnelse for Spleis-behov {}", keyValue("eventId", eventId))
        restoreAndInvoke(eventId) {}
    }

    fun håndter(vedtaksperiodeId: UUID, løsning: TilInfotrygdMessage) {
        spleisbehovDao.findBehovMedSpleisReferanse(vedtaksperiodeId)?.also { spleisbehovDBDto ->
            val nåværendeOppgave = findNåværendeOppgave(spleisbehovDBDto.id) ?: return
            log.info("Vedtaksperiode {} i Spleis gikk TIL_INFOTRYGD", keyValue("vedtaksperiodeId", vedtaksperiodeId))
            spleisbehovExecutor(spleisbehovDBDto.id, vedtaksperiodeId, spleisbehovDBDto.data, nåværendeOppgave)
                .invalider()
        }
    }

    fun håndter(tilbakerullingMessage: TilbakerullingMessage) {
        tilbakerullingMessage.vedtakperioderSlettet.forEach {
            spleisbehovDao.findBehovMedSpleisReferanse(it)?.also { spleisbehovDBDto ->
                val nåværendeOppgave = findNåværendeOppgave(spleisbehovDBDto.id) ?: return
                log.info(
                    "Invaliderer oppgave {} fordi vedtaksperiode {} ble slettet",
                    keyValue("oppgaveId", nåværendeOppgave.id),
                    keyValue("vedtaksperiodeId", it)
                )
                spleisbehovExecutor(spleisbehovDBDto.id, it, spleisbehovDBDto.data, nåværendeOppgave)
                    .invalider()
                deleteVedtak(it)
            }
        }
    }

    fun håndter(eventId: UUID, vedtaksperiodeEndretMessage: VedtaksperiodeEndretMessage) {
        log.info(
            "Mottok vedtaksperiode endret {}, {}",
            keyValue("vedtaksperiodeId", vedtaksperiodeEndretMessage.vedtaksperiodeId),
            keyValue("eventId", eventId)
        )
        oppdaterVedtaksperiode(
            eventId = eventId,
            fødselsnummer = vedtaksperiodeEndretMessage.fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeEndretMessage.vedtaksperiodeId
        )
    }

    fun håndter(eventId: UUID, vedtaksperiodeForkastetMessage: VedtaksperiodeForkastetMessage) {
        log.info(
            "Mottok vedtaksperiode forkastet {}, {}",
            keyValue("vedtaksperiodeId", vedtaksperiodeForkastetMessage.vedtaksperiodeId),
            keyValue("eventId", eventId)
        )
        oppdaterVedtaksperiode(
            eventId = eventId,
            fødselsnummer = vedtaksperiodeForkastetMessage.fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeForkastetMessage.vedtaksperiodeId
        )
    }

    private fun oppdaterVedtaksperiode(eventId: UUID, fødselsnummer: String, vedtaksperiodeId: UUID) {
        try {
            val commandExecutor = CommandExecutor(
                dataSource = dataSource,
                command = OppdaterVedtaksperiode(
                    fødselsnummer = fødselsnummer,
                    vedtaksperiodeId = vedtaksperiodeId,
                    eventId = eventId,
                    speilSnapshotRestDao = speilSnapshotRestDao,
                    snapshotDao = snapshotDao
                ),
                spesialistOid = spesialistOID,
                eventId = eventId,
                nåværendeOppgave = null,
                loggingData = *arrayOf(
                    keyValue("vedtaksperiodeId", vedtaksperiodeId),
                    keyValue("eventId", eventId)
                )
            )

            val resultater = measureAsHistogram("vedtaksperiode_endret") { commandExecutor.execute() }
            publiserBehov(
                spleisreferanse = eventId,
                resultater = resultater,
                commandExecutor = commandExecutor
            )
        } catch (e: Exception) {
            log.error("Klarte ikke å oppdaterer vedtaksperiode", e)
            throw RuntimeException("Klarte ikke å oppdaterer vedtaksperiode", e)
        }
    }

    internal fun rollbackPerson(rollback: Rollback) {
        log.info("Publiserer rollback på aktør: ${rollback.aktørId}")
        rapidsConnection.publish(
            rollback.fødselsnummer, JsonMessage.newMessage(
                mutableMapOf(
                    "@id" to UUID.randomUUID(),
                    "@event_name" to "rollback_person",
                    "@opprettet" to LocalDateTime.now(),
                    "aktørId" to rollback.aktørId,
                    "fødselsnummer" to rollback.fødselsnummer,
                    "personVersjon" to rollback.personVersjon
                )
            ).toJson()
        )
    }

    internal fun rollbackDeletePerson(rollback: RollbackDelete) {
        log.info("Publiserer rollback_delete på aktør: ${rollback.aktørId}")
        rapidsConnection.publish(
            JsonMessage.newMessage(
                mutableMapOf(
                    "@id" to UUID.randomUUID(),
                    "@event_name" to "rollback_person_delete",
                    "@opprettet" to LocalDateTime.now(),
                    "aktørId" to rollback.aktørId,
                    "fødselsnummer" to rollback.fødselsnummer
                )
            ).toJson()
        )
    }

    private fun restoreAndInvoke(eventId: UUID, invoke: CommandExecutor.() -> Unit) {
        if (shutdown) {
            throw IllegalStateException("Stopper håndtering av behov når appen er i shutdown")
        }

        val spleisbehovDBDto = requireNotNull(spleisbehovDao.findBehov(eventId)) {
            "Fant ikke behov med id $eventId"
        }

        val nåværendeOppgave = requireNotNull(findNåværendeOppgave(eventId)) {
            "Svar på behov krever at det er en nåværende oppgave"
        }

        val commandExecutor = spleisbehovExecutor(
            id = eventId,
            spleisReferanse = spleisbehovDBDto.spleisReferanse,
            spleisbehovJson = spleisbehovDBDto.data,
            nåværendeOppgave = nåværendeOppgave
        )
        commandExecutor.invoke()


        val resultater = commandExecutor.execute()
        spleisbehovDao.updateBehov(eventId, commandExecutor.command.toJson())
        publiserBehov(
            spleisreferanse = eventId,
            resultater = resultater,
            commandExecutor = commandExecutor
        )
    }

    private fun publiserBehov(
        spleisreferanse: UUID,
        resultater: List<Command.Resultat>,
        commandExecutor: CommandExecutor
    ) {
        val behovstyper = resultater.filterIsInstance<Command.Resultat.HarBehov>().flatMap { it.behovstyper.toList() }
        if (behovstyper.isNotEmpty()) {
            val behov = Behov(
                typer = behovstyper,
                fødselsnummer = commandExecutor.command.fødselsnummer,
                orgnummer = commandExecutor.command.orgnummer,
                spleisBehovId = spleisreferanse,
                vedtaksperiodeId = commandExecutor.command.vedtaksperiodeId
            )
            log.info(
                "Sender ut behov for {}, {}, {}",
                keyValue("vedtaksperiodeId", behov.vedtaksperiodeId),
                keyValue("eventId", behov.spleisBehovId),
                keyValue("behov", behov.typer.toString())
            )
            rapidsConnection.publish(commandExecutor.command.fødselsnummer, behov.toJson().also {
                sikkerLogg.info(
                    "sender behov for {}, {}, {}:\n\t$it",
                    keyValue("fødselsnummer", commandExecutor.command.fødselsnummer),
                    keyValue("vedtaksperiodeId", commandExecutor.command.vedtaksperiodeId),
                    keyValue("spleisBehovId", spleisreferanse)
                )
            })
        }

        resultater
            .filterIsInstance<Command.Resultat.Ok.Løst>()
            .forEach { løst ->
                val originalJson = requireNotNull(spleisbehovDao.findOriginalBehov(spleisreferanse))
                val løsningJson = JsonMessage(originalJson, MessageProblems(originalJson))
                løsningJson["@løsning"] = løst.løsning
                rapidsConnection.publish(commandExecutor.command.fødselsnummer, løsningJson.toJson().also {
                    sikkerLogg.info(
                        "sender løsning for {}, {}, {}:\n\t$it",
                        keyValue("fødselsnummer", commandExecutor.command.fødselsnummer),
                        keyValue("vedtaksperiodeId", commandExecutor.command.vedtaksperiodeId),
                        keyValue("spleisBehovId", spleisreferanse)
                    )
                })
            }

        log.info(
            "Produserer oppgave endret event for {}, {}",
            keyValue("eventId", spleisreferanse),
            keyValue("vedtaksperiodeId", commandExecutor.command.vedtaksperiodeId)
        )

        rapidsConnection.publish(JsonMessage.newMessage(
            mapOf(
                "@event_name" to "oppgave_oppdatert",
                "@id" to UUID.randomUUID(),
                "@opprettet" to LocalDateTime.now(),
                "timeout" to commandExecutor.currentTimeout().toSeconds(),
                "eventId" to spleisreferanse,
                "fødselsnummer" to commandExecutor.command.fødselsnummer,
                "endringstidspunkt" to LocalDateTime.now(),
                "ferdigstilt" to (resultater.last() is Command.Resultat.Ok)
            )
        ).toJson().also {
            sikkerLogg.info(
                "sender oppgave_oppdatert for {}, {}, {}:\n\t$it",
                keyValue("fødselsnummer", commandExecutor.command.fødselsnummer),
                keyValue("vedtaksperiodeId", commandExecutor.command.vedtaksperiodeId),
                keyValue("spleisBehovId", spleisreferanse)
            )
        })
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
        dataSource = dataSource,
        command = Godkjenningsbehov.restore(
            id = id,
            vedtaksperiodeId = spleisReferanse,
            data = spleisbehovJson,
            personDao = personDao,
            arbeidsgiverDao = arbeidsgiverDao,
            snapshotDao = snapshotDao,
            speilSnapshotRestDao = speilSnapshotRestDao
        ),
        spesialistOid = spesialistOID,
        eventId = id,
        nåværendeOppgave = nåværendeOppgave,
        loggingData = *arrayOf(keyValue("vedtaksperiodeId", spleisReferanse), keyValue("eventId", id))
    )

    fun findNåværendeOppgave(eventId: UUID) = using(sessionOf(dataSource)) { session ->
        session.findNåværendeOppgave(eventId)
    }

    fun deleteVedtak(vedtaksperiodeId: UUID) = using(sessionOf(dataSource)) { session ->
        session.deleteVedtak(vedtaksperiodeId)
    }
}
