package no.nav.helse.mediator

import com.fasterxml.jackson.databind.JsonNode
import kotliquery.sessionOf
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.bootstrap.Environment
import no.nav.helse.db.AvviksvurderingDao
import no.nav.helse.db.CommandContextRepository
import no.nav.helse.db.PgVedtakDao
import no.nav.helse.db.VedtakDao
import no.nav.helse.duplikatsjekkTidsbruk
import no.nav.helse.kafka.AdressebeskyttelseEndretRiver
import no.nav.helse.kafka.ArbeidsforholdLøsningRiver
import no.nav.helse.kafka.ArbeidsgiverinformasjonLøsningRiver
import no.nav.helse.kafka.AvsluttetMedVedtakRiver
import no.nav.helse.kafka.AvsluttetUtenVedtakRiver
import no.nav.helse.kafka.AvvikVurdertRiver
import no.nav.helse.kafka.BehandlingOpprettetRiver
import no.nav.helse.kafka.DokumentRiver
import no.nav.helse.kafka.EgenAnsattLøsningRiver
import no.nav.helse.kafka.EndretSkjermetinfoRiver
import no.nav.helse.kafka.FlerePersoninfoRiver
import no.nav.helse.kafka.FullmaktLøsningRiver
import no.nav.helse.kafka.GodkjenningsbehovRiver
import no.nav.helse.kafka.GosysOppgaveEndretRiver
import no.nav.helse.kafka.HentEnhetLøsningRiver
import no.nav.helse.kafka.InfotrygdutbetalingerLøsningRiver
import no.nav.helse.kafka.InntektLøsningRiver
import no.nav.helse.kafka.KlargjørPersonForVisningRiver
import no.nav.helse.kafka.KommandokjedePåminnelseRiver
import no.nav.helse.kafka.MetrikkRiver
import no.nav.helse.kafka.MidnattRiver
import no.nav.helse.kafka.NyeVarslerRiver
import no.nav.helse.kafka.OppdaterPersondataRiver
import no.nav.helse.kafka.OverstyringIgangsattRiver
import no.nav.helse.kafka.PersoninfoløsningRiver
import no.nav.helse.kafka.SaksbehandlerløsningRiver
import no.nav.helse.kafka.StansAutomatiskBehandlingRiver
import no.nav.helse.kafka.SøknadSendtArbeidsledigRiver
import no.nav.helse.kafka.SøknadSendtRiver
import no.nav.helse.kafka.TilbakedateringBehandletRiver
import no.nav.helse.kafka.UtbetalingEndretRiver
import no.nav.helse.kafka.VarseldefinisjonRiver
import no.nav.helse.kafka.VedtakFattetRiver
import no.nav.helse.kafka.VedtaksperiodeForkastetRiver
import no.nav.helse.kafka.VedtaksperiodeNyUtbetalingRiver
import no.nav.helse.kafka.VedtaksperiodeReberegnetRiver
import no.nav.helse.kafka.VergemålLøsningRiver
import no.nav.helse.kafka.VurderingsmomenterLøsningRiver
import no.nav.helse.kafka.ÅpneGosysOppgaverLøsningRiver
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.mediator.meldinger.PoisonPills
import no.nav.helse.mediator.meldinger.Vedtaksperiodemelding
import no.nav.helse.modell.MeldingDao
import no.nav.helse.modell.MeldingDuplikatkontrollDao
import no.nav.helse.modell.dokument.DokumentDao
import no.nav.helse.modell.dokument.PgDokumentDao
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.CommandContextDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.person.PersonService
import no.nav.helse.modell.person.SøknadSendt
import no.nav.helse.modell.varsel.VarselRepository
import no.nav.helse.modell.varsel.Varseldefinisjon
import no.nav.helse.modell.vedtaksperiode.vedtak.VedtakFattet
import no.nav.helse.objectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.toUUID
import no.nav.helse.spesialist.api.Personhåndterer
import org.slf4j.LoggerFactory
import java.util.UUID
import javax.sql.DataSource
import kotlin.time.DurationUnit.MILLISECONDS
import kotlin.time.measureTimedValue

internal class MeldingMediator(
    private val dataSource: DataSource,
    private val rapidsConnection: RapidsConnection,
    private val vedtakDao: VedtakDao = PgVedtakDao(dataSource),
    private val personDao: PersonDao = PersonDao(dataSource),
    private val commandContextDao: CommandContextDao = CommandContextDao(dataSource),
    private val meldingDao: MeldingDao = MeldingDao(dataSource),
    private val meldingDuplikatkontrollDao: MeldingDuplikatkontrollDao = MeldingDuplikatkontrollDao(dataSource),
    private val kommandofabrikk: Kommandofabrikk,
    private val dokumentDao: DokumentDao = PgDokumentDao(dataSource),
    avviksvurderingDao: AvviksvurderingDao,
    private val varselRepository: VarselRepository = VarselRepository(dataSource),
    private val personService: PersonService = PersonService(dataSource),
    private val poisonPills: PoisonPills,
) : Personhåndterer {
    private companion object {
        private val env = Environment()
        private val logg = LoggerFactory.getLogger(MeldingMediator::class.java)
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    private fun skalBehandleMelding(melding: String): Boolean {
        val jsonNode = objectMapper.readTree(melding)
        if (poisonPills.erPoisonPill(jsonNode)) return false
        if (env.erProd) return true
        return skalBehandleMeldingIDev(jsonNode)
    }

    private fun erDuplikat(id: UUID): Boolean {
        val (erDuplikat, tid) = measureTimedValue { meldingDuplikatkontrollDao.erBehandlet(id) }
        logg.info("Det tok ${tid.inWholeMilliseconds} ms å gjøre duplikatsjekk mot databasen")
        duplikatsjekkTidsbruk.labels(erDuplikat.toString()).observe(tid.toDouble(MILLISECONDS))

        return erDuplikat
    }

    private fun duplikatsjekkendeRiver(river: River.PacketListener) =
        object : River.PacketListener by river {
            override fun onPacket(
                packet: JsonMessage,
                context: MessageContext,
            ) {
                val id = packet.id.toUUID()
                if (erDuplikat(id)) {
                    logg.info("Ignorerer melding {} pga duplikatkontroll", id)
                    return
                }
                river.onPacket(packet, context)
            }
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
        val harPerson = personDao.finnPersonMedFødselsnummer(fødselsnummer) != null
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
                PersoninfoløsningRiver(this),
                FlerePersoninfoRiver(this),
                HentEnhetLøsningRiver(this),
                InfotrygdutbetalingerLøsningRiver(this),
                SaksbehandlerløsningRiver(this),
                ArbeidsgiverinformasjonLøsningRiver(this),
                ArbeidsforholdLøsningRiver(this),
                VedtaksperiodeForkastetRiver(this),
                AdressebeskyttelseEndretRiver(this),
                OverstyringIgangsattRiver(this),
                EgenAnsattLøsningRiver(this),
                VergemålLøsningRiver(this),
                FullmaktLøsningRiver(this),
                ÅpneGosysOppgaverLøsningRiver(this),
                VurderingsmomenterLøsningRiver(this),
                InntektLøsningRiver(this),
                OppdaterPersondataRiver(this),
                KlargjørPersonForVisningRiver(this),
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
                .register(duplikatsjekkendeRiver(river))
                .onSuccess { packet, _ ->
                    logg.info(
                        "${river.name()} leste melding id=${packet.id}, event_name=${packet.eventName()}, meldingPasserteValidering=$meldingPasserteValidering",
                    )
                    meldingPasserteValidering = true
                }
        }
    }

    private fun JsonMessage.eventName() =
        run {
            interestedIn("@event_name")
            get("@event_name").textValue() ?: "ukjent"
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

    fun slettGamleDokumenter(): Int = dokumentDao.slettGamleDokumenter()

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
            val melding = finnMelding(meldingId) ?: return null
            Løsninger(messageContext, melding, contextId, commandContext).also { løsninger = it }
        }
    }

    private fun påminnelse(
        messageContext: MessageContext,
        meldingId: UUID,
        contextId: UUID,
    ): Påminnelse? {
        val commandContext =
            commandContextDao.finnSuspendertEllerFeil(contextId) ?: run {
                logg.info("Ignorerer melding fordi kommandokonteksten ikke er suspendert eller feil")
                return null
            }
        val melding = finnMelding(meldingId) ?: return null

        return Påminnelse(messageContext, melding, contextId, commandContext)
    }

    private fun finnMelding(meldingId: UUID): Personmelding? =
        meldingDao.finn(meldingId) ?: run {
            logg.info("Ignorerer melding fordi opprinnelig melding ikke finnes i databasen")
            return null
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
        val messageJson = objectMapper.writeValueAsString(message)
        logg.error("alvorlig feil: ${err.message} (se sikkerlogg for melding)", err)
        sikkerlogg.error("alvorlig feil: ${err.message}", kv("stack_trace", err), kv("json", messageJson))
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
            behandleMelding(melding, messageContext) { commandContext }
        }
    }

    // Denne kalles når vi behandler en melding som starter en kommandokjede, eller den er i hvert fall ikke inne i
    // bildet når vi gjenopptar kommandokjeder
    private fun behandleMelding(
        melding: Personmelding,
        messageContext: MessageContext,
    ) {
        behandleMelding(melding, messageContext) { it.nyContext(melding.id) }
    }

    // Denne kalles både ved oppstart av en kommandokjede og ved gjenopptak etter svar på behov
    private fun behandleMelding(
        melding: Personmelding,
        messageContext: MessageContext,
        commandContext: (CommandContextRepository) -> CommandContext,
    ) {
        val meldingnavn = requireNotNull(melding::class.simpleName)
        val utgåendeMeldingerMediator = UtgåendeMeldingerMediator()
        val commandContextTilstandMediator = CommandContextTilstandMediator()
        val vedtakFattetMelder = VedtakFattetMelder(messageContext)
        try {
            personService.brukPersonHvisFinnes(melding.fødselsnummer()) {
                this.nyObserver(vedtakFattetMelder)
                logg.info("Personen finnes i databasen, behandler melding $meldingnavn")
                sikkerlogg.info("Personen finnes i databasen, behandler melding $meldingnavn")
                sessionOf(dataSource, returnGeneratedKey = true).use { session ->
                    session.transaction { transactionalSession ->
                        val kommandostarter =
                            kommandofabrikk.lagKommandostarter(
                                setOf(utgåendeMeldingerMediator, commandContextTilstandMediator),
                                commandContext(CommandContextDao(transactionalSession)),
                                transactionalSession,
                            )
                        melding.behandle(this, kommandostarter, transactionalSession)
                    }
                }
            }
            if (melding is VedtakFattet) melding.doFinally(vedtakDao) // Midlertidig frem til spesialsak ikke er en ting lenger
            vedtakFattetMelder.publiserUtgåendeMeldinger()
            utgåendeMeldingerMediator.publiserOppsamledeMeldinger(melding, messageContext)
        } catch (e: Exception) {
            throw RuntimeException("Feil ved behandling av melding $meldingnavn", e)
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

    override fun oppdaterSnapshot(fødselsnummer: String) {
        val event =
            JsonMessage
                .newMessage("oppdater_persondata", mapOf("fødselsnummer" to fødselsnummer))
                .toJson()
        rapidsConnection.publish(fødselsnummer, event)
    }

    override fun klargjørPersonForVisning(fødselsnummer: String) {
        val event =
            JsonMessage
                .newMessage("klargjør_person_for_visning", mapOf("fødselsnummer" to fødselsnummer))
                .toJson()
        rapidsConnection.publish(fødselsnummer, event)
    }
}
