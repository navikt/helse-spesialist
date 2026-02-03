package no.nav.helse.mediator

import com.fasterxml.jackson.databind.JsonNode
import com.github.benmanes.caffeine.cache.CacheLoader
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import no.nav.helse.MeldingPubliserer
import no.nav.helse.db.CommandContextDao
import no.nav.helse.db.DokumentDao
import no.nav.helse.db.MeldingDao
import no.nav.helse.db.MeldingDuplikatkontrollDao
import no.nav.helse.db.PersonDao
import no.nav.helse.db.PoisonPillDao
import no.nav.helse.db.SessionFactory
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.mediator.meldinger.PoisonPills
import no.nav.helse.mediator.meldinger.Vedtaksperiodemelding
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.person.vedtaksperiode.LegacyVedtaksperiode
import no.nav.helse.modell.varsel.LegacyVarselRepository
import no.nav.helse.modell.varsel.Varseldefinisjon
import no.nav.helse.spesialist.application.logg.MdcKey
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.application.logg.loggThrowable
import no.nav.helse.spesialist.application.logg.loggWarn
import no.nav.helse.spesialist.application.logg.medMdc
import no.nav.helse.spesialist.kafka.objectMapper
import java.time.Duration
import java.util.UUID

class MeldingMediator(
    private val sessionFactory: SessionFactory,
    private val personDao: PersonDao,
    private val commandContextDao: CommandContextDao,
    private val meldingDao: MeldingDao,
    private val meldingDuplikatkontrollDao: MeldingDuplikatkontrollDao,
    private val kommandofabrikk: Kommandofabrikk,
    private val dokumentDao: DokumentDao,
    private val legacyVarselRepository: LegacyVarselRepository,
    private val poisonPillDao: PoisonPillDao,
    private val ignorerMeldingerForUkjentePersoner: Boolean,
    poisonPillTimeToLive: Duration = Duration.ofMinutes(1),
) {
    private val poisonPillsCache: LoadingCache<Unit, PoisonPills> =
        Caffeine
            .newBuilder()
            .refreshAfterWrite(poisonPillTimeToLive)
            .expireAfterWrite(poisonPillTimeToLive + poisonPillTimeToLive)
            .build(CacheLoader { _ -> poisonPillDao.poisonPills() })

    fun skalBehandleMelding(melding: String): Boolean {
        val jsonNode = objectMapper.readTree(melding)
        if (poisonPillsCache.get(Unit).erPoisonPill(jsonNode)) {
            loggInfo(
                "Hopper over melding med @id: ${jsonNode["@id"].asText()}",
                "json: \n$jsonNode",
            )

            return false
        }
        return if (ignorerMeldingerForUkjentePersoner) {
            skalBehandleMeldingIDev(jsonNode)
        } else {
            true
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
            loggWarn(
                "Ignorerer melding med event_name: $eventName, person finnes ikke i databasen",
                "fødselsnummer: $fødselsnummer",
            )
        }
        return harPerson
    }

    private var løsninger: ThreadLocal<Løsninger?> = ThreadLocal.withInitial { null }
    var meldingenHarBlittBehandletAvEnRiver: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }

    // samler opp løsninger
    fun løsning(
        hendelseId: UUID,
        contextId: UUID,
        behovId: UUID,
        løsning: Any,
        kontekstbasertPubliserer: MeldingPubliserer,
    ) {
        medMdc(
            MdcKey.CONTEXT_ID to contextId.toString(),
            MdcKey.MELDING_ID to behovId.toString(),
            MdcKey.OPPRINNELIG_MELDING_ID to hendelseId.toString(),
        ) {
            løsninger(kontekstbasertPubliserer, hendelseId, contextId)?.also { it.add(hendelseId, contextId, løsning) }
                ?: loggInfo(
                    "mottok løsning som ikke kan brukes fordi kommandoen ikke lengre er suspendert, eller fordi hendelsen er ukjent",
                )
        }
    }

    fun påminnelse(
        meldingId: UUID,
        contextId: UUID,
        hendelseId: UUID,
        kontekstbasertPubliserer: MeldingPubliserer,
    ) {
        medMdc(
            MdcKey.CONTEXT_ID to contextId.toString(),
            MdcKey.OPPRINNELIG_MELDING_ID to hendelseId.toString(),
            MdcKey.MELDING_ID to meldingId.toString(),
        ) {
            påminnelse(kontekstbasertPubliserer, hendelseId, contextId)?.fortsett(this)
                ?: loggInfo("mottok påminnelse som ikke kan brukes fordi kommandoen ikke lengre er suspendert, eller fordi hendelsen er ukjent")
        }
    }

    fun håndter(varseldefinisjon: Varseldefinisjon) {
        val varseldefinisjonDto = varseldefinisjon.toDto()
        legacyVarselRepository.lagreDefinisjon(varseldefinisjonDto)
        if (varseldefinisjonDto.avviklet) {
            legacyVarselRepository.avvikleVarsel(varseldefinisjonDto)
        }
    }

    fun mottaDokument(
        fødselsnummer: String,
        dokumentId: UUID,
        dokument: JsonNode,
    ) {
        dokumentDao.lagre(fødselsnummer, dokumentId, dokument)
    }

    fun finnBehandlingerFor(fødselsnummer: String): List<LegacyVedtaksperiode.BehandlingData> {
        var behandlinger: List<LegacyVedtaksperiode.BehandlingData> = emptyList()
        sessionFactory.transactionalSessionScope { sessionContext ->
            sessionContext.legacyPersonRepository.brukPersonHvisFinnes(fødselsnummer) {
                behandlinger = behandlinger()
            }
        }
        return behandlinger
    }

    fun nullstillTilstand() {
        løsninger.set(null)
        meldingenHarBlittBehandletAvEnRiver.set(false)
    }

    private fun løsninger(
        kontekstbasertPubliserer: MeldingPubliserer,
        meldingId: UUID,
        contextId: UUID,
    ): Løsninger? {
        return løsninger.get() ?: run {
            val commandContext =
                commandContextDao.finnSuspendert(contextId) ?: run {
                    loggInfo("Ignorerer melding fordi kommandokonteksten ikke er suspendert")
                    return null
                }
            val melding = finnMelding(meldingId) ?: return null
            Løsninger(kontekstbasertPubliserer, melding, contextId, commandContext).also { løsninger.set(it) }
        }
    }

    private fun påminnelse(
        kontekstbasertPubliserer: MeldingPubliserer,
        meldingId: UUID,
        contextId: UUID,
    ): Påminnelse? {
        val commandContext =
            commandContextDao.finnSuspendertEllerFeil(contextId) ?: run {
                loggInfo("Ignorerer melding fordi kommandokonteksten ikke er suspendert eller feil")
                return null
            }
        val melding = finnMelding(meldingId) ?: return null

        return Påminnelse(kontekstbasertPubliserer, melding, commandContext)
    }

    private fun finnMelding(meldingId: UUID): Personmelding? =
        meldingDao.finn(meldingId) ?: run {
            loggInfo("Ignorerer melding fordi opprinnelig melding ikke finnes i databasen")
            return null
        }

    // fortsetter en command (resume) med oppsamlet løsninger
    fun fortsett(message: String) {
        val jsonNode = objectMapper.readTree(message)
        løsninger.get()?.fortsett(this, jsonNode)
        if (meldingenHarBlittBehandletAvEnRiver.get()) {
            jsonNode["@id"]?.asUUID()?.let { id ->
                val type =
                    when (val eventName = jsonNode["@event_name"]?.asText()) {
                        null -> "ukjent"
                        "behov" -> "behov: " + jsonNode["@behov"].joinToString { it.asText() }
                        else -> eventName
                    }
                loggInfo("Markerer melding id=$id, type=$type som behandlet i duplikatkontroll")
                meldingDuplikatkontrollDao.lagre(id, type)
            }
        }
    }

    fun errorHandler(
        err: Exception,
        message: String,
    ) {
        val meldingId = runCatching { objectMapper.readTree(message)["@id"]?.asUUID() }.getOrNull()
        medMdc(meldingId?.let { MdcKey.MELDING_ID to it.toString() }) {
            loggThrowable(
                "alvorlig feil: ${err.message}",
                "json:\n$message",
                err,
            )
        }
    }

    fun mottaMelding(
        melding: Personmelding,
        kontekstbasertPubliserer: MeldingPubliserer,
    ) {
        val meldingnavn = requireNotNull(melding::class.simpleName)
        medMdc(
            MdcKey.MELDING_ID to melding.id.toString(),
            MdcKey.MELDINGNAVN to meldingnavn,
            (melding as? Vedtaksperiodemelding)?.let { MdcKey.VEDTAKSPERIODE_ID to it.vedtaksperiodeId().toString() },
        ) {
            loggInfo("Melding $meldingnavn mottatt", "json: \n${melding.toJson()}")
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
        medMdc(
            MdcKey.MELDING_ID to melding.id.toString(),
            MdcKey.MELDINGNAVN to meldingnavn,
        ) {
            loggInfo("Melding $meldingnavn gjenopptatt", "json: \n${melding.toJson()}")
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
        commandContext: (CommandContextDao) -> CommandContext,
    ) {
        val meldingnavn = requireNotNull(melding::class.simpleName)
        val utgåendeMeldingerMediator = UtgåendeMeldingerMediator()
        try {
            sessionFactory.transactionalSessionScope { sessionContext ->
                sessionContext.legacyPersonRepository.brukPersonHvisFinnes(melding.fødselsnummer()) {
                    loggInfo("Personen finnes i databasen, behandler melding $meldingnavn", "fødselsnummer: ${melding.fødselsnummer()}")
                    val kommandostarter =
                        kommandofabrikk.lagKommandostarter(
                            setOf(utgåendeMeldingerMediator),
                            commandContext(sessionContext.commandContextDao),
                            sessionContext,
                        )
                    melding.behandle(this, kommandostarter, sessionContext)
                }
            }
            utgåendeMeldingerMediator.publiserOppsamledeMeldinger(melding, kontekstbasertPubliserer)
        } catch (e: Exception) {
            throw RuntimeException("Feil ved behandling av melding $meldingnavn", e)
        } finally {
            loggInfo("Melding $meldingnavn lest")
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
            loggInfo("fortsetter utførelse av kommandokjede for ${melding::class.simpleName} som følge av løsninger på $behov", "løsning-json: \n$jsonNode")
            mediator.gjenopptaMelding(melding, commandContext, kontekstbasertPubliserer)
        }
    }

    private class Påminnelse(
        private val kontekstbasertPubliserer: MeldingPubliserer,
        private val melding: Personmelding,
        private val commandContext: CommandContext,
    ) {
        fun fortsett(mediator: MeldingMediator) {
            loggInfo("fortsetter utførelse av kommandokjede for ${melding::class.simpleName} som følge av påminnelse", "melding-json: \n${melding.toJson()}")
            mediator.gjenopptaMelding(melding, commandContext, kontekstbasertPubliserer)
        }
    }
}
