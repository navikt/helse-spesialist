package no.nav.helse.mediator

import SøknadSendtArbeidsledigRiver
import java.time.LocalDate
import java.time.LocalDateTime
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
import no.nav.helse.mediator.meldinger.EndretSkjermetinfoRiver
import no.nav.helse.mediator.meldinger.GodkjenningsbehovRiver
import no.nav.helse.mediator.meldinger.GosysOppgaveEndretRiver
import no.nav.helse.mediator.meldinger.Kommandohendelse
import no.nav.helse.mediator.meldinger.NyeVarslerRiver
import no.nav.helse.mediator.meldinger.OppdaterPersonsnapshotRiver
import no.nav.helse.mediator.meldinger.OverstyringArbeidsforholdRiver
import no.nav.helse.mediator.meldinger.OverstyringIgangsattRiver
import no.nav.helse.mediator.meldinger.OverstyringInntektOgRefusjonRiver
import no.nav.helse.mediator.meldinger.Personhendelse
import no.nav.helse.mediator.meldinger.SkjønnsfastsettingSykepengegrunnlagRiver
import no.nav.helse.mediator.meldinger.SykefraværstilfellerRiver
import no.nav.helse.mediator.meldinger.SøknadSendtRiver
import no.nav.helse.mediator.meldinger.TilbakedatertRiver
import no.nav.helse.mediator.meldinger.UtbetalingAnnullertRiver
import no.nav.helse.mediator.meldinger.UtbetalingEndretRiver
import no.nav.helse.mediator.meldinger.VarseldefinisjonRiver
import no.nav.helse.mediator.meldinger.VedtakFattetRiver
import no.nav.helse.mediator.meldinger.VedtaksperiodeEndretRiver
import no.nav.helse.mediator.meldinger.VedtaksperiodeForkastetRiver
import no.nav.helse.mediator.meldinger.VedtaksperiodeHendelse
import no.nav.helse.mediator.meldinger.VedtaksperiodeNyUtbetalingRiver
import no.nav.helse.mediator.meldinger.VedtaksperiodeOpprettetRiver
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
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.overstyring.OverstyrtArbeidsgiver
import no.nav.helse.modell.overstyring.SkjønnsfastsattArbeidsgiver
import no.nav.helse.modell.person.AdressebeskyttelseEndretRiver
import no.nav.helse.modell.person.EndretEgenAnsattStatus
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.varsel.ActualVarselRepository
import no.nav.helse.modell.varsel.Varsel
import no.nav.helse.modell.varsel.Varseldefinisjon
import no.nav.helse.modell.vedtaksperiode.ActualGenerasjonRepository
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeOppdatering
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeOpprettet
import no.nav.helse.objectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.registrerTidsbrukForGodkjenningsbehov
import no.nav.helse.registrerTidsbrukForHendelse
import no.nav.helse.spesialist.api.Personhåndterer
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrArbeidsforholdHandlingFraApi
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
    private val hendelsefabrikk: Hendelsefabrikk,
    private val egenAnsattDao: EgenAnsattDao = EgenAnsattDao(dataSource),
    private val dokumentDao: DokumentDao = DokumentDao(dataSource),
    private val avviksvurderingDao: AvviksvurderingDao,
    private val varselRepository: ActualVarselRepository = ActualVarselRepository(dataSource),
    private val generasjonRepository: ActualGenerasjonRepository = ActualGenerasjonRepository(dataSource),
    private val metrikkDao: MetrikkDao = MetrikkDao(dataSource),
) : Personhåndterer {
    private companion object {
        private val logg = LoggerFactory.getLogger(HendelseMediator::class.java)
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }

    private val behovMediator = BehovMediator()

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
            OverstyringInntektOgRefusjonRiver(it, this)
            OverstyringArbeidsforholdRiver(it, this)
            SkjønnsfastsettingSykepengegrunnlagRiver(it, this)
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
            VedtaksperiodeOpprettetRiver(it, this)
            GosysOppgaveEndretRiver(it, this, oppgaveDao, personDao)
            TilbakedatertRiver(it, this, oppgaveDao)
            EndretSkjermetinfoRiver(it, personDao, egenAnsattDao, oppgaveDao, godkjenningMediator, this)
            DokumentRiver(it, dokumentDao)
            VedtakFattetRiver(it, this)
            NyeVarslerRiver(it, this)
            AvvikVurdertRiver(it, this)
            VarseldefinisjonRiver(it, this)
            VedtaksperiodeNyUtbetalingRiver(it, this)
            SykefraværstilfellerRiver(it, this)
            MetrikkRiver(it)
            AvsluttetMedVedtakRiver(it, this, avviksvurderingDao)
            AvsluttetUtenVedtakRiver(it, this)
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
                "behovId" to "$behovId"
            )
        ) {
            løsninger(context, hendelseId, contextId)?.also { it.add(hendelseId, contextId, løsning) }
                ?: logg.info(
                    "mottok løsning med behovId=$behovId som ikke kunne brukes fordi kommandoen ikke lengre er suspendert, " +
                            "eller fordi hendelsen $hendelseId er ukjent"
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

    internal fun håndter(avsluttetMedVedtakMessage: AvsluttetMedVedtakMessage) {
        val fødselsnummer = avsluttetMedVedtakMessage.fødselsnummer()
        val skjæringstidspunkt = avsluttetMedVedtakMessage.skjæringstidspunkt()
        val sykefraværstilfelle = hendelsefabrikk.sykefraværstilfelle(fødselsnummer, skjæringstidspunkt)
        val sykefraværstilfelleMediator = SykefraværstilfelleMediator(rapidsConnection)
        sykefraværstilfelle.registrer(sykefraværstilfelleMediator)
        avsluttetMedVedtakMessage.sendInnTil(sykefraværstilfelle)
    }

    internal fun håndter(avsluttetUtenVedtakMessage: AvsluttetUtenVedtakMessage) {
        val fødselsnummer = avsluttetUtenVedtakMessage.fødselsnummer()
        val skjæringstidspunkt = avsluttetUtenVedtakMessage.skjæringstidspunkt()
        val sykefraværstilfelle = hendelsefabrikk.sykefraværstilfelle(fødselsnummer, skjæringstidspunkt)
        val sykefraværstilfelleMediator = SykefraværstilfelleMediator(rapidsConnection)
        sykefraværstilfelle.registrer(sykefraværstilfelleMediator)
        avsluttetUtenVedtakMessage.sendInnTil(sykefraværstilfelle)
    }

    internal fun håndter(avviksvurdering: AvviksvurderingDto) {
        hendelsefabrikk.avviksvurdering(avviksvurdering)
    }

    fun adressebeskyttelseEndret(
        message: JsonMessage,
        id: UUID,
        fødselsnummer: String,
        context: MessageContext,
    ) {
        håndter(hendelsefabrikk.adressebeskyttelseEndret(id, fødselsnummer, message.toJson()), context)
    }

    fun sykefraværstilfeller(
        json: String,
        id: UUID,
        vedtaksperioder: List<VedtaksperiodeOppdatering>,
        fødselsnummer: String,
        aktørId: String,
        context: MessageContext,
    ) {
        val hendelse = hendelsefabrikk.sykefraværstilfeller(
            id,
            vedtaksperioder,
            fødselsnummer,
            aktørId,
            json,
        )

        return utfør(hendelse, context)
    }

    fun vedtaksperiodeEndret(
        message: JsonMessage,
        id: UUID,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        forårsaketAvId: UUID,
        forrigeTilstand: String,
        gjeldendeTilstand: String,
        context: MessageContext,
    ) {
        if (personDao.findPersonByFødselsnummer(fødselsnummer) == null) {
            logg.info("ignorerer hendelseId=${id} fordi vi kjenner ikke til personen")
            sikkerLogg.info("ignorerer hendelseId=${id} fordi vi kjenner ikke til personen med fnr=${fødselsnummer}")
            return
        }
        val hendelse = hendelsefabrikk.vedtaksperiodeEndret(
            id = id,
            vedtaksperiodeId = vedtaksperiodeId,
            fødselsnummer = fødselsnummer,
            forårsaketAvId = forårsaketAvId,
            forrigeTilstand = forrigeTilstand,
            gjeldendeTilstand = gjeldendeTilstand,
            json = message.toJson()
        )
        return utfør(hendelse, context)
    }

    fun vedtaksperiodeOpprettet(
        message: JsonMessage,
        id: UUID,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        fom: LocalDate,
        tom: LocalDate,
        skjæringstidspunkt: LocalDate,
        context: MessageContext,
    ) {
        val hendelse = hendelsefabrikk.vedtaksperiodeOpprettet(
            id,
            fødselsnummer,
            organisasjonsnummer,
            vedtaksperiodeId,
            fom,
            tom,
            skjæringstidspunkt,
            message.toJson()
        )
        if (personDao.findPersonByFødselsnummer(fødselsnummer) == null) {
            logg.error("vedtaksperiodeOpprettet: ignorerer hendelseId=${hendelse.id} fordi vi kjenner ikke til personen")
            sikkerLogg.error("vedtaksperiodeOpprettet: ignorerer hendelseId=${hendelse.id} fordi vi kjenner ikke til personen med fnr=${fødselsnummer}")
            return
        }
        return håndter(hendelse, context)
    }

    fun vedtaksperiodeForkastet(
        message: JsonMessage,
        id: UUID,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        context: MessageContext,
    ) {
        val hendelse = hendelsefabrikk.vedtaksperiodeForkastet(id, vedtaksperiodeId, fødselsnummer, message.toJson())
        if (vedtakDao.finnVedtakId(vedtaksperiodeId) == null) {
            logg.info("ignorerer hendelseId=${hendelse.id} fordi vi ikke kjenner til $vedtaksperiodeId")
            return
        }
        return utfør(hendelse, context)
    }

    fun godkjenningsbehov(
        message: JsonMessage,
        id: UUID,
        fødselsnummer: String,
        aktørId: String,
        organisasjonsnummer: String,
        periodeFom: LocalDate,
        periodeTom: LocalDate,
        skjæringstidspunkt: LocalDate,
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        periodetype: Periodetype,
        førstegangsbehandling: Boolean,
        utbetalingtype: Utbetalingtype,
        inntektskilde: Inntektskilde,
        orgnummereMedRelevanteArbeidsforhold: List<String>,
        kanAvvises: Boolean,
        context: MessageContext,
        avviksvurderingId: UUID?,
        vilkårsgrunnlagId: UUID,
    ) {
        if (avviksvurderingId != null)
            avviksvurderingDao.opprettKobling(avviksvurderingId, vilkårsgrunnlagId)
        if (oppgaveDao.harGyldigOppgave(utbetalingId) || vedtakDao.erAutomatiskGodkjent(utbetalingId)) {
            sikkerLogg.info("vedtaksperiodeId=$vedtaksperiodeId med utbetalingId=$utbetalingId har gyldig oppgave eller er automatisk godkjent. Ignorerer godkjenningsbehov med id=$id")
            return
        }
        if (generasjonRepository.finnVedtaksperiodeIderFor(fødselsnummer, skjæringstidspunkt).isEmpty()) {
            sikkerLogg.error("""
                vedtaksperiodeId=$vedtaksperiodeId med utbetalingId=$utbetalingId, periodeFom=$periodeFom, periodeTom=$periodeTom 
                og skjæringstidspunkt=$skjæringstidspunkt er i et sykefraværstilfelle uten generasjoner lagret. 
                Ignorerer godkjenningsbehov med id=$id""")
            return
        }
        utfør(
            hendelsefabrikk.godkjenning(
                id,
                fødselsnummer,
                aktørId,
                organisasjonsnummer,
                periodeFom,
                periodeTom,
                vedtaksperiodeId,
                utbetalingId,
                skjæringstidspunkt,
                periodetype,
                førstegangsbehandling,
                utbetalingtype,
                inntektskilde,
                orgnummereMedRelevanteArbeidsforhold,
                kanAvvises,
                message.toJson(),
            ), context
        )
    }

    fun søknadSendt(
        message: JsonMessage,
        id: UUID,
        fødselsnummer: String,
        aktørId: String,
        organisasjonsnummer: String,
        context: MessageContext,
    ) {
        utfør(
            hendelsefabrikk.søknadSendt(
                id,
                fødselsnummer,
                aktørId,
                organisasjonsnummer,
                message.toJson()
            ), context
        )
    }

    fun overstyringIgangsatt(
        message: JsonMessage,
        id: UUID,
        fødselsnummer: String,
        kilde: UUID,
        berørteVedtaksperiodeIder: List<UUID>,
        context: MessageContext,
    ) {
        utfør(
            hendelsefabrikk.overstyringIgangsatt(
                id,
                fødselsnummer,
                kilde,
                berørteVedtaksperiodeIder,
                message.toJson()
            ), context
        )
    }

    fun saksbehandlerløsning(
        message: JsonMessage,
        id: UUID,
        behandlingId: UUID,
        godkjenningsbehovhendelseId: UUID,
        fødselsnummer: String,
        godkjent: Boolean,
        saksbehandlerident: String,
        saksbehandlerepost: String,
        godkjenttidspunkt: LocalDateTime,
        årsak: String?,
        begrunnelser: List<String>?,
        kommentar: String?,
        saksbehandleroverstyringer: List<UUID>,
        oppgaveId: Long,
        context: MessageContext,
    ) {
        utfør(
            fødselsnummer, hendelsefabrikk.saksbehandlerløsning(
                id,
                behandlingId,
                godkjenningsbehovhendelseId,
                fødselsnummer,
                godkjent,
                saksbehandlerident,
                saksbehandlerepost,
                godkjenttidspunkt,
                årsak,
                begrunnelser,
                kommentar,
                saksbehandleroverstyringer,
                oppgaveId,
                message.toJson()
            ), context
        )
    }

    fun overstyringInntektOgRefusjon(
        id: UUID,
        fødselsnummer: String,
        oid: UUID,
        arbeidsgivere: List<OverstyrtArbeidsgiver>,
        skjæringstidspunkt: LocalDate,
        opprettet: LocalDateTime,
        json: String,
        context: MessageContext,
    ) {
        utfør(
            fødselsnummer, hendelsefabrikk.overstyringInntektOgRefusjon(
                id = id,
                fødselsnummer = fødselsnummer,
                oid = oid,
                arbeidsgivere = arbeidsgivere,
                skjæringstidspunkt = skjæringstidspunkt,
                opprettet = opprettet,
                json = json
            ), context
        )
    }

    fun skjønnsfastsettingSykepengegrunnlag(
        id: UUID,
        fødselsnummer: String,
        oid: UUID,
        arbeidsgivere: List<SkjønnsfastsattArbeidsgiver>,
        skjæringstidspunkt: LocalDate,
        opprettet: LocalDateTime,
        json: String,
        context: MessageContext,
    ) {
        utfør(
            fødselsnummer, hendelsefabrikk.skjønnsfastsettingSykepengegrunnlag(
                id = id,
                fødselsnummer = fødselsnummer,
                oid = oid,
                arbeidsgivere = arbeidsgivere,
                skjæringstidspunkt = skjæringstidspunkt,
                opprettet = opprettet,
                json = json,
            ), context
        )
    }

    fun overstyringArbeidsforhold(
        id: UUID,
        fødselsnummer: String,
        oid: UUID,
        overstyrteArbeidsforhold: List<OverstyrArbeidsforholdHandlingFraApi.ArbeidsforholdFraApi>,
        skjæringstidspunkt: LocalDate,
        opprettet: LocalDateTime,
        json: String,
        context: MessageContext,
    ) {
        utfør(
            fødselsnummer, hendelsefabrikk.overstyringArbeidsforhold(
                id = id,
                fødselsnummer = fødselsnummer,
                oid = oid,
                overstyrteArbeidsforhold = overstyrteArbeidsforhold,
                skjæringstidspunkt = skjæringstidspunkt,
                opprettet = opprettet,
                json = json
            ), context
        )
    }

    fun utbetalingAnnullert(
        message: JsonMessage,
        context: MessageContext,
    ) {
        utfør(hendelsefabrikk.utbetalingAnnullert(message.toJson()), context)
    }

    fun utbetalingEndret(
        fødselsnummer: String,
        organisasjonsnummer: String,
        message: JsonMessage,
        context: MessageContext,
    ) {
        if (arbeidsgiverDao.findArbeidsgiverByOrgnummer(organisasjonsnummer) == null) {
            logg.warn(
                "Fant ikke arbeidsgiver med {}, se sikkerlogg for mer informasjon",
                keyValue("hendelseId", message["@id"].asText())
            )
            sikkerLogg.warn(
                "Forstår ikke utbetaling_endret: fant ikke arbeidsgiver med {}, {}, {}, {}. Meldingen er lagret i feilende_meldinger",
                keyValue("hendelseId", message["@id"].asText()),
                keyValue("fødselsnummer", fødselsnummer),
                keyValue("organisasjonsnummer", organisasjonsnummer),
                keyValue("utbetalingId", message["utbetalingId"].asText())
            )
            feilendeMeldingerDao.lagre(
                UUID.fromString(message["@id"].asText()),
                message["@event_name"].asText(),
                message.toJson()
            )
            return
        }
        utfør(fødselsnummer, hendelsefabrikk.utbetalingEndret(message.toJson()), context)
    }

    fun vedtaksperiodeNyUtbetaling(
        fødselsnummer: String,
        hendelseId: UUID,
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        json: String,
        context: MessageContext,
    ) {
        utfør(
            hendelsefabrikk.vedtaksperiodeNyUtbetaling(
                hendelseId,
                fødselsnummer,
                vedtaksperiodeId,
                utbetalingId,
                json
            ), context
        )
    }

    fun oppdaterPersonsnapshot(message: JsonMessage, context: MessageContext) {
        utfør(hendelsefabrikk.oppdaterPersonsnapshot(message.toJson()), context)
    }

    fun vedtaksperiodeReberegnet(message: JsonMessage, context: MessageContext) {
        utfør(hendelsefabrikk.vedtaksperiodeReberegnet(message.toJson()), context)
    }

    fun gosysOppgaveEndret(hendelseId: UUID, fødselsnummer: String, aktørId: String, json: String, context: MessageContext) {
        utfør(hendelsefabrikk.gosysOppgaveEndret(hendelseId, fødselsnummer, aktørId, json), context)
    }

    fun egenAnsattStatusEndret(json: String, context: MessageContext) {
        håndter(hendelsefabrikk.endretEgenAnsattStatus(json), context)
    }

    fun vedtakFattet(id: UUID, fødselsnummer: String, vedtaksperiodeId: UUID, json: String, context: MessageContext) {
        utfør(hendelsefabrikk.vedtakFattet(id, fødselsnummer, vedtaksperiodeId, json), context)
    }

    fun godkjentTilbakedatertSykmelding(id: UUID, fødselsnummer: String, vedtaksperiodeId: UUID, skjæringstidspunkt: LocalDate, json: String, context: MessageContext) {
        if (!hendelsefabrikk.sykefraværstilfelle(fødselsnummer, skjæringstidspunkt).erTilbakedatert(vedtaksperiodeId)) return logg.info("ignorerer hendelseId=${id} fordi det ikke er en tilbakedatering")

        utfør(hendelsefabrikk.godkjentTilbakedatertSykmelding(id, fødselsnummer, json), context)
    }

    fun nyeVarsler(
        id: UUID,
        fødselsnummer: String,
        varsler: List<Varsel>,
        json: String,
        context: MessageContext,
    ) {
        utfør(hendelsefabrikk.nyeVarsler(id, fødselsnummer, varsler, json), context)
    }

    private fun forbered() {
        løsninger = null
    }

    private fun løsninger(messageContext: MessageContext, hendelseId: UUID, contextId: UUID): Løsninger? {
        return løsninger ?: run {
            val commandContext = commandContextDao.finnSuspendert(contextId) ?: run {
                logg.info("Ignorerer melding fordi: command context $contextId er ikke suspendert")
                return null
            }
            val hendelse = hendelseDao.finn(hendelseId, hendelsefabrikk) ?: run {
                logg.info("Ignorerer melding fordi: finner ikke hendelse med id=$hendelseId")
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
        sikkerLogg.error("alvorlig feil: ${err.message}\n\t$message", err, err.printStackTrace())
    }

    private fun nyContext(hendelse: Personhendelse, contextId: UUID) = CommandContext(contextId).apply {
        hendelseDao.opprett(hendelse)
        opprett(commandContextDao, hendelse.id)
    }

    private fun håndter(hendelse: Personhendelse, messageContext: MessageContext) {
        val contextId = UUID.randomUUID()
        logg.info("oppretter ny kommandokontekst med context_id=$contextId for hendelse_id=${hendelse.id} og type=${hendelse::class.simpleName}")
        håndter(hendelse, nyContext(hendelse, contextId), messageContext)
    }

    private fun håndter(hendelse: Personhendelse, commandContext: CommandContext, messageContext: MessageContext) {
        val contextId = commandContext.id()
        val hendelsenavn = hendelse::class.simpleName ?: "ukjent hendelse"
        try {
            when (hendelse) {
                is AdressebeskyttelseEndret -> iverksett(AdressebeskyttelseEndretCommand(hendelse.fødselsnummer(), personDao, oppgaveDao, godkjenningMediator), hendelse.id, commandContext)
                is EndretEgenAnsattStatus -> iverksett(hendelsefabrikk.endretEgenAnsattStatus(hendelse.fødselsnummer(), hendelse.erEgenAnsatt), hendelse.id, commandContext)
                is VedtaksperiodeOpprettet -> iverksett(hendelsefabrikk.opprettVedtaksperiode(hendelse.fødselsnummer(), hendelse), hendelse.id, commandContext)
                else -> throw IllegalArgumentException("Personhendelse må håndteres")
            }
            behovMediator.håndter(hendelse, commandContext, contextId, messageContext)
        } catch (e: Exception) {
            logg.warn(
                "Feil ved kjøring av $hendelsenavn: contextId={}, message={}",
                contextId, e.message, e
            )
        } finally {
            logg.info("utført $hendelsenavn med context_id=$contextId for hendelse_id=${hendelse.id}")
        }
    }

    private fun iverksett(command: Command, hendelseId: UUID, commandContext: CommandContext) {
        val contextId = commandContext.id()
        withMDC(
            mapOf(
                "context_id" to "$contextId",
                "hendelse_id" to "$hendelseId"
            )
        ) {
            try {
                if (commandContext.utfør(commandContextDao, hendelseId, command)) {
                    val kjøretid = commandContextDao.tidsbrukForContext(contextId)
                    metrikker(command.name, kjøretid, contextId)
                    logg.info(
                        "Kommando(er) for ${command.name} er utført ferdig. Det tok ca {}ms å kjøre hele kommandokjeden",
                        kjøretid
                    )
                } else logg.info("${command.name} er suspendert")
            } catch (err: Exception) {
                command.undo(commandContext)
                throw err
            }
        }
    }

    private fun utfør(fødselsnummer: String, hendelse: Kommandohendelse, messageContext: MessageContext) {
        if (personDao.findPersonByFødselsnummer(fødselsnummer) == null) return logg.info("ignorerer hendelseId=${hendelse.id} fordi vi ikke kjenner til personen")
        return utfør(hendelse, messageContext)
    }

    private fun utfør(hendelse: Kommandohendelse, messageContext: MessageContext) {
        val contextId = UUID.randomUUID()
        logg.info("oppretter ny kommandokontekst med context_id=$contextId for hendelse_id=${hendelse.id} og type=${hendelse::class.simpleName}")
        utfør(hendelse, nyContext(hendelse, contextId), contextId, messageContext)
    }

    private fun utfør(
        hendelse: Kommandohendelse,
        context: CommandContext,
        contextId: UUID,
        messageContext: MessageContext,
    ) {
        withMDC(
            mapOf(
                "context_id" to "$contextId",
                "hendelse_id" to "${hendelse.id}",
                "vedtaksperiode_id" to "${if (hendelse is VedtaksperiodeHendelse) hendelse.vedtaksperiodeId() else "N/A"}"
            )
        ) {
            val hendelsenavn = hendelse::class.simpleName ?: "ukjent hendelse"
            try {
                logg.info("utfører $hendelsenavn med context_id=$contextId for hendelse_id=${hendelse.id}")
                if (context.utfør(commandContextDao, hendelse)) {
                    val kjøretid = commandContextDao.tidsbrukForContext(contextId)
                    metrikker(hendelsenavn, kjøretid, contextId)
                    logg.info(
                        "Kommando(er) for $hendelsenavn er utført ferdig. Det tok ca {}ms å kjøre hele kommandokjeden",
                        kjøretid
                    )
                } else logg.info("$hendelsenavn er suspendert")
                behovMediator.håndter(hendelse, context, contextId, messageContext)
            } catch (err: Exception) {
                logg.warn(
                    "Feil ved kjøring av $hendelsenavn: contextId={}, message={}",
                    contextId,
                    err.message,
                    err
                )
                hendelse.undo(context)
                throw err
            } finally {
                logg.info("utført $hendelsenavn med context_id=$contextId for hendelse_id=${hendelse.id}")
            }
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
        private val hendelse: Personhendelse,
        private val contextId: UUID,
        private val commandContext: CommandContext,
    ) {
        fun add(hendelseId: UUID, contextId: UUID, løsning: Any) {
            check(hendelseId == hendelse.id)
            check(contextId == this.contextId)
            commandContext.add(løsning)
        }

        fun fortsett(mediator: HendelseMediator, message: String) {
            logg.info("fortsetter utførelse av kommandokontekst pga. behov_id=${hendelse.id} med context_id=$contextId for hendelse_id=${hendelse.id}")
            sikkerLogg.info(
                "fortsetter utførelse av kommandokontekst pga. behov_id=${hendelse.id} med context_id=$contextId for hendelse_id=${hendelse.id}.\n" +
                        "Innkommende melding:\n\t$message"
            )
            if (hendelse is Kommandohendelse) mediator.utfør(hendelse, commandContext, contextId, messageContext)
            else mediator.håndter(hendelse, commandContext, messageContext)
        }
    }

    override fun oppdaterSnapshot(fnr: String) {
        val json = JsonMessage.newMessage(
            "oppdater_personsnapshot", mapOf(
                "fødselsnummer" to fnr
            )
        )
        oppdaterPersonsnapshot(json, rapidsConnection)
    }
}
