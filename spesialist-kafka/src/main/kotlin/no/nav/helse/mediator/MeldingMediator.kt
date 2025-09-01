package no.nav.helse.mediator

import com.fasterxml.jackson.databind.JsonNode
import com.github.benmanes.caffeine.cache.CacheLoader
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import no.nav.helse.MeldingPubliserer
import no.nav.helse.db.AnnulleringDao
import no.nav.helse.db.AnnullertAvSaksbehandlerRow
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
import no.nav.helse.modell.person.SøknadSendt
import no.nav.helse.modell.person.vedtaksperiode.Vedtaksperiode
import no.nav.helse.modell.varsel.VarselRepository
import no.nav.helse.modell.varsel.Varseldefinisjon
import no.nav.helse.spesialist.kafka.objectMapper
import org.slf4j.LoggerFactory
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
    private val varselRepository: VarselRepository,
    private val poisonPillDao: PoisonPillDao,
    private val ignorerMeldingerForUkjentePersoner: Boolean,
    private val annulleringDao: AnnulleringDao,
    poisonPillTimeToLive: Duration = Duration.ofMinutes(1),
) {
    private companion object {
        private val logg = LoggerFactory.getLogger(MeldingMediator::class.java)
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    private val poisonPillsCache: LoadingCache<Unit, PoisonPills> =
        Caffeine
            .newBuilder()
            .refreshAfterWrite(poisonPillTimeToLive)
            .build(CacheLoader { _ -> poisonPillDao.poisonPills() })

    fun skalBehandleMelding(melding: String): Boolean {
        val jsonNode = objectMapper.readTree(melding)
        if (poisonPillsCache.get(Unit).erPoisonPill(jsonNode)) {
            logg.info("Hopper over melding med @id={}", jsonNode["@id"].asText())
            sikkerlogg.info("Hopper over melding med @id={}, json=\n{}", jsonNode["@id"].asText(), jsonNode)

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
            sikkerlogg.warn(
                "Ignorerer melding med event_name: {}, person med fødselsnummer {} fins ikke i databasen",
                eventName,
                fødselsnummer,
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

    fun finnBehandlingerFor(fødselsnummer: String): List<Vedtaksperiode.BehandlingData> {
        var behandlinger: List<Vedtaksperiode.BehandlingData> = emptyList()
        sessionFactory.transactionalSessionScope { sessionContext ->
            sessionContext.personRepository.brukPersonHvisFinnes(fødselsnummer) {
                behandlinger = behandlinger()
            }
        }
        return behandlinger
    }

    fun slettGamleDokumenter(): Int = dokumentDao.slettGamleDokumenter()

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
                    logg.info("Ignorerer melding fordi kommandokonteksten ikke er suspendert")
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
        løsninger.get()?.fortsett(this, jsonNode)
        if (meldingenHarBlittBehandletAvEnRiver.get()) {
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
            sessionFactory.transactionalSessionScope { sessionContext ->
                kommandofabrikk.iverksettSøknadSendt(melding, utgåendeMeldingerMediator, sessionContext)
            }
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
            buildMap {
                put("meldingId", melding.id.toString())
                put("meldingnavn", meldingnavn)
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
        commandContext: (CommandContextDao) -> CommandContext,
    ) {
        val meldingnavn = requireNotNull(melding::class.simpleName)
        val utgåendeMeldingerMediator = UtgåendeMeldingerMediator()
        try {
            sessionFactory.transactionalSessionScope { sessionContext ->
                sessionContext.personRepository.brukPersonHvisFinnes(melding.fødselsnummer()) {
                    logg.info("Personen finnes i databasen, behandler melding $meldingnavn")
                    sikkerlogg.info("Personen finnes i databasen, behandler melding $meldingnavn")
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
            "Fortsetter utførelse av kommandokontekst som følge av påminnelse".let {
                logg.info(it)
                sikkerlogg.info("$it:\n{}", melding.toJson())
            }
            mediator.gjenopptaMelding(melding, commandContext, kontekstbasertPubliserer)
        }
    }

    fun oppdaterAnnulleringerMedManglendeVedtaksperiodeId(id: String) =
        withMDC(
            mapOf("meldingId" to id),
        ) {
            logg.info("Setter i gang med oppdatering av annulleringer som mangler vedtaksperiodeId")

            val annulleringer: List<AnnullertAvSaksbehandlerRow> =
                annulleringDao
                    .find10Annulleringer()
                    .also { logg.info("Hentet ${it.size} annulleringer ${it.map { annullering -> annullering.id }}") }
            val annulleringerOppdatert =
                annulleringer.mapNotNull { annullering ->
                    annulleringDao
                        .findUtbetalingId(
                            arbeidsgiverFagsystemId = annullering.arbeidsgiver_fagsystem_id,
                            personFagsystemId = annullering.person_fagsystem_id,
                        ).also { utbetalingId ->
                            if (utbetalingId != null) {
                                logg.info("Fant utbetalingid $utbetalingId for annullering ${annullering.id}")
                            } else {
                                logg.warn("Fant ingen utbetalingid for annullering ${annullering.id}")
                            }
                        }?.let { utbetalingId ->
                            annulleringDao
                                .finnBehandlingISykefraværstilfelle(utbetalingId)
                                .also { behandling ->
                                    if (behandling != null) {
                                        logg.info("Fant behandling ${behandling.behandlingId} for utbetalingid $utbetalingId")
                                    } else {
                                        logg.warn("Fant ingen behandling for utbetalingid $utbetalingId")
                                    }
                                }
                        }?.let { behandling ->
                            annulleringDao
                                .finnFørsteVedtaksperiodeIdForEttSykefraværstilfelle(behandling)
                                .also { vedtaksperiodeId ->
                                    if (vedtaksperiodeId != null) {
                                        logg.info("Fant vedtaksperiodeid $vedtaksperiodeId for behandling ${behandling.behandlingId}")
                                    } else {
                                        logg.warn("Fant ingen vedtaksperiodeid for behandling ${behandling.behandlingId}")
                                    }
                                }
                        }?.let { førsteVedtaksperiodeId ->
                            logg.info("Oppdaterer annullering ${annullering.id} med vedtaksperiodeid $førsteVedtaksperiodeId")
                            annulleringDao.oppdaterAnnulleringMedVedtaksperiodeId(
                                annullering.id,
                                førsteVedtaksperiodeId,
                            )
                        }
                }
            logg.info("Hentet ${annulleringer.size} annulleringer og oppdaterte ${annulleringerOppdatert.size} annulleringer med vedtaksperiodeId")
        }
}
