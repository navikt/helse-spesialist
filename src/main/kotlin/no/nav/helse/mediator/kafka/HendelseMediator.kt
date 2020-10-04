package no.nav.helse.mediator.kafka

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.annulleringsteller
import no.nav.helse.api.*
import no.nav.helse.mediator.kafka.meldinger.*
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverLøsning
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.automatisering.AutomatiseringDao
import no.nav.helse.modell.command.HendelseDao
import no.nav.helse.modell.command.OppgaveDao
import no.nav.helse.modell.command.nyny.CommandContext
import no.nav.helse.modell.dkif.DigitalKontaktinformasjonDao
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.person.HentEnhetLøsning
import no.nav.helse.modell.person.HentInfotrygdutbetalingerLøsning
import no.nav.helse.modell.person.HentPersoninfoLøsning
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.saksbehandler.SaksbehandlerDao
import no.nav.helse.modell.vedtak.SaksbehandlerLøsning
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.overstyringsteller
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.tildeling.ReservasjonDao
import no.nav.helse.tildeling.TildelingDao
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

internal class HendelseMediator(
    private val rapidsConnection: RapidsConnection,
    speilSnapshotRestClient: SpeilSnapshotRestClient,
    dataSource: DataSource,
    private val oppgaveDao: OppgaveDao = OppgaveDao(dataSource),
    private val vedtakDao: VedtakDao = VedtakDao(dataSource),
    private val tildelingDao: TildelingDao = TildelingDao(dataSource),
    private val personDao: PersonDao = PersonDao(dataSource),
    private val arbeidsgiverDao: ArbeidsgiverDao = ArbeidsgiverDao(dataSource),
    private val snapshotDao: SnapshotDao = SnapshotDao(dataSource),
    private val commandContextDao: CommandContextDao = CommandContextDao(dataSource),
    private val reservasjonDao: ReservasjonDao = ReservasjonDao(dataSource),
    private val saksbehandlerDao: SaksbehandlerDao = SaksbehandlerDao(dataSource),
    private val overstyringDao: OverstyringDao = OverstyringDao(dataSource),
    private val risikovurderingDao: RisikovurderingDao = RisikovurderingDao(dataSource),
    private val digitalKontaktinformasjonDao: DigitalKontaktinformasjonDao = DigitalKontaktinformasjonDao(dataSource),
    private val oppgaveMediator: OppgaveMediator = OppgaveMediator(oppgaveDao, vedtakDao, tildelingDao),
    private val miljøstyrtFeatureToggle: MiljøstyrtFeatureToggle
) : IHendelseMediator {
    private companion object {
        private val log = LoggerFactory.getLogger(HendelseMediator::class.java)
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }

    private val automatisering = Automatisering(vedtakDao, risikovurderingDao, AutomatiseringDao(dataSource), digitalKontaktinformasjonDao)
    private val hendelsefabrikk = Hendelsefabrikk(
        personDao = personDao,
        arbeidsgiverDao = arbeidsgiverDao,
        vedtakDao = vedtakDao,
        commandContextDao = commandContextDao,
        snapshotDao = snapshotDao,
        oppgaveDao = oppgaveDao,
        reservasjonDao = reservasjonDao,
        saksbehandlerDao = saksbehandlerDao,
        overstyringDao = overstyringDao,
        risikovurderingDao = risikovurderingDao,
        digitalKontaktinformasjonDao = digitalKontaktinformasjonDao,
        speilSnapshotRestClient = speilSnapshotRestClient,
        oppgaveMediator = oppgaveMediator,
        miljøstyrtFeatureToggle = miljøstyrtFeatureToggle,
        automatisering = automatisering
    )
    private val hendelseDao = HendelseDao(dataSource, hendelsefabrikk)

    private var shutdown = false
    private val behovMediator = BehovMediator(rapidsConnection, sikkerLogg)

    init {
        DelegatedRapid(rapidsConnection, ::forbered, ::fortsett, ::errorHandler).also {
            NyGodkjenningMessage.GodkjenningMessageRiver(it, this)
            NyTilbakerullingMessage.TilbakerullingRiver(it, this)
            HentPersoninfoLøsning.PersoninfoRiver(it, this)
            HentEnhetLøsning.HentEnhetRiver(it, this)
            HentInfotrygdutbetalingerLøsning.InfotrygdutbetalingerRiver(it, this)
            SaksbehandlerLøsning.SaksbehandlerLøsningRiver(it, this)
            ArbeidsgiverLøsning.ArbeidsgiverRiver(it, this)
            NyVedtaksperiodeForkastetMessage.VedtaksperiodeForkastetRiver(it, this)
            NyVedtaksperiodeEndretMessage.VedtaksperiodeEndretRiver(it, this)
            OverstyringMessage.OverstyringRiver(it, this)
            DigitalKontaktinformasjonLøsning.DigitalKontaktinformasjonRiver(it, this)
            RisikovurderingLøsning.V2River(it, this)
        }
    }

    private var løsninger: Løsninger? = null

    // samler opp løsninger
    override fun løsning(hendelseId: UUID, contextId: UUID, løsning: Any, context: RapidsConnection.MessageContext) {
        løsninger(hendelseId, contextId)?.add(hendelseId, contextId, løsning)
    }

    internal fun håndter(godkjenningDTO: GodkjenningDTO, epost: String, oid: UUID) {
        val contextId = oppgaveDao.finnContextId(godkjenningDTO.oppgavereferanse)
        val hendelseId = oppgaveDao.finnHendelseId(godkjenningDTO.oppgavereferanse)
        val fødselsnummer = hendelseDao.finnFødselsnummer(hendelseId)
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

    override fun vedtaksperiodeEndret(
        message: JsonMessage,
        id: UUID,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        context: RapidsConnection.MessageContext
    ) {
        utfør(vedtaksperiodeId, hendelsefabrikk.nyNyVedtaksperiodeEndret(id, vedtaksperiodeId, fødselsnummer, message.toJson()), context)
    }

    override fun vedtaksperiodeForkastet(
        message: JsonMessage,
        id: UUID,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        context: RapidsConnection.MessageContext
    ) {
        utfør(
            vedtaksperiodeId,
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
        utfør(fødselsnummer, hendelsefabrikk.overstyring(message.toJson()), context)
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
            sikkerLogg.info("Publiserer overstyring:\n${it.toJson()}")
        }

        rapidsConnection.publish(overstyring.toJson())
    }

    internal fun håndter(tilbakerullingMedSlettingDTO: TilbakerullingMedSlettingDTO) {

        val tilbakerulling = JsonMessage.newMessage(
            standardfelter("rollback_person_delete", tilbakerullingMedSlettingDTO.fødselsnummer).apply {
                put("aktørId", tilbakerullingMedSlettingDTO.aktørId)
            }
        ).also {
            sikkerLogg.info("Publiserer rollback_person_delete for ${tilbakerullingMedSlettingDTO.fødselsnummer}:\n${it.toJson()}")
        }
        rapidsConnection.publish(tilbakerulling.toJson())
    }

    internal fun håndter(tilbakerullingDTO: TilbakerullingDTO) {
        val tilbakerulling = JsonMessage.newMessage(
            standardfelter("rollback_person", tilbakerullingDTO.fødselsnummer).apply {
                put("aktørId", tilbakerullingDTO.aktørId)
                put("personVersjon", tilbakerullingDTO.personVersjon)
            }
        ).also {
            sikkerLogg.info("Publiserer rollback_person for ${tilbakerullingDTO.fødselsnummer}:\n${it.toJson()}")
        }
        rapidsConnection.publish(tilbakerulling.toJson())
    }

    internal fun håndter(annulleringDto: AnnulleringDto, saksbehandlerOid: UUID, epostadresse: String) {
        annulleringsteller.inc()

        val annulleringMessage = annulleringDto.run {
            JsonMessage.newMessage(
                standardfelter("annuller", fødselsnummer).apply {
                    putAll(
                        mapOf(
                            "organisasjonsnummer" to organisasjonsnummer,
                            "aktørId" to aktørId,
                            "saksbehandler" to saksbehandlerOid,
                            "saksbehandlerEpost" to epostadresse,
                            "dager" to dager
                        )
                    )
                }
            )
        }

        rapidsConnection.publish(annulleringMessage.toJson().also {
            sikkerLogg.info(
                "sender annullering for {}, {}\n\t$it",
                keyValue("fødselsnummer", annulleringDto.fødselsnummer),
                keyValue("organisasjonsnummer", annulleringDto.organisasjonsnummer)
            )
        })
    }

    fun shutdown() {
        shutdown = true
    }

    private fun forbered() {
        løsninger = null
    }

    private fun standardfelter(hendelsetype: String, fødselsnummer: String) = mutableMapOf(
        "@event_name" to hendelsetype,
        "@opprettet" to LocalDateTime.now(),
        "@id" to UUID.randomUUID(),
        "fødselsnummer" to fødselsnummer
    )

    private fun løsninger(hendelseId: UUID, contextId: UUID): Løsninger? {
        return løsninger ?: run {
            val hendelse = hendelseDao.finn(hendelseId)
            val commandContext = commandContextDao.finnSuspendert(contextId)
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

    private fun nyContext(hendelse: Hendelse, contextId: UUID) = CommandContext(contextId).apply {
        hendelseDao.opprett(hendelse)
        opprett(commandContextDao, hendelse)
    }

    private fun utfør(vedtaksperiodeId: UUID, hendelse: Hendelse, messageContext: RapidsConnection.MessageContext) {
        if (vedtakDao.finnVedtakId(vedtaksperiodeId) == null) return log.debug("ignorerer hendelseId=${hendelse.id} fordi vi ikke kjenner til $vedtaksperiodeId")
        return utfør(hendelse, messageContext)
    }

    private fun utfør(fødselsnummer: String, hendelse: Hendelse, messageContext: RapidsConnection.MessageContext) {
        if (personDao.findPersonByFødselsnummer(fødselsnummer) == null) return log.debug("ignorerer hendelseId=${hendelse.id} fordi vi ikke kjenner til personen")
        return utfør(hendelse, messageContext)
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
                log.info("utfører ${hendelse::class.simpleName} med context_id=$contextId for hendelse_id=${hendelse.id}")
                if (context.utfør(commandContextDao, hendelse)) log.info("kommando er utført ferdig")
                else log.info("${hendelse::class.simpleName} er suspendert")
                behovMediator.håndter(hendelse, context, contextId)
                oppgaveMediator.lagreOppgaver(hendelse, messageContext, contextId)
            } catch (err: Exception) {
                log.warn("Feil ved kjøring av ${hendelse::class.simpleName}: contextId={}, message={}", contextId, err.message, err)
                hendelse.undo(context)
                throw err
            } finally {
                log.info("utført ${hendelse::class.simpleName} med context_id=$contextId for hendelse_id=${hendelse.id}")
            }
        }
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
