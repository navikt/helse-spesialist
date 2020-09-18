package no.nav.helse.mediator.kafka

import kotliquery.Session
import kotliquery.sessionOf
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.api.*
import no.nav.helse.mediator.kafka.meldinger.*
import no.nav.helse.modell.*
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverLøsning
import no.nav.helse.modell.command.*
import no.nav.helse.modell.command.ny.RollbackDeletePersonCommand
import no.nav.helse.modell.command.ny.RollbackPersonCommand
import no.nav.helse.modell.command.nyny.CommandContext
import no.nav.helse.modell.overstyring.BistandSaksbehandlerCommand
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.person.HentEnhetLøsning
import no.nav.helse.modell.person.HentInfotrygdutbetalingerLøsning
import no.nav.helse.modell.person.HentPersoninfoLøsning
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.saksbehandler.SaksbehandlerDao
import no.nav.helse.modell.vedtak.SaksbehandlerLøsning
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import no.nav.helse.modell.vedtak.deleteVedtak
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.overstyringsteller
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.tildeling.ReservasjonDao
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
    private val spesialistOID: UUID,
    private val oppgaveDao: OppgaveDao = OppgaveDao(dataSource),
    private val vedtakDao: VedtakDao = VedtakDao(dataSource),
    private val oppgaveMediator: OppgaveMediator = OppgaveMediator(oppgaveDao, vedtakDao),
    private val miljøstyrtFeatureToggle: MiljøstyrtFeatureToggle
) : IHendelseMediator {
    private companion object {
        private val log = LoggerFactory.getLogger(HendelseMediator::class.java)
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }

    private val personDao = PersonDao(dataSource)
    private val arbeidsgiverDao = ArbeidsgiverDao(dataSource)
    private val snapshotDao = SnapshotDao(dataSource)
    private val commandContextDao = CommandContextDao(dataSource)
    private val reservasjonDao = ReservasjonDao(dataSource)
    private val saksbehandlerDao = SaksbehandlerDao(dataSource)
    private val overstyringDao = OverstyringDao(dataSource)
    private val risikovurderingDao = RisikovurderingDao(dataSource)
    private val hendelsefabrikk = Hendelsefabrikk(
        personDao = personDao,
        arbeidsgiverDao = arbeidsgiverDao,
        vedtakDao = vedtakDao,
        commandContextDao = commandContextDao,
        snapshotDao = snapshotDao,
        oppgaveDao = oppgaveDao,
        reservasjonsDao = reservasjonDao,
        saksbehandlerDao = saksbehandlerDao,
        overstyringDao = overstyringDao,
        risikovurderingDao = risikovurderingDao,
        speilSnapshotRestClient = speilSnapshotRestClient,
        oppgaveMediator = oppgaveMediator,
        miljøstyrtFeatureToggle = miljøstyrtFeatureToggle
    )
    private val hendelseDao = HendelseDao(dataSource, hendelsefabrikk)

    private var shutdown = false
    private val behovMediator = BehovMediator(rapidsConnection, sikkerLogg)

    init {
        DelegatedRapid(rapidsConnection, ::forbered, ::fortsett, ::errorHandler).also {
            if (FeatureToggle.nyGodkjenningRiver) {
                NyGodkjenningMessage.GodkjenningMessageRiver(it, this)
                NyTilbakerullingMessage.TilbakerullingRiver(it, this)
                HentPersoninfoLøsning.PersoninfoRiver(it, this)
                HentEnhetLøsning.HentEnhetRiver(it, this)
                HentInfotrygdutbetalingerLøsning.InfotrygdutbetalingerRiver(it, this)
                SaksbehandlerLøsning.SaksbehandlerLøsningRiver(it, this)
            } else {
                GodkjenningMessage.Factory(it, this)
                PersoninfoLøsningMessage.Factory(it, this)
            }
            ArbeidsgiverMessage.Factory(it, this)
            PåminnelseMessage.Factory(it, this)
            TilbakerullingMessage.Factory(it, this)
            NyVedtaksperiodeForkastetMessage.VedtaksperiodeForkastetRiver(it, this)
            NyVedtaksperiodeEndretMessage.VedtaksperiodeEndretRiver(it, this)
            OverstyringMessage.OverstyringRiver(it, this)
            if (miljøstyrtFeatureToggle.risikovurdering()) {
                RisikovurderingLøsning.V1River(it, this)
                RisikovurderingLøsning.V2River(it, this)
            }
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
            val hendelse = hendelseDao.finn(hendelseId)
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
                    speilSnapshotRestClient = speilSnapshotRestClient,
                    reservasjonDao = reservasjonDao,
                    risikovurderingDao = risikovurderingDao,
                    miljøstyrtFeatureToggle = miljøstyrtFeatureToggle
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

    internal fun håndter(eventId: UUID, risikovurderingLøsning: RisikovurderingLøsning) {
        val løsning = LøsningerOld()
        løsning.add(risikovurderingLøsning)
        resume(eventId, løsning)
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

    private fun standardfelter(hendelsetype: String, fødselsnummer: String): MutableMap<String, Any> {
        return mutableMapOf(
            "@event_name" to hendelsetype,
            "@opprettet" to LocalDateTime.now(),
            "@id" to UUID.randomUUID(),
            "fødselsnummer" to fødselsnummer
        )
    }

    internal fun håndter(godkjenningDTO: GodkjenningDTO, epost: String, oid: UUID) {
        val contextId = oppgaveDao.finnContextId(godkjenningDTO.oppgavereferanse)
        val hendelseId = oppgaveDao.finnHendelseId(godkjenningDTO.oppgavereferanse)
        val fødselsnummer = personDao.finnFødselsnummer(hendelseId)
        val godkjenningMessage = JsonMessage.newMessage(
            standardfelter("saksbehandler_løsning", fødselsnummer).apply {
                put("oppgaveId", godkjenningDTO.oppgavereferanse)
                put("contextId", contextId)
                put("hendelseId", hendelseId)
                put("godkjent", godkjenningDTO.godkjent)
                put("saksbehandlerident", godkjenningDTO.saksbehandlerIdent)
                put("saksbehandleroid", oid)
                put("saksbehandlerepost", epost)
                put("godkjenttidspunkt", LocalDateTime.now())
                godkjenningDTO.årsak?.let { put("årsak", it) }
                godkjenningDTO.begrunnelser?.let { put("begrunnelser", it) }
                godkjenningDTO.kommentar?.let<String, Unit> { put("kommentar", it) }
            }).also {
            sikkerLogg.info("Publiserer saksbehandler-løsning: ${it.toJson()}")
        }
        log.info(
            "Publiserer saksbehandler-løsning for {}. {}. {}",
            keyValue("oppgaveId", godkjenningDTO.oppgavereferanse),
            keyValue("contextId", contextId),
            keyValue("hendelseId", hendelseId)
        )
        rapidsConnection.publish(godkjenningMessage.toJson())
    }

    fun håndter(eventId: UUID, påminnelseMessage: PåminnelseMessage) {
        log.info("Mottok påminnelse for Spleis-behov {}", keyValue("eventId", eventId))
        val løsninger = LøsningerOld().also { it.add(påminnelseMessage) }
        resume(eventId, løsninger)
    }

    internal fun håndter(annulleringDto: AnnulleringDto, epostadresse: String) {
        val annulleringMessage = annulleringDto.run {
            JsonMessage.newMessage(
                standardfelter("kanseller_utbetaling", fødselsnummer).apply {
                    putAll(mapOf(
                        "organisasjonsnummer" to organisasjonsnummer,
                        "aktørId" to aktørId,
                        "fagsystemId" to fagsystemId,
                        "saksbehandler" to saksbehandlerIdent,
                        "saksbehandlerEpost" to epostadresse
                    ))
                }
            )
        }

        rapidsConnection.publish(annulleringMessage.toJson().also {
            sikkerLogg.info(
                "sender annullering for {}\n\t$it",
                keyValue("fagsystemId", annulleringDto.fagsystemId)
            )
        })
    }

    override fun vedtaksperiodeEndret(
        message: JsonMessage,
        id: UUID,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        context: RapidsConnection.MessageContext
    ) {
        utfør(hendelsefabrikk.nyNyVedtaksperiodeEndret(id, vedtaksperiodeId, fødselsnummer, message.toJson()), context)
    }

    override fun vedtaksperiodeForkastet(
        message: JsonMessage,
        id: UUID,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        context: RapidsConnection.MessageContext
    ) {
        utfør(
            hendelsefabrikk.nyNyVedtaksperiodeForkastet(id, vedtaksperiodeId, fødselsnummer, message.toJson()),
            context
        )
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
        utfør(
            hendelsefabrikk.nyGodkjenning(
                id,
                fødselsnummer,
                aktørId,
                organisasjonsnummer,
                periodeFom,
                periodeTom,
                vedtaksperiodeId,
                warnings,
                periodetype,
                message.toJson()
            ), context
        )
    }

    override fun overstyring(
        message: JsonMessage,
        id: UUID,
        fødselsnummer: String,
        context: RapidsConnection.MessageContext
    ) {
        utfør(hendelsefabrikk.overstyring(message.toJson()), context)
    }

    override fun tilbakerulling(
        message: JsonMessage,
        id: UUID,
        fødselsnummer: String,
        vedtaksperiodeIder: List<UUID>,
        context: RapidsConnection.MessageContext
    ) {
        utfør(hendelsefabrikk.tilbakerulling(message.toJson()), context)
    }

    private fun nyContext(hendelse: Hendelse, contextId: UUID) = CommandContext(contextId).apply {
        hendelseDao.opprett(hendelse)
        opprett(commandContextDao, hendelse)
    }

    private fun utfør(hendelse: Hendelse, messageContext: RapidsConnection.MessageContext) {
        val contextId = UUID.randomUUID()
        log.info("oppretter ny kommandokontekst med context_id=$contextId for hendelse_id=${hendelse.id}")
        utfør(hendelse, nyContext(hendelse, contextId), contextId, messageContext)
    }

    private fun utfør(
        hendelse: Hendelse,
        context: CommandContext,
        contextId: UUID,
        messageContext: RapidsConnection.MessageContext
    ) {
        withMDC(mapOf("context_id" to "$contextId", "hendelse_id" to "${hendelse.id}")) {
            try {
                log.info("utfører kommando med context_id=$contextId for hendelse_id=${hendelse.id}")
                if (context.utfør(commandContextDao, hendelse)) log.info("kommando er utført ferdig")
                else log.info("kommando er suspendert")
                behovMediator.håndter(hendelse, context, contextId)
                oppgaveMediator.lagreOppgaver(hendelse, messageContext, contextId)
            } catch (err: Exception) {
                log.warn("Feil ved kjøring av kommando: contextId={}, message={}", contextId, err.message, err)
                hendelse.undo(context)
                throw err
            } finally {
                log.info("utført kommando med context_id=$contextId for hendelse_id=${hendelse.id}")
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
                        spleisbehovDBDto = spleisbehovDBDto,
                        session = session,
                        nåværendeOppgave = nåværendeOppgave
                    ).invalider()
                    session.deleteVedtak(it)
                }
            }
        }
    }

    fun håndter(overstyringMessage: OverstyringRestDto) {
        overstyringsteller.inc()

        val overstyring = JsonMessage.newMessage(
            standardfelter("overstyr_tidslinje", overstyringMessage.fødselsnummer).apply {
                put("aktørId", overstyringMessage.aktørId)
                put("organisasjonsnummer", overstyringMessage.organisasjonsnummer)
                put("dager", overstyringMessage.dager)
                put("begrunnelse", overstyringMessage.begrunnelse)
                put("saksbehandlerOid", overstyringMessage.saksbehandlerOid)
                put("saksbehandlerNavn", overstyringMessage.saksbehandlerNavn)
                put("saksbehandlerEpost", overstyringMessage.saksbehandlerEpost)
            }
        ).also {
            sikkerLogg.info("Publiserer overstyring: ${it.toJson()}")
        }

        rapidsConnection.publish(overstyring.toJson())
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
        spleisbehovDBDto: SpleisbehovDBDto,
        session: Session,
        nåværendeOppgave: OppgaveDto
    ) = CommandExecutor(
        session = session,
        command = restore(spleisbehovDBDto),
        spesialistOid = spesialistOID,
        eventId = spleisbehovDBDto.id,
        nåværendeOppgave = nåværendeOppgave,
        loggingData = *arrayOf(
            keyValue("vedtaksperiodeId", spleisbehovDBDto.spleisReferanse),
            keyValue("eventId", spleisbehovDBDto.id)
        )
    )

    private fun restore(spleisbehovDbDTO: SpleisbehovDBDto): MacroCommand = when (spleisbehovDbDTO.type) {
        MacroCommandType.Godkjenningsbehov -> Godkjenningsbehov.restore(
            id = spleisbehovDbDTO.id,
            vedtaksperiodeId = spleisbehovDbDTO.spleisReferanse,
            data = spleisbehovDbDTO.data,
            reservasjonDao = reservasjonDao,
            risikovurderingDao = risikovurderingDao,
            speilSnapshotRestClient = speilSnapshotRestClient,
            miljøstyrtFeatureToggle = miljøstyrtFeatureToggle
        )
        MacroCommandType.BistandSaksbehandler -> BistandSaksbehandlerCommand.restore(
            id = spleisbehovDbDTO.id,
            vedtaksperiodeId = spleisbehovDbDTO.spleisReferanse,
            data = spleisbehovDbDTO.data,
            speilSnapshotRestClient = speilSnapshotRestClient
        )
    }

    private class Løsninger(
        private val hendelse: Hendelse,
        private val contextId: UUID,
        private val commandContext: CommandContext
    ) {
        fun add(hendelseId: UUID, contextId: UUID, løsning: Any) {
            check(hendelseId == hendelse.id)
            check(contextId == this.contextId)
            commandContext.add(løsning)
        }

        fun fortsett(mediator: HendelseMediator, message: String, context: RapidsConnection.MessageContext) {
            log.info("fortsetter utførelse av kommandokontekst pga. behov_id=${hendelse.id} med context_id=$contextId for hendelse_id=${hendelse.id}")
            sikkerLogg.info(
                "fortsetter utførelse av kommandokontekst pga. behov_id=${hendelse.id} med context_id=$contextId for hendelse_id=${hendelse.id}.\n" +
                    "Innkommende melding:\n\t$message"
            )
            mediator.utfør(hendelse, commandContext, contextId, context)
        }
    }
}
