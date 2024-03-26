package no.nav.helse.mediator

import SøknadSendtArbeidsledigRiver
import java.util.UUID
import javax.sql.DataSource
import no.nav.helse.MetrikkRiver
import no.nav.helse.db.AvviksvurderingDao
import no.nav.helse.mediator.meldinger.AdressebeskyttelseEndret
import no.nav.helse.mediator.meldinger.AdressebeskyttelseEndretCommand
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
import no.nav.helse.mediator.meldinger.PersonmeldingOld
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
import no.nav.helse.mediator.meldinger.hendelser.AvsluttetMedVedtakMessage
import no.nav.helse.mediator.meldinger.hendelser.AvsluttetUtenVedtakMessage
import no.nav.helse.mediator.meldinger.løsninger.ArbeidsforholdRiver
import no.nav.helse.mediator.meldinger.løsninger.ArbeidsgiverRiver
import no.nav.helse.mediator.meldinger.løsninger.DokumentRiver
import no.nav.helse.mediator.meldinger.løsninger.EgenAnsattløsning
import no.nav.helse.mediator.meldinger.løsninger.FlerePersoninfoRiver
import no.nav.helse.mediator.meldinger.løsninger.HentEnhetRiver
import no.nav.helse.mediator.meldinger.løsninger.InfotrygdutbetalingerRiver
import no.nav.helse.mediator.meldinger.løsninger.Inntektløsning
import no.nav.helse.mediator.meldinger.løsninger.PersoninfoRiver
import no.nav.helse.mediator.meldinger.løsninger.Risikovurderingløsning
import no.nav.helse.mediator.meldinger.løsninger.SaksbehandlerløsningRiver
import no.nav.helse.mediator.meldinger.løsninger.Vergemålløsning
import no.nav.helse.mediator.meldinger.løsninger.ÅpneGosysOppgaverløsning
import no.nav.helse.mediator.meldinger.påminnelser.KommandokjedePåminnelseRiver
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.MeldingDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.avviksvurdering.AvviksvurderingDto
import no.nav.helse.modell.dokument.DokumentDao
import no.nav.helse.modell.gosysoppgaver.GosysOppgaveEndret
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.TilbakedateringBehandlet
import no.nav.helse.modell.person.AdressebeskyttelseEndretRiver
import no.nav.helse.modell.person.EndretEgenAnsattStatus
import no.nav.helse.modell.person.OppdaterPersonsnapshot
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.person.PersonRepository
import no.nav.helse.modell.person.SøknadSendt
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.varsel.ActualVarselRepository
import no.nav.helse.modell.varsel.Varseldefinisjon
import no.nav.helse.modell.vedtaksperiode.GenerasjonDao
import no.nav.helse.modell.vedtaksperiode.GenerasjonRepository
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import no.nav.helse.modell.vedtaksperiode.vedtak.Saksbehandlerløsning
import no.nav.helse.modell.vedtaksperiode.vedtak.VedtakFattet
import no.nav.helse.objectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.registrerTidsbrukForGodkjenningsbehov
import no.nav.helse.registrerTidsbrukForHendelse
import no.nav.helse.spesialist.api.Personhåndterer
import org.slf4j.LoggerFactory

internal class MeldingMediator(
    private val dataSource: DataSource,
    private val rapidsConnection: RapidsConnection,
    private val oppgaveDao: OppgaveDao = OppgaveDao(dataSource),
    private val vedtakDao: VedtakDao = VedtakDao(dataSource),
    private val personDao: PersonDao = PersonDao(dataSource),
    private val commandContextDao: CommandContextDao = CommandContextDao(dataSource),
    private val meldingDao: MeldingDao = MeldingDao(dataSource),
    private val godkjenningMediator: GodkjenningMediator,
    private val kommandofabrikk: Kommandofabrikk,
    private val dokumentDao: DokumentDao = DokumentDao(dataSource),
    private val avviksvurderingDao: AvviksvurderingDao,
    private val utbetalingDao: UtbetalingDao = UtbetalingDao(dataSource),
    private val varselRepository: ActualVarselRepository = ActualVarselRepository(dataSource),
    private val generasjonRepository: GenerasjonRepository = GenerasjonRepository(dataSource),
    private val metrikkDao: MetrikkDao = MetrikkDao(dataSource),
    private val generasjonDao: GenerasjonDao,
) : Personhåndterer {
    private companion object {
        private val logg = LoggerFactory.getLogger(MeldingMediator::class.java)
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    private val personRepository = PersonRepository(dataSource)

    private fun skalBehandleMelding(melding: String): Boolean {
        if (erProd()) return true
        val jsonNode = objectMapper.readTree(melding)
        if (jsonNode["@event_name"]?.asText() in setOf("sendt_søknad_arbeidsgiver", "sendt_søknad_nav")) return true
        val fødselsnummer = jsonNode["fødselsnummer"]?.asText() ?: return true
        if (fødselsnummer.toDoubleOrNull() == null) return true
        return personDao.findPersonByFødselsnummer(fødselsnummer) != null
    }

    init {
        DelegatedRapid(rapidsConnection, ::forbered, ::skalBehandleMelding, ::fortsett, ::errorHandler).also {
            GodkjenningsbehovRiver(it, this)
            SøknadSendtRiver(it, this)
            SøknadSendtArbeidsledigRiver(it, this)
            PersoninfoRiver(it, this)
            FlerePersoninfoRiver(it, this)
            HentEnhetRiver(it, this)
            InfotrygdutbetalingerRiver(it, this)
            SaksbehandlerløsningRiver(it, this)
            ArbeidsgiverRiver(it, this)
            ArbeidsforholdRiver(it, this)
            VedtaksperiodeForkastetRiver(it, this)
            VedtaksperiodeEndretRiver(it, this)
            AdressebeskyttelseEndretRiver(it, this)
            OverstyringIgangsattRiver(it, this)
            EgenAnsattløsning.EgenAnsattRiver(it, this)
            Vergemålløsning.VergemålRiver(it, this)
            ÅpneGosysOppgaverløsning.ÅpneGosysOppgaverRiver(it, this)
            Risikovurderingløsning.V2River(it, this)
            Inntektløsning.InntektRiver(it, this)
            UtbetalingAnnullertRiver(it, this)
            OppdaterPersonsnapshotRiver(it, this)
            UtbetalingEndretRiver(it, this)
            VedtaksperiodeReberegnetRiver(it, this)
            GosysOppgaveEndretRiver(it, this)
            TilbakedateringBehandletRiver(it, this)
            EndretSkjermetinfoRiver(it, this)
            DokumentRiver(it, dokumentDao)
            VedtakFattetRiver(it, this)
            NyeVarslerRiver(it, this)
            AvvikVurdertRiver(it, this)
            VarseldefinisjonRiver(it, this)
            VedtaksperiodeNyUtbetalingRiver(it, this)
            MetrikkRiver(it)
            AvsluttetMedVedtakRiver(it, this, avviksvurderingDao, generasjonDao)
            AvsluttetUtenVedtakRiver(it, this)
            MidnattRiver(it, this)
            BehandlingOpprettetRiver(it, this)
            KommandokjedePåminnelseRiver(it, this)
        }
    }

    private var løsninger: Løsninger? = null

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
                "opprinneligMeldingId" to "$hendelseId"
            )
        ) {
            løsninger(context, hendelseId, contextId)?.also { it.add(hendelseId, contextId, løsning) }
                ?: logg.info("mottok løsning som ikke kunne brukes fordi kommandoen ikke lengre er suspendert, eller fordi hendelsen er ukjent")
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
                "meldingId" to "$meldingId"
            )
        ) {
            påminnelse(context, hendelseId, contextId)?.also {
                it.add(hendelseId, contextId, påminnelse)
                it.fortsett(this)
            }
                ?: logg.info("mottok påminnelse som ikke kunne brukes fordi kommandoen ikke lengre er suspendert, eller fordi hendelsen er ukjent")
        }
    }

    internal fun håndter(varseldefinisjon: Varseldefinisjon) {
        val varseldefinisjonDto = varseldefinisjon.toDto()
        varselRepository.lagreDefinisjon(varseldefinisjonDto)
        if (varseldefinisjonDto.avviklet) {
            varselRepository.avvikleVarsel(varseldefinisjonDto)
        }
    }

    internal fun håndter(avsluttetMedVedtakMessage: AvsluttetMedVedtakMessage) {
        val fødselsnummer = avsluttetMedVedtakMessage.fødselsnummer()
        val skjæringstidspunkt = avsluttetMedVedtakMessage.skjæringstidspunkt()
        val sykefraværstilfelle = kommandofabrikk.sykefraværstilfelle(fødselsnummer, skjæringstidspunkt)
        val vedtakFattetMelder = VedtakFattetMelder(rapidsConnection)
        sykefraværstilfelle.registrer(vedtakFattetMelder)
        avsluttetMedVedtakMessage.sendInnTil(sykefraværstilfelle)
    }

    internal fun håndter(avsluttetUtenVedtakMessage: AvsluttetUtenVedtakMessage) {
        generasjonRepository.brukGenerasjon(avsluttetUtenVedtakMessage.vedtaksperiodeId()) { generasjon ->
            val vedtakFattetMelder = VedtakFattetMelder(rapidsConnection)
            generasjon.registrer(vedtakFattetMelder)
            avsluttetUtenVedtakMessage.sendInnTil(generasjon)
        }
    }

    internal fun håndter(avviksvurdering: AvviksvurderingDto) {
        kommandofabrikk.avviksvurdering(avviksvurdering)
    }

    fun godkjenningsbehov(
        godkjenningsbehov: Godkjenningsbehov,
        context: MessageContext,
        avviksvurderingId: UUID?,
        vilkårsgrunnlagId: UUID,
    ) {
        if (avviksvurderingId != null)
            avviksvurderingDao.opprettKobling(avviksvurderingId, vilkårsgrunnlagId)

        personRepository.brukPersonHvisFinnes(godkjenningsbehov.fødselsnummer()) {
            mottaSpleisVedtaksperioder(godkjenningsbehov.spleisVedtaksperioder)
        }

        generasjonRepository.brukVedtaksperiode(godkjenningsbehov.fødselsnummer(), godkjenningsbehov.vedtaksperiodeId()) { vedtaksperiode ->
            vedtaksperiode.mottaBehandlingsinformasjon(godkjenningsbehov.tags, godkjenningsbehov.spleisBehandlingId)
        }

        val utbetalingId = godkjenningsbehov.utbetalingId
        val vedtaksperiodeId = godkjenningsbehov.vedtaksperiodeId()
        val id = godkjenningsbehov.id
        if (utbetalingDao.erUtbetalingForkastet(utbetalingId)) {
            sikkerlogg.info("Ignorerer godkjenningsbehov med id=$id for utbetalingId=$utbetalingId fordi utbetalingen er forkastet")
            return
        }
        if (oppgaveDao.harGyldigOppgave(utbetalingId) || vedtakDao.erAutomatiskGodkjent(utbetalingId)) {
            sikkerlogg.info("vedtaksperiodeId=$vedtaksperiodeId med utbetalingId=$utbetalingId har gyldig oppgave eller er automatisk godkjent. Ignorerer godkjenningsbehov med id=$id")
            return
        }
        håndter(godkjenningsbehov, context)
    }

    fun oppdaterPersonsnapshot(hendelse: OppdaterPersonsnapshot, context: MessageContext) {
        håndter(hendelse, context)
    }

    fun gosysOppgaveEndret(fødselsnummer: String, oppgaveEndret: GosysOppgaveEndret, context: MessageContext) {
        oppgaveDao.finnOppgaveId(fødselsnummer)?.also { oppgaveId ->
            sikkerlogg.info("Fant en oppgave for {}: {}", fødselsnummer, oppgaveId)
            val commandData = oppgaveDao.oppgaveDataForAutomatisering(oppgaveId)
            if (commandData == null) {
                sikkerlogg.info("Fant ikke commandData for {} og {}", fødselsnummer, oppgaveId)
                return
            }
            sikkerlogg.info("Har oppgave til_godkjenning og commandData for fnr $fødselsnummer og vedtaksperiodeId ${commandData.vedtaksperiodeId}")
            håndter(oppgaveEndret, context)
        } ?: sikkerlogg.info("Ingen åpne oppgaver i Speil for {}", fødselsnummer)
    }

    fun tilbakedateringBehandlet(fødselsnummer: String, tilbakedateringBehandlet: TilbakedateringBehandlet, context: MessageContext) {
        personRepository.brukPersonHvisFinnes(fødselsnummer) {
            behandleTilbakedateringBehandlet(tilbakedateringBehandlet.perioder)
        }
        oppgaveDao.finnOppgaveId(fødselsnummer)?.also { oppgaveId ->
            sikkerlogg.info("Fant en oppgave for {}: {}", fødselsnummer, oppgaveId)

            val oppgaveDataForAutomatisering = oppgaveDao.oppgaveDataForAutomatisering(oppgaveId)
                ?: return@also sikkerlogg.info("Fant ikke commandData for {} og {}", fødselsnummer, oppgaveId)

            if (!oppgaveDataForAutomatisering.periodeOverlapperMed(tilbakedateringBehandlet.perioder)) {
                sikkerlogg.info("Ingen av periodene i sykmeldingen er innenfor vedtaksperiodens fom og tom, for tilbakedateringen {} og {}", fødselsnummer, oppgaveId)
                return
            }

            sikkerlogg.info("Har oppgave til_godkjenning og commandData for fnr $fødselsnummer og vedtaksperiodeId ${oppgaveDataForAutomatisering.vedtaksperiodeId}")
            håndter(tilbakedateringBehandlet, context)
        } ?: sikkerlogg.info("Ingen åpne oppgaver for {} ifm. godkjent tilbakedatering", fødselsnummer)
    }

    fun slettGamleDokumenter(): Int {
        return dokumentDao.slettGamleDokumenter()
    }

    private fun forbered() {
        løsninger = null
    }

    private fun løsninger(messageContext: MessageContext, meldingId: UUID, contextId: UUID): Løsninger? {
        return løsninger ?: run {
            val commandContext = commandContextDao.finnSuspendert(contextId) ?: run {
                logg.info("Ignorerer melding fordi kommandokonteksten ikke er suspendert")
                return null
            }
            val melding = meldingDao.finn(meldingId) ?: run {
                logg.info("Ignorerer melding fordi opprinnelig melding ikke finnes i databasen")
                return null
            }
            Løsninger(messageContext, melding, contextId, commandContext).also { løsninger = it }
        }
    }

    private fun påminnelse(messageContext: MessageContext, meldingId: UUID, contextId: UUID): Påminnelse? {
        val commandContext = commandContextDao.finnSuspendertEllerFeil(contextId) ?: run {
            logg.info("Ignorerer melding fordi kommandokonteksten ikke er suspendert eller feil")
            return null
        }
        val hendelse = meldingDao.finn(meldingId) ?: run {
            logg.info("Ignorerer melding fordi opprinnelig melding ikke finnes i databasen")
            return null
        }

        return Påminnelse(messageContext, hendelse, contextId, commandContext)
    }

    // fortsetter en command (resume) med oppsamlet løsninger
    private fun fortsett(message: String) {
        løsninger?.fortsett(this, message)
    }

    private fun errorHandler(err: Exception, message: String) {
        logg.error("alvorlig feil: ${err.message} (se sikkerlogg for melding)", err)
        sikkerlogg.error("alvorlig feil: ${err.message}\n\t$message", err, err.printStackTrace())
    }

    private fun nyContext(hendelse: PersonmeldingOld, contextId: UUID) = CommandContext(contextId).apply {
        meldingDao.lagre(hendelse)
        opprett(commandContextDao, hendelse.id)
    }

    internal fun mottaMelding(melding: Personmelding, messageContext: MessageContext) {
        val meldingnavn = requireNotNull(melding::class.simpleName)
        withMDC(
            mapOf(
                "meldingId" to melding.id.toString(),
                "meldingnavn" to meldingnavn
            )
        ) {
            logg.info("Melding $meldingnavn mottatt")
            sikkerlogg.info("Melding $meldingnavn mottatt:\n${melding.toJson()}")
            meldingDao.lagre(melding)
            behandleMelding(melding, messageContext)

            logg.info("Melding $meldingnavn lest")
            sikkerlogg.info("Melding $meldingnavn lest")
        }
    }

    private fun gjenopptaMelding(melding: Personmelding, commandContext: CommandContext, messageContext: MessageContext){
        val meldingnavn = requireNotNull(melding::class.simpleName)
        withMDC(
            mapOf(
                "meldingId" to melding.id.toString(),
                "meldingnavn" to meldingnavn
            )
        ) {
            logg.info("Melding $meldingnavn gjenopptatt")
            sikkerlogg.info("Melding $meldingnavn gjenopptatt:\n${melding.toJson()}")
            kommandofabrikk.settEksisterendeContext(commandContext)
            behandleMelding(melding, messageContext)
            kommandofabrikk.nullstilleEksisterendeContext()

            logg.info("Melding $meldingnavn lest")
            sikkerlogg.info("Melding $meldingnavn lest")
        }
    }

    private fun behandleMelding(melding: Personmelding, messageContext: MessageContext){
        val meldingnavn = requireNotNull(melding::class.simpleName)
        val utgåendeMeldingerMediator = UtgåendeMeldingerMediator()
        val commandContextTilstandMediator = CommandContextTilstandMediator()
        try {
            kommandofabrikk.nyObserver(utgåendeMeldingerMediator, commandContextTilstandMediator)
            personRepository.brukPersonHvisFinnes(melding.fødselsnummer()) {
                logg.info("Personen finnes i databasen, behandler melding $meldingnavn")
                sikkerlogg.info("Personen finnes i databasen, behandler melding $meldingnavn")

                melding.behandle(this, kommandofabrikk)
            }
            if (melding is VedtakFattet) melding.doFinally(vedtakDao) // Midlertidig frem til spesialsak ikke er en ting lenger
            utgåendeMeldingerMediator.publiserOppsamledeMeldinger(melding, messageContext)
        } catch (e: Exception) {
            logg.error("Feil ved behandling av melding $meldingnavn", e.message, e)
            throw e
        } finally {
            kommandofabrikk.avregistrerObserver(utgåendeMeldingerMediator, commandContextTilstandMediator)
            commandContextTilstandMediator.publiserTilstandsendringer(melding, messageContext)
        }
    }

    internal fun håndter(fødselsnummer: String, melding: PersonmeldingOld, messageContext: MessageContext) {
        withMDC(mapOf("meldingId" to melding.id.toString())) {
            if (personDao.findPersonByFødselsnummer(fødselsnummer) == null) {
                logg.info("Ignorerer melding ${melding::class.simpleName} fordi personen ikke finnes i databasen")
                return@withMDC
            }
            håndter(melding, messageContext)
        }
    }

    internal fun håndter(melding: PersonmeldingOld, messageContext: MessageContext) {
        val contextId = UUID.randomUUID()
        withMDC(
            mapOf(
                "meldingId" to melding.id.toString(),
                "contextId" to contextId.toString()
            )
        ) {
            logg.info("Behandler melding ${melding::class.simpleName}")
            logg.info("Oppretter ny kommandokontekst som følge av ${melding::class.simpleName}")
            håndter(melding, nyContext(melding, contextId), messageContext)
        }
    }

    private fun håndter(melding: PersonmeldingOld, commandContext: CommandContext, messageContext: MessageContext) {
        val utgåendeMeldingerMediator = UtgåendeMeldingerMediator()
        val commandContextTilstandMediator = CommandContextTilstandMediator()
        commandContext.nyObserver(utgåendeMeldingerMediator)
        commandContext.nyObserver(commandContextTilstandMediator)
        val hendelsenavn = melding::class.simpleName ?: "ukjent hendelse"
        try {
            when (melding) {
                is AdressebeskyttelseEndret -> iverksett(AdressebeskyttelseEndretCommand(melding.fødselsnummer(), personDao, oppgaveDao, godkjenningMediator), melding.id, commandContext)
                is EndretEgenAnsattStatus -> iverksett(kommandofabrikk.endretEgenAnsattStatus(melding.fødselsnummer(), melding), melding.id, commandContext)
                is GosysOppgaveEndret -> iverksett(kommandofabrikk.gosysOppgaveEndret(melding.fødselsnummer(), melding), melding.id, commandContext)
                is TilbakedateringBehandlet -> iverksett(kommandofabrikk.tilbakedateringGodkjent(melding.fødselsnummer()), melding.id, commandContext)
                is SøknadSendt -> iverksett(kommandofabrikk.søknadSendt(melding), melding.id, commandContext)
                is OppdaterPersonsnapshot -> iverksett(kommandofabrikk.oppdaterPersonsnapshot(melding), melding.id, commandContext)
                is Godkjenningsbehov -> iverksett(kommandofabrikk.godkjenningsbehov(melding), melding.id, commandContext)
                is Saksbehandlerløsning -> iverksett(kommandofabrikk.utbetalingsgodkjenning(melding), melding.id, commandContext)
                else -> throw IllegalArgumentException("Personhendelse må håndteres")
            }
            utgåendeMeldingerMediator.publiserOppsamledeMeldinger(melding, messageContext)
        } catch (e: Exception) {
            logg.error("Feil ved behandling av melding $hendelsenavn", e.message, e)
            throw e
        } finally {
            commandContextTilstandMediator.publiserTilstandsendringer(melding, messageContext)
            logg.info("Melding $hendelsenavn ferdigbehandlet")
        }
    }

    private fun iverksett(command: Command, hendelseId: UUID, commandContext: CommandContext) {
        val contextId = commandContext.id()
        try {
            if (commandContext.utfør(commandContextDao, hendelseId, command)) {
                val kjøretid = commandContextDao.tidsbrukForContext(contextId)
                metrikker(command.name, kjøretid, contextId)
                logg.info("Kommando(er) for ${command.name} er utført ferdig. Det tok ca {}ms å kjøre hele kommandokjeden", kjøretid)
            } else logg.info("${command.name} er suspendert")
        } catch (err: Exception) {
            command.undo(commandContext)
            throw err
        }
    }

    private fun metrikker(hendelsenavn: String, kjøretidMs: Int, contextId: UUID) {
        if (hendelsenavn == Godkjenningsbehov::class.simpleName) {
            val utfall: GodkjenningsbehovUtfall = metrikkDao.finnUtfallForGodkjenningsbehov(contextId)
            registrerTidsbrukForGodkjenningsbehov(utfall, kjøretidMs)
        }
        registrerTidsbrukForHendelse(hendelsenavn, kjøretidMs)
    }

    private class Løsninger(
        private val messageContext: MessageContext,
        private val melding: PersonmeldingOld,
        private val contextId: UUID,
        private val commandContext: CommandContext,
    ) {
        fun add(hendelseId: UUID, contextId: UUID, løsning: Any) {
            check(hendelseId == melding.id)
            check(contextId == this.contextId)
            commandContext.add(løsning)
        }

        fun fortsett(mediator: MeldingMediator, message: String) {
            logg.info("fortsetter utførelse av kommandokontekst som følge av løsninger på behov for ${melding::class.simpleName}")
            sikkerlogg.info("fortsetter utførelse av kommandokontekst som følge av løsninger på behov for ${melding::class.simpleName}\nInnkommende melding:\n\t$message")
            if (melding is Personmelding) mediator.gjenopptaMelding(melding, commandContext, messageContext)
            else mediator.håndter(melding, commandContext, messageContext)
        }
    }

    private class Påminnelse(
        private val messageContext: MessageContext,
        private val melding: PersonmeldingOld,
        private val contextId: UUID,
        private val commandContext: CommandContext,
    ) {
        fun add(hendelseId: UUID, contextId: UUID, påminnelse: Any) {
            check(hendelseId == melding.id)
            check(contextId == this.contextId)
            commandContext.add(påminnelse)
        }

        fun fortsett(mediator: MeldingMediator) {
            logg.info("fortsetter utførelse av kommandokontekst som følge av påminnelse")
            if (melding is Personmelding) mediator.gjenopptaMelding(melding, commandContext, messageContext)
            else mediator.håndter(melding, commandContext, messageContext)
        }
    }

    override fun oppdaterSnapshot(fnr: String) {
        val json = objectMapper.readTree(JsonMessage.newMessage(
            "oppdater_personsnapshot", mapOf(
                "fødselsnummer" to fnr
            )
        ).toJson())

        oppdaterPersonsnapshot(OppdaterPersonsnapshot(json), rapidsConnection)
    }
}
