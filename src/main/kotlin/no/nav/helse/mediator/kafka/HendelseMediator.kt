package no.nav.helse.mediator.kafka

import kotliquery.Session
import kotliquery.sessionOf
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.api.Rollback
import no.nav.helse.api.RollbackDelete
import no.nav.helse.mediator.kafka.meldinger.*
import no.nav.helse.modell.*
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverLøsning
import no.nav.helse.modell.command.*
import no.nav.helse.modell.command.ny.AnnulleringCommand
import no.nav.helse.modell.command.ny.RollbackDeletePersonCommand
import no.nav.helse.modell.command.ny.RollbackPersonCommand
import no.nav.helse.modell.command.nyny.CommandContext
import no.nav.helse.modell.overstyring.BistandSaksbehandlerCommand
import no.nav.helse.modell.overstyring.OverstyringSaksbehandlerCommand
import no.nav.helse.modell.person.HentEnhetLøsning
import no.nav.helse.modell.person.HentInfotrygdutbetalingerLøsning
import no.nav.helse.modell.person.HentPersoninfoLøsning
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.vedtak.SaksbehandlerLøsning
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import no.nav.helse.modell.vedtak.deleteVedtak
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.overstyringsteller
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource
import no.nav.helse.modell.command.Løsninger as LøsningerOld

internal class HendelseMediator(
    private val rapidsConnection: RapidsConnection,
    private val speilSnapshotRestClient: SpeilSnapshotRestClient,
    private val dataSource: DataSource,
    private val spesialistOID: UUID
) : IHendelseMediator {
    private companion object {
        private val log = LoggerFactory.getLogger(HendelseMediator::class.java)
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }
    private val personDao = PersonDao(dataSource)
    private val arbeidsgiverDao = ArbeidsgiverDao(dataSource)
    private val vedtakDao = VedtakDao(dataSource)
    private val snapshotDao = SnapshotDao(dataSource)
    private val oppgaveDao = OppgaveDao(dataSource)
    private val commandContextDao = CommandContextDao(dataSource)
    private val hendelsefabrikk = Hendelsefabrikk(personDao, arbeidsgiverDao, vedtakDao, oppgaveDao, commandContextDao, snapshotDao, speilSnapshotRestClient)
    private val spleisbehovDao = SpleisbehovDao(dataSource, hendelsefabrikk)

    private var shutdown = false
    private val behovMediator = BehovMediator(rapidsConnection, sikkerLogg)

    init {
        DelegatedRapid(rapidsConnection, ::forbered, ::fortsett, ::errorHandler).also {
            ArbeidsgiverMessage.Factory(it, this)
            GodkjenningMessage.Factory(it, this)
            PersoninfoLøsningMessage.Factory(it, this)
            SaksbehandlerLøsning.SaksbehandlerLøsningRiver(it, this)
            HentPersoninfoLøsning.PersoninfoRiver(it, this)
            HentEnhetLøsning.HentEnhetRiver(it, this)
            HentInfotrygdutbetalingerLøsning.InfotrygdutbetalingerRiver(it, this)
            PåminnelseMessage.Factory(it, this)
            TilInfotrygdMessage.Factory(it, this)
            TilbakerullingMessage.Factory(it, this)

            //NyGodkjenningMessage.GodkjenningMessageRiver(it, this)
            NyVedtaksperiodeEndretMessage.VedtaksperiodeEndretRiver(it, this)
            NyVedtaksperiodeForkastetMessage.VedtaksperiodeForkastetRiver(it, this)
        }
    }
    private var løsninger: Løsninger? = null

    private fun forbered() {
        løsninger = null
    }

    // samler opp løsninger
    override fun løsning(hendelseId: UUID, contextId: UUID, løsning: Any, context: RapidsConnection.MessageContext) {
        løsninger(hendelseId, contextId)?.add(hendelseId, contextId, løsning)
    }

    private fun løsninger(hendelseId: UUID, contextId: UUID): Løsninger? {
        return løsninger ?: run {
            val hendelse = spleisbehovDao.finn(hendelseId)
            val commandContext = commandContextDao.finn(contextId)
            if (hendelse == null || commandContext == null) {
                log.error("finner ikke hendelse med id=$hendelseId eller command context med id=$contextId; ignorerer melding")
                return null
            }
            Løsninger(hendelse, contextId, commandContext).also { løsninger = it }
        }
    }

    // fortsetter en command (resume) med oppsamlet løsninger
    private fun fortsett(message: String, context: RapidsConnection.MessageContext) {
        løsninger?.fortsett(this, message, context)
    }

    private fun errorHandler(err: Exception, message: String) {
        log.error("alvorlig feil: ${err.message} (se sikkerlogg for melding)", err)
        sikkerLogg.error("alvorlig feil: ${err.message}\n\t$message", err)
    }

    internal fun håndter(godkjenningMessage: GodkjenningMessage, originalJson: String) {
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            if (session.findBehov(godkjenningMessage.id) != null) {
                log.warn(
                    "Mottok duplikat godkjenningsbehov, {}, {}",
                    keyValue("eventId", godkjenningMessage.id),
                    keyValue("vedtaksperiodeId", godkjenningMessage.vedtaksperiodeId)
                )
                return@use
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
                originalJson,
                MacroCommandType.Godkjenningsbehov.name
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

    internal fun håndter(bistandSaksbehandler: BistandSaksbehandlerMessage, originalJson: String) {
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            val spleisbehovExecutor = CommandExecutor(
                session = session,
                command = BistandSaksbehandlerCommand(
                    id = bistandSaksbehandler.id,
                    fødselsnummer = bistandSaksbehandler.fødselsnummer,
                    periodeFom = bistandSaksbehandler.periodeFom,
                    periodeTom = bistandSaksbehandler.periodeTom,
                    vedtaksperiodeId = bistandSaksbehandler.vedtaksperiodeId,
                    aktørId = bistandSaksbehandler.aktørId,
                    orgnummer = bistandSaksbehandler.orgnummer,
                    speilSnapshotRestClient = speilSnapshotRestClient
                ),
                spesialistOid = spesialistOID,
                eventId = bistandSaksbehandler.id,
                nåværendeOppgave = null,
                loggingData = *arrayOf(
                    keyValue("vedtaksperiodeId", bistandSaksbehandler.vedtaksperiodeId),
                    keyValue("eventId", bistandSaksbehandler.id)
                )
            )
            log.info(
                "Mottok godkjenningsbehov med {}, {}",
                keyValue("vedtaksperiodeId", bistandSaksbehandler.vedtaksperiodeId),
                keyValue("eventId", bistandSaksbehandler.id)
            )
            val resultater = spleisbehovExecutor.execute()

            session.insertBehov(
                bistandSaksbehandler.id,
                bistandSaksbehandler.vedtaksperiodeId,
                spleisbehovExecutor.command.toJson(),
                originalJson,
                MacroCommandType.BistandSaksbehandler.name
            )
            publiserBehov(
                spleisreferanse = bistandSaksbehandler.id,
                resultater = resultater,
                commandExecutor = spleisbehovExecutor,
                session = session
            )
        }
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
        val løsninger = LøsningerOld()
        behandlendeEnhet?.also(løsninger::add)
        hentPersoninfoLøsning?.also(løsninger::add)
        hentInfotrygdutbetalingerLøsning?.also(løsninger::add)
        resume(eventId, løsninger)
    }

    fun håndter(eventId: UUID, løsning: ArbeidsgiverLøsning) {
        log.info("Mottok arbeidsgiverløsning for Spleis-behov {}", keyValue("eventId", eventId))
        val løsninger = LøsningerOld().also { it.add(løsning) }
        resume(eventId, løsninger)
    }

    fun håndter(eventId: UUID, løsning: SaksbehandlerLøsning) {
        log.info("Mottok godkjenningsløsning for Spleis-behov {}", keyValue("eventId", eventId))
        val løsninger = LøsningerOld().also { it.add(løsning) }
        resume(eventId, løsninger)
    }

    fun håndter(eventId: UUID, påminnelseMessage: PåminnelseMessage) {
        log.info("Mottok påminnelse for Spleis-behov {}", keyValue("eventId", eventId))
        val løsninger = LøsningerOld().also { it.add(påminnelseMessage) }
        resume(eventId, løsninger)
    }

    internal fun håndter(annullering: AnnulleringMessage) {
        log.info("Publiserer annullering på fagsystemId {}", keyValue("fagsystemId", annullering.fagsystemId))
        val annulleringCommand = AnnulleringCommand(rapidsConnection, annullering)
        sessionOf(dataSource).use(annulleringCommand::execute)
    }

    override fun vedtaksperiodeEndret(message: JsonMessage, id: UUID, vedtaksperiodeId: UUID, fødselsnummer: String, context: RapidsConnection.MessageContext) {
        utfør(hendelsefabrikk.nyNyVedtaksperiodeEndret(id, vedtaksperiodeId, fødselsnummer, message.toJson()))
    }

    override fun vedtaksperiodeForkastet(message: JsonMessage, id: UUID, vedtaksperiodeId: UUID, fødselsnummer: String, context: RapidsConnection.MessageContext) {
        utfør(hendelsefabrikk.nyNyVedtaksperiodeForkastet(id, vedtaksperiodeId, fødselsnummer, message.toJson()))
    }

    override fun godkjenning(
        message: JsonMessage,
        id: UUID,
        fødselsnummer: String,
        aktørId: String,
        organisasjonsnummer: String,
        periodeFom: LocalDate,
        periodeTom: LocalDate,
        vedtaksperiodeId: UUID,
        warnings: List<String>,
        periodetype: Saksbehandleroppgavetype?,
        context: RapidsConnection.MessageContext
    ) {
        utfør(hendelsefabrikk.nyGodkjenning(id, fødselsnummer, aktørId, organisasjonsnummer, periodeFom, periodeTom, vedtaksperiodeId, warnings, periodetype, message.toJson()))
    }

    private fun nyContext(hendelse: Hendelse, contextId: UUID) = CommandContext(contextId).apply {
        spleisbehovDao.opprett(hendelse)
        opprett(commandContextDao, hendelse)
    }

    private fun utfør(hendelse: Hendelse) {
        val contextId = UUID.randomUUID()
        log.info("oppretter ny kommandokontekst med context_id=$contextId for hendelse_id=${hendelse.id}")
        utfør(hendelse, nyContext(hendelse, contextId), contextId)
    }

    private fun utfør(hendelse: Hendelse, context: CommandContext, contextId: UUID) {
        withMDC(mapOf("context_id" to "$contextId")) {
            try {
                log.info("utfører kommando med context_id=$contextId for hendelse_id=${hendelse.id}")
                if (context.utfør(commandContextDao, hendelse)) log.info("kommando er utført ferdig")
                else log.info("kommando er suspendert")
                behovMediator.håndter(hendelse, context, contextId)
            } catch (err: Exception) {
                log.warn("Feil ved kjøring av kommando: contextId={}, message={}", contextId, err.message, err)
                hendelse.undo(context)
                throw err
            } finally {
                log.info("utført kommando med context_id=$contextId for hendelse_id=${hendelse.id}")
            }
        }
    }

    fun håndter(vedtaksperiodeId: UUID, tilInfotrygdMessage: TilInfotrygdMessage) {
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            session.findBehovMedSpleisReferanse(vedtaksperiodeId)?.also { spleisbehovDBDto ->
                val nåværendeOppgave = session.findNåværendeOppgave(spleisbehovDBDto.id) ?: return@use
                log.info(
                    "Vedtaksperiode {} i Spleis gikk TIL_INFOTRYGD",
                    keyValue("vedtaksperiodeId", vedtaksperiodeId)
                )
                spleisbehovExecutor(
                    spleisbehovDBDto = spleisbehovDBDto ,
                    session = session,
                    nåværendeOppgave = nåværendeOppgave
                ).invalider()
            }
        }
    }

    fun håndter(tilbakerullingMessage: TilbakerullingMessage) {
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            tilbakerullingMessage.vedtakperioderSlettet.forEach {
                session.findBehovMedSpleisReferanse(it)?.also { spleisbehovDBDto ->
                    val nåværendeOppgave = session.findNåværendeOppgave(spleisbehovDBDto.id) ?: return@use
                    log.info(
                        "Invaliderer oppgave {} fordi vedtaksperiode {} ble slettet",
                        keyValue("oppgaveId", nåværendeOppgave.id),
                        keyValue("vedtaksperiodeId", it)
                    )
                    spleisbehovExecutor(
                        spleisbehovDBDto = spleisbehovDBDto ,
                        session = session,
                        nåværendeOppgave = nåværendeOppgave
                    ).invalider()
                    session.deleteVedtak(it)
                }
            }
        }
    }

    fun håndter(overstyringMessage: OverstyringMessage) {
        val eventId = UUID.randomUUID()
        val overstyringCommand = OverstyringSaksbehandlerCommand(
            eventId = eventId,
            rapidsConnection = rapidsConnection,
            oid = overstyringMessage.saksbehandlerOid,
            navn = overstyringMessage.saksbehandlerNavn,
            epost = overstyringMessage.saksbehandlerEpost,
            fødselsnummer = overstyringMessage.fødselsnummer,
            orgnummer = overstyringMessage.organisasjonsnummer
        )
        val invaliderSaksbehandlerOppgaveCommand = InvaliderSaksbehandlerOppgaveCommand(
            fødselsnummer = overstyringMessage.fødselsnummer,
            orgnummer = overstyringMessage.organisasjonsnummer,
            eventId = eventId
        )

        overstyringsteller.inc()

        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            val commandExecutor = CommandExecutor(
                command = overstyringCommand,
                spesialistOid = spesialistOID,
                eventId = eventId,
                nåværendeOppgave = null,
                session = session
            )
            commandExecutor.execute()
            commandExecutor.resume(session, LøsningerOld().apply {
                add(overstyringMessage)
            })
            invaliderSaksbehandlerOppgaveCommand.execute(session)
        }
    }

    internal fun rollbackPerson(rollback: Rollback) {
        log.info("Publiserer rollback på aktør: ${rollback.aktørId}")
        val rollbackPersonCommand = RollbackPersonCommand(rapidsConnection, rollback)
        sessionOf(dataSource).use(rollbackPersonCommand::execute)
    }

    internal fun rollbackDeletePerson(rollback: RollbackDelete) {
        log.info("Publiserer rollback_delete på aktør: ${rollback.aktørId}")
        val rollbackDeletePersonCommand = RollbackDeletePersonCommand(rapidsConnection, rollback)
        sessionOf(dataSource).use(rollbackDeletePersonCommand::execute)
    }

    private fun resume(eventId: UUID, løsninger: LøsningerOld) {
        if (shutdown) {
            throw IllegalStateException("Stopper håndtering av behov når appen er i shutdown")
        }

        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            val spleisbehovDBDto = requireNotNull(session.findBehov(eventId)) {
                "Fant ikke behov med id $eventId"
            }

            val nåværendeOppgave = session.findNåværendeOppgave(eventId) ?: run {
                sikkerLogg.warn(
                    "Finner ikke en nåværende oppgave knyttet til behov med {}, {}:\n{}",
                    keyValue("id", eventId),
                    keyValue("spleisBehovId", spleisbehovDBDto.spleisReferanse),
                    keyValue("json", spleisbehovDBDto.data)
                )
                log.warn(
                    "Finner ikke en nåværende oppgave knyttet til behov med id {}, ignorerer løsning",
                    keyValue("id", eventId)
                )
                return
            }

            val commandExecutor =
                spleisbehovExecutor(
                    spleisbehovDBDto = spleisbehovDBDto,
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
        spleisbehovDBDto : SpleisbehovDBDto,
        session: Session,
        nåværendeOppgave: OppgaveDto
    ) = CommandExecutor(
        session = session,
        command = restore(spleisbehovDBDto ),
        spesialistOid = spesialistOID,
        eventId = spleisbehovDBDto .id,
        nåværendeOppgave = nåværendeOppgave,
        loggingData = *arrayOf(
            keyValue("vedtaksperiodeId", spleisbehovDBDto .spleisReferanse),
            keyValue("eventId", spleisbehovDBDto.id)
        )
    )

    private fun restore(spleisbehovDbDTO: SpleisbehovDBDto): MacroCommand = when (spleisbehovDbDTO.type) {
        MacroCommandType.Godkjenningsbehov -> Godkjenningsbehov.restore(
            id = spleisbehovDbDTO.id,
            vedtaksperiodeId = spleisbehovDbDTO.spleisReferanse,
            data = spleisbehovDbDTO.data,
            speilSnapshotRestClient = speilSnapshotRestClient
        )
        MacroCommandType.BistandSaksbehandler -> BistandSaksbehandlerCommand.restore(
            id = spleisbehovDbDTO.id,
            vedtaksperiodeId = spleisbehovDbDTO.spleisReferanse,
            data = spleisbehovDbDTO.data,
            speilSnapshotRestClient = speilSnapshotRestClient
        )
    }

    private class Løsninger(private val hendelse: Hendelse, private val contextId: UUID, private val commandContext: CommandContext) {
        fun add(hendelseId: UUID, contextId: UUID, løsning: Any) {
            check(hendelseId == hendelse.id)
            check(contextId == this.contextId)
            commandContext.add(løsning)
        }

        fun fortsett(mediator: HendelseMediator, message: String, context: RapidsConnection.MessageContext) {
            log.info("fortsetter utførelse av kommandokontekst pga. behov_id=${hendelse.id} med context_id=$contextId for hendelse_id=${hendelse.id}")
            sikkerLogg.info("fortsetter utførelse av kommandokontekst pga. behov_id=${hendelse.id} med context_id=$contextId for hendelse_id=${hendelse.id}.\n" +
                "Innkommende melding:\n\t$message")
            mediator.utfør(hendelse, commandContext, contextId)
        }
    }
}
