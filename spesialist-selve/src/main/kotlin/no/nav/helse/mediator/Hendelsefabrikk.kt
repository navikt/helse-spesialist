package no.nav.helse.mediator

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import no.nav.helse.mediator.api.OverstyrArbeidsforholdDto
import no.nav.helse.mediator.meldinger.AdressebeskyttelseEndret
import no.nav.helse.mediator.meldinger.EndretSkjermetinfo
import no.nav.helse.mediator.meldinger.Godkjenningsbehov
import no.nav.helse.mediator.meldinger.GosysOppgaveEndret
import no.nav.helse.mediator.meldinger.OppdaterPersonsnapshot
import no.nav.helse.mediator.meldinger.OverstyringArbeidsforhold
import no.nav.helse.mediator.meldinger.OverstyringInntekt
import no.nav.helse.mediator.meldinger.OverstyringTidslinje
import no.nav.helse.mediator.meldinger.RevurderingAvvist
import no.nav.helse.mediator.meldinger.Saksbehandlerløsning
import no.nav.helse.mediator.meldinger.UtbetalingAnnullert
import no.nav.helse.mediator.meldinger.UtbetalingEndret
import no.nav.helse.mediator.meldinger.VedtaksperiodeEndret
import no.nav.helse.mediator.meldinger.VedtaksperiodeForkastet
import no.nav.helse.mediator.meldinger.VedtaksperiodeReberegnet
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.HendelseDao
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.arbeidsforhold.ArbeidsforholdDao
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.dkif.DigitalKontaktinformasjonDao
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.oppgave.OppgaveDao
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.utbetaling.LagreOppdragCommand
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vergemal.VergemålDao
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.helse.spesialist.api.abonnement.OpptegnelseDao
import no.nav.helse.modell.oppgave.OppgaveMediator
import no.nav.helse.spesialist.api.overstyring.Dagtype
import no.nav.helse.spesialist.api.overstyring.OverstyringDagDto
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.reservasjon.ReservasjonDao
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerDao
import no.nav.helse.spesialist.api.snapshot.SnapshotClient
import no.nav.helse.spesialist.api.tildeling.TildelingDao
import org.slf4j.LoggerFactory

internal class Hendelsefabrikk(
    dataSource: DataSource,
    private val hendelseDao: HendelseDao = HendelseDao(dataSource),
    private val personDao: PersonDao = PersonDao(dataSource),
    private val arbeidsgiverDao: ArbeidsgiverDao = ArbeidsgiverDao(dataSource),
    private val vedtakDao: VedtakDao = VedtakDao(dataSource),
    private val warningDao: WarningDao = WarningDao(dataSource),
    private val oppgaveDao: OppgaveDao = OppgaveDao(dataSource),
    private val commandContextDao: CommandContextDao = CommandContextDao(dataSource),
    private val reservasjonDao: ReservasjonDao = ReservasjonDao(dataSource),
    private val tildelingDao: TildelingDao = TildelingDao(dataSource),
    private val saksbehandlerDao: SaksbehandlerDao = SaksbehandlerDao(dataSource),
    private val overstyringDao: OverstyringDao = OverstyringDao(dataSource),
    private val risikovurderingDao: RisikovurderingDao = RisikovurderingDao(dataSource),
    private val digitalKontaktinformasjonDao: DigitalKontaktinformasjonDao = DigitalKontaktinformasjonDao(dataSource),
    private val åpneGosysOppgaverDao: ÅpneGosysOppgaverDao = ÅpneGosysOppgaverDao(dataSource),
    private val snapshotDao: SnapshotDao = SnapshotDao(dataSource),
    private val egenAnsattDao: EgenAnsattDao = EgenAnsattDao(dataSource),
    private val snapshotClient: SnapshotClient,
    private val oppgaveMediator: OppgaveMediator,
    private val godkjenningMediator: GodkjenningMediator,
    private val automatisering: Automatisering,
    private val arbeidsforholdDao: ArbeidsforholdDao = ArbeidsforholdDao(dataSource),
    private val utbetalingDao: UtbetalingDao = UtbetalingDao(dataSource),
    private val opptegnelseDao: OpptegnelseDao = OpptegnelseDao(dataSource),
    private val vergemålDao: VergemålDao = VergemålDao(dataSource),
    private val periodehistorikkDao: PeriodehistorikkDao = PeriodehistorikkDao(dataSource),
    private val overstyringMediator: OverstyringMediator,
) {
    private val sikkerLog = LoggerFactory.getLogger("tjenestekall")

    internal companion object {
        private val mapper = jacksonObjectMapper()

        fun JsonNode.toOverstyrteDagerDto() =
            map {
                OverstyringDagDto(
                    dato = it.path("dato").asLocalDate(),
                    type = enumValueOf<Dagtype>(it.path("type").asText()),
                    fraType = enumValueOf<Dagtype>(it.path("fraType").asText()),
                    grad = it.path("grad").asInt(),
                    fraGrad = it.path("fraGrad").asInt()
                )
            }
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
        arbeidsforholdId: String?,
        skjæringstidspunkt: LocalDate,
        periodetype: Periodetype,
        førstegangsbehandling: Boolean,
        utbetalingtype: Utbetalingtype,
        inntektskilde: Inntektskilde,
        orgnummereMedRelevanteArbeidsforhold: List<String>,
        json: String
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
            inntektskilde = inntektskilde,
            orgnummereMedRelevanteArbeidsforhold = orgnummereMedRelevanteArbeidsforhold,
            json = json,
            personDao = personDao,
            arbeidsgiverDao = arbeidsgiverDao,
            vedtakDao = vedtakDao,
            warningDao = warningDao,
            snapshotDao = snapshotDao,
            commandContextDao = commandContextDao,
            risikovurderingDao = risikovurderingDao,
            digitalKontaktinformasjonDao = digitalKontaktinformasjonDao,
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
            arbeidsforholdId = jsonNode.path("Godkjenning").path("arbeidsforholdId")
                .takeUnless(JsonNode::isMissingOrNull)?.asText(),
            skjæringstidspunkt = LocalDate.parse(jsonNode.path("Godkjenning").path("skjæringstidspunkt").asText()),
            periodetype = periodetype,
            førstegangsbehandling = førstegangsbehandling,
            utbetalingtype = Utbetalingtype.valueOf(jsonNode.path("Godkjenning").path("utbetalingtype").asText()),
            inntektskilde = Inntektskilde.valueOf(jsonNode.path("Godkjenning").path("inntektskilde").asText()),
            orgnummereMedRelevanteArbeidsforhold = jsonNode.path("Godkjenning").path("orgnummereMedRelevanteArbeidsforhold")
                .takeUnless(JsonNode::isMissingOrNull)?.map { it.asText() } ?: emptyList(),
            json = json
        )
    }

    fun saksbehandlerløsning(
        id: UUID,
        godkjenningsbehovhendelseId: UUID,
        fødselsnummer: String,
        godkjent: Boolean,
        saksbehandlerident: String,
        oid: UUID,
        epostadresse: String,
        godkjenttidspunkt: LocalDateTime,
        årsak: String?,
        begrunnelser: List<String>?,
        kommentar: String?,
        oppgaveId: Long,
        json: String
    ) = Saksbehandlerløsning(
        id = id,
        fødselsnummer = fødselsnummer,
        json = json,
        godkjent = godkjent,
        saksbehandlerIdent = saksbehandlerident,
        oid = oid,
        epostadresse = epostadresse,
        godkjenttidspunkt = godkjenttidspunkt,
        årsak = årsak,
        begrunnelser = begrunnelser,
        kommentar = kommentar,
        oppgaveId = oppgaveId,
        godkjenningsbehovhendelseId = godkjenningsbehovhendelseId,
        hendelseDao = hendelseDao,
        oppgaveDao = oppgaveDao,
        godkjenningMediator = godkjenningMediator
    )

    fun saksbehandlerløsning(json: String): Saksbehandlerløsning {
        val jsonNode = mapper.readTree(json)
        return saksbehandlerløsning(
            id = UUID.fromString(jsonNode["@id"].asText()),
            godkjenningsbehovhendelseId = UUID.fromString(jsonNode["hendelseId"].asText()),
            fødselsnummer = jsonNode["fødselsnummer"].asText(),
            godkjent = jsonNode["godkjent"].asBoolean(),
            saksbehandlerident = jsonNode["saksbehandlerident"].asText(),
            oid = UUID.fromString(jsonNode["saksbehandleroid"].asText()),
            epostadresse = jsonNode["saksbehandlerepost"].asText(),
            godkjenttidspunkt = jsonNode["godkjenttidspunkt"].asLocalDateTime(),
            årsak = jsonNode["årsak"].takeUnless(JsonNode::isMissingOrNull)?.asText(),
            begrunnelser = jsonNode["begrunnelser"].takeUnless(JsonNode::isMissingOrNull)?.map(JsonNode::asText),
            kommentar = jsonNode["kommentar"].takeUnless(JsonNode::isMissingOrNull)?.asText(),
            oppgaveId = jsonNode["oppgaveId"].asLong(),
            json = json
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
        json: String
    ) = OverstyringTidslinje(
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
        json = json,
        reservasjonDao = reservasjonDao,
        saksbehandlerDao = saksbehandlerDao,
        overstyringDao = overstyringDao,
        overstyringMediator = overstyringMediator,
    )

    fun overstyringTidslinje(json: String): OverstyringTidslinje {
        val jsonNode = mapper.readTree(json)
        return overstyringTidslinje(
            id = UUID.fromString(jsonNode.path("@id").asText()),
            fødselsnummer = jsonNode.path("fødselsnummer").asText(),
            oid = UUID.fromString(jsonNode.path("saksbehandlerOid").asText()),
            navn = jsonNode.path("saksbehandlerNavn").asText(),
            ident = jsonNode.path("saksbehandlerIdent").asText(),
            epost = jsonNode.path("saksbehandlerEpost").asText(),
            orgnummer = jsonNode.path("organisasjonsnummer").asText(),
            begrunnelse = jsonNode.path("begrunnelse").asText(),
            overstyrteDager = jsonNode.path("dager").toOverstyrteDagerDto(),
            opprettet = LocalDateTime.parse(jsonNode.path("@opprettet").asText()),
            json = json
        )
    }

    fun overstyringInntekt(
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
        fraMånedligInntekt: Double,
        skjæringstidspunkt: LocalDate,
        opprettet: LocalDateTime,
        json: String
    ) = OverstyringInntekt(
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
        fraMånedligInntekt = fraMånedligInntekt,
        skjæringstidspunkt = skjæringstidspunkt,
        reservasjonDao = reservasjonDao,
        saksbehandlerDao = saksbehandlerDao,
        overstyringDao = overstyringDao,
        opprettet = opprettet,
        json = json,
        overstyringMediator = overstyringMediator,
    )

    fun overstyringArbeidsforhold(
        id: UUID,
        fødselsnummer: String,
        oid: UUID,
        navn: String,
        ident: String,
        epost: String,
        overstyrteArbeidsforhold : List<OverstyrArbeidsforholdDto.ArbeidsforholdOverstyrt>,
        skjæringstidspunkt: LocalDate,
        opprettet: LocalDateTime,
        json: String
    ) = OverstyringArbeidsforhold(
        id = id,
        fødselsnummer = fødselsnummer,
        oid = oid,
        navn = navn,
        ident = ident,
        epost = epost,
        overstyrteArbeidsforhold = overstyrteArbeidsforhold,
        skjæringstidspunkt = skjæringstidspunkt,
        opprettet = opprettet,
        reservasjonDao = reservasjonDao,
        saksbehandlerDao = saksbehandlerDao,
        overstyringDao = overstyringDao,
        json = json,
        overstyringMediator = overstyringMediator,
    )

    fun overstyringArbeidsforhold(json: String): OverstyringArbeidsforhold {
        val jsonNode = mapper.readTree(json)
        return overstyringArbeidsforhold(
            id = UUID.fromString(jsonNode.path("@id").asText()),
            fødselsnummer = jsonNode.path("fødselsnummer").asText(),
            oid = UUID.fromString(jsonNode.path("saksbehandlerOid").asText()),
            navn = jsonNode.path("saksbehandlerNavn").asText(),
            ident = jsonNode.path("saksbehandlerIdent").asText(),
            epost = jsonNode.path("saksbehandlerEpost").asText(),
            overstyrteArbeidsforhold = jsonNode.path("overstyrteArbeidsforhold").map {
                OverstyrArbeidsforholdDto.ArbeidsforholdOverstyrt(
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

    fun overstyringInntekt(json: String): OverstyringInntekt {
        val jsonNode = mapper.readTree(json)
        return overstyringInntekt(
            id = UUID.fromString(jsonNode.path("@id").asText()),
            fødselsnummer = jsonNode.path("fødselsnummer").asText(),
            oid = UUID.fromString(jsonNode.path("saksbehandlerOid").asText()),
            navn = jsonNode.path("saksbehandlerNavn").asText(),
            ident = jsonNode.path("saksbehandlerIdent").asText(),
            epost = jsonNode.path("saksbehandlerEpost").asText(),
            orgnummer = jsonNode.path("organisasjonsnummer").asText(),
            begrunnelse = jsonNode.path("begrunnelse").asText(),
            forklaring = jsonNode.path("forklaring").asText(),
            månedligInntekt = jsonNode.path("månedligInntekt").asDouble(),
            fraMånedligInntekt = jsonNode.path("fraMånedligInntekt").asDouble(),
            skjæringstidspunkt = jsonNode.path("skjæringstidspunkt").asLocalDate(),
            opprettet = jsonNode.path("@opprettet").asLocalDateTime(),
            json = json
        )
    }

    fun adressebeskyttelseEndret(id: UUID, fødselsnummer: String, json: String) =
        AdressebeskyttelseEndret(id, fødselsnummer, json, personDao)

    fun adressebeskyttelseEndret(json: String): AdressebeskyttelseEndret {
        val jsonNode = mapper.readTree(json)
        return AdressebeskyttelseEndret(
            id = UUID.fromString(jsonNode.path("@id").asText()),
            fødselsnummer = jsonNode.path("fødselsnummer").asText(),
            json = json,
            personDao = personDao
        )
    }

    fun vedtaksperiodeEndret(
        id: UUID,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        forårsaketAvId: UUID,
        json: String
    ): VedtaksperiodeEndret {
        return VedtaksperiodeEndret(
            id = id,
            vedtaksperiodeId = vedtaksperiodeId,
            fødselsnummer = fødselsnummer,
            forårsaketAvId = forårsaketAvId,
            json = json,
            warningDao = warningDao,
            snapshotDao = snapshotDao,
            snapshotClient = snapshotClient,
            personDao = personDao,
            overstyringDao = overstyringDao,
        )
    }

    fun vedtaksperiodeEndret(json: String): VedtaksperiodeEndret {
        val jsonNode = mapper.readTree(json)
        return vedtaksperiodeEndret(
            id = UUID.fromString(jsonNode.path("@id").asText()),
            vedtaksperiodeId = UUID.fromString(jsonNode.path("vedtaksperiodeId").asText()),
            fødselsnummer = jsonNode.path("fødselsnummer").asText(),
            forårsaketAvId = UUID.fromString(jsonNode.path("@forårsaket_av.id").asText()),
            json = json
        )
    }

    fun vedtaksperiodeForkastet(
        id: UUID,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        json: String
    ): VedtaksperiodeForkastet {
        return VedtaksperiodeForkastet(
            id = id,
            vedtaksperiodeId = vedtaksperiodeId,
            fødselsnummer = fødselsnummer,
            json = json,
            commandContextDao = commandContextDao,
            warningDao = warningDao,
            oppgaveMediator = oppgaveMediator,
            snapshotClient = snapshotClient,
            snapshotDao = snapshotDao,
            personDao = personDao
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
            snapshotDao = snapshotDao
        )
    }

    fun utbetalingEndret(json: String): UtbetalingEndret {
        val jsonNode = mapper.readTree(json)
        return UtbetalingEndret(
            id = UUID.fromString(jsonNode.path("@id").asText()),
            fødselsnummer = jsonNode.path("fødselsnummer").asText(),
            orgnummer = jsonNode.path("organisasjonsnummer").asText(),
            utbetalingId = UUID.fromString(jsonNode.path("utbetalingId").asText()),
            type = jsonNode.path("type").asText(),
            gjeldendeStatus = Utbetalingsstatus.valueOf(jsonNode.path("gjeldendeStatus").asText()),
            opprettet = jsonNode.path("@opprettet").asLocalDateTime(),
            arbeidsgiverOppdrag = tilOppdrag(jsonNode.path("arbeidsgiverOppdrag")),
            personOppdrag = tilOppdrag(jsonNode.path("personOppdrag")),
            json = json,
            utbetalingDao = utbetalingDao,
            opptegnelseDao = opptegnelseDao,
            oppgaveDao = oppgaveDao,
            oppgaveMediator = oppgaveMediator
        )
    }

    private fun tilOppdrag(jsonNode: JsonNode) = LagreOppdragCommand.Oppdrag(
        fagsystemId = jsonNode.path("fagsystemId").asText(),
        fagområde = jsonNode.path("fagområde").asText(),
        mottaker = jsonNode.path("mottaker").asText(),
        endringskode = jsonNode.path("endringskode").asText(),
        sisteArbeidsgiverdag = jsonNode.path("sisteArbeidsgiverdag")
            .takeIf(JsonNode::isTextual)
            ?.asLocalDate()
            ?.takeUnless { it == LocalDate.MIN },
        linjer = jsonNode.path("linjer").map { linje ->
            LagreOppdragCommand.Oppdrag.Utbetalingslinje(
                endringskode = linje.path("endringskode").asText(),
                klassekode = linje.path("klassekode").asText(),
                statuskode = linje.path("statuskode").takeIf(JsonNode::isTextual)?.asText(),
                datoStatusFom = linje.path("datoStatusFom").takeIf(JsonNode::isTextual)?.asLocalDate(),
                fom = linje.path("fom").asLocalDate(),
                tom = linje.path("tom").asLocalDate(),
                dagsats = linje.path("dagsats").asInt(),
                totalbeløp = linje.path("totalbeløp").takeIf(JsonNode::isInt)?.asInt(),
                lønn = linje.path("lønn").asInt(),
                grad = linje.path("grad").asDouble(),
                delytelseId = linje.path("delytelseId").asInt(),
                refDelytelseId = linje.path("refDelytelseId").takeIf(JsonNode::isInt)?.asInt(),
                refFagsystemId = linje.path("refFagsystemId").takeIf(JsonNode::isTextual)?.asText()
            )
        }
    )

    fun oppdaterPersonsnapshot(json: String): OppdaterPersonsnapshot {
        val jsonNode = mapper.readTree(json)
        return OppdaterPersonsnapshot(
            id = UUID.fromString(jsonNode["@id"].asText()),
            fødselsnummer = jsonNode["fødselsnummer"].asText(),
            json = json,
            snapshotClient = snapshotClient,
            snapshotDao = snapshotDao,
            personDao = personDao,
            opptegnelseDao = opptegnelseDao,
        )
    }

    fun endretSkjermetinfo(json: String): EndretSkjermetinfo {
        val jsonNode = mapper.readTree(json)
        return EndretSkjermetinfo(
            id = UUID.fromString(jsonNode["@id"].asText()),
            fødselsnummer = jsonNode["fødselsnummer"].asText(),
            json = json,
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
            reservasjonDao = reservasjonDao,
            tildelingDao = tildelingDao,
            periodehistorikkDao = periodehistorikkDao,
            oppgaveDao = oppgaveDao,
            utbetalingDao = utbetalingDao,
        )
    }

    fun gosysOppgaveEndret(json: String): GosysOppgaveEndret {
        val jsonNode = mapper.readTree(json)
        val fødselsnummer = jsonNode["fødselsnummer"].asText()

        // Vi kan ikke sende med oss dataene ned i løypa, så derfor må vi hente det ut på nytt her.
        val commandData = oppgaveDao.finnOppgaveIdUansettStatus(fødselsnummer).let { oppgaveId ->
            oppgaveDao.gosysOppgaveEndretCommandData(oppgaveId)!!
        }

        sikkerLog.info("Gjør ny sjekk om det finnes åpne gosysoppgaver for fnr $fødselsnummer og vedtaksperiodeId ${commandData.vedtaksperiodeId}")

        return GosysOppgaveEndret(
            id = UUID.fromString(jsonNode["@id"].asText()),
            fødselsnummer = fødselsnummer,
            aktørId = jsonNode["aktørId"].asText(),
            json = json,
            gosysOppgaveEndretCommandData = commandData,
            åpneGosysOppgaverDao = åpneGosysOppgaverDao,
            warningDao = warningDao,
            automatisering = automatisering,
            godkjenningMediator = godkjenningMediator,
            oppgaveMediator = oppgaveMediator,
            oppgaveDao = oppgaveDao
        )
    }

    fun revurderingAvvist(json: String): RevurderingAvvist {
        val jsonNode = mapper.readTree(json)
        return RevurderingAvvist(
            id = UUID.fromString(jsonNode["@id"].asText()),
            fødselsnummer = jsonNode["fødselsnummer"].asText(),
            errors = jsonNode["errors"].map { it.asText() },
            json = json,
            opptegnelseDao = opptegnelseDao
        )
    }

    fun revurderingAvvist(fødselsnummer: String, errors: List<String>, json:String): RevurderingAvvist {
        return RevurderingAvvist(UUID.randomUUID(), fødselsnummer, errors, json, opptegnelseDao)
    }
}
