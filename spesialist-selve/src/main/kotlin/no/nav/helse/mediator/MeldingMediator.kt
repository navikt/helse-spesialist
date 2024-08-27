package no.nav.helse.mediator

import SøknadSendtArbeidsledigRiver
import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.MetrikkRiver
import no.nav.helse.bootstrap.Environment
import no.nav.helse.db.AvviksvurderingDao
import no.nav.helse.mediator.meldinger.AvsluttetMedVedtakRiver
import no.nav.helse.mediator.meldinger.AvsluttetUtenVedtakRiver
import no.nav.helse.mediator.meldinger.AvvikVurdertRiver
import no.nav.helse.mediator.meldinger.BehandlingOpprettetRiver
import no.nav.helse.mediator.meldinger.EndretSkjermetinfoRiver
import no.nav.helse.mediator.meldinger.GodkjenningsbehovRiver
import no.nav.helse.mediator.meldinger.GosysOppgaveEndretRiver
import no.nav.helse.mediator.meldinger.MidnattRiver
import no.nav.helse.mediator.meldinger.NyeVarslerRiver
import no.nav.helse.mediator.meldinger.OppdaterPersonsnapshotRiver
import no.nav.helse.mediator.meldinger.OverstyringIgangsattRiver
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.mediator.meldinger.StansAutomatiskBehandlingRiver
import no.nav.helse.mediator.meldinger.SøknadSendtRiver
import no.nav.helse.mediator.meldinger.TilbakedateringBehandletRiver
import no.nav.helse.mediator.meldinger.UtbetalingAnnullertRiver
import no.nav.helse.mediator.meldinger.UtbetalingEndretRiver
import no.nav.helse.mediator.meldinger.VarseldefinisjonRiver
import no.nav.helse.mediator.meldinger.VedtakFattetRiver
import no.nav.helse.mediator.meldinger.VedtaksperiodeEndretRiver
import no.nav.helse.mediator.meldinger.VedtaksperiodeForkastetRiver
import no.nav.helse.mediator.meldinger.VedtaksperiodeNyUtbetalingRiver
import no.nav.helse.mediator.meldinger.VedtaksperiodeReberegnetRiver
import no.nav.helse.mediator.meldinger.Vedtaksperiodemelding
import no.nav.helse.mediator.meldinger.løsninger.ArbeidsforholdRiver
import no.nav.helse.mediator.meldinger.løsninger.ArbeidsgiverRiver
import no.nav.helse.mediator.meldinger.løsninger.DokumentRiver
import no.nav.helse.mediator.meldinger.løsninger.EgenAnsattløsning
import no.nav.helse.mediator.meldinger.løsninger.FlerePersoninfoRiver
import no.nav.helse.mediator.meldinger.løsninger.Fullmaktløsning
import no.nav.helse.mediator.meldinger.løsninger.HentEnhetRiver
import no.nav.helse.mediator.meldinger.løsninger.InfotrygdutbetalingerRiver
import no.nav.helse.mediator.meldinger.løsninger.Inntektløsning
import no.nav.helse.mediator.meldinger.løsninger.PersoninfoRiver
import no.nav.helse.mediator.meldinger.løsninger.Risikovurderingløsning
import no.nav.helse.mediator.meldinger.løsninger.SaksbehandlerløsningRiver
import no.nav.helse.mediator.meldinger.løsninger.Vergemålløsning
import no.nav.helse.mediator.meldinger.løsninger.ÅpneGosysOppgaverløsning
import no.nav.helse.mediator.meldinger.påminnelser.KommandokjedePåminnelseRiver
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.MeldingDao
import no.nav.helse.modell.MeldingDuplikatkontrollDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.dokument.DokumentDao
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.person.AdressebeskyttelseEndretRiver
import no.nav.helse.modell.person.OppdaterPersonsnapshot
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.person.PersonRepository
import no.nav.helse.modell.person.SøknadSendt
import no.nav.helse.modell.stoppautomatiskbehandling.StansAutomatiskBehandlingMediator
import no.nav.helse.modell.stoppautomatiskbehandling.StoppknappÅrsak
import no.nav.helse.modell.varsel.VarselRepository
import no.nav.helse.modell.varsel.Varseldefinisjon
import no.nav.helse.modell.vedtaksperiode.vedtak.VedtakFattet
import no.nav.helse.modell.vilkårsprøving.AvviksvurderingDto
import no.nav.helse.objectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.spesialist.api.Personhåndterer
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

internal interface SpesialistRiver : River.PacketListener {
    fun validations(): River.PacketValidation
}

internal class MeldingMediator(
    private val dataSource: DataSource,
    private val rapidsConnection: RapidsConnection,
    private val vedtakDao: VedtakDao = VedtakDao(dataSource),
    private val personDao: PersonDao = PersonDao(dataSource),
    private val commandContextDao: CommandContextDao = CommandContextDao(dataSource),
    private val meldingDao: MeldingDao = MeldingDao(dataSource),
    private val meldingDuplikatkontrollDao: MeldingDuplikatkontrollDao = MeldingDuplikatkontrollDao(dataSource),
    private val kommandofabrikk: Kommandofabrikk,
    private val dokumentDao: DokumentDao = DokumentDao(dataSource),
    avviksvurderingDao: AvviksvurderingDao,
    private val varselRepository: VarselRepository = VarselRepository(dataSource),
    private val stansAutomatiskBehandlingMediator: StansAutomatiskBehandlingMediator,
    private val personRepository: PersonRepository = PersonRepository(dataSource),
) : Personhåndterer {
    private companion object {
        private val env = Environment()
        private val logg = LoggerFactory.getLogger(MeldingMediator::class.java)
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    private fun skalBehandleMelding(melding: String): Boolean {
        val jsonNode = objectMapper.readTree(melding)
        if (erDuplikat(jsonNode)) return false
        if (env.erProd) return true
        return skalBehandleMeldingIDev(jsonNode)
    }

    private fun erDuplikat(jsonNode: JsonNode): Boolean {
        jsonNode["@id"]?.asUUID()?.let { id ->
            if (meldingDuplikatkontrollDao.erBehandlet(id)) {
                logg.info("Ignorerer melding {} pga duplikatkontroll", id)
                return true
            }
        }
        return false
    }

    private fun skalBehandleMeldingIDev(jsonNode: JsonNode): Boolean {
        val eventName = jsonNode["@event_name"]?.asText()
        if (eventName in
            setOf(
                "sendt_søknad_arbeidsgiver",
                "sendt_søknad_nav",
                "stans_automatisk_behandling",
            )
        ) {
            return true
        }
        val fødselsnummer = jsonNode["fødselsnummer"]?.asText() ?: return true
        if (fødselsnummer.toDoubleOrNull() == null) return true
        val harPerson = personDao.findPersonByFødselsnummer(fødselsnummer) != null
        if (!harPerson) {
            sikkerlogg.warn(
                "Ignorerer melding med event_name: {}, for fødselsnummer: {}",
                eventName,
                fødselsnummer,
            )
        }
        return harPerson
    }

    init {
        val delegatedRapid =
            DelegatedRapid(rapidsConnection, ::nullstillTilstand, ::skalBehandleMelding, ::fortsett, ::errorHandler)
        val rivers =
            setOf(
                GodkjenningsbehovRiver(this),
                SøknadSendtRiver(this),
                SøknadSendtArbeidsledigRiver(this),
                PersoninfoRiver(this),
                FlerePersoninfoRiver(this),
                HentEnhetRiver(this),
                InfotrygdutbetalingerRiver(this),
                SaksbehandlerløsningRiver(this),
                ArbeidsgiverRiver(this),
                ArbeidsforholdRiver(this),
                VedtaksperiodeForkastetRiver(this),
                VedtaksperiodeEndretRiver(this),
                AdressebeskyttelseEndretRiver(this),
                OverstyringIgangsattRiver(this),
                EgenAnsattløsning.EgenAnsattRiver(this),
                Vergemålløsning.VergemålRiver(this),
                Fullmaktløsning.FullmaktRiver(this),
                ÅpneGosysOppgaverløsning.ÅpneGosysOppgaverRiver(this),
                Risikovurderingløsning.V2River(this),
                Inntektløsning.InntektRiver(this),
                UtbetalingAnnullertRiver(this),
                OppdaterPersonsnapshotRiver(this),
                UtbetalingEndretRiver(this),
                VedtaksperiodeReberegnetRiver(this),
                GosysOppgaveEndretRiver(this),
                TilbakedateringBehandletRiver(this),
                EndretSkjermetinfoRiver(this),
                DokumentRiver(dokumentDao),
                VedtakFattetRiver(this),
                NyeVarslerRiver(this),
                AvvikVurdertRiver(this),
                VarseldefinisjonRiver(this),
                VedtaksperiodeNyUtbetalingRiver(this),
                MetrikkRiver(),
                AvsluttetMedVedtakRiver(this, avviksvurderingDao),
                AvsluttetUtenVedtakRiver(this),
                MidnattRiver(this),
                BehandlingOpprettetRiver(this),
                KommandokjedePåminnelseRiver(this),
                StansAutomatiskBehandlingRiver(this),
            )
        rivers.forEach { river ->
            River(delegatedRapid)
                .validate(river.validations())
                .register(river)
                .onSuccess { packet, _ ->
                    packet.interestedIn("@id", "@event_name")
                    val id = packet["@id"].asText() ?: "ukjent"
                    val type = packet["@event_name"].asText() ?: "ukjent"
                    logg.info(
                        "${river.name()} leste melding id=$id, event_name=$type, meldingPasserteValidering=$meldingPasserteValidering",
                    )
                    meldingPasserteValidering = true
                }
        }
    }

    private var løsninger: Løsninger? = null
    private var meldingPasserteValidering = false

    // samler opp løsninger
    fun løsning(
        hendelseId: UUID,
        contextId: UUID,
        behovId: UUID,
        løsning: Any,
        context: MessageContext,
    ) {
        withMDC(
            mapOf(
                "contextId" to "$contextId",
                "meldingId" to "$behovId",
                "opprinneligMeldingId" to "$hendelseId",
            ),
        ) {
            løsninger(context, hendelseId, contextId)?.also { it.add(hendelseId, contextId, løsning) }
                ?: logg.info(
                    "mottok løsning som ikke kunne brukes fordi kommandoen ikke lengre er suspendert, eller fordi hendelsen er ukjent",
                )
        }
    }

    fun påminnelse(
        meldingId: UUID,
        contextId: UUID,
        hendelseId: UUID,
        påminnelse: Any,
        context: MessageContext,
    ) {
        withMDC(
            mapOf(
                "contextId" to "$contextId",
                "opprinneligMeldingId" to "$hendelseId",
                "meldingId" to "$meldingId",
            ),
        ) {
            påminnelse(context, hendelseId, contextId)?.also {
                it.add(hendelseId, contextId, påminnelse)
                it.fortsett(this)
            }
                ?: logg.info(
                    "mottok påminnelse som ikke kunne brukes fordi kommandoen ikke lengre er suspendert, eller fordi hendelsen er ukjent",
                )
        }
    }

    internal fun håndter(varseldefinisjon: Varseldefinisjon) {
        val varseldefinisjonDto = varseldefinisjon.toDto()
        varselRepository.lagreDefinisjon(varseldefinisjonDto)
        if (varseldefinisjonDto.avviklet) {
            varselRepository.avvikleVarsel(varseldefinisjonDto)
        }
    }

    internal fun håndter(avviksvurdering: AvviksvurderingDto) {
        kommandofabrikk.avviksvurdering(avviksvurdering)
    }

    fun stansAutomatiskBehandling(
        fødselsnummer: String,
        status: String,
        årsaker: Set<StoppknappÅrsak>,
        opprettet: LocalDateTime,
        originalMelding: String,
        kilde: String,
    ) = stansAutomatiskBehandlingMediator.håndter(fødselsnummer, status, årsaker, opprettet, originalMelding, kilde)

    fun slettGamleDokumenter(): Int {
        return dokumentDao.slettGamleDokumenter()
    }

    private fun nullstillTilstand() {
        løsninger = null
        meldingPasserteValidering = false
    }

    private fun løsninger(
        messageContext: MessageContext,
        meldingId: UUID,
        contextId: UUID,
    ): Løsninger? {
        return løsninger ?: run {
            val commandContext =
                commandContextDao.finnSuspendert(contextId) ?: run {
                    logg.info("Ignorerer melding fordi kommandokonteksten ikke er suspendert")
                    return null
                }
            val melding =
                meldingDao.finn(meldingId) ?: run {
                    logg.info("Ignorerer melding fordi opprinnelig melding ikke finnes i databasen")
                    return null
                }
            Løsninger(messageContext, melding, contextId, commandContext).also { løsninger = it }
        }
    }

    private fun påminnelse(
        messageContext: MessageContext,
        meldingId: UUID,
        contextId: UUID,
    ): Påminnelse? {
        if (meldingId.toString() in
            setOf(
                "c2ffe6ca-4b63-421d-b4e8-e57f34da5bef",
                "20bf35c2-1ff7-4af1-933c-0ffc91e206c6",
                "08625b54-dfbe-4bf4-94b8-3bf6f81fbf73",
                "233060cc-162c-4344-8052-daeae34c412b",
            )
        ) {
            logg.info("Ignorerer påminnelse for kontekster for melding_id=$meldingId som ikke lar seg gjenoppta")
            return null
        }
        val commandContext =
            commandContextDao.finnSuspendertEllerFeil(contextId) ?: run {
                logg.info("Ignorerer melding fordi kommandokonteksten ikke er suspendert eller feil")
                return null
            }
        val hendelse =
            meldingDao.finn(meldingId) ?: run {
                logg.info("Ignorerer melding fordi opprinnelig melding ikke finnes i databasen")
                return null
            }

        return Påminnelse(messageContext, hendelse, contextId, commandContext)
    }

    // fortsetter en command (resume) med oppsamlet løsninger
    private fun fortsett(message: String) {
        val jsonNode = objectMapper.readTree(message)
        løsninger?.fortsett(this, jsonNode)
        if (meldingPasserteValidering) {
            jsonNode["@id"]?.asUUID()?.let { id ->
                val type =
                    when (val eventName = jsonNode["@event_name"]?.asText()) {
                        null -> "ukjent"
                        "behov" -> "behov: " + jsonNode["@behov"].joinToString { it.asText() }
                        else -> eventName
                    }
                logg.info("Markerer melding id=$id, type=$type som behandlet i duplikatkontroll")
                meldingDuplikatkontrollDao.lagre(id, type)
            }
        }
    }

    private fun errorHandler(
        err: Exception,
        message: String,
    ) {
        logg.error("alvorlig feil: ${err.message} (se sikkerlogg for melding)", err)
        sikkerlogg.error("alvorlig feil: ${err.message}\n\t$message", err, err.printStackTrace())
    }

    internal fun mottaSøknadSendt(
        melding: SøknadSendt,
        messageContext: MessageContext,
    ) {
        val meldingnavn = requireNotNull(melding::class.simpleName)
        withMDC(
            mutableMapOf(
                "meldingId" to melding.id.toString(),
                "meldingnavn" to meldingnavn,
            ),
        ) {
            logg.info("Melding SøknadSendt mottatt")
            sikkerlogg.info("Melding SøknadSendt mottatt:\n${melding.toJson()}")
            meldingDao.lagre(melding)
            val commandContextTilstandMediator = CommandContextTilstandMediator()
            kommandofabrikk.iverksettSøknadSendt(melding, commandContextTilstandMediator)
            commandContextTilstandMediator.publiserTilstandsendringer(melding, messageContext)
            logg.info("Melding SøknadSendt lest")
            sikkerlogg.info("Melding SøknadSendt lest")
        }
    }

    internal fun mottaMelding(
        melding: Personmelding,
        messageContext: MessageContext,
    ) {
        val meldingnavn = requireNotNull(melding::class.simpleName)
        withMDC(
            mutableMapOf(
                "meldingId" to melding.id.toString(),
                "meldingnavn" to meldingnavn,
            ).apply {
                if (melding is Vedtaksperiodemelding) put("vedtaksperiodeId", melding.vedtaksperiodeId().toString())
            },
        ) {
            logg.info("Melding $meldingnavn mottatt")
            sikkerlogg.info("Melding $meldingnavn mottatt:\n${melding.toJson()}")
            meldingDao.lagre(melding)
            behandleMelding(melding, messageContext)
        }
    }

    private fun gjenopptaMelding(
        melding: Personmelding,
        commandContext: CommandContext,
        messageContext: MessageContext,
    ) {
        val meldingnavn = requireNotNull(melding::class.simpleName)
        withMDC(
            mapOf(
                "meldingId" to melding.id.toString(),
                "meldingnavn" to meldingnavn,
            ),
        ) {
            logg.info("Melding $meldingnavn gjenopptatt")
            sikkerlogg.info("Melding $meldingnavn gjenopptatt:\n${melding.toJson()}")
            behandleMelding(melding, messageContext, commandContext)
        }
    }

    private fun nyContext(meldingId: UUID) =
        CommandContext(UUID.randomUUID()).apply {
            opprett(commandContextDao, meldingId)
        }

    // Denne kalles når vi behandler en melding som starter en kommandokjede, eller den er i hvert fall ikke inne i
    // bildet når vi gjenopptar kommandokjeder
    private fun behandleMelding(
        melding: Personmelding,
        messageContext: MessageContext,
    ) {
        behandleMelding(melding, messageContext, nyContext(melding.id))
    }

    // Denne kalles både ved oppstart av en kommandokjede og ved gjenopptak etter svar på behov
    private fun behandleMelding(
        melding: Personmelding,
        messageContext: MessageContext,
        commandContext: CommandContext,
    ) {
        val meldingnavn = requireNotNull(melding::class.simpleName)
        val utgåendeMeldingerMediator = UtgåendeMeldingerMediator()
        val commandContextTilstandMediator = CommandContextTilstandMediator()
        val vedtakFattetMelder = VedtakFattetMelder(messageContext)
        try {
            personRepository.brukPersonHvisFinnes(melding.fødselsnummer()) {
                this.nyObserver(vedtakFattetMelder)
                logg.info("Personen finnes i databasen, behandler melding $meldingnavn")
                sikkerlogg.info("Personen finnes i databasen, behandler melding $meldingnavn")

                val kommandostarter =
                    kommandofabrikk.lagKommandostarter(
                        commandContext,
                        setOf(utgåendeMeldingerMediator, commandContextTilstandMediator),
                    )
                melding.behandle(this, kommandostarter)
            }
            if (melding is VedtakFattet) melding.doFinally(vedtakDao) // Midlertidig frem til spesialsak ikke er en ting lenger
            vedtakFattetMelder.publiserUtgåendeMeldinger()
            utgåendeMeldingerMediator.publiserOppsamledeMeldinger(melding, messageContext)
        } catch (e: Exception) {
            logg.error("Feil ved behandling av melding $meldingnavn", e.message, e)
            throw e
        } finally {
            commandContextTilstandMediator.publiserTilstandsendringer(melding, messageContext)
            logg.info("Melding $meldingnavn lest")
            sikkerlogg.info("Melding $meldingnavn lest")
        }
    }

    private class Løsninger(
        private val messageContext: MessageContext,
        private val melding: Personmelding,
        private val contextId: UUID,
        private val commandContext: CommandContext,
    ) {
        fun add(
            hendelseId: UUID,
            contextId: UUID,
            løsning: Any,
        ) {
            check(hendelseId == melding.id)
            check(contextId == this.contextId)
            commandContext.add(løsning)
        }

        fun fortsett(
            mediator: MeldingMediator,
            jsonNode: JsonNode,
        ) {
            val behov = jsonNode["@behov"].map(JsonNode::asText)
            "fortsetter utførelse av kommandokjede for ${melding::class.simpleName} som følge av løsninger $behov".let {
                logg.info(it)
                sikkerlogg.info("$it\nInnkommende melding:\n\t$jsonNode")
            }
            mediator.gjenopptaMelding(melding, commandContext, messageContext)
        }
    }

    private class Påminnelse(
        private val messageContext: MessageContext,
        private val melding: Personmelding,
        private val contextId: UUID,
        private val commandContext: CommandContext,
    ) {
        fun add(
            hendelseId: UUID,
            contextId: UUID,
            påminnelse: Any,
        ) {
            check(hendelseId == melding.id)
            check(contextId == this.contextId)
            commandContext.add(påminnelse)
        }

        fun fortsett(mediator: MeldingMediator) {
            logg.info("fortsetter utførelse av kommandokontekst som følge av påminnelse")
            mediator.gjenopptaMelding(melding, commandContext, messageContext)
        }
    }

    override fun oppdaterSnapshot(fnr: String) {
        val json =
            objectMapper.readTree(
                JsonMessage.newMessage(
                    "oppdater_personsnapshot",
                    mapOf(
                        "fødselsnummer" to fnr,
                    ),
                ).toJson(),
            )

        mottaMelding(OppdaterPersonsnapshot(json), rapidsConnection)
    }
}
