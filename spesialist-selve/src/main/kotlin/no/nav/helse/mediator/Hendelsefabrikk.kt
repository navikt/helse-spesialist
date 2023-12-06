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
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.overstyring.OverstyringIgangsatt
import no.nav.helse.modell.overstyring.OverstyrtArbeidsgiver
import no.nav.helse.modell.overstyring.OverstyrtArbeidsgiver.Companion.arbeidsgiverelementer
import no.nav.helse.modell.overstyring.SkjønnsfastsattArbeidsgiver
import no.nav.helse.modell.person.EndretSkjermetinfo
import no.nav.helse.modell.person.OppdaterPersonsnapshot
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.person.SøknadSendt
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
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.utbetaling.UtbetalingEndret
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.varsel.ActualVarselRepository
import no.nav.helse.modell.varsel.Varsel
import no.nav.helse.modell.varsel.Varsel.Companion.varsler
import no.nav.helse.modell.vedtaksperiode.ActualGenerasjonRepository
import no.nav.helse.modell.vedtaksperiode.Generasjon
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.NyeVarsler
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.PåminnetGodkjenningsbehov
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeEndret
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeForkastet
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeNyUtbetaling
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeOppdatering
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeOpprettet
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeReberegnet
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeSkjønnsmessigFastsettelse
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
import no.nav.helse.spesialist.api.snapshot.SnapshotMediator
import no.nav.helse.spesialist.api.tildeling.TildelingDao
import org.slf4j.LoggerFactory
import no.nav.helse.modell.overstyring.SkjønnsfastsattArbeidsgiver.Companion.arbeidsgiverelementer as skjønnsfastsattArbeidsgiverelementer

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
    private val snapshotMediator: SnapshotMediator,
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

    private fun generasjonerFor(fødselsnummer: String): List<Generasjon> {
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

    private fun gjeldendeGenerasjon(vedtaksperiodeId: UUID): Generasjon {
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
            snapshotMediator = snapshotMediator,
        )
    }

    fun påminnetGodkjenningsbehov(id: UUID, fødselsnummer: String, json: String) =
        PåminnetGodkjenningsbehov(
            id = id,
            fødselsnummer = fødselsnummer,
            json = json,
            snapshotDao = snapshotDao,
            snapshotClient = snapshotClient,
        )

    fun søknadSendt(
        json: String,
    ): SøknadSendt {
        val jsonNode = mapper.readTree(json)
        return SøknadSendt(
            id = UUID.fromString(jsonNode.path("@id").asText()),
            fødselsnummer = jsonNode.path("fødselsnummer").asText(),
            aktørId = jsonNode.path("aktorId").asText(),
            organisasjonsnummer = jsonNode.path("organisasjonsnummer").asText(),
            json = json,
            personDao = personDao,
            arbeidsgiverDao = arbeidsgiverDao
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
            json = json,
            personDao = personDao,
            arbeidsgiverDao = arbeidsgiverDao
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
            overstyringDao = overstyringDao,
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
            json = json,
            overstyringDao = overstyringDao
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

    fun påminnetGodkjenningsbehov(json: String): PåminnetGodkjenningsbehov {
        val jsonNode = mapper.readTree(json)
        return påminnetGodkjenningsbehov(
            id = UUID.fromString(jsonNode.path("@id").asText()),
            fødselsnummer = jsonNode.path("fødselsnummer").asText(),
            json = json
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

    fun saksbehandlerløsning(json: String): Saksbehandlerløsning {
        val jsonNode = mapper.readTree(json)
        return saksbehandlerløsning(
            id = UUID.fromString(jsonNode["@id"].asText()),
            behandlingId = UUID.fromString(jsonNode["behandlingId"].asText()),
            godkjenningsbehovhendelseId = UUID.fromString(jsonNode["hendelseId"].asText()),
            fødselsnummer = jsonNode["fødselsnummer"].asText(),
            godkjent = jsonNode["godkjent"].asBoolean(),
            saksbehandlerident = jsonNode["saksbehandlerident"].asText(),
            epostadresse = jsonNode["saksbehandlerepost"].asText(),
            godkjenttidspunkt = jsonNode["godkjenttidspunkt"].asLocalDateTime(),
            årsak = jsonNode["årsak"].takeUnless(JsonNode::isMissingOrNull)?.asText(),
            begrunnelser = jsonNode["begrunnelser"].takeUnless(JsonNode::isMissingOrNull)?.map(JsonNode::asText),
            kommentar = jsonNode["kommentar"].takeUnless(JsonNode::isMissingOrNull)?.asText(),
            saksbehandleroverstyringer = jsonNode["saksbehandleroverstyringer"].takeUnless(JsonNode::isMissingOrNull)
            ?.map {
                UUID.fromString(it.asText())
            } ?: emptyList(),
            oppgaveId = jsonNode["oppgaveId"].asLong(),
            json = json
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

    fun overstyringArbeidsforhold(json: String): OverstyringArbeidsforhold {
        val jsonNode = mapper.readTree(json)
        return overstyringArbeidsforhold(
            id = UUID.fromString(jsonNode.path("@id").asText()),
            fødselsnummer = jsonNode.path("fødselsnummer").asText(),
            oid = UUID.fromString(jsonNode.path("saksbehandlerOid").asText()),
            overstyrteArbeidsforhold = jsonNode.path("overstyrteArbeidsforhold").map {
                OverstyrArbeidsforholdHandlingFraApi.ArbeidsforholdFraApi(
                    it["orgnummer"].asText(),
                    it["deaktivert"].asBoolean(),
                    it["begrunnelse"].asText(),
                    it["forklaring"].asText()
                )
            },
            skjæringstidspunkt = jsonNode.path("skjæringstidspunkt").asLocalDate(),
            opprettet = jsonNode.path("@opprettet").asLocalDateTime(),
            json = json
        )
    }

    fun overstyringInntektOgRefusjon(json: String): OverstyringInntektOgRefusjon {
        val jsonNode = mapper.readTree(json)
        return overstyringInntektOgRefusjon(
            id = UUID.fromString(jsonNode.path("@id").asText()),
            fødselsnummer = jsonNode.path("fødselsnummer").asText(),
            oid = UUID.fromString(jsonNode.path("saksbehandlerOid").asText()),
            arbeidsgivere = jsonNode.path("arbeidsgivere").arbeidsgiverelementer(),
            skjæringstidspunkt = jsonNode.path("skjæringstidspunkt").asLocalDate(),
            opprettet = jsonNode.path("@opprettet").asLocalDateTime(),
            json = json
        )
    }

    fun skjønnsfastsettingSykepengegrunnlag(json: String): SkjønnsfastsettingSykepengegrunnlag {
        val jsonNode = mapper.readTree(json)
        return skjønnsfastsettingSykepengegrunnlag(
            id = UUID.fromString(jsonNode.path("@id").asText()),
            fødselsnummer = jsonNode.path("fødselsnummer").asText(),
            oid = UUID.fromString(jsonNode.path("saksbehandlerOid").asText()),
            arbeidsgivere = jsonNode.path("arbeidsgivere").skjønnsfastsattArbeidsgiverelementer(),
            skjæringstidspunkt = jsonNode.path("skjæringstidspunkt").asLocalDate(),
            opprettet = jsonNode.path("@opprettet").asLocalDateTime(),
            json = json
        )
    }

    fun adressebeskyttelseEndret(id: UUID, fødselsnummer: String, json: String) =
        AdressebeskyttelseEndret(
            id,
            fødselsnummer,
            json,
            personDao,
            oppgaveDao,
            godkjenningMediator,
        )

    fun adressebeskyttelseEndret(json: String): AdressebeskyttelseEndret {
        val jsonNode = mapper.readTree(json)
        return AdressebeskyttelseEndret(
            id = UUID.fromString(jsonNode.path("@id").asText()),
            fødselsnummer = jsonNode.path("fødselsnummer").asText(),
            json = json,
            personDao = personDao,
            oppgaveDao = oppgaveDao,
            godkjenningMediator = godkjenningMediator,
        )
    }

    fun sykefraværstilfeller(
        id: UUID,
        vedtaksperiodeOppdateringer: List<VedtaksperiodeOppdatering>,
        fødselsnummer: String,
        aktørId: String,
        json: String,
    ): Sykefraværstilfeller {
        val generasjoner = generasjonerFor(fødselsnummer)
        return Sykefraværstilfeller(
            id = id,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            vedtaksperiodeOppdateringer = vedtaksperiodeOppdateringer,
            gjeldendeGenerasjoner = generasjoner,
            json = json,
        )
    }

    fun sykefraværstilfeller(json: String): Sykefraværstilfeller {
        val jsonNode = mapper.readTree(json)
        return sykefraværstilfeller(
            id = UUID.fromString(jsonNode["@id"].asText()),
            vedtaksperiodeOppdateringer = jsonNode["tilfeller"].flatMap { tilfelleNode ->
                val skjæringstidspunkt = tilfelleNode["dato"].asLocalDate()
                tilfelleNode["perioder"].map {
                    VedtaksperiodeOppdatering(
                        skjæringstidspunkt = skjæringstidspunkt,
                        fom = LocalDate.parse(it["fom"].asText()),
                        tom = LocalDate.parse(it["tom"].asText()),
                        vedtaksperiodeId = UUID.fromString(it["vedtaksperiodeId"].asText()),
                    )
                }
            },
            fødselsnummer = jsonNode.path("fødselsnummer").asText(),
            aktørId = jsonNode.path("aktørId").asText(),
            json = json
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

    fun vedtaksperiodeEndret(json: String): VedtaksperiodeEndret {
        val jsonNode = mapper.readTree(json)
        return vedtaksperiodeEndret(
            id = UUID.fromString(jsonNode.path("@id").asText()),
            vedtaksperiodeId = UUID.fromString(jsonNode.path("vedtaksperiodeId").asText()),
            fødselsnummer = jsonNode.path("fødselsnummer").asText(),
            forårsaketAvId = UUID.fromString(jsonNode.path("@forårsaket_av.id").asText()),
            forrigeTilstand = jsonNode.path("forrigeTilstand").asText(),
            gjeldendeTilstand = jsonNode.path("gjeldendeTilstand").asText(),
            json = json
        )
    }

    fun vedtaksperiodeSkjønnsmessigFastsettelse(
        id: UUID,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        aktørId: String,
        organisasjonsnummer: String,
        json: String,
    ): VedtaksperiodeSkjønnsmessigFastsettelse {
        return VedtaksperiodeSkjønnsmessigFastsettelse(
            id = id,
            vedtaksperiodeId = vedtaksperiodeId,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            json = json,
            organisasjonsnummer = organisasjonsnummer,
            snapshotDao = snapshotDao,
            snapshotClient = snapshotClient,
            personDao = personDao,
            generasjonRepository = generasjonRepository,
            arbeidsgiverDao = arbeidsgiverDao,
            arbeidsforholdDao = arbeidsforholdDao,
            egenAnsattDao = egenAnsattDao,
        )
    }

    fun vedtaksperiodeSkjønnsmessigFastsettelse(json: String): VedtaksperiodeSkjønnsmessigFastsettelse {
        val jsonNode = mapper.readTree(json)
        return vedtaksperiodeSkjønnsmessigFastsettelse(
            id = UUID.fromString(jsonNode.path("@id").asText()),
            vedtaksperiodeId = UUID.fromString(jsonNode.path("vedtaksperiodeId").asText()),
            fødselsnummer = jsonNode.path("fødselsnummer").asText(),
            aktørId = jsonNode.path("aktørId").asText(),
            organisasjonsnummer = jsonNode.path("organisasjonsnummer").asText(),
            json = json
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
            utbetalingDao = utbetalingDao,
            saksbehandlerDao = saksbehandlerDao,
            snapshotClient = snapshotClient,
            snapshotDao = snapshotDao,
            personDao = personDao,
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
            utbetalingDao,
            gjeldendeGenerasjon(vedtaksperiodeId),
        )
    }

    fun vedtaksperiodeNyUtbetaling(json: String): VedtaksperiodeNyUtbetaling {
        val jsonNode = mapper.readTree(json)
        return vedtaksperiodeNyUtbetaling(
            hendelseId = UUID.fromString(jsonNode.path("@id").asText()),
            fødselsnummer = jsonNode.path("fødselsnummer").asText(),
            vedtaksperiodeId = UUID.fromString(jsonNode.path("vedtaksperiodeId").asText()),
            utbetalingId = UUID.fromString(jsonNode.path("utbetalingId").asText()),
            json = json,
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
            snapshotClient = snapshotClient,
            snapshotDao = snapshotDao,
            personDao = personDao,
            opptegnelseDao = opptegnelseDao,
            førsteKjenteDagFinner = { generasjonRepository.førsteKjenteDag(fødselsnummer) },
        )
    }

    fun endretSkjermetinfo(json: String): EndretSkjermetinfo {
        val erEgenAnsatt = mapper.readTree(json)["skjermet"].asBoolean()
        val jsonNode = mapper.readTree(json)
        return EndretSkjermetinfo(
            id = UUID.fromString(jsonNode["@id"].asText()),
            fødselsnummer = jsonNode["fødselsnummer"].asText(),
            erEgenAnsatt = erEgenAnsatt,
            json = json,
            oppgaveMediator = oppgaveMediator,
        )
    }

    fun vedtaksperiodeReberegnet(json: String): VedtaksperiodeReberegnet {
        val jsonNode = mapper.readTree(json)
        return VedtaksperiodeReberegnet(
            id = UUID.fromString(jsonNode["@id"].asText()),
            fødselsnummer = jsonNode["fødselsnummer"].asText(),
            vedtaksperiodeId = UUID.fromString(jsonNode.path("vedtaksperiodeId").asText()),
            json = json,
            commandContextDao = commandContextDao,
            oppgaveMediator = oppgaveMediator,
            periodehistorikkDao = periodehistorikkDao,
            utbetalingDao = utbetalingDao,
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
            personDao = personDao,
            arbeidsgiverDao = arbeidsgiverDao,
            vedtakDao = vedtakDao,
            generasjon = førsteGenerasjon(vedtaksperiodeId, fom, tom, skjæringstidspunkt),
            json = json,
        )
    }

    fun vedtaksperiodeOpprettet(json: String): VedtaksperiodeOpprettet {
        val jsonNode = mapper.readTree(json)
        return vedtaksperiodeOpprettet(
            id = UUID.fromString(jsonNode["@id"].asText()),
            fødselsnummer = jsonNode["fødselsnummer"].asText(),
            organisasjonsnummer = jsonNode.path("organisasjonsnummer").asText(),
            vedtaksperiodeId = UUID.fromString(jsonNode.path("vedtaksperiodeId").asText()),
            fom = LocalDate.parse(jsonNode.path("fom").asText()),
            tom = LocalDate.parse(jsonNode.path("tom").asText()),
            skjæringstidspunkt = LocalDate.parse(jsonNode.path("skjæringstidspunkt").asText()),
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
            oppgaveDao.gosysOppgaveEndretCommandData(oppgaveId)!!
        }

        sikkerLog.info("Gjør ny sjekk om det finnes åpne gosysoppgaver for fnr $fødselsnummer og vedtaksperiodeId ${commandData.vedtaksperiodeId}")
        return GosysOppgaveEndret(
            id = hendelseId,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            sykefraværstilfelle = sykefraværstilfelle(fødselsnummer, commandData.skjæringstidspunkt),
            json = json,
            gosysOppgaveEndretCommandData = commandData,
            åpneGosysOppgaverDao = åpneGosysOppgaverDao,
            automatisering = automatisering,
            godkjenningMediator = godkjenningMediator,
            oppgaveMediator = oppgaveMediator,
            oppgaveDao = oppgaveDao,
            utbetalingDao = utbetalingDao,
            tildelingDao = tildelingDao,
        )
    }

    fun vedtakFattet(id: UUID, fødselsnummer: String, vedtaksperiodeId: UUID, json: String): VedtakFattet {
        return VedtakFattet(id, fødselsnummer, vedtaksperiodeId, json, gjeldendeGenerasjon(vedtaksperiodeId), vedtakDao)
    }

    fun vedtakFattet(json: String): VedtakFattet {
        val jsonNode = mapper.readTree(json)
        return vedtakFattet(
            id = UUID.fromString(jsonNode.path("@id").asText()),
            fødselsnummer = jsonNode.path("fødselsnummer").asText(),
            vedtaksperiodeId = UUID.fromString(jsonNode.path("vedtaksperiodeId").asText()),
            json
        )
    }

    fun nyeVarsler(id: UUID, fødselsnummer: String, varsler: List<Varsel>, json: String): NyeVarsler {
        return NyeVarsler(id, fødselsnummer, varsler, generasjonerFor(fødselsnummer), json)
    }

    fun nyeVarsler(json: String): NyeVarsler {
        val jsonNode = mapper.readTree(json)
        return nyeVarsler(
            id = UUID.fromString(jsonNode.path("@id").asText()),
            fødselsnummer = jsonNode.path("fødselsnummer").asText(),
            varsler = jsonNode.path("aktiviteter").varsler(),
            json,
        )
    }
}
