package no.nav.helse.mediator

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.time.temporal.ChronoUnit.SECONDS
import java.util.UUID
import javax.sql.DataSource
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.abonnement.OpptegnelseDao
import no.nav.helse.annulleringsteller
import no.nav.helse.mediator.api.AnnulleringDto
import no.nav.helse.mediator.api.GodkjenningDTO
import no.nav.helse.mediator.api.OppdaterPersonsnapshotDto
import no.nav.helse.mediator.api.OverstyrArbeidsforholdDto
import no.nav.helse.mediator.api.OverstyrArbeidsforholdKafkaDto
import no.nav.helse.mediator.api.OverstyrInntektKafkaDto
import no.nav.helse.mediator.api.OverstyrTidslinjeKafkaDto
import no.nav.helse.mediator.api.modell.Saksbehandler
import no.nav.helse.mediator.meldinger.AdressebeskyttelseEndret
import no.nav.helse.mediator.meldinger.Arbeidsgiverinformasjonløsning
import no.nav.helse.mediator.meldinger.DigitalKontaktinformasjonløsning
import no.nav.helse.mediator.meldinger.EgenAnsattløsning
import no.nav.helse.mediator.meldinger.Godkjenningsbehov
import no.nav.helse.mediator.meldinger.Hendelse
import no.nav.helse.mediator.meldinger.HentEnhetløsning
import no.nav.helse.mediator.meldinger.HentInfotrygdutbetalingerløsning
import no.nav.helse.mediator.meldinger.HentPersoninfoløsning
import no.nav.helse.mediator.meldinger.OppdaterPersonsnapshot
import no.nav.helse.mediator.meldinger.OverstyringArbeidsforhold
import no.nav.helse.mediator.meldinger.OverstyringInntekt
import no.nav.helse.mediator.meldinger.OverstyringTidslinje
import no.nav.helse.mediator.meldinger.RevurderingAvvist
import no.nav.helse.mediator.meldinger.Risikovurderingløsning
import no.nav.helse.mediator.meldinger.Saksbehandlerløsning
import no.nav.helse.mediator.meldinger.UtbetalingAnnullert
import no.nav.helse.mediator.meldinger.UtbetalingEndret
import no.nav.helse.mediator.meldinger.VedtaksperiodeEndret
import no.nav.helse.mediator.meldinger.VedtaksperiodeForkastet
import no.nav.helse.mediator.meldinger.VedtaksperiodeReberegnet
import no.nav.helse.mediator.meldinger.Vergemålløsning
import no.nav.helse.mediator.meldinger.ÅpneGosysOppgaverløsning
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.HendelseDao
import no.nav.helse.modell.IHendelsefabrikk
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.arbeidsforhold.Arbeidsforholdløsning
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.objectMapper
import no.nav.helse.oppgave.Oppgave
import no.nav.helse.oppgave.OppgaveDao
import no.nav.helse.oppgave.OppgaveMediator
import no.nav.helse.overstyring.OverstyringDagDto
import no.nav.helse.overstyringsteller
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.reservasjon.ReservasjonDao
import no.nav.helse.saksbehandler.SaksbehandlerDao
import no.nav.helse.tildeling.TildelingDao
import org.slf4j.LoggerFactory

internal class HendelseMediator(
    private val dataSource: DataSource,
    private val rapidsConnection: RapidsConnection,
    private val oppgaveDao: OppgaveDao = OppgaveDao(dataSource),
    private val vedtakDao: VedtakDao = VedtakDao(dataSource),
    private val utbetalingDao: UtbetalingDao = UtbetalingDao(dataSource),
    private val personDao: PersonDao = PersonDao(dataSource),
    private val commandContextDao: CommandContextDao = CommandContextDao(dataSource),
    private val arbeidsgiverDao: ArbeidsgiverDao = ArbeidsgiverDao(dataSource),
    private val hendelseDao: HendelseDao = HendelseDao(dataSource),
    private val tildelingDao: TildelingDao = TildelingDao(dataSource),
    private val reservasjonDao: ReservasjonDao = ReservasjonDao(dataSource),
    private val saksbehandlerDao: SaksbehandlerDao = SaksbehandlerDao(dataSource),
    private val feilendeMeldingerDao: FeilendeMeldingerDao = FeilendeMeldingerDao(dataSource),
    private val opptegnelseDao: OpptegnelseDao,
    private val oppgaveMediator: OppgaveMediator,
    private val hendelsefabrikk: IHendelsefabrikk
) : IHendelseMediator {
    private companion object {
        private val log = LoggerFactory.getLogger(HendelseMediator::class.java)
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }

    private val behovMediator = BehovMediator(rapidsConnection, sikkerLogg)

    init {
        DelegatedRapid(rapidsConnection, ::forbered, ::fortsett, ::errorHandler).also {
            Godkjenningsbehov.GodkjenningsbehovRiver(it, this)
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
            OverstyringInntekt.OverstyringInntektRiver(it, this)
            OverstyringArbeidsforhold.OverstyringArbeidsforholdRiver(it, this)
            DigitalKontaktinformasjonløsning.DigitalKontaktinformasjonRiver(it, this)
            EgenAnsattløsning.EgenAnsattRiver(it, this)
            Vergemålløsning.VergemålRiver(it, this)
            ÅpneGosysOppgaverløsning.ÅpneGosysOppgaverRiver(it, this)
            Risikovurderingløsning.V2River(it, this)
            UtbetalingAnnullert.UtbetalingAnnullertRiver(it, this)
            OppdaterPersonsnapshot.River(it, this)
            UtbetalingEndret.River(it, this)
            VedtaksperiodeReberegnet.River(it, this)
            RevurderingAvvist.River(it, this)
        }
    }

    private var løsninger: Løsninger? = null

    // samler opp løsninger
    override fun løsning(
        hendelseId: UUID,
        contextId: UUID,
        behovId: UUID,
        løsning: Any,
        context: MessageContext
    ) {
        withMDC(
            mapOf(
                "behovId" to "$behovId"
            )
        ) {
            løsninger(hendelseId, contextId)?.also { it.add(hendelseId, contextId, løsning) }
                ?: log.info(
                    "mottok løsning med behovId=$behovId som ikke kunne brukes fordi kommandoen ikke lengre er suspendert, " +
                        "eller fordi hendelsen $hendelseId er ukjent"
                )
        }
    }

    internal fun håndter(godkjenningDTO: GodkjenningDTO, epost: String, oid: UUID) {
        val contextId = oppgaveDao.finnContextId(godkjenningDTO.oppgavereferanse)
        val hendelseId = oppgaveDao.finnHendelseId(godkjenningDTO.oppgavereferanse)
        val fødselsnummer = hendelseDao.finnFødselsnummer(hendelseId)
        val godkjenningMessage = JsonMessage.newMessage(
            standardfelter("saksbehandler_løsning", fødselsnummer, "Godkjenning" to hendelseId).apply {
                put("oppgaveId", godkjenningDTO.oppgavereferanse)
                put("hendelseId", hendelseId)
                put("godkjent", godkjenningDTO.godkjent)
                put("saksbehandlerident", godkjenningDTO.saksbehandlerIdent)
                put("saksbehandleroid", oid)
                put("saksbehandlerepost", epost)
                put("godkjenttidspunkt", now())
                godkjenningDTO.årsak?.let { put("årsak", it) }
                godkjenningDTO.begrunnelser?.let { put("begrunnelser", it) }
                godkjenningDTO.kommentar?.let { put("kommentar", it) }
            }).also {
            sikkerLogg.info("Publiserer saksbehandler-løsning: ${it.toJson()}")
        }
        log.info(
            "Publiserer saksbehandler-løsning for {}. {}. {}",
            keyValue("oppgaveId", godkjenningDTO.oppgavereferanse),
            keyValue("hendelseId", hendelseId)
        )
        rapidsConnection.publish(fødselsnummer, godkjenningMessage.toJson())

        val internOppgaveMediator = OppgaveMediator(oppgaveDao, tildelingDao, reservasjonDao, opptegnelseDao)
        internOppgaveMediator.reserverOppgave(oid, fødselsnummer)
        internOppgaveMediator.avventerSystem(godkjenningDTO.oppgavereferanse, godkjenningDTO.saksbehandlerIdent, oid)
        internOppgaveMediator.lagreOppgaver(rapidsConnection, hendelseId, contextId)
    }

    internal fun tildelOppgaveTilSaksbehandler(
        oppgaveId: Long,
        saksbehandlerreferanse: UUID,
    ): Boolean {
        val suksess = oppgaveMediator.tildel(oppgaveId, saksbehandlerreferanse)
        if (suksess) rapidsConnection.publish(Oppgave.lagMelding(oppgaveId, "oppgave_oppdatert", oppgaveDao).toJson())
        return suksess
    }

    internal fun leggOppgavePåVent(
        oppgaveId: Long,
    ) {
        rapidsConnection.publish(Oppgave.lagMelding(oppgaveId, "oppgave_oppdatert", oppgaveDao).toJson())
    }

    internal fun sendMeldingPåTopic(melding: JsonNode) {
        val fnr = melding["fødselsnummer"].asText()
        val rawJson = objectMapper.writeValueAsString(melding)
        sikkerLogg.info("Manuell publisering av melding for fnr=${fnr}, melding=${rawJson}")
        rapidsConnection.publish(fnr, rawJson)
    }

    override fun adressebeskyttelseEndret(
        message: JsonMessage,
        id: UUID,
        fødselsnummer: String,
        context: MessageContext
    ) {
        utfør(fødselsnummer, hendelsefabrikk.adressebeskyttelseEndret(id, fødselsnummer, message.toJson()), context)
    }

    override fun vedtaksperiodeEndret(
        message: JsonMessage,
        id: UUID,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        context: MessageContext
    ) {
        utfør(
            vedtaksperiodeId,
            hendelsefabrikk.vedtaksperiodeEndret(id, vedtaksperiodeId, fødselsnummer, message.toJson()),
            context
        )
    }

    override fun vedtaksperiodeForkastet(
        message: JsonMessage,
        id: UUID,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        context: MessageContext
    ) {
        utfør(
            vedtaksperiodeId,
            hendelsefabrikk.vedtaksperiodeForkastet(id, vedtaksperiodeId, fødselsnummer, message.toJson()),
            context
        )
    }

    override fun godkjenningsbehov(
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
        arbeidsforholdId: String?,
        periodetype: Periodetype,
        utbetalingtype: Utbetalingtype,
        inntektskilde: Inntektskilde,
        aktiveVedtaksperioder: List<Godkjenningsbehov.AktivVedtaksperiode>,
        orgnummereMedRelevanteArbeidsforhold: List<String>,
        context: MessageContext
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
                arbeidsforholdId,
                skjæringstidspunkt,
                periodetype,
                utbetalingtype,
                inntektskilde,
                aktiveVedtaksperioder,
                orgnummereMedRelevanteArbeidsforhold,
                message.toJson()
            ), context
        )
    }

    override fun saksbehandlerløsning(
        message: JsonMessage,
        id: UUID,
        godkjenningsbehovhendelseId: UUID,
        fødselsnummer: String,
        godkjent: Boolean,
        saksbehandlerident: String,
        saksbehandleroid: UUID,
        saksbehandlerepost: String,
        godkjenttidspunkt: LocalDateTime,
        årsak: String?,
        begrunnelser: List<String>?,
        kommentar: String?,
        oppgaveId: Long,
        context: MessageContext
    ) {
        utfør(
            fødselsnummer, hendelsefabrikk.saksbehandlerløsning(
                id,
                godkjenningsbehovhendelseId,
                fødselsnummer,
                godkjent,
                saksbehandlerident,
                saksbehandleroid,
                saksbehandlerepost,
                godkjenttidspunkt,
                årsak,
                begrunnelser,
                kommentar,
                oppgaveId,
                message.toJson()
            ), context
        )
    }

    override fun overstyringTidslinje(
        id: UUID,
        fødselsnummer: String,
        oid: UUID,
        navn: String,
        ident: String,
        epost: String,
        orgnummer: String,
        begrunnelse: String,
        overstyrteDager: List<OverstyringDagDto>,
        json: String,
        context: MessageContext
    ) {
        utfør(fødselsnummer, hendelsefabrikk.overstyringTidslinje(
            id = id,
            fødselsnummer = fødselsnummer,
            oid = oid,
            navn = navn,
            ident = ident,
            epost = epost,
            orgnummer = orgnummer,
            begrunnelse = begrunnelse,
            overstyrteDager = overstyrteDager,
            json = json
        ), context)
    }

    override fun overstyringInntekt(
        id: UUID,
        fødselsnummer: String,
        oid: UUID,
        navn: String,
        ident: String,
        epost: String,
        orgnummer: String,
        begrunnelse: String,
        forklaring: String,
        månedligInntekt: Double,
        skjæringstidspunkt: LocalDate,
        json: String,
        context: MessageContext
    ) {
        utfør(
            fødselsnummer, hendelsefabrikk.overstyringInntekt(
                id = id,
                fødselsnummer = fødselsnummer,
                oid = oid,
                navn = navn,
                ident = ident,
                epost = epost,
                orgnummer = orgnummer,
                begrunnelse = begrunnelse,
                forklaring = forklaring,
                månedligInntekt = månedligInntekt,
                skjæringstidspunkt = skjæringstidspunkt,
                json = json
            ), context
        )
    }

    override fun overstyringArbeidsforhold(
        id: UUID,
        fødselsnummer: String,
        oid: UUID,
        navn: String,
        ident: String,
        epost: String,
        organisasjonsnummer: String,
        overstyrteArbeidsforhold : List<OverstyrArbeidsforholdDto.ArbeidsforholdOverstyrt>,
        skjæringstidspunkt: LocalDate,
        json: String,
        context: MessageContext
    ) {
        utfør(
            fødselsnummer, hendelsefabrikk.overstyringArbeidsforhold(
                id = id,
                fødselsnummer = fødselsnummer,
                oid = oid,
                navn = navn,
                ident = ident,
                epost = epost,
                organisasjonsnummer = organisasjonsnummer,
                overstyrteArbeidsforhold = overstyrteArbeidsforhold,
                skjæringstidspunkt = skjæringstidspunkt,
                json = json
            ), context
        )
    }



    override fun utbetalingAnnullert(
        message: JsonMessage,
        context: MessageContext
    ) {
        utfør(hendelsefabrikk.utbetalingAnnullert(message.toJson()), context)
    }

    override fun utbetalingEndret(
        fødselsnummer: String,
        organisasjonsnummer: String,
        utbetalingId: UUID,
        utbetalingType: Utbetalingtype,
        message: JsonMessage,
        context: MessageContext
    ) {
        if (utbetalingType == Utbetalingtype.UTBETALING && !utbetalingDao.harVærtTilGodkjenning(utbetalingId)) {
            sikkerLogg.info("Ignorerer utbetaling_endret for {}, har ikke vært til godkjenning", keyValue("utbetalingId", utbetalingId))
            return
        }
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

    override fun oppdaterPersonsnapshot(message: JsonMessage, context: MessageContext) {
        utfør(hendelsefabrikk.oppdaterPersonsnapshot(message.toJson()), context)
    }

    override fun avbrytSaksbehandling(message: JsonMessage, context: MessageContext) {
        utfør(hendelsefabrikk.vedtaksperiodeReberegnet(message.toJson()), context)
    }

    fun revurderingAvvist(fødselsnummer: String, error: List<String>, json:String, context: MessageContext) {
        utfør(hendelsefabrikk.revurderingAvvist(fødselsnummer, error, json), context)
    }

    fun håndter(overstyringMessage: OverstyrTidslinjeKafkaDto) {
        overstyringsteller.labels("opplysningstype", "tidslinje").inc()

        val overstyring = JsonMessage.newMessage(
            standardfelter("overstyr_tidslinje", overstyringMessage.fødselsnummer).apply {
                put("aktørId", overstyringMessage.aktørId)
                put("organisasjonsnummer", overstyringMessage.organisasjonsnummer)
                put("dager", overstyringMessage.dager)
                put("begrunnelse", overstyringMessage.begrunnelse)
                put("saksbehandlerOid", overstyringMessage.saksbehandlerOid)
                put("saksbehandlerNavn", overstyringMessage.saksbehandlerNavn)
                put("saksbehandlerIdent", overstyringMessage.saksbehandlerIdent)
                put("saksbehandlerEpost", overstyringMessage.saksbehandlerEpost)
            }
        ).also {
            sikkerLogg.info("Publiserer overstyring:\n${it.toJson()}")
        }

        rapidsConnection.publish(overstyringMessage.fødselsnummer, overstyring.toJson())
    }

    fun håndter(overstyringMessage: OverstyrInntektKafkaDto) {
        overstyringsteller.labels("opplysningstype", "inntekt").inc()

        val overstyring = overstyringMessage.somKafkaMessage().also {
            sikkerLogg.info("Publiserer overstyring av inntekt:\n${it.toJson()}")
        }

        rapidsConnection.publish(overstyringMessage.fødselsnummer, overstyring.toJson())
    }

    fun håndter(overstyringMessage: OverstyrArbeidsforholdKafkaDto) {
        overstyringsteller.labels("opplysningstype", "arbeidsforhold").inc()

        val overstyring = overstyringMessage.somKafkaMessage().also {
            sikkerLogg.info("Publiserer overstyring av arbeidsforhold:\n${it.toJson()}")
        }

        rapidsConnection.publish(overstyringMessage.fødselsnummer, overstyring.toJson())
    }

    internal fun håndter(annulleringDto: AnnulleringDto, saksbehandler: Saksbehandler) {
        annulleringsteller.inc()
        saksbehandler.persister(saksbehandlerDao)

        val annulleringMessage = annulleringDto.run {
            JsonMessage.newMessage(
                standardfelter("annullering", fødselsnummer).apply {
                    putAll(
                        mapOf(
                            "organisasjonsnummer" to organisasjonsnummer,
                            "aktørId" to aktørId,
                            "saksbehandler" to saksbehandler.json().toMutableMap()
                                .apply { put("ident", annulleringDto.saksbehandlerIdent) },
                            "fagsystemId" to fagsystemId,
                            "begrunnelser" to (begrunnelser) as Any,
                            "gjelderSisteSkjæringstidspunkt" to annulleringDto.gjelderSisteSkjæringstidspunkt,
                        ) + (kommentar?.let {
                            mapOf<String, Any>("kommentar" to it)
                        } ?: mapOf()))
                }
            )
        }

        rapidsConnection.publish(annulleringDto.fødselsnummer, annulleringMessage.toJson().also {
            sikkerLogg.info(
                "sender annullering for {}, {}\n\t$it",
                keyValue("fødselsnummer", annulleringDto.fødselsnummer),
                keyValue("organisasjonsnummer", annulleringDto.organisasjonsnummer)
            )
        })
    }

    fun håndter(oppdaterPersonsnapshotDto: OppdaterPersonsnapshotDto) {
        rapidsConnection.publish(
            oppdaterPersonsnapshotDto.fødselsnummer,
            JsonMessage.newMessage(
                standardfelter("oppdater_personsnapshot", oppdaterPersonsnapshotDto.fødselsnummer)
            ).toJson()
        )
        sikkerLogg.info("Publiserte event for å be om siste versjon av person: ${oppdaterPersonsnapshotDto.fødselsnummer}")
    }

    private fun forbered() {
        løsninger = null
    }

    private fun løsninger(hendelseId: UUID, contextId: UUID): Løsninger? {
        return løsninger ?: run {
            val hendelse = hendelseDao.finn(hendelseId, hendelsefabrikk)
            val commandContext = commandContextDao.finnSuspendert(contextId)
            if (hendelse == null || commandContext == null) {
                log.info("finner ikke hendelse med id=$hendelseId eller command context med id=$contextId; ignorerer melding")
                return null
            }
            Løsninger(hendelse, contextId, commandContext).also { løsninger = it }
        }
    }

    // fortsetter en command (resume) med oppsamlet løsninger
    private fun fortsett(message: String, context: MessageContext) {
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

    private fun utfør(vedtaksperiodeId: UUID, hendelse: Hendelse, messageContext: MessageContext) {
        if (!hendelseDao.harKoblingTil(vedtaksperiodeId)) return log.debug("ignorerer hendelseId=${hendelse.id} fordi vi ikke kjenner til $vedtaksperiodeId")
        return utfør(hendelse, messageContext)
    }

    private fun utfør(fødselsnummer: String, hendelse: Hendelse, messageContext: MessageContext) {
        if (personDao.findPersonByFødselsnummer(fødselsnummer) == null) return log.debug("ignorerer hendelseId=${hendelse.id} fordi vi ikke kjenner til personen")
        return utfør(hendelse, messageContext)
    }

    private fun utfør(hendelse: Hendelse, messageContext: MessageContext) {
        val contextId = UUID.randomUUID()
        log.info("oppretter ny kommandokontekst med context_id=$contextId for hendelse_id=${hendelse.id}")
        utfør(hendelse, nyContext(hendelse, contextId), contextId, messageContext)
    }

    private fun utfør(
        hendelse: Hendelse,
        context: CommandContext,
        contextId: UUID,
        messageContext: MessageContext
    ) {
        withMDC(
            mapOf(
                "context_id" to "$contextId",
                "hendelse_id" to "${hendelse.id}",
                "vedtaksperiode_id" to "${hendelse.vedtaksperiodeId() ?: "N/A"}"
            )
        ) {
            try {
                log.info("utfører ${hendelse::class.simpleName} med context_id=$contextId for hendelse_id=${hendelse.id}")
                if (context.utfør(commandContextDao, hendelse)) {
                    log.info(
                        "Kommando(er) for ${hendelse::class.simpleName} er utført ferdig. Det tok ca {}s å kjøre hele kommandokjeden",
                        SECONDS.between(commandContextDao.contextOpprettetTidspunkt(contextId), now())
                    )
                } else log.info("${hendelse::class.simpleName} er suspendert")
                behovMediator.håndter(hendelse, context, contextId)
                oppgaveMediator.lagreOgTildelOppgaver(hendelse.id, hendelse.fødselsnummer(), contextId, messageContext)
            } catch (err: Exception) {
                log.warn(
                    "Feil ved kjøring av ${hendelse::class.simpleName}: contextId={}, message={}",
                    contextId,
                    err.message,
                    err
                )
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

        fun fortsett(mediator: HendelseMediator, message: String, context: MessageContext) {
            log.info("fortsetter utførelse av kommandokontekst pga. behov_id=${hendelse.id} med context_id=$contextId for hendelse_id=${hendelse.id}")
            sikkerLogg.info(
                "fortsetter utførelse av kommandokontekst pga. behov_id=${hendelse.id} med context_id=$contextId for hendelse_id=${hendelse.id}.\n" +
                    "Innkommende melding:\n\t$message"
            )
            mediator.utfør(hendelse, commandContext, contextId, context)
        }
    }
}

internal fun standardfelter(hendelsetype: String, fødselsnummer: String, parent: Pair<String, UUID>? = null) = mutableMapOf<String, Any>(
    "@event_name" to hendelsetype,
    "@opprettet" to now(),
    "@id" to UUID.randomUUID(),
    "fødselsnummer" to fødselsnummer
).apply {
    if (parent != null) {
        this["@forårsaket_av"] = mapOf(
            "event_name" to parent.first,
            "id" to parent.second
        )
    }
}
