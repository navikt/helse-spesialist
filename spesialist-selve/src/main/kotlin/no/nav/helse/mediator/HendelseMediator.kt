package no.nav.helse.mediator

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.time.temporal.ChronoUnit.MILLIS
import java.util.UUID
import javax.sql.DataSource
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.MetrikkRiver
import no.nav.helse.mediator.api.Arbeidsgiver
import no.nav.helse.mediator.api.OverstyrArbeidsforholdDto
import no.nav.helse.mediator.api.OverstyrArbeidsforholdKafkaDto
import no.nav.helse.mediator.api.OverstyrInntektOgRefusjonKafkaDto
import no.nav.helse.mediator.api.OverstyrTidslinjeKafkaDto
import no.nav.helse.mediator.meldinger.AdressebeskyttelseEndret
import no.nav.helse.mediator.meldinger.EndretSkjermetinfo
import no.nav.helse.mediator.meldinger.Godkjenningsbehov
import no.nav.helse.mediator.meldinger.GosysOppgaveEndret
import no.nav.helse.mediator.meldinger.Hendelse
import no.nav.helse.mediator.meldinger.NyeVarsler
import no.nav.helse.mediator.meldinger.OppdaterPersonsnapshot
import no.nav.helse.mediator.meldinger.OverstyringArbeidsforhold
import no.nav.helse.mediator.meldinger.OverstyringIgangsatt
import no.nav.helse.mediator.meldinger.OverstyringInntektOgRefusjon
import no.nav.helse.mediator.meldinger.OverstyringTidslinje
import no.nav.helse.mediator.meldinger.PåminnetGodkjenningsbehov
import no.nav.helse.mediator.meldinger.Sykefraværstilfeller
import no.nav.helse.mediator.meldinger.SøknadSendt
import no.nav.helse.mediator.meldinger.UtbetalingAnnullert
import no.nav.helse.mediator.meldinger.UtbetalingEndret
import no.nav.helse.mediator.meldinger.Varseldefinisjon
import no.nav.helse.mediator.meldinger.VedtakFattet
import no.nav.helse.mediator.meldinger.VedtaksperiodeEndret
import no.nav.helse.mediator.meldinger.VedtaksperiodeForkastet
import no.nav.helse.mediator.meldinger.VedtaksperiodeNyUtbetaling
import no.nav.helse.mediator.meldinger.VedtaksperiodeOpprettet
import no.nav.helse.mediator.meldinger.VedtaksperiodeReberegnet
import no.nav.helse.mediator.meldinger.løsninger.Arbeidsgiverinformasjonløsning
import no.nav.helse.mediator.meldinger.løsninger.EgenAnsattløsning
import no.nav.helse.mediator.meldinger.løsninger.HentEnhetløsning
import no.nav.helse.mediator.meldinger.løsninger.HentInfotrygdutbetalingerløsning
import no.nav.helse.mediator.meldinger.løsninger.HentPersoninfoløsning
import no.nav.helse.mediator.meldinger.løsninger.Inntektløsning
import no.nav.helse.mediator.meldinger.løsninger.Risikovurderingløsning
import no.nav.helse.mediator.meldinger.løsninger.Saksbehandlerløsning
import no.nav.helse.mediator.meldinger.løsninger.Vergemålløsning
import no.nav.helse.mediator.meldinger.løsninger.ÅpneGosysOppgaverløsning
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.HendelseDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.arbeidsforhold.Arbeidsforholdløsning
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.oppgave.OppgaveDao
import no.nav.helse.modell.oppgave.OppgaveMediator
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.varsel.ActualVarselRepository
import no.nav.helse.modell.varsel.Varsel
import no.nav.helse.modell.varsel.VarselRepository
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeOppdatering
import no.nav.helse.overstyringsteller
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.registrerTidsbrukForGodkjenningsbehov
import no.nav.helse.registrerTidsbrukForHendelse
import no.nav.helse.spesialist.api.overstyring.OverstyringDagDto
import no.nav.helse.spesialist.api.tildeling.TildelingDao
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
    private val tildelingDao: TildelingDao = TildelingDao(dataSource),
    private val feilendeMeldingerDao: FeilendeMeldingerDao = FeilendeMeldingerDao(dataSource),
    private val oppgaveMediator: OppgaveMediator,
    private val hendelsefabrikk: Hendelsefabrikk,
    private val egenAnsattDao: EgenAnsattDao = EgenAnsattDao(dataSource),
    private val varselRepository: VarselRepository = ActualVarselRepository(dataSource),
    private val metrikkDao: MetrikkDao = MetrikkDao(dataSource),
) {
    private companion object {
        private val log = LoggerFactory.getLogger(HendelseMediator::class.java)
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }

    private val behovMediator = BehovMediator(sikkerLogg)

    init {
        DelegatedRapid(rapidsConnection, ::forbered, ::fortsett, ::errorHandler).also {
            Godkjenningsbehov.GodkjenningsbehovRiver(it, this)
            SøknadSendt.SøknadSendtRiver(it, this)
            HentPersoninfoløsning.PersoninfoRiver(it, this)
            HentPersoninfoløsning.FlerePersoninfoRiver(it, this)
            HentEnhetløsning.HentEnhetRiver(it, this)
            HentInfotrygdutbetalingerløsning.InfotrygdutbetalingerRiver(it, this)
            Saksbehandlerløsning.SaksbehandlerløsningRiver(it, this)
            Arbeidsgiverinformasjonløsning.ArbeidsgiverRiver(it, this)
            Arbeidsforholdløsning.ArbeidsforholdRiver(it, this)
            VedtaksperiodeForkastet.VedtaksperiodeForkastetRiver(it, this)
            VedtaksperiodeEndret.VedtaksperiodeEndretRiver(it, this)
            AdressebeskyttelseEndret.AdressebeskyttelseEndretRiver(it, this)
            OverstyringTidslinje.OverstyringTidslinjeRiver(it, this)
            OverstyringInntektOgRefusjon.OverstyringInntektOgRefusjonRiver(it, this)
            OverstyringArbeidsforhold.OverstyringArbeidsforholdRiver(it, this)
            OverstyringIgangsatt.OverstyringIgangsattRiver(it, this)
            EgenAnsattløsning.EgenAnsattRiver(it, this)
            Vergemålløsning.VergemålRiver(it, this)
            ÅpneGosysOppgaverløsning.ÅpneGosysOppgaverRiver(it, this)
            Risikovurderingløsning.V2River(it, this)
            if (Toggle.Inntekter.enabled) Inntektløsning.InntektRiver(it, this)
            UtbetalingAnnullert.UtbetalingAnnullertRiver(it, this)
            OppdaterPersonsnapshot.River(it, this)
            UtbetalingEndret.River(it, this)
            VedtaksperiodeReberegnet.River(it, this)
            VedtaksperiodeOpprettet.River(it, this)
            GosysOppgaveEndret.River(it, this, oppgaveDao, tildelingDao, personDao)
            EndretSkjermetinfo.River(it, personDao, egenAnsattDao)
            VedtakFattet.River(it, this)
            NyeVarsler.River(it, this)
            Varseldefinisjon.River(it, varselRepository)
            Varseldefinisjon.VarseldefinisjonRiver(it, varselRepository)
            VedtaksperiodeNyUtbetaling.River(it, this)
            Sykefraværstilfeller.River(it, this)
            MetrikkRiver(it)
            PåminnetGodkjenningsbehov.River(it, this)
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
                ?: log.info(
                    "mottok løsning med behovId=$behovId som ikke kunne brukes fordi kommandoen ikke lengre er suspendert, " +
                            "eller fordi hendelsen $hendelseId er ukjent"
                )
        }
    }

    internal fun tildelOppgaveTilSaksbehandler(
        oppgaveId: Long,
        saksbehandlerreferanse: UUID,
    ): Boolean {
        val suksess = tildelingDao.opprettTildeling(oppgaveId, saksbehandlerreferanse)
        if (suksess) sendMeldingOppgaveOppdatert(oppgaveId)
        return suksess
    }

    internal fun sendMeldingOppgaveOppdatert(oppgaveId: Long, påVent: Boolean? = null) {
        Oppgave.lagMelding(oppgaveId, "oppgave_oppdatert", påVent, oppgaveDao).also { (key, message) ->
            rapidsConnection.publish(key, message.toJson())
        }
    }

    fun adressebeskyttelseEndret(
        message: JsonMessage,
        id: UUID,
        fødselsnummer: String,
        context: MessageContext,
    ) {
        utfør(fødselsnummer, hendelsefabrikk.adressebeskyttelseEndret(id, fødselsnummer, message.toJson()), context)
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
            log.info("ignorerer hendelseId=${id} fordi vi kjenner ikke til personen")
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
            log.error("vedtaksperiodeOpprettet: ignorerer hendelseId=${hendelse.id} fordi vi kjenner ikke til personen")
            sikkerLogg.error("vedtaksperiodeOpprettet: ignorerer hendelseId=${hendelse.id} fordi vi kjenner ikke til personen med fnr=${fødselsnummer}")
            return
        }
        return utfør(hendelse, context)
    }

    fun vedtaksperiodeForkastet(
        message: JsonMessage,
        id: UUID,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        context: MessageContext,
    ) {
        val hendelse = hendelsefabrikk.vedtaksperiodeForkastet(id, vedtaksperiodeId, fødselsnummer, message.toJson())
        if (!hendelseDao.harKoblingTil(vedtaksperiodeId)) {
            log.info("ignorerer hendelseId=${hendelse.id} fordi vi ikke kjenner til $vedtaksperiodeId")
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
        context: MessageContext,
    ) {
        if (oppgaveDao.harGyldigOppgave(utbetalingId) || vedtakDao.erAutomatiskGodkjent(utbetalingId)) {
            sikkerLogg.info("vedtaksperiodeId=$vedtaksperiodeId med utbetalingId=$utbetalingId har gyldig oppgave eller er automatisk godkjent. Ignorerer godkjenningsbehov med id=$id")
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
                message.toJson()
            ), context
        )
    }

    fun påminnetGodkjenningsbehov(
        message: JsonMessage,
        id: UUID,
        fødselsnummer: String,
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        context: MessageContext,
    ) {
        if (!oppgaveDao.harGyldigOppgave(utbetalingId)) return

        log.info("Behandler påminnet godkjenningsbehov (id=$id) for vedtaksperiodeId=$vedtaksperiodeId, utbetalingId=$utbetalingId.")
        utfør(hendelsefabrikk.påminnetGodkjenningsbehov(id, fødselsnummer, message.toJson()), context)
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

    fun overstyringTidslinje(
        id: UUID,
        fødselsnummer: String,
        oid: UUID,
        navn: String,
        ident: String,
        epost: String,
        orgnummer: String,
        begrunnelse: String,
        overstyrteDager: List<OverstyringDagDto>,
        opprettet: LocalDateTime,
        json: String,
        context: MessageContext,
    ) {
        utfør(
            fødselsnummer, hendelsefabrikk.overstyringTidslinje(
                id = id,
                fødselsnummer = fødselsnummer,
                oid = oid,
                navn = navn,
                ident = ident,
                epost = epost,
                orgnummer = orgnummer,
                begrunnelse = begrunnelse,
                overstyrteDager = overstyrteDager,
                opprettet = opprettet,
                json = json
            ), context
        )
    }

    fun overstyringInntektOgRefusjon(
        id: UUID,
        fødselsnummer: String,
        oid: UUID,
        navn: String,
        ident: String,
        epost: String,
        arbeidsgivere: List<Arbeidsgiver>,
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
                navn = navn,
                ident = ident,
                epost = epost,
                arbeidsgivere = arbeidsgivere,
                skjæringstidspunkt = skjæringstidspunkt,
                opprettet = opprettet,
                json = json
            ), context
        )
    }

    fun overstyringArbeidsforhold(
        id: UUID,
        fødselsnummer: String,
        oid: UUID,
        navn: String,
        ident: String,
        epost: String,
        overstyrteArbeidsforhold: List<OverstyrArbeidsforholdDto.ArbeidsforholdOverstyrt>,
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
                navn = navn,
                ident = ident,
                epost = epost,
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
            log.warn(
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

    fun avbrytSaksbehandling(message: JsonMessage, context: MessageContext) {
        utfør(hendelsefabrikk.vedtaksperiodeReberegnet(message.toJson()), context)
    }

    fun gosysOppgaveEndret(hendelseId: UUID, fødselsnummer: String, aktørId: String, json: String, context: MessageContext) {
        utfør(hendelsefabrikk.gosysOppgaveEndret(hendelseId, fødselsnummer, aktørId, json), context)
    }

    fun vedtakFattet(id: UUID, fødselsnummer: String, vedtaksperiodeId: UUID, json: String, context: MessageContext) {
        utfør(hendelsefabrikk.vedtakFattet(id, fødselsnummer, vedtaksperiodeId, json), context)
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

    fun håndter(overstyringMessage: OverstyrTidslinjeKafkaDto) {
        overstyringsteller.labels("opplysningstype", "tidslinje").inc()
        val overstyring = JsonMessage.newMessage(
            "saksbehandler_overstyrer_tidslinje", mutableMapOf(
                "fødselsnummer" to overstyringMessage.fødselsnummer,
                "aktørId" to overstyringMessage.aktørId,
                "organisasjonsnummer" to overstyringMessage.organisasjonsnummer,
                "dager" to overstyringMessage.dager,
                "begrunnelse" to overstyringMessage.begrunnelse,
                "saksbehandlerOid" to overstyringMessage.saksbehandlerOid,
                "saksbehandlerNavn" to overstyringMessage.saksbehandlerNavn,
                "saksbehandlerIdent" to overstyringMessage.saksbehandlerIdent,
                "saksbehandlerEpost" to overstyringMessage.saksbehandlerEpost,
            )
        ).also {
            sikkerLogg.info("Publiserer overstyring fra api:\n${it.toJson()}")
        }
        rapidsConnection.publish(overstyringMessage.fødselsnummer, overstyring.toJson())
    }

    fun håndter(overstyringMessage: OverstyrInntektOgRefusjonKafkaDto) {
        overstyringsteller.labels("opplysningstype", "inntektogrefusjon").inc()

        val overstyring = overstyringMessage.somKafkaMessage().also {
            sikkerLogg.info("Publiserer overstyring fra api:\n${it.toJson()}")
        }

        rapidsConnection.publish(overstyringMessage.fødselsnummer, overstyring.toJson())
    }

    fun håndter(overstyringMessage: OverstyrArbeidsforholdKafkaDto) {
        overstyringsteller.labels("opplysningstype", "arbeidsforhold").inc()

        val overstyring = overstyringMessage.somKafkaMessage().also {
            sikkerLogg.info("Publiserer overstyring fra api:\n${it.toJson()}")
        }

        rapidsConnection.publish(overstyringMessage.fødselsnummer, overstyring.toJson())
    }

    private fun forbered() {
        løsninger = null
    }

    private fun løsninger(messageContext: MessageContext, hendelseId: UUID, contextId: UUID): Løsninger? {
        return løsninger ?: run {
            val hendelse = hendelseDao.finn(hendelseId, hendelsefabrikk)
            val commandContext = commandContextDao.finnSuspendert(contextId)
            if (hendelse == null || commandContext == null) {
                mutableListOf<String>().let {
                    if (hendelse == null) it += "Finner ikke hendelse med id=$hendelseId"
                    if (commandContext == null) it += "Command context $contextId er ikke suspendert"
                    log.info("Ignorerer melding fordi: " + it.joinToString())
                }

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
        log.error("alvorlig feil: ${err.message} (se sikkerlogg for melding)", err)
        sikkerLogg.error("alvorlig feil: ${err.message}\n\t$message", err, err.printStackTrace())
    }

    private fun nyContext(hendelse: Hendelse, contextId: UUID) = CommandContext(contextId).apply {
        hendelseDao.opprett(hendelse)
        opprett(commandContextDao, hendelse)
    }

    private fun utfør(fødselsnummer: String, hendelse: Hendelse, messageContext: MessageContext) {
        if (personDao.findPersonByFødselsnummer(fødselsnummer) == null) return log.info("ignorerer hendelseId=${hendelse.id} fordi vi ikke kjenner til personen")
        return utfør(hendelse, messageContext)
    }

    private fun utfør(hendelse: Hendelse, messageContext: MessageContext) {
        val contextId = UUID.randomUUID()
        log.info("oppretter ny kommandokontekst med context_id=$contextId for hendelse_id=${hendelse.id} og type=${hendelse::class.simpleName}")
        utfør(hendelse, nyContext(hendelse, contextId), contextId, messageContext)
    }

    private fun utfør(
        hendelse: Hendelse,
        context: CommandContext,
        contextId: UUID,
        messageContext: MessageContext,
    ) {
        withMDC(
            mapOf(
                "context_id" to "$contextId",
                "hendelse_id" to "${hendelse.id}",
                "vedtaksperiode_id" to "${hendelse.vedtaksperiodeId() ?: "N/A"}"
            )
        ) {
            val hendelsenavn = hendelse::class.simpleName ?: "ukjent hendelse"
            try {
                log.info("utfører $hendelsenavn med context_id=$contextId for hendelse_id=${hendelse.id}")
                if (context.utfør(commandContextDao, hendelse)) {
                    val kjøretid = MILLIS.between(commandContextDao.contextOpprettetTidspunkt(contextId), now())
                    metrikker(hendelsenavn, kjøretid, contextId)
                    log.info(
                        "Kommando(er) for $hendelsenavn er utført ferdig. Det tok ca {}ms å kjøre hele kommandokjeden",
                        kjøretid
                    )
                } else log.info("$hendelsenavn er suspendert")
                behovMediator.håndter(hendelse, context, contextId, messageContext)
                oppgaveMediator.lagreOgTildelOppgaver(hendelse.id, hendelse.fødselsnummer(), contextId, messageContext)
            } catch (err: Exception) {
                log.warn(
                    "Feil ved kjøring av $hendelsenavn: contextId={}, message={}",
                    contextId,
                    err.message,
                    err
                )
                hendelse.undo(context)
                throw err
            } finally {
                log.info("utført $hendelsenavn med context_id=$contextId for hendelse_id=${hendelse.id}")
            }
        }
    }

    private fun metrikker(hendelsenavn: String, kjøretid: Long, contextId: UUID) {
        if (hendelsenavn == Godkjenningsbehov::class.simpleName) {
            val utfall: GodkjenningsbehovUtfall = metrikkDao.finnUtfallForGodkjenningsbehov(contextId)
            registrerTidsbrukForGodkjenningsbehov(utfall, kjøretid)
        }
        registrerTidsbrukForHendelse(hendelsenavn, kjøretid)
    }

    private class Løsninger(
        private val messageContex: MessageContext,
        private val hendelse: Hendelse,
        private val contextId: UUID,
        private val commandContext: CommandContext,
    ) {
        fun add(hendelseId: UUID, contextId: UUID, løsning: Any) {
            check(hendelseId == hendelse.id)
            check(contextId == this.contextId)
            commandContext.add(løsning)
        }

        fun fortsett(mediator: HendelseMediator, message: String) {
            log.info("fortsetter utførelse av kommandokontekst pga. behov_id=${hendelse.id} med context_id=$contextId for hendelse_id=${hendelse.id}")
            sikkerLogg.info(
                "fortsetter utførelse av kommandokontekst pga. behov_id=${hendelse.id} med context_id=$contextId for hendelse_id=${hendelse.id}.\n" +
                        "Innkommende melding:\n\t$message"
            )
            mediator.utfør(hendelse, commandContext, contextId, messageContex)
        }
    }
}
