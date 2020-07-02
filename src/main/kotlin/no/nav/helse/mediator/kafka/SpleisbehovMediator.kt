package no.nav.helse.mediator.kafka

import kotliquery.Session
import kotliquery.sessionOf
import kotliquery.using
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.api.Rollback
import no.nav.helse.api.RollbackDelete
import no.nav.helse.mediator.kafka.meldinger.*
import no.nav.helse.modell.Behov
import no.nav.helse.modell.Godkjenningsbehov
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverLøsning
import no.nav.helse.modell.command.*
import no.nav.helse.modell.command.ny.NyOppdaterVedtaksperiodeCommand
import no.nav.helse.modell.person.HentEnhetLøsning
import no.nav.helse.modell.person.HentInfotrygdutbetalingerLøsning
import no.nav.helse.modell.person.HentPersoninfoLøsning
import no.nav.helse.modell.person.findVedtaksperioderByAktørId
import no.nav.helse.modell.vedtak.SaksbehandlerLøsning
import no.nav.helse.modell.vedtak.deleteVedtak
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource
import kotlin.NoSuchElementException

internal class SpleisbehovMediator(
    private val speilSnapshotRestClient: SpeilSnapshotRestClient,
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
        using(sessionOf(dataSource, returnGeneratedKey = true)) { session ->
            if (session.findBehov(godkjenningMessage.id) != null) {
                log.warn(
                    "Mottok duplikat godkjenningsbehov, {}, {}",
                    keyValue("eventId", godkjenningMessage.id),
                    keyValue("vedtaksperiodeId", godkjenningMessage.vedtaksperiodeId)
                )
                return@using
            }

            val spleisbehovExecutor = CommandExecutor(
                session = session,
                command = Godkjenningsbehov(
                    id = godkjenningMessage.id,
                    fødselsnummer = godkjenningMessage.fødselsnummer,
                    periodeFom = godkjenningMessage.periodeFom,
                    periodeTom = godkjenningMessage.periodeTom,
                    vedtaksperiodeId = godkjenningMessage.vedtaksperiodeId,
                    aktørId = godkjenningMessage.aktørId,
                    orgnummer = godkjenningMessage.organisasjonsnummer,
                    speilSnapshotRestClient = speilSnapshotRestClient
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

            session.insertBehov(
                godkjenningMessage.id,
                godkjenningMessage.vedtaksperiodeId,
                spleisbehovExecutor.command.toJson(),
                originalJson
            )
            godkjenningMessage.warnings.forEach {
                session.insertWarning(
                    melding = it,
                    spleisbehovRef = godkjenningMessage.id
                )
            }
            godkjenningMessage.periodetype?.let {
                session.insertSaksbehandleroppgavetype(type = it, spleisbehovRef = godkjenningMessage.id)
            }
            publiserBehov(
                spleisreferanse = godkjenningMessage.id,
                resultater = resultater,
                commandExecutor = spleisbehovExecutor,
                session = session
            )
        }
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
        val løsninger = Løsninger()
        behandlendeEnhet?.also(løsninger::add)
        hentPersoninfoLøsning?.also(løsninger::add)
        hentInfotrygdutbetalingerLøsning?.also(løsninger::add)
        resume(eventId, løsninger)
    }

    fun håndter(eventId: UUID, løsning: ArbeidsgiverLøsning) {
        log.info("Mottok arbeidsgiverløsning for Spleis-behov {}", keyValue("eventId", eventId))
        val løsninger = Løsninger().also { it.add(løsning) }
        resume(eventId, løsninger)
    }

    fun håndter(eventId: UUID, løsning: SaksbehandlerLøsning) {
        log.info("Mottok godkjenningsløsning for Spleis-behov {}", keyValue("eventId", eventId))
        val løsninger = Løsninger().also { it.add(løsning) }
        resume(eventId, løsninger)
    }

    fun håndter(eventId: UUID, påminnelseMessage: PåminnelseMessage) {
        log.info("Mottok påminnelse for Spleis-behov {}", keyValue("eventId", eventId))
        val løsninger = Løsninger().also { it.add(påminnelseMessage) }
        resume(eventId, løsninger)
    }

    fun håndter(vedtaksperiodeId: UUID, tilInfotrygdMessage: TilInfotrygdMessage) {
        using(sessionOf(dataSource, returnGeneratedKey = true)) { session ->
            session.findBehovMedSpleisReferanse(vedtaksperiodeId)?.also { spleisbehovDBDto ->
                val nåværendeOppgave = session.findNåværendeOppgave(spleisbehovDBDto.id) ?: return@using
                log.info(
                    "Vedtaksperiode {} i Spleis gikk TIL_INFOTRYGD",
                    keyValue("vedtaksperiodeId", vedtaksperiodeId)
                )
                spleisbehovExecutor(
                    id = spleisbehovDBDto.id,
                    spleisReferanse = vedtaksperiodeId,
                    spleisbehovJson = spleisbehovDBDto.data,
                    session = session,
                    nåværendeOppgave = nåværendeOppgave
                ).invalider()
            }
        }
    }

    fun håndter(tilbakerullingMessage: TilbakerullingMessage) {
        using(sessionOf(dataSource, returnGeneratedKey = true)) { session ->
            tilbakerullingMessage.vedtakperioderSlettet.forEach {
                session.findBehovMedSpleisReferanse(it)?.also { spleisbehovDBDto ->
                    val nåværendeOppgave = session.findNåværendeOppgave(spleisbehovDBDto.id) ?: return@using
                    log.info(
                        "Invaliderer oppgave {} fordi vedtaksperiode {} ble slettet",
                        keyValue("oppgaveId", nåværendeOppgave.id),
                        keyValue("vedtaksperiodeId", it)
                    )
                    spleisbehovExecutor(
                        id = spleisbehovDBDto.id,
                        spleisReferanse = it,
                        spleisbehovJson = spleisbehovDBDto.data,
                        session = session,
                        nåværendeOppgave = nåværendeOppgave
                    ).invalider()
                    session.deleteVedtak(it)
                }
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
            fødselsnummer = vedtaksperiodeForkastetMessage.fødselsnummer,
            vedtaksperiodeId = vedtaksperiodeForkastetMessage.vedtaksperiodeId
        )
    }

    private fun oppdaterVedtaksperiode(fødselsnummer: String, vedtaksperiodeId: UUID) {
        using(sessionOf(dataSource, returnGeneratedKey = true)) { session ->
            val oppdaterVedtaksperiodeCommand = NyOppdaterVedtaksperiodeCommand(
                speilSnapshotRestClient = speilSnapshotRestClient,
                vedtaksperiodeId = vedtaksperiodeId,
                fødselsnummer = fødselsnummer
            )
            oppdaterVedtaksperiodeCommand.execute(session)
        }
    }

    internal fun oppdaterVedtaksperioder(aktørId: Long) {
        using(sessionOf(dataSource, returnGeneratedKey = true)) { session ->
            session.findVedtaksperioderByAktørId(aktørId)?.let {
                log.info(
                    "Publiserer vedtaksperiode_endret_manuelt på {} for {}",
                    keyValue("vedtaksperioder", it.second),
                    keyValue("aktørId", aktørId)
                )
                it.second.forEach { vedtaksperiodeId ->
                    rapidsConnection.publish(
                        it.first, JsonMessage.newMessage(
                            mutableMapOf(
                                "@id" to UUID.randomUUID(),
                                "@event_name" to "vedtaksperiode_endret_manuelt",
                                "@opprettet" to LocalDateTime.now(),
                                "aktørId" to aktørId,
                                "fødselsnummer" to it.first,
                                "vedtaksperiodeId" to vedtaksperiodeId
                            )
                        ).toJson()
                    )
                }
            } ?: throw NoSuchElementException()
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

    private fun resume(eventId: UUID, løsninger: Løsninger) {
        if (shutdown) {
            throw IllegalStateException("Stopper håndtering av behov når appen er i shutdown")
        }

        using(sessionOf(dataSource, returnGeneratedKey = true)) { session ->
            val spleisbehovDBDto = requireNotNull(session.findBehov(eventId)) {
                "Fant ikke behov med id $eventId"
            }

            val nåværendeOppgave = requireNotNull(session.findNåværendeOppgave(eventId)) {
                "Svar på behov krever at det er en nåværende oppgave"
            }

            val commandExecutor =
                spleisbehovExecutor(
                    id = eventId,
                    spleisReferanse = spleisbehovDBDto.spleisReferanse,
                    spleisbehovJson = spleisbehovDBDto.data,
                    session = session,
                    nåværendeOppgave = nåværendeOppgave
                )
            commandExecutor.resume(session, løsninger)

            val resultater = commandExecutor.execute()
            session.updateBehov(eventId, commandExecutor.command.toJson())
            publiserBehov(
                spleisreferanse = eventId,
                resultater = resultater,
                commandExecutor = commandExecutor,
                session = session
            )
        }
    }

    private fun publiserBehov(
        spleisreferanse: UUID,
        resultater: List<Command.Resultat>,
        commandExecutor: CommandExecutor,
        session: Session
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
                val originalJson = requireNotNull(session.findOriginalBehov(spleisreferanse))
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
        session: Session,
        nåværendeOppgave: OppgaveDto
    ) = CommandExecutor(
        session = session,
        command = Godkjenningsbehov.restore(
            id = id,
            vedtaksperiodeId = spleisReferanse,
            data = spleisbehovJson,
            speilSnapshotRestClient = speilSnapshotRestClient
        ),
        spesialistOid = spesialistOID,
        eventId = id,
        nåværendeOppgave = nåværendeOppgave,
        loggingData = *arrayOf(keyValue("vedtaksperiodeId", spleisReferanse), keyValue("eventId", id))
    )
}
