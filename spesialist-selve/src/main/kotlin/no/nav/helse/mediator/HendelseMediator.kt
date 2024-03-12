package no.nav.helse.mediator

import SøknadSendtArbeidsledigRiver
import java.util.UUID
import javax.sql.DataSource
import net.logstash.logback.argument.StructuredArguments.keyValue
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
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.HendelseDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.avviksvurdering.AvviksvurderingDto
import no.nav.helse.modell.dokument.DokumentDao
import no.nav.helse.modell.gosysoppgaver.GosysOppgaveEndret
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.TilbakedateringBehandlet
import no.nav.helse.modell.overstyring.OverstyringIgangsatt
import no.nav.helse.modell.person.AdressebeskyttelseEndretRiver
import no.nav.helse.modell.person.EndretEgenAnsattStatus
import no.nav.helse.modell.person.OppdaterPersonsnapshot
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.person.PersonRepository
import no.nav.helse.modell.person.SøknadSendt
import no.nav.helse.modell.utbetaling.UtbetalingAnnullert
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.utbetaling.UtbetalingEndret
import no.nav.helse.modell.varsel.ActualVarselRepository
import no.nav.helse.modell.varsel.Varseldefinisjon
import no.nav.helse.modell.vedtaksperiode.BehandlingOpprettet
import no.nav.helse.modell.vedtaksperiode.GenerasjonDao
import no.nav.helse.modell.vedtaksperiode.GenerasjonRepository
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import no.nav.helse.modell.vedtaksperiode.NyeVarsler
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeEndret
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeForkastet
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeNyUtbetaling
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeReberegnet
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

internal class HendelseMediator(
    private val dataSource: DataSource,
    private val rapidsConnection: RapidsConnection,
    private val oppgaveDao: OppgaveDao = OppgaveDao(dataSource),
    private val vedtakDao: VedtakDao = VedtakDao(dataSource),
    private val personDao: PersonDao = PersonDao(dataSource),
    private val commandContextDao: CommandContextDao = CommandContextDao(dataSource),
    private val arbeidsgiverDao: ArbeidsgiverDao = ArbeidsgiverDao(dataSource),
    private val hendelseDao: HendelseDao = HendelseDao(dataSource),
    private val feilendeMeldingerDao: FeilendeMeldingerDao = FeilendeMeldingerDao(dataSource),
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
        private val logg = LoggerFactory.getLogger(HendelseMediator::class.java)
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
        val sykefraværstilfelleMediator = SykefraværstilfelleMediator(rapidsConnection)
        sykefraværstilfelle.registrer(sykefraværstilfelleMediator)
        avsluttetMedVedtakMessage.sendInnTil(sykefraværstilfelle)
    }

    internal fun håndter(avsluttetUtenVedtakMessage: AvsluttetUtenVedtakMessage) {
        generasjonRepository.brukGenerasjon(avsluttetUtenVedtakMessage.vedtaksperiodeId()) { generasjon ->
            val sykefraværstilfelleMediator = SykefraværstilfelleMediator(rapidsConnection)
            generasjon.registrer(sykefraværstilfelleMediator)
            avsluttetUtenVedtakMessage.sendInnTil(generasjon)
        }
    }

    internal fun håndter(nyeVarsler: NyeVarsler) {
        nyeVarsler.varsler.forEach { varsel ->
            generasjonRepository.brukGenerasjonHvisFinnes(varsel.vedtaksperiodeId()) {
                it.håndterNyttVarsel(varsel, nyeVarsler.id)
            }
        }
    }

    internal fun håndter(vedtakFattet: VedtakFattet) {
        val vedtaksperiodeId = vedtakFattet.vedtaksperiodeId()
        generasjonRepository.brukVedtaksperiode(vedtakFattet.fødselsnummer(), vedtaksperiodeId) {
            it.vedtakFattet(vedtakFattet.id)
        }
        if (vedtakDao.erSpesialsak(vedtaksperiodeId)) vedtakDao.spesialsakFerdigbehandlet(vedtaksperiodeId)
    }

    internal fun håndter(melding: BehandlingOpprettet) {
        val meldingnavn = BehandlingOpprettet::class.simpleName
        sikkerlogg.info("Mottatt melding $meldingnavn")
        personRepository.brukPersonHvisFinnes(melding.fødselsnummer()) {
            sikkerlogg.info("Person finnes, behandler melding $meldingnavn")
            melding.behandleAv(this)
        }
    }

    internal fun håndter(avviksvurdering: AvviksvurderingDto) {
        kommandofabrikk.avviksvurdering(avviksvurdering)
    }

    fun vedtaksperiodeForkastet(
        hendelse: VedtaksperiodeForkastet,
        context: MessageContext,
    ) {
        val vedtaksperiodeId = hendelse.vedtaksperiodeId()
        if (vedtakDao.finnVedtakId(vedtaksperiodeId) == null) {
            logg.info("ignorerer hendelseId=${hendelse.id} fordi vi ikke kjenner til $vedtaksperiodeId")
            return
        }
        return håndter(hendelse, context)
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
        val skjæringstidspunkt = godkjenningsbehov.skjæringstidspunkt
        val id = godkjenningsbehov.id
        if (utbetalingDao.erUtbetalingForkastet(utbetalingId)) {
            sikkerlogg.info("Ignorerer godkjenningsbehov med id=$id for utbetalingId=$utbetalingId fordi utbetalingen er forkastet")
            return
        }
        if (oppgaveDao.harGyldigOppgave(utbetalingId) || vedtakDao.erAutomatiskGodkjent(utbetalingId)) {
            sikkerlogg.info("vedtaksperiodeId=$vedtaksperiodeId med utbetalingId=$utbetalingId har gyldig oppgave eller er automatisk godkjent. Ignorerer godkjenningsbehov med id=$id")
            return
        }
        if (generasjonRepository.finnVedtaksperiodeIderFor(godkjenningsbehov.fødselsnummer(), skjæringstidspunkt).isEmpty()) {
            sikkerlogg.error("""
                vedtaksperiodeId=$vedtaksperiodeId med utbetalingId=$utbetalingId, periodeFom=${godkjenningsbehov.periodeFom}, periodeTom=${godkjenningsbehov.periodeTom} 
                og skjæringstidspunkt=$skjæringstidspunkt er i et sykefraværstilfelle uten generasjoner lagret. 
                Ignorerer godkjenningsbehov med id=$id""".trimIndent()
            )
            return
        }
        håndter(godkjenningsbehov, context)
    }

    fun utbetalingEndret(
        utbetalingEndret: UtbetalingEndret,
        eventName: String,
        context: MessageContext,
    ) {
        val organisasjonsnummer = utbetalingEndret.organisasjonsnummer
        val id = utbetalingEndret.id
        if (arbeidsgiverDao.findArbeidsgiverByOrgnummer(organisasjonsnummer) == null) {
            logg.warn(
                "Fant ikke arbeidsgiver med {}, se sikkerlogg for mer informasjon",
                keyValue("hendelseId", id)
            )
            sikkerlogg.warn(
                "Forstår ikke utbetaling_endret: fant ikke arbeidsgiver med {}, {}, {}, {}. Meldingen er lagret i feilende_meldinger",
                keyValue("hendelseId", id),
                keyValue("fødselsnummer", utbetalingEndret.fødselsnummer()),
                keyValue("organisasjonsnummer", organisasjonsnummer),
                keyValue("utbetalingId", utbetalingEndret.utbetalingId)
            )
            feilendeMeldingerDao.lagre(id, eventName, utbetalingEndret.toJson())
            return
        }
        håndter(utbetalingEndret.fødselsnummer(), utbetalingEndret, context)
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
        val syketilfelleStartDato = tilbakedateringBehandlet.syketilfelleStartdato
        personRepository.brukPersonHvisFinnes(fødselsnummer) {
            behandleTilbakedateringBehandlet(tilbakedateringBehandlet.perioder)
        }
        oppgaveDao.finnOppgaveId(fødselsnummer)?.also { oppgaveId ->
            sikkerlogg.info("Fant en oppgave for {}: {}", fødselsnummer, oppgaveId)

            val oppgaveDataForAutomatisering = oppgaveDao.oppgaveDataForAutomatisering(oppgaveId)
                ?: return@also sikkerlogg.info("Fant ikke commandData for {} og {}", fødselsnummer, oppgaveId)

            if (!oppgaveDataForAutomatisering.periodeOverlapperMed(syketilfelleStartDato)) {
                sikkerlogg.info("SyketilfellestartDato er ikke innenfor periodens fom og tom, for tilbakedateringen {} og {}", fødselsnummer, oppgaveId)
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

    private fun løsninger(messageContext: MessageContext, hendelseId: UUID, contextId: UUID): Løsninger? {
        return løsninger ?: run {
            val commandContext = commandContextDao.finnSuspendert(contextId) ?: run {
                logg.info("Ignorerer melding fordi kommandokonteksten ikke er suspendert")
                return null
            }
            val hendelse = hendelseDao.finn(hendelseId) ?: run {
                logg.info("Ignorerer melding fordi opprinnelig melding ikke finnes i databasen")
                return null
            }
            Løsninger(messageContext, hendelse, contextId, commandContext).also { løsninger = it }
        }
    }

    // fortsetter en command (resume) med oppsamlet løsninger
    private fun fortsett(message: String) {
        løsninger?.fortsett(this, message)
    }

    private fun errorHandler(err: Exception, message: String) {
        logg.error("alvorlig feil: ${err.message} (se sikkerlogg for melding)", err)
        sikkerlogg.error("alvorlig feil: ${err.message}\n\t$message", err, err.printStackTrace())
    }

    private fun nyContext(hendelse: Personmelding, contextId: UUID) = CommandContext(contextId).apply {
        hendelseDao.opprett(hendelse)
        opprett(commandContextDao, hendelse.id)
    }

    internal fun håndter(fødselsnummer: String, melding: Personmelding, messageContext: MessageContext) {
        withMDC(mapOf("meldingId" to melding.id.toString())) {
            if (personDao.findPersonByFødselsnummer(fødselsnummer) == null) {
                logg.info("Ignorerer melding ${melding::class.simpleName} fordi personen ikke finnes i databasen")
                return@withMDC
            }
            håndter(melding, messageContext)
        }
    }

    internal fun håndter(melding: Personmelding, messageContext: MessageContext) {
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

    private fun håndter(melding: Personmelding, commandContext: CommandContext, messageContext: MessageContext) {
        val utgåendeMeldingerMediator = UtgåendeMeldingerMediator()
        commandContext.nyObserver(utgåendeMeldingerMediator)
        val hendelsenavn = melding::class.simpleName ?: "ukjent hendelse"
        try {
            when (melding) {
                is AdressebeskyttelseEndret -> iverksett(AdressebeskyttelseEndretCommand(melding.fødselsnummer(), personDao, oppgaveDao, godkjenningMediator), melding.id, commandContext)
                is EndretEgenAnsattStatus -> iverksett(kommandofabrikk.endretEgenAnsattStatus(melding.fødselsnummer(), melding), melding.id, commandContext)
                is GosysOppgaveEndret -> iverksett(kommandofabrikk.gosysOppgaveEndret(melding.fødselsnummer(), melding), melding.id, commandContext)
                is NyeVarsler -> håndter(melding)
                is TilbakedateringBehandlet -> iverksett(kommandofabrikk.tilbakedateringGodkjent(melding.fødselsnummer()), melding.id, commandContext)
                is VedtaksperiodeReberegnet -> iverksett(kommandofabrikk.vedtaksperiodeReberegnet(melding), melding.id, commandContext)
                is VedtaksperiodeNyUtbetaling -> iverksett(kommandofabrikk.vedtaksperiodeNyUtbetaling(melding), melding.id, commandContext)
                is SøknadSendt -> iverksett(kommandofabrikk.søknadSendt(melding), melding.id, commandContext)
                is OppdaterPersonsnapshot -> iverksett(kommandofabrikk.oppdaterPersonsnapshot(melding), melding.id, commandContext)
                is OverstyringIgangsatt -> iverksett(kommandofabrikk.kobleVedtaksperiodeTilOverstyring(melding), melding.id, commandContext)
                is UtbetalingAnnullert -> iverksett(kommandofabrikk.utbetalingAnnullert(melding), melding.id, commandContext)
                is UtbetalingEndret -> iverksett(kommandofabrikk.utbetalingEndret(melding), melding.id, commandContext)
                is VedtakFattet -> håndter(melding)
                is VedtaksperiodeEndret -> iverksett(kommandofabrikk.vedtaksperiodeEndret(melding), melding.id, commandContext)
                is VedtaksperiodeForkastet -> iverksett(kommandofabrikk.vedtaksperiodeForkastet(melding), melding.id, commandContext)
                is Godkjenningsbehov -> iverksett(kommandofabrikk.godkjenningsbehov(melding), melding.id, commandContext)
                is Saksbehandlerløsning -> iverksett(kommandofabrikk.utbetalingsgodkjenning(melding), melding.id, commandContext)
                else -> throw IllegalArgumentException("Personhendelse må håndteres")
            }
            utgåendeMeldingerMediator.håndter(melding, messageContext)
        } catch (e: Exception) {
            logg.error("Feil ved behandling av melding $hendelsenavn", e.message, e)
            throw e
        } finally {
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
        private val melding: Personmelding,
        private val contextId: UUID,
        private val commandContext: CommandContext,
    ) {
        fun add(hendelseId: UUID, contextId: UUID, løsning: Any) {
            check(hendelseId == melding.id)
            check(contextId == this.contextId)
            commandContext.add(løsning)
        }

        fun fortsett(mediator: HendelseMediator, message: String) {
            logg.info("fortsetter utførelse av kommandokontekst som følge av løsninger på behov for ${melding::class.simpleName}")
            sikkerlogg.info("fortsetter utførelse av kommandokontekst som følge av løsninger på behov for ${melding::class.simpleName}\nInnkommende melding:\n\t$message")
            mediator.håndter(melding, commandContext, messageContext)
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
