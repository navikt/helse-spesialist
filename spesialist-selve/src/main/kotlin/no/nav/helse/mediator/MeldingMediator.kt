package no.nav.helse.mediator

import com.fasterxml.jackson.databind.JsonNode
import kotliquery.sessionOf
import no.nav.helse.MeldingPubliserer
import no.nav.helse.bootstrap.Environment
import no.nav.helse.db.CommandContextRepository
import no.nav.helse.db.Repositories
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.mediator.meldinger.PoisonPills
import no.nav.helse.mediator.meldinger.Vedtaksperiodemelding
import no.nav.helse.modell.MeldingDao
import no.nav.helse.modell.MeldingDuplikatkontrollDao
import no.nav.helse.modell.dokument.DokumentDao
import no.nav.helse.modell.dokument.PgDokumentDao
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.CommandContextDao
import no.nav.helse.modell.melding.KlargjørPersonForVisning
import no.nav.helse.modell.melding.OppdaterPersondata
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.person.PersonService
import no.nav.helse.modell.person.SøknadSendt
import no.nav.helse.modell.varsel.VarselRepository
import no.nav.helse.modell.varsel.Varseldefinisjon
import no.nav.helse.objectMapper
import no.nav.helse.spesialist.api.Personhåndterer
import org.slf4j.LoggerFactory
import java.util.UUID
import javax.sql.DataSource

class MeldingMediator(
    private val dataSource: DataSource,
    private val repositories: Repositories,
    private val publiserer: MeldingPubliserer,
    private val personDao: PersonDao = PersonDao(dataSource),
    private val commandContextDao: CommandContextDao = CommandContextDao(dataSource),
    private val meldingDao: MeldingDao = MeldingDao(dataSource),
    private val meldingDuplikatkontrollDao: MeldingDuplikatkontrollDao = MeldingDuplikatkontrollDao(dataSource),
    private val kommandofabrikk: Kommandofabrikk,
    private val dokumentDao: DokumentDao = PgDokumentDao(dataSource),
    private val varselRepository: VarselRepository = VarselRepository(dataSource),
    private val personService: PersonService = PersonService(dataSource, repositories),
    private val poisonPills: PoisonPills,
) : Personhåndterer {
    private companion object {
        private val env = Environment()
        private val logg = LoggerFactory.getLogger(MeldingMediator::class.java)
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    fun skalBehandleMelding(melding: String): Boolean {
        val jsonNode = objectMapper.readTree(melding)
        if (poisonPills.erPoisonPill(jsonNode)) return false
        if (env.erProd) return true
        return skalBehandleMeldingIDev(jsonNode)
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
                "Ignorerer melding med event_name: {}, person med fødselsnummer {} fins ikke i databasen",
                eventName,
                fødselsnummer,
            )
        }
        return harPerson
    }

    private var løsninger: Løsninger? = null
    var meldingPasserteValidering = false

    // samler opp løsninger
    fun løsning(
        hendelseId: UUID,
        contextId: UUID,
        behovId: UUID,
        løsning: Any,
        kontekstbasertPubliserer: MeldingPubliserer,
    ) {
        withMDC(
            mapOf(
                "contextId" to "$contextId",
                "meldingId" to "$behovId",
                "opprinneligMeldingId" to "$hendelseId",
            ),
        ) {
            løsninger(kontekstbasertPubliserer, hendelseId, contextId)?.also { it.add(hendelseId, contextId, løsning) }
                ?: logg.info(
                    "mottok løsning som ikke kunne brukes fordi kommandoen ikke lengre er suspendert, eller fordi hendelsen er ukjent",
                )
        }
    }

    fun påminnelse(
        meldingId: UUID,
        contextId: UUID,
        hendelseId: UUID,
        kontekstbasertPubliserer: MeldingPubliserer,
    ) {
        withMDC(
            mapOf(
                "contextId" to "$contextId",
                "opprinneligMeldingId" to "$hendelseId",
                "meldingId" to "$meldingId",
            ),
        ) {
            påminnelse(kontekstbasertPubliserer, hendelseId, contextId)?.fortsett(this)
                ?: logg.info("mottok påminnelse som ikke kunne brukes fordi kommandoen ikke lengre er suspendert, eller fordi hendelsen er ukjent")
        }
    }

    fun håndter(varseldefinisjon: Varseldefinisjon) {
        val varseldefinisjonDto = varseldefinisjon.toDto()
        varselRepository.lagreDefinisjon(varseldefinisjonDto)
        if (varseldefinisjonDto.avviklet) {
            varselRepository.avvikleVarsel(varseldefinisjonDto)
        }
    }

    fun mottaDokument(
        fødselsnummer: String,
        dokumentId: UUID,
        dokument: JsonNode,
    ) {
        dokumentDao.lagre(fødselsnummer, dokumentId, dokument)
    }

    fun slettGamleDokumenter(): Int = dokumentDao.slettGamleDokumenter()

    fun nullstillTilstand() {
        løsninger = null
        meldingPasserteValidering = false
    }

    private fun løsninger(
        kontekstbasertPubliserer: MeldingPubliserer,
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
            Løsninger(kontekstbasertPubliserer, melding, contextId, commandContext).also { løsninger = it }
        }
    }

    private fun påminnelse(
        kontekstbasertPubliserer: MeldingPubliserer,
        meldingId: UUID,
        contextId: UUID,
    ): Påminnelse? {
        val commandContext =
            commandContextDao.finnSuspendertEllerFeil(contextId) ?: run {
                logg.info("Ignorerer melding fordi kommandokonteksten ikke er suspendert eller feil")
                return null
            }
        val melding = finnMelding(meldingId) ?: return null

        return Påminnelse(kontekstbasertPubliserer, melding, commandContext)
    }

    private fun finnMelding(meldingId: UUID): Personmelding? =
        meldingDao.finn(meldingId) ?: run {
            logg.info("Ignorerer melding fordi opprinnelig melding ikke finnes i databasen")
            return null
        }

    // fortsetter en command (resume) med oppsamlet løsninger
    fun fortsett(message: String) {
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

    fun errorHandler(
        err: Exception,
        message: String,
    ) {
        val messageJson = objectMapper.readTree(message)
        val meldingId = messageJson["@id"]?.asUUID()
        withMDC(
            mapOf(
                "@id" to meldingId.toString(),
            ),
        ) {
            logg.error("alvorlig feil: ${err.message} (se sikkerlogg for melding)", err)
            sikkerlogg.error("alvorlig feil: ${err.message}\n${objectMapper.writeValueAsString(messageJson)}", err)
        }
    }

    fun mottaSøknadSendt(
        melding: SøknadSendt,
        kontekstbasertPubliserer: MeldingPubliserer,
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
            val utgåendeMeldingerMediator = UtgåendeMeldingerMediator()
            kommandofabrikk.iverksettSøknadSendt(melding, utgåendeMeldingerMediator)
            utgåendeMeldingerMediator.publiserOppsamledeMeldinger(melding, kontekstbasertPubliserer)
            logg.info("Melding SøknadSendt lest")
            sikkerlogg.info("Melding SøknadSendt lest")
        }
    }

    fun mottaMelding(
        melding: Personmelding,
        kontekstbasertPubliserer: MeldingPubliserer,
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
            behandleMelding(melding, kontekstbasertPubliserer)
        }
    }

    private fun gjenopptaMelding(
        melding: Personmelding,
        commandContext: CommandContext,
        kontekstbasertPubliserer: MeldingPubliserer,
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
            behandleMelding(melding, kontekstbasertPubliserer) { commandContext }
        }
    }

    // Denne kalles når vi behandler en melding som starter en kommandokjede, eller den er i hvert fall ikke inne i
    // bildet når vi gjenopptar kommandokjeder
    private fun behandleMelding(
        melding: Personmelding,
        kontekstbasertPubliserer: MeldingPubliserer,
    ) {
        behandleMelding(melding, kontekstbasertPubliserer) { it.nyContext(melding.id) }
    }

    // Denne kalles både ved oppstart av en kommandokjede og ved gjenopptak etter svar på behov
    private fun behandleMelding(
        melding: Personmelding,
        kontekstbasertPubliserer: MeldingPubliserer,
        commandContext: (CommandContextRepository) -> CommandContext,
    ) {
        val meldingnavn = requireNotNull(melding::class.simpleName)
        val utgåendeMeldingerMediator = UtgåendeMeldingerMediator()
        try {
            personService.brukPersonHvisFinnes(melding.fødselsnummer()) {
                logg.info("Personen finnes i databasen, behandler melding $meldingnavn")
                sikkerlogg.info("Personen finnes i databasen, behandler melding $meldingnavn")
                sessionOf(dataSource, returnGeneratedKey = true).use { session ->
                    session.transaction { transactionalSession ->
                        val kommandostarter =
                            kommandofabrikk.lagKommandostarter(
                                setOf(utgåendeMeldingerMediator),
                                commandContext(CommandContextDao(transactionalSession)),
                                transactionalSession,
                            )
                        melding.behandle(this, kommandostarter, transactionalSession)
                    }
                }
                utgåendeMeldinger().forEach(utgåendeMeldingerMediator::hendelse)
            }
            utgåendeMeldingerMediator.publiserOppsamledeMeldinger(melding, kontekstbasertPubliserer)
        } catch (e: Exception) {
            throw RuntimeException("Feil ved behandling av melding $meldingnavn", e)
        } finally {
            logg.info("Melding $meldingnavn lest")
            sikkerlogg.info("Melding $meldingnavn lest")
        }
    }

    private class Løsninger(
        private val kontekstbasertPubliserer: MeldingPubliserer,
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
            mediator.gjenopptaMelding(melding, commandContext, kontekstbasertPubliserer)
        }
    }

    private class Påminnelse(
        private val kontekstbasertPubliserer: MeldingPubliserer,
        private val melding: Personmelding,
        private val commandContext: CommandContext,
    ) {
        fun fortsett(mediator: MeldingMediator) {
            logg.info("fortsetter utførelse av kommandokontekst som følge av påminnelse")
            mediator.gjenopptaMelding(melding, commandContext, kontekstbasertPubliserer)
        }
    }

    override fun oppdaterSnapshot(fødselsnummer: String) {
        publiserer.publiser(fødselsnummer, OppdaterPersondata, "oppdaterSnapshot")
    }

    override fun klargjørPersonForVisning(fødselsnummer: String) {
        publiserer.publiser(fødselsnummer, KlargjørPersonForVisning, "klargjørPersonForVisning")
    }
}
