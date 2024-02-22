package no.nav.helse.mediator

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import no.nav.helse.db.AvviksvurderingDao
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.SaksbehandlerDao
import no.nav.helse.db.SykefraværstilfelleDao
import no.nav.helse.db.TotrinnsvurderingDao
import no.nav.helse.mediator.builders.GenerasjonBuilder
import no.nav.helse.mediator.meldinger.AdressebeskyttelseEndret
import no.nav.helse.mediator.meldinger.Personhendelse
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.mediator.oppgave.OppgaveMediator
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.HendelseDao
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.arbeidsforhold.ArbeidsforholdDao
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.avviksvurdering.AvviksvurderingDto
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.gosysoppgaver.GosysOppgaveEndret
import no.nav.helse.modell.gosysoppgaver.GosysOppgaveEndretCommand
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.kommando.KobleVedtaksperiodeTilOverstyring
import no.nav.helse.modell.kommando.TilbakedateringGodkjent
import no.nav.helse.modell.kommando.TilbakedateringGodkjentCommand
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.overstyring.OverstyringIgangsatt
import no.nav.helse.modell.overstyring.OverstyrtArbeidsgiver
import no.nav.helse.modell.overstyring.SkjønnsfastsattArbeidsgiver
import no.nav.helse.modell.person.EndretEgenAnsattStatus
import no.nav.helse.modell.person.EndretEgenAnsattStatusCommand
import no.nav.helse.modell.person.OppdaterPersonsnapshot
import no.nav.helse.modell.person.OppdaterPersonsnapshotCommand
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.person.SøknadSendt
import no.nav.helse.modell.person.SøknadSendtCommand
import no.nav.helse.modell.påvent.PåVentDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.saksbehandler.handlinger.OverstyringArbeidsforhold
import no.nav.helse.modell.saksbehandler.handlinger.OverstyringInntektOgRefusjon
import no.nav.helse.modell.saksbehandler.handlinger.SkjønnsfastsettingSykepengegrunnlag
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfeller
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingMediator
import no.nav.helse.modell.utbetaling.LagreOppdragCommand
import no.nav.helse.modell.utbetaling.UtbetalingAnnullert
import no.nav.helse.modell.utbetaling.UtbetalingAnnullertCommand
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.utbetaling.UtbetalingEndret
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.varsel.ActualVarselRepository
import no.nav.helse.modell.varsel.Varsel
import no.nav.helse.modell.vedtaksperiode.ActualGenerasjonRepository
import no.nav.helse.modell.vedtaksperiode.Generasjon
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.NyeVarsler
import no.nav.helse.modell.vedtaksperiode.NyeVarslerCommand
import no.nav.helse.modell.vedtaksperiode.OpprettVedtaksperiodeCommand
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeEndret
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeForkastet
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeNyUtbetaling
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeNyUtbetalingCommand
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeOppdatering
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeOpprettet
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeReberegnet
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeReberegnetCommand
import no.nav.helse.modell.vedtaksperiode.vedtak.Saksbehandlerløsning
import no.nav.helse.modell.vedtaksperiode.vedtak.VedtakFattet
import no.nav.helse.modell.vergemal.VergemålDao
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.helse.spesialist.api.abonnement.OpptegnelseDao
import no.nav.helse.spesialist.api.notat.NotatDao
import no.nav.helse.spesialist.api.notat.NotatMediator
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.saksbehandler.handlinger.OverstyrArbeidsforholdHandlingFraApi
import no.nav.helse.spesialist.api.snapshot.SnapshotClient
import no.nav.helse.spesialist.api.tildeling.TildelingDao
import org.slf4j.LoggerFactory

internal class Hendelsefabrikk(
    dataSource: DataSource,
    private val hendelseDao: HendelseDao = HendelseDao(dataSource),
    private val personDao: PersonDao = PersonDao(dataSource),
    private val arbeidsgiverDao: ArbeidsgiverDao = ArbeidsgiverDao(dataSource),
    private val vedtakDao: VedtakDao = VedtakDao(dataSource),
    private val oppgaveDao: OppgaveDao = OppgaveDao(dataSource),
    private val commandContextDao: CommandContextDao = CommandContextDao(dataSource),
    private val reservasjonDao: ReservasjonDao = ReservasjonDao(dataSource),
    private val tildelingDao: TildelingDao = TildelingDao(dataSource),
    private val saksbehandlerDao: SaksbehandlerDao = SaksbehandlerDao(dataSource),
    private val overstyringDao: OverstyringDao = OverstyringDao(dataSource),
    private val risikovurderingDao: RisikovurderingDao = RisikovurderingDao(dataSource),
    private val åpneGosysOppgaverDao: ÅpneGosysOppgaverDao = ÅpneGosysOppgaverDao(dataSource),
    private val snapshotDao: SnapshotDao = SnapshotDao(dataSource),
    private val egenAnsattDao: EgenAnsattDao = EgenAnsattDao(dataSource),
    private val snapshotClient: SnapshotClient,
    oppgaveMediator: () -> OppgaveMediator,
    private val totrinnsvurderingDao: TotrinnsvurderingDao = TotrinnsvurderingDao(dataSource),
    private val notatDao: NotatDao = NotatDao(dataSource),
    private val notatMediator: NotatMediator = NotatMediator(notatDao),
    private val periodehistorikkDao: PeriodehistorikkDao = PeriodehistorikkDao(dataSource),
    private val påVentDao: PåVentDao = PåVentDao(dataSource),
    private val totrinnsvurderingMediator: TotrinnsvurderingMediator = TotrinnsvurderingMediator(
        totrinnsvurderingDao,
        oppgaveDao,
        periodehistorikkDao,
        notatMediator,
    ),
    private val godkjenningMediator: GodkjenningMediator,
    private val automatisering: Automatisering,
    private val arbeidsforholdDao: ArbeidsforholdDao = ArbeidsforholdDao(dataSource),
    private val utbetalingDao: UtbetalingDao = UtbetalingDao(dataSource),
    private val opptegnelseDao: OpptegnelseDao = OpptegnelseDao(dataSource),
    private val generasjonRepository: ActualGenerasjonRepository = ActualGenerasjonRepository(dataSource),
    private val vergemålDao: VergemålDao = VergemålDao(dataSource),
    private val varselRepository: ActualVarselRepository = ActualVarselRepository(dataSource),
    private val overstyringMediator: OverstyringMediator,
    private val versjonAvKode: String?,
) {
    private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
    private val sykefraværstilfelleDao = SykefraværstilfelleDao(dataSource)
    private val avviksvurderingDao = AvviksvurderingDao(dataSource)
    private val oppgaveMediator: OppgaveMediator by lazy { oppgaveMediator() }

    internal companion object {
        private val mapper = jacksonObjectMapper()
    }

    internal fun sykefraværstilfelle(fødselsnummer: String, skjæringstidspunkt: LocalDate): Sykefraværstilfelle {
        val gjeldendeGenerasjoner = generasjonerFor(fødselsnummer, skjæringstidspunkt)
        val skjønnsfastsatteSykepengegrunnlag = sykefraværstilfelleDao.finnSkjønnsfastsatteSykepengegrunnlag(fødselsnummer, skjæringstidspunkt)
        return Sykefraværstilfelle(fødselsnummer, skjæringstidspunkt, gjeldendeGenerasjoner, skjønnsfastsatteSykepengegrunnlag)
    }

    private fun generasjonerFor(fødselsnummer: String, skjæringstidspunkt: LocalDate): List<Generasjon> {
        return gjeldendeGenerasjoner {
            generasjonRepository.finnVedtaksperiodeIderFor(fødselsnummer, skjæringstidspunkt)
        }
    }

    internal fun generasjonerFor(fødselsnummer: String): List<Generasjon> {
        return gjeldendeGenerasjoner {
            generasjonRepository.finnVedtaksperiodeIderFor(fødselsnummer)
        }
    }

    private fun generasjonerFor(utbetalingId: UUID): List<Generasjon> {
        return gjeldendeGenerasjoner {
            generasjonRepository.finnVedtaksperiodeIderFor(utbetalingId)
        }
    }

    private fun gjeldendeGenerasjoner(iderGetter: () -> Set<UUID>): List<Generasjon> {
        return iderGetter().map {
            gjeldendeGenerasjon(it)
        }
    }

    internal fun gjeldendeGenerasjon(vedtaksperiodeId: UUID): Generasjon {
        return GenerasjonBuilder(vedtaksperiodeId = vedtaksperiodeId).build(generasjonRepository, varselRepository)
    }

    private fun førsteGenerasjon(vedtaksperiodeId: UUID, fom: LocalDate, tom: LocalDate, skjæringstidspunkt: LocalDate): Generasjon {
        return GenerasjonBuilder(vedtaksperiodeId).buildFirst(
            fom = fom,
            tom = tom,
            skjæringstidspunkt = skjæringstidspunkt,
            observers = arrayOf(generasjonRepository, varselRepository)
        )
    }

    internal fun avviksvurdering(avviksvurdering: AvviksvurderingDto) {
        avviksvurderingDao.lagre(avviksvurdering)
    }

    fun godkjenning(
        id: UUID,
        fødselsnummer: String,
        aktørId: String,
        organisasjonsnummer: String,
        periodeFom: LocalDate,
        periodeTom: LocalDate,
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        skjæringstidspunkt: LocalDate,
        periodetype: Periodetype,
        førstegangsbehandling: Boolean,
        utbetalingtype: Utbetalingtype,
        inntektskilde: Inntektskilde,
        orgnummereMedRelevanteArbeidsforhold: List<String>,
        kanAvvises: Boolean,
        json: String,
    ): Godkjenningsbehov {
        return Godkjenningsbehov(
            id = id,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            periodeFom = periodeFom,
            periodeTom = periodeTom,
            periodetype = periodetype,
            førstegangsbehandling = førstegangsbehandling,
            utbetalingtype = utbetalingtype,
            kanAvvises = kanAvvises,
            inntektskilde = inntektskilde,
            orgnummereMedRelevanteArbeidsforhold = orgnummereMedRelevanteArbeidsforhold,
            skjæringstidspunkt = skjæringstidspunkt,
            sykefraværstilfelle = sykefraværstilfelle(fødselsnummer, skjæringstidspunkt),
            json = json,
            personDao = personDao,
            generasjonRepository = generasjonRepository,
            arbeidsgiverDao = arbeidsgiverDao,
            vedtakDao = vedtakDao,
            snapshotDao = snapshotDao,
            commandContextDao = commandContextDao,
            risikovurderingDao = risikovurderingDao,
            åpneGosysOppgaverDao = åpneGosysOppgaverDao,
            egenAnsattDao = egenAnsattDao,
            arbeidsforholdDao = arbeidsforholdDao,
            vergemålDao = vergemålDao,
            snapshotClient = snapshotClient,
            oppgaveMediator = oppgaveMediator,
            automatisering = automatisering,
            godkjenningMediator = godkjenningMediator,
            utbetalingDao = utbetalingDao,
            periodehistorikkDao = periodehistorikkDao,
            overstyringDao = overstyringDao,
            påVentDao = påVentDao,
            totrinnsvurderingMediator = totrinnsvurderingMediator,
        )
    }

    fun søknadSendt(
        id: UUID,
        fødselsnummer: String,
        aktørId: String,
        organisasjonsnummer: String,
        json: String,
    ): SøknadSendt {
        return SøknadSendt(
            id = id,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            organisasjonsnummer = organisasjonsnummer,
            json = json
        )
    }

    fun overstyringIgangsatt(
        json: String,
    ): OverstyringIgangsatt {
        val jsonNode = mapper.readTree(json)
        return OverstyringIgangsatt(
            id = UUID.fromString(jsonNode.path("@id").asText()),
            fødselsnummer = jsonNode.path("fødselsnummer").asText(),
            kilde = UUID.fromString(jsonNode.path("kilde").asText()),
            berørteVedtaksperiodeIder = jsonNode.path("berørtePerioder")
                .map { UUID.fromString(it["vedtaksperiodeId"].asText()) },
            json = json,
        )
    }

    fun overstyringIgangsatt(
        id: UUID,
        fødselsnummer: String,
        kilde: UUID,
        berørteVedtaksperiodeIder: List<UUID>,
        json: String,
    ): OverstyringIgangsatt {
        return OverstyringIgangsatt(
            id = id,
            fødselsnummer = fødselsnummer,
            kilde = kilde,
            berørteVedtaksperiodeIder = berørteVedtaksperiodeIder,
            json = json
        )
    }

    fun godkjenning(json: String): Godkjenningsbehov {
        val jsonNode = mapper.readTree(json)
        val periodetype = Periodetype.valueOf(jsonNode.path("Godkjenning").path("periodetype").asText())
        val førstegangsbehandling = jsonNode.path("Godkjenning").path("førstegangsbehandling")
            .asBoolean(periodetype == Periodetype.FØRSTEGANGSBEHANDLING) // bruker default-value enn så lenge for å kunne parse eldre godkjenningsbehov
        return godkjenning(
            id = UUID.fromString(jsonNode.path("@id").asText()),
            fødselsnummer = jsonNode.path("fødselsnummer").asText(),
            aktørId = jsonNode.path("aktørId").asText(),
            organisasjonsnummer = jsonNode.path("organisasjonsnummer").asText(),
            periodeFom = LocalDate.parse(jsonNode.path("Godkjenning").path("periodeFom").asText()),
            periodeTom = LocalDate.parse(jsonNode.path("Godkjenning").path("periodeTom").asText()),
            vedtaksperiodeId = UUID.fromString(jsonNode.path("vedtaksperiodeId").asText()),
            utbetalingId = UUID.fromString(jsonNode.path("utbetalingId").asText()),
            skjæringstidspunkt = LocalDate.parse(jsonNode.path("Godkjenning").path("skjæringstidspunkt").asText()),
            periodetype = periodetype,
            førstegangsbehandling = førstegangsbehandling,
            utbetalingtype = Utbetalingtype.valueOf(jsonNode.path("Godkjenning").path("utbetalingtype").asText()),
            inntektskilde = Inntektskilde.valueOf(jsonNode.path("Godkjenning").path("inntektskilde").asText()),
            orgnummereMedRelevanteArbeidsforhold = jsonNode.path("Godkjenning")
                .path("orgnummereMedRelevanteArbeidsforhold")
                .takeUnless(JsonNode::isMissingOrNull)?.map { it.asText() } ?: emptyList(),
            kanAvvises = jsonNode.path("Godkjenning").path("kanAvvises").asBoolean(),
            json = json,
        )
    }

    fun saksbehandlerløsning(
        id: UUID,
        behandlingId: UUID,
        godkjenningsbehovhendelseId: UUID,
        fødselsnummer: String,
        godkjent: Boolean,
        saksbehandlerident: String,
        epostadresse: String,
        godkjenttidspunkt: LocalDateTime,
        årsak: String?,
        begrunnelser: List<String>?,
        kommentar: String?,
        saksbehandleroverstyringer: List<UUID>,
        oppgaveId: Long,
        json: String,
    ): Saksbehandlerløsning {
        val vedtaksperiodeId = oppgaveDao.finnVedtaksperiodeId(oppgaveId)
        val skjæringstidspunkt = generasjonRepository.skjæringstidspunktFor(vedtaksperiodeId = vedtaksperiodeId)
        val sykefraværstilfelle = sykefraværstilfelle(fødselsnummer, skjæringstidspunkt)
        return Saksbehandlerløsning(
            id = id,
            behandlingId = behandlingId,
            fødselsnummer = fødselsnummer,
            json = json,
            godkjent = godkjent,
            saksbehandlerIdent = saksbehandlerident,
            epostadresse = epostadresse,
            godkjenttidspunkt = godkjenttidspunkt,
            årsak = årsak,
            begrunnelser = begrunnelser,
            kommentar = kommentar,
            saksbehandleroverstyringer = saksbehandleroverstyringer,
            oppgaveId = oppgaveId,
            godkjenningsbehovhendelseId = godkjenningsbehovhendelseId,
            hendelseDao = hendelseDao,
            oppgaveDao = oppgaveDao,
            godkjenningMediator = godkjenningMediator,
            utbetalingDao = utbetalingDao,
            sykefraværstilfelle = sykefraværstilfelle
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
    ) = OverstyringInntektOgRefusjon(
        id = id,
        fødselsnummer = fødselsnummer,
        oid = oid,
        arbeidsgivere = arbeidsgivere,
        skjæringstidspunkt = skjæringstidspunkt,
        opprettet = opprettet,
        json = json,
        overstyringDao = overstyringDao,
        overstyringMediator = overstyringMediator,
    )

    fun skjønnsfastsettingSykepengegrunnlag(
        id: UUID,
        fødselsnummer: String,
        oid: UUID,
        arbeidsgivere: List<SkjønnsfastsattArbeidsgiver>,
        skjæringstidspunkt: LocalDate,
        opprettet: LocalDateTime,
        json: String,
    ) = SkjønnsfastsettingSykepengegrunnlag(
        id = id,
        fødselsnummer = fødselsnummer,
        oid = oid,
        arbeidsgivere = arbeidsgivere,
        skjæringstidspunkt = skjæringstidspunkt,
        opprettet = opprettet,
        json = json,
        overstyringDao = overstyringDao,
        overstyringMediator = overstyringMediator,
        versjonAvKode = versjonAvKode,
    )

    fun overstyringArbeidsforhold(
        id: UUID,
        fødselsnummer: String,
        oid: UUID,
        overstyrteArbeidsforhold: List<OverstyrArbeidsforholdHandlingFraApi.ArbeidsforholdFraApi>,
        skjæringstidspunkt: LocalDate,
        opprettet: LocalDateTime,
        json: String,
    ) = OverstyringArbeidsforhold(
        id = id,
        fødselsnummer = fødselsnummer,
        oid = oid,
        overstyrteArbeidsforhold = overstyrteArbeidsforhold,
        skjæringstidspunkt = skjæringstidspunkt,
        opprettet = opprettet,
        json = json,
        overstyringDao = overstyringDao,
        overstyringMediator = overstyringMediator,
    )

    fun adressebeskyttelseEndret(id: UUID, fødselsnummer: String, json: String) =
        AdressebeskyttelseEndret(
            id = id,
            fødselsnummer = fødselsnummer,
            json = json,
        )

    fun adressebeskyttelseEndret(json: String): AdressebeskyttelseEndret {
        val jsonNode = mapper.readTree(json)
        return adressebeskyttelseEndret(
            id = UUID.fromString(jsonNode.path("@id").asText()),
            fødselsnummer = jsonNode.path("fødselsnummer").asText(),
            json = json,
        )
    }

    fun sykefraværstilfeller(
        id: UUID,
        vedtaksperiodeOppdateringer: List<VedtaksperiodeOppdatering>,
        fødselsnummer: String,
        aktørId: String,
        json: String,
    ): Sykefraværstilfeller {
        return Sykefraværstilfeller(
            id = id,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            vedtaksperiodeOppdateringer = vedtaksperiodeOppdateringer,
            json = json,
        )
    }

    fun vedtaksperiodeEndret(
        id: UUID,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        forårsaketAvId: UUID,
        forrigeTilstand: String,
        gjeldendeTilstand: String,
        json: String,
    ): VedtaksperiodeEndret {
        return VedtaksperiodeEndret(
            id = id,
            vedtaksperiodeId = vedtaksperiodeId,
            fødselsnummer = fødselsnummer,
            json = json,
            forårsaketAvId = forårsaketAvId,
            forrigeTilstand = forrigeTilstand,
            gjeldendeTilstand = gjeldendeTilstand,
            snapshotDao = snapshotDao,
            snapshotClient = snapshotClient,
            personDao = personDao,
            gjeldendeGenerasjon = gjeldendeGenerasjon(vedtaksperiodeId)
        )
    }

    fun vedtaksperiodeForkastet(
        id: UUID,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        json: String,
    ): VedtaksperiodeForkastet {
        return VedtaksperiodeForkastet(
            id = id,
            vedtaksperiodeId = vedtaksperiodeId,
            fødselsnummer = fødselsnummer,
            json = json,
            commandContextDao = commandContextDao,
            oppgaveMediator = oppgaveMediator,
            snapshotClient = snapshotClient,
            snapshotDao = snapshotDao,
            personDao = personDao,
            vedtakDao = vedtakDao
        )
    }

    fun vedtaksperiodeForkastet(json: String): VedtaksperiodeForkastet {
        val jsonNode = mapper.readTree(json)
        return vedtaksperiodeForkastet(
            id = UUID.fromString(jsonNode.path("@id").asText()),
            vedtaksperiodeId = UUID.fromString(jsonNode.path("vedtaksperiodeId").asText()),
            fødselsnummer = jsonNode.path("fødselsnummer").asText(),
            json = json
        )
    }

    fun utbetalingAnnullert(json: String): UtbetalingAnnullert {
        val jsonNode = mapper.readTree(json)
        return UtbetalingAnnullert(
            id = UUID.fromString(jsonNode["@id"].asText()),
            fødselsnummer = jsonNode["fødselsnummer"].asText(),
            utbetalingId = UUID.fromString(jsonNode["utbetalingId"].asText()),
            annullertTidspunkt = LocalDateTime.parse(jsonNode["tidspunkt"].asText()),
            saksbehandlerEpost = jsonNode["epost"].asText(),
            json = json,
        )
    }

    fun utbetalingEndret(json: String): UtbetalingEndret {
        val jsonNode = mapper.readTree(json)
        val utbetalingId = UUID.fromString(jsonNode.path("utbetalingId").asText())
        return UtbetalingEndret(
            id = UUID.fromString(jsonNode.path("@id").asText()),
            fødselsnummer = jsonNode.path("fødselsnummer").asText(),
            orgnummer = jsonNode.path("organisasjonsnummer").asText(),
            utbetalingId = utbetalingId,
            type = jsonNode.path("type").asText(),
            gjeldendeStatus = Utbetalingsstatus.valueOf(jsonNode.path("gjeldendeStatus").asText()),
            opprettet = jsonNode.path("@opprettet").asLocalDateTime(),
            arbeidsgiverbeløp = jsonNode.path("arbeidsgiverOppdrag").path("nettoBeløp").asInt(),
            personbeløp = jsonNode.path("personOppdrag").path("nettoBeløp").asInt(),
            arbeidsgiverOppdrag = tilOppdrag(jsonNode.path("arbeidsgiverOppdrag"), jsonNode.path("organisasjonsnummer").asText()),
            personOppdrag = tilOppdrag(jsonNode.path("personOppdrag"), jsonNode.path("fødselsnummer").asText()),
            json = json,
            utbetalingDao = utbetalingDao,
            opptegnelseDao = opptegnelseDao,
            oppgaveDao = oppgaveDao,
            reservasjonDao = reservasjonDao,
            tildelingDao = tildelingDao,
            oppgaveMediator = oppgaveMediator,
            totrinnsvurderingMediator = totrinnsvurderingMediator,
            gjeldendeGenerasjoner = generasjonerFor(utbetalingId)
        )
    }

    fun vedtaksperiodeNyUtbetaling(
        hendelseId: UUID,
        fødselsnummer: String,
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        json: String,
    ): VedtaksperiodeNyUtbetaling {
        return VedtaksperiodeNyUtbetaling(
            hendelseId,
            fødselsnummer,
            vedtaksperiodeId,
            utbetalingId,
            json,
        )
    }

    private fun tilOppdrag(jsonNode: JsonNode, mottaker: String) = LagreOppdragCommand.Oppdrag(
        fagsystemId = jsonNode.path("fagsystemId").asText(),
        mottaker = jsonNode.path("mottaker").takeIf(JsonNode::isTextual)?.asText() ?: mottaker,
        linjer = jsonNode.path("linjer").map { linje ->
            LagreOppdragCommand.Oppdrag.Utbetalingslinje(
                fom = linje.path("fom").asLocalDate(),
                tom = linje.path("tom").asLocalDate(),
                totalbeløp = linje.path("totalbeløp").takeIf(JsonNode::isInt)?.asInt()
            )
        }
    )

    fun oppdaterPersonsnapshot(json: String): OppdaterPersonsnapshot {
        val jsonNode = mapper.readTree(json)
        val fødselsnummer = jsonNode["fødselsnummer"].asText()
        return OppdaterPersonsnapshot(
            id = UUID.fromString(jsonNode["@id"].asText()),
            fødselsnummer = fødselsnummer,
            json = json,
        )
    }

    fun endretEgenAnsattStatus(json: String): EndretEgenAnsattStatus {
        val erEgenAnsatt = mapper.readTree(json)["skjermet"].asBoolean()
        val jsonNode = mapper.readTree(json)
        return EndretEgenAnsattStatus(
            id = UUID.fromString(jsonNode["@id"].asText()),
            fødselsnummer = jsonNode["fødselsnummer"].asText(),
            erEgenAnsatt = erEgenAnsatt,
            json = json,
        )
    }

    fun endretEgenAnsattStatus(fødselsnummer: String, erEgenAnsatt: Boolean): EndretEgenAnsattStatusCommand {
        return EndretEgenAnsattStatusCommand(
            fødselsnummer = fødselsnummer,
            erEgenAnsatt = erEgenAnsatt,
            oppgaveMediator = oppgaveMediator
        )
    }

    fun opprettVedtaksperiode(fødselsnummer: String, hendelse: VedtaksperiodeOpprettet): OpprettVedtaksperiodeCommand {
        return OpprettVedtaksperiodeCommand(
            id = hendelse.id,
            vedtaksperiodeId = hendelse.vedtaksperiodeId(),
            fødselsnummer = fødselsnummer,
            generasjon = førsteGenerasjon(hendelse.vedtaksperiodeId(), hendelse.fom, hendelse.tom, hendelse.skjæringstidspunkt),
            organisasjonsnummer = hendelse.organisasjonsnummer,
            fom = hendelse.fom,
            tom = hendelse.tom,
            personDao = personDao,
            arbeidsgiverDao = arbeidsgiverDao,
            vedtakDao = vedtakDao
        )
    }

    fun gosysOppgaveEndret(fødselsnummer: String, hendelse: GosysOppgaveEndret): GosysOppgaveEndretCommand {
        val oppgaveDataForAutomatisering = oppgaveDao.finnOppgaveIdUansettStatus(fødselsnummer).let { oppgaveId ->
            oppgaveDao.oppgaveDataForAutomatisering(oppgaveId)!!
        }

        val skjæringstidspunkt = gjeldendeGenerasjon(oppgaveDataForAutomatisering.vedtaksperiodeId).skjæringstidspunkt()

        val utbetaling = utbetalingDao.hentUtbetaling(oppgaveDataForAutomatisering.utbetalingId)
        val oppgaveId by lazy { oppgaveDao.finnOppgaveId(fødselsnummer) }
        val harTildeltOppgave = oppgaveId?.let { tildelingDao.tildelingForOppgave(it) != null } ?: false

        return GosysOppgaveEndretCommand(
            id = hendelse.id,
            fødselsnummer = fødselsnummer,
            aktørId = hendelse.aktørId,
            utbetaling = utbetaling,
            sykefraværstilfelle = sykefraværstilfelle(fødselsnummer, skjæringstidspunkt),
            harTildeltOppgave = harTildeltOppgave,
            oppgavedataForAutomatisering = oppgaveDataForAutomatisering,
            automatisering = automatisering,
            åpneGosysOppgaverDao = åpneGosysOppgaverDao,
            oppgaveDao = oppgaveDao,
            oppgaveMediator = oppgaveMediator,
            godkjenningMediator = godkjenningMediator
        )
    }

    fun nyeVarsler(fødselsnummer: String, hendelse: NyeVarsler): NyeVarslerCommand {
        return NyeVarslerCommand(
            hendelse.id,
            fødselsnummer,
            generasjonerFor(fødselsnummer),
            hendelse.varsler,
        )
    }

    fun tilbakedateringGodkjent(fødselsnummer: String): TilbakedateringGodkjentCommand {
        val oppgaveDataForAutomatisering = oppgaveDao.finnOppgaveIdUansettStatus(fødselsnummer).let { oppgaveId ->
            oppgaveDao.oppgaveDataForAutomatisering(oppgaveId)!!
        }
        val sykefraværstilfelle = sykefraværstilfelle(fødselsnummer, oppgaveDataForAutomatisering.skjæringstidspunkt)
        val utbetaling = utbetalingDao.hentUtbetaling(oppgaveDataForAutomatisering.utbetalingId)

        sikkerLog.info("Henter oppgaveDataForAutomatisering ifm. godkjent tilbakedatering for fnr $fødselsnummer og vedtaksperiodeId ${oppgaveDataForAutomatisering.vedtaksperiodeId}")

        return TilbakedateringGodkjentCommand(
            fødselsnummer = fødselsnummer,
            sykefraværstilfelle = sykefraværstilfelle,
            utbetaling = utbetaling,
            automatisering = automatisering,
            oppgaveDataForAutomatisering = oppgaveDataForAutomatisering,
            oppgaveDao = oppgaveDao,
            oppgaveMediator = oppgaveMediator,
            godkjenningMediator = godkjenningMediator
        )
    }

    fun vedtaksperiodeReberegnet(hendelse: VedtaksperiodeReberegnet): VedtaksperiodeReberegnetCommand {
        return VedtaksperiodeReberegnetCommand(
            vedtaksperiodeId = hendelse.vedtaksperiodeId(),
            utbetalingDao = utbetalingDao,
            periodehistorikkDao = periodehistorikkDao,
            commandContextDao = commandContextDao,
            oppgaveMediator = oppgaveMediator
        )
    }

    fun vedtaksperiodeNyUtbetaling(hendelse: VedtaksperiodeNyUtbetaling): VedtaksperiodeNyUtbetalingCommand {
        return VedtaksperiodeNyUtbetalingCommand(
            id = hendelse.id,
            vedtaksperiodeId = hendelse.vedtaksperiodeId(),
            utbetalingId = hendelse.utbetalingId,
            utbetalingDao = utbetalingDao,
            gjeldendeGenerasjon = gjeldendeGenerasjon(hendelse.vedtaksperiodeId())
        )
    }

    fun søknadSendt(hendelse: SøknadSendt): SøknadSendtCommand {
        return SøknadSendtCommand(
            fødselsnummer = hendelse.fødselsnummer(),
            aktørId = hendelse.aktørId,
            organisasjonsnummer = hendelse.organisasjonsnummer,
            personDao = personDao,
            arbeidsgiverDao = arbeidsgiverDao
        )
    }

    fun oppdaterPersonsnapshot(hendelse: Personhendelse): OppdaterPersonsnapshotCommand {
        return OppdaterPersonsnapshotCommand(
            fødselsnummer = hendelse.fødselsnummer(),
            førsteKjenteDagFinner = { generasjonRepository.førsteKjenteDag(hendelse.fødselsnummer()) },
            personDao = personDao,
            snapshotDao = snapshotDao,
            opptegnelseDao = opptegnelseDao,
            snapshotClient = snapshotClient
        )
    }

    fun kobleVedtaksperiodeTilOverstyring(hendelse: OverstyringIgangsatt): KobleVedtaksperiodeTilOverstyring {
        return KobleVedtaksperiodeTilOverstyring(
            berørteVedtaksperiodeIder = hendelse.berørteVedtaksperiodeIder,
            kilde = hendelse.kilde,
            overstyringDao = overstyringDao,
        )
    }

    fun utbetalingAnnullert(hendelse: UtbetalingAnnullert): UtbetalingAnnullertCommand {
        return UtbetalingAnnullertCommand(
            fødselsnummer = hendelse.fødselsnummer(),
            utbetalingId = hendelse.utbetalingId,
            saksbehandlerEpost = hendelse.saksbehandlerEpost,
            annullertTidspunkt = hendelse.annullertTidspunkt,
            utbetalingDao = utbetalingDao,
            personDao = personDao,
            snapshotDao = snapshotDao,
            snapshotClient = snapshotClient,
            saksbehandlerDao = saksbehandlerDao
        )
    }

    fun vedtaksperiodeReberegnet(json: String): VedtaksperiodeReberegnet {
        val jsonNode = mapper.readTree(json)
        return VedtaksperiodeReberegnet(
            id = UUID.fromString(jsonNode["@id"].asText()),
            fødselsnummer = jsonNode["fødselsnummer"].asText(),
            vedtaksperiodeId = UUID.fromString(jsonNode.path("vedtaksperiodeId").asText()),
            json = json,
        )
    }

    fun vedtaksperiodeOpprettet(
        id: UUID,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
        fom: LocalDate,
        tom: LocalDate,
        skjæringstidspunkt: LocalDate,
        json: String,
    ): VedtaksperiodeOpprettet {
        return VedtaksperiodeOpprettet(
            id = id,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            fom = fom,
            tom = tom,
            skjæringstidspunkt = skjæringstidspunkt,
            json = json,
        )
    }

    fun godkjentTilbakedatertSykmelding(
        id: UUID,
        fødselsnummer: String,
        json: String,
    ): TilbakedateringGodkjent {


        return TilbakedateringGodkjent(
            id = id,
            fødselsnummer = fødselsnummer,
            json = json,
        )
    }

    fun gosysOppgaveEndret(json: String): GosysOppgaveEndret {
        val jsonNode = mapper.readTree(json)
        val fødselsnummer = jsonNode["fødselsnummer"].asText()
        return gosysOppgaveEndret(
            UUID.fromString(jsonNode["@id"].asText()),
            fødselsnummer,
            requireNotNull(personDao.finnAktørId(fødselsnummer)),
            json
        )
    }

    fun gosysOppgaveEndret(hendelseId: UUID, fødselsnummer: String, aktørId: String, json: String): GosysOppgaveEndret {
        // Vi kan ikke sende med oss dataene ned i løypa, så derfor må vi hente det ut på nytt her.
        val commandData = oppgaveDao.finnOppgaveIdUansettStatus(fødselsnummer).let { oppgaveId ->
            oppgaveDao.oppgaveDataForAutomatisering(oppgaveId)!!
        }

        sikkerLog.info("Gjør ny sjekk om det finnes åpne gosysoppgaver for fnr $fødselsnummer og vedtaksperiodeId ${commandData.vedtaksperiodeId}")
        return GosysOppgaveEndret(
            id = hendelseId,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            json = json,
        )
    }

    fun vedtakFattet(id: UUID, fødselsnummer: String, vedtaksperiodeId: UUID, json: String): VedtakFattet {
        return VedtakFattet(id, fødselsnummer, vedtaksperiodeId, json, gjeldendeGenerasjon(vedtaksperiodeId), vedtakDao)
    }

    fun nyeVarsler(id: UUID, fødselsnummer: String, varsler: List<Varsel>, json: String): NyeVarsler {
        return NyeVarsler(id, fødselsnummer, varsler, json)
    }

}
