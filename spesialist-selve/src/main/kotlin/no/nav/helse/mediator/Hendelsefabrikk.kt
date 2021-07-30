package no.nav.helse.mediator

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.mediator.meldinger.*
import no.nav.helse.modell.*
import no.nav.helse.modell.arbeidsforhold.ArbeidsforholdDao
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.dkif.DigitalKontaktinformasjonDao
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.opptegnelse.OpptegnelseDao
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.utbetaling.LagreOppdragCommand
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.utbetaling.Utbetalingsstatus
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.oppgave.OppgaveDao
import no.nav.helse.oppgave.OppgaveMediator
import no.nav.helse.overstyring.OverstyringDagDto
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.helse.reservasjon.ReservasjonDao
import no.nav.helse.saksbehandler.SaksbehandlerDao
import no.nav.helse.tildeling.TildelingDao
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class Hendelsefabrikk(
    private val hendelseDao: HendelseDao,
    private val personDao: PersonDao,
    private val arbeidsgiverDao: ArbeidsgiverDao,
    private val vedtakDao: VedtakDao,
    private val warningDao: WarningDao,
    private val oppgaveDao: OppgaveDao,
    private val commandContextDao: CommandContextDao,
    private val snapshotDao: SnapshotDao,
    private val reservasjonDao: ReservasjonDao,
    private val tildelingDao: TildelingDao,
    private val saksbehandlerDao: SaksbehandlerDao,
    private val overstyringDao: OverstyringDao,
    private val risikovurderingDao: RisikovurderingDao,
    private val digitalKontaktinformasjonDao: DigitalKontaktinformasjonDao,
    private val åpneGosysOppgaverDao: ÅpneGosysOppgaverDao,
    private val egenAnsattDao: EgenAnsattDao,
    private val speilSnapshotRestClient: SpeilSnapshotRestClient,
    private val oppgaveMediator: OppgaveMediator,
    private val godkjenningMediator: GodkjenningMediator,
    private val automatisering: Automatisering,
    private val arbeidsforholdDao: ArbeidsforholdDao,
    private val utbetalingDao: UtbetalingDao,
    private val opptegnelseDao: OpptegnelseDao
) : IHendelsefabrikk {
    private companion object {
        private val mapper = jacksonObjectMapper()
    }

    override fun godkjenning(
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
        utbetalingtype: Utbetalingtype,
        inntektskilde: Inntektskilde,
        aktiveVedtaksperioder: List<Godkjenningsbehov.AktivVedtaksperiode>,
        orgnummereMedAktiveArbeidsforhold: List<String>,
        json: String
    ): Godkjenningsbehov {
        return Godkjenningsbehov(
            id = id,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            utbetalingId = utbetalingId,
            arbeidsforholdId = arbeidsforholdId,
            periodeFom = periodeFom,
            periodeTom = periodeTom,
            skjæringstidspunkt = skjæringstidspunkt,
            periodetype = periodetype,
            utbetalingtype = utbetalingtype,
            inntektskilde = inntektskilde,
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
            speilSnapshotRestClient = speilSnapshotRestClient,
            oppgaveMediator = oppgaveMediator,
            automatisering = automatisering,
            godkjenningMediator = godkjenningMediator,
            aktiveVedtaksperioder = aktiveVedtaksperioder,
            orgnummereMedAktiveArbeidsforhold = orgnummereMedAktiveArbeidsforhold,
            opptegnelseDao = opptegnelseDao
        )
    }

    override fun godkjenning(json: String): Godkjenningsbehov {
        val jsonNode = mapper.readTree(json)
        return godkjenning(
            id = UUID.fromString(jsonNode.path("@id").asText()),
            fødselsnummer = jsonNode.path("fødselsnummer").asText(),
            aktørId = jsonNode.path("aktørId").asText(),
            organisasjonsnummer = jsonNode.path("organisasjonsnummer").asText(),
            periodeFom = LocalDate.parse(jsonNode.path("Godkjenning").path("periodeFom").asText()),
            periodeTom = LocalDate.parse(jsonNode.path("Godkjenning").path("periodeTom").asText()),
            vedtaksperiodeId = UUID.fromString(jsonNode.path("vedtaksperiodeId").asText()),
            utbetalingId = UUID.fromString(jsonNode.path("utbetalingId").asText()),
            arbeidsforholdId = jsonNode.path("Godkjenning").path("arbeidsforholdId").takeUnless(JsonNode::isMissingOrNull)?.asText(),
            skjæringstidspunkt = LocalDate.parse(jsonNode.path("Godkjenning").path("skjæringstidspunkt").asText()),
            periodetype = Periodetype.valueOf(jsonNode.path("Godkjenning").path("periodetype").asText()),
            utbetalingtype = Utbetalingtype.valueOf(jsonNode.path("Godkjenning").path("utbetalingtype").asText()),
            inntektskilde = Inntektskilde.valueOf(jsonNode.path("Godkjenning").path("inntektskilde").asText()),
            aktiveVedtaksperioder = Godkjenningsbehov.AktivVedtaksperiode.fromNode(jsonNode.path("Godkjenning").path("aktiveVedtaksperioder")),
            orgnummereMedAktiveArbeidsforhold = jsonNode.path("Godkjenning").path("orgnummereMedAktiveArbeidsforhold").takeUnless(JsonNode::isMissingOrNull)?.map { it.asText() } ?: emptyList(),
            json = json
        )
    }

    override fun saksbehandlerløsning(
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

    override fun saksbehandlerløsning(json: String): Saksbehandlerløsning {
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

    override fun overstyring(
        id: UUID,
        fødselsnummer: String,
        oid: UUID,
        navn: String,
        ident: String,
        epost: String,
        orgnummer: String,
        begrunnelse: String,
        overstyrteDager: List<OverstyringDagDto>,
        json: String
    ) = Overstyring(
        id = id,
        fødselsnummer = fødselsnummer,
        oid = oid,
        navn = navn,
        ident = ident,
        epost = epost,
        orgnummer = orgnummer,
        begrunnelse = begrunnelse,
        overstyrteDager = overstyrteDager,
        json = json,
        reservasjonDao = reservasjonDao,
        saksbehandlerDao = saksbehandlerDao,
        overstyringDao = overstyringDao
    )

    override fun overstyring(json: String): Overstyring {
        val jsonNode = mapper.readTree(json)
        return overstyring(
            id = UUID.fromString(jsonNode.path("@id").asText()),
            fødselsnummer = jsonNode.path("fødselsnummer").asText(),
            oid = UUID.fromString(jsonNode.path("saksbehandlerOid").asText()),
            navn = jsonNode.path("saksbehandlerNavn").asText(),
            ident = jsonNode.path("saksbehandlerIdent").asText(),
            epost = jsonNode.path("saksbehandlerEpost").asText(),
            orgnummer = jsonNode.path("organisasjonsnummer").asText(),
            begrunnelse = jsonNode.path("begrunnelse").asText(),
            overstyrteDager = jsonNode.path("dager").toOverstyrteDagerDto(),
            json = json
        )
    }

    private fun JsonNode.toOverstyrteDagerDto() =
        map {
            OverstyringDagDto(
                dato = it.path("dato").asLocalDate(),
                type = enumValueOf(it.path("type").asText()),
                grad = it.path("grad").asInt()
            )
        }

    override fun vedtaksperiodeEndret(
        id: UUID,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        json: String
    ): VedtaksperiodeEndret {
        return VedtaksperiodeEndret(
            id = id,
            vedtaksperiodeId = vedtaksperiodeId,
            fødselsnummer = fødselsnummer,
            json = json,
            vedtakDao = vedtakDao,
            warningDao = warningDao,
            snapshotDao = snapshotDao,
            speilSnapshotRestClient = speilSnapshotRestClient
        )
    }

    override fun vedtaksperiodeEndret(json: String): VedtaksperiodeEndret {
        val jsonNode = mapper.readTree(json)
        return vedtaksperiodeEndret(
            id = UUID.fromString(jsonNode.path("@id").asText()),
            vedtaksperiodeId = UUID.fromString(jsonNode.path("vedtaksperiodeId").asText()),
            fødselsnummer = jsonNode.path("fødselsnummer").asText(),
            json = json
        )
    }

    override fun vedtaksperiodeForkastet(
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
            vedtakDao = vedtakDao,
            warningDao = warningDao,
            snapshotDao = snapshotDao,
            speilSnapshotRestClient = speilSnapshotRestClient,
            oppgaveMediator = oppgaveMediator
        )
    }

    override fun vedtaksperiodeForkastet(json: String): VedtaksperiodeForkastet {
        val jsonNode = mapper.readTree(json)
        return vedtaksperiodeForkastet(
            id = UUID.fromString(jsonNode.path("@id").asText()),
            vedtaksperiodeId = UUID.fromString(jsonNode.path("vedtaksperiodeId").asText()),
            fødselsnummer = jsonNode.path("fødselsnummer").asText(),
            json = json
        )
    }

    override fun utbetalingAnnullert(json: String): UtbetalingAnnullert {
        val jsonNode = mapper.readTree(json)
        return UtbetalingAnnullert(
            id = UUID.fromString(jsonNode["@id"].asText()),
            fødselsnummer = jsonNode["fødselsnummer"].asText(),
            utbetalingId = UUID.fromString(jsonNode["utbetalingId"].asText()),
            annullertTidspunkt = LocalDateTime.parse(jsonNode["annullertAvSaksbehandler"].asText()),
            saksbehandlerEpost = jsonNode["saksbehandlerEpost"].asText(),
            json = json,
            speilSnapshotRestClient = speilSnapshotRestClient,
            snapshotDao = snapshotDao,
            utbetalingDao = utbetalingDao,
            saksbehandlerDao = saksbehandlerDao,
        )
    }

    override fun utbetalingEndret(json: String): UtbetalingEndret {
        val jsonNode = mapper.readTree(json)
        return UtbetalingEndret(
            id = UUID.fromString(jsonNode.path("@id").asText()),
            fødselsnummer = jsonNode.path("fødselsnummer").asText(),
            orgnummer = jsonNode.path("organisasjonsnummer").asText(),
            utbetalingId = UUID.fromString(jsonNode.path("utbetalingId").asText()),
            type = jsonNode.path("type").asText(),
            opprettet = jsonNode.path("@opprettet").asLocalDateTime(),
            forrigeStatus = Utbetalingsstatus.valueOf(jsonNode.path("forrigeStatus").asText()),
            gjeldendeStatus = Utbetalingsstatus.valueOf(jsonNode.path("gjeldendeStatus").asText()),
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

    override fun oppdaterPersonsnapshot(json: String): OppdaterPersonsnapshot {
        val jsonNode = mapper.readTree(json)
        return OppdaterPersonsnapshot(
            id = UUID.fromString(jsonNode["@id"].asText()),
            fødselsnummer = jsonNode["fødselsnummer"].asText(),
            json = json,
            speilSnapshotRestClient = speilSnapshotRestClient,
            snapshotDao = snapshotDao
        )
    }

    override fun vedtaksperiodeReberegnet(json: String): VedtaksperiodeReberegnet {
        val jsonNode = mapper.readTree(json)
        return VedtaksperiodeReberegnet(
            id = UUID.fromString(jsonNode["@id"].asText()),
            fødselsnummer = jsonNode["fødselsnummer"].asText(),
            vedtaksperiodeId = UUID.fromString(jsonNode.path("vedtaksperiodeId").asText()),
            json = json,
            commandContextDao = commandContextDao,
            oppgaveMediator = oppgaveMediator,
            reservasjonDao = reservasjonDao,
            tildelingDao = tildelingDao
        )
    }
}
