package no.nav.helse.mediator

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.mediator.meldinger.*
import no.nav.helse.modell.*
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.dkif.DigitalKontaktinformasjonDao
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.overstyring.OverstyringDagDto
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.saksbehandler.SaksbehandlerDao
import no.nav.helse.modell.tildeling.ReservasjonDao
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import no.nav.helse.modell.vedtak.Warning
import no.nav.helse.modell.vedtak.WarningKilde
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.isMissingOrNull
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
    private val saksbehandlerDao: SaksbehandlerDao,
    private val overstyringDao: OverstyringDao,
    private val risikovurderingDao: RisikovurderingDao,
    private val digitalKontaktinformasjonDao: DigitalKontaktinformasjonDao,
    private val åpneGosysOppgaverDao: ÅpneGosysOppgaverDao,
    private val egenAnsattDao: EgenAnsattDao,
    private val speilSnapshotRestClient: SpeilSnapshotRestClient,
    private val oppgaveMediator: OppgaveMediator,
    private val godkjenningMediator: GodkjenningMediator,
    private val miljøstyrtFeatureToggle: MiljøstyrtFeatureToggle,
    private val automatisering: Automatisering,
    private val utbetalingDao: UtbetalingDao
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
        warnings: List<String>,
        periodetype: Saksbehandleroppgavetype,
        json: String
    ): Godkjenningsbehov {
        return Godkjenningsbehov(
            id = id,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            periodeFom = periodeFom,
            periodeTom = periodeTom,
            warnings = warnings.map { Warning(it, WarningKilde.Spleis) },
            periodetype = periodetype,
            json = json,
            personDao = personDao,
            arbeidsgiverDao = arbeidsgiverDao,
            commandContextDao = commandContextDao,
            vedtakDao = vedtakDao,
            warningDao = warningDao,
            snapshotDao = snapshotDao,
            risikovurderingDao = risikovurderingDao,
            digitalKontaktinformasjonDao = digitalKontaktinformasjonDao,
            åpneGosysOppgaverDao = åpneGosysOppgaverDao,
            reservasjonDao = reservasjonDao,
            egenAnsattDao = egenAnsattDao,
            speilSnapshotRestClient = speilSnapshotRestClient,
            oppgaveMediator = oppgaveMediator,
            miljøstyrtFeatureToggle = miljøstyrtFeatureToggle,
            automatisering = automatisering,
            godkjenningMediator = godkjenningMediator
        )
    }

    override fun godkjenning(json: String): Godkjenningsbehov {
        val jsonNode = mapper.readTree(json)
        return godkjenning(
            id = UUID.fromString(jsonNode.path("@id").asText()),
            fødselsnummer = jsonNode.path("fødselsnummer").asText(),
            aktørId = jsonNode.path("aktørId").asText(),
            organisasjonsnummer = jsonNode.path("organisasjonsnummer").asText(),
            periodeFom = LocalDate.parse(jsonNode.path("periodeFom").asText()),
            periodeTom = LocalDate.parse(jsonNode.path("periodeTom").asText()),
            vedtaksperiodeId = UUID.fromString(jsonNode.path("vedtaksperiodeId").asText()),
            warnings = jsonNode.path("warnings").path("aktiviteter").map { it["melding"].asText() },
            periodetype = Saksbehandleroppgavetype.valueOf(jsonNode.path("periodetype").asText()),
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
        oppgaveMediator = oppgaveMediator,
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
            epost = jsonNode.path("saksbehandlerEpost").asText(),
            orgnummer = jsonNode.path("organisasjonsnummer").asText(),
            begrunnelse = jsonNode.path("begrunnelse").asText(),
            overstyrteDager = jsonNode.path("dager").toOverstyrteDagerDto(),
            json = json
        )
    }

    override fun tilbakerulling(
        id: UUID,
        fødselsnummer: String,
        vedtaksperiodeIder: List<UUID>,
        json: String
    ): Tilbakerulling {
        return Tilbakerulling(
            id = id,
            fødselsnummer = fødselsnummer,
            json = json,
            vedtaksperiodeIder = vedtaksperiodeIder,
            commandContextDao = commandContextDao,
            oppgaveMediator = oppgaveMediator,
            vedtakDao = vedtakDao
        )
    }

    override fun tilbakerulling(json: String): Tilbakerulling {
        val jsonNode = mapper.readTree(json)
        return tilbakerulling(
            id = UUID.fromString(jsonNode.path("@id").asText()),
            fødselsnummer = jsonNode.path("fødselsnummer ").asText(),
            vedtaksperiodeIder = jsonNode.path("vedtaksperioderSlettet").map { UUID.fromString(it.asText()) },
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
            json = json,
            speilSnapshotRestClient = speilSnapshotRestClient,
            vedtakDao = vedtakDao
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
            status = jsonNode.path("gjeldendeStatus").asText(),
            opprettet = jsonNode.path("@opprettet").asLocalDateTime(),
            arbeidsgiverFagsystemId = jsonNode.path("arbeidsgiverOppdrag").path("fagsystemId").asText(),
            personFagsystemId = jsonNode.path("personOppdrag").path("fagsystemId").asText(),
            json = json,
            utbetalingDao = utbetalingDao
        )
    }

    override fun oppdaterPersonsnapshot(json: String): OppdaterPersonsnapshot {
        val jsonNode = mapper.readTree(json)
        return OppdaterPersonsnapshot(
            id = UUID.fromString(jsonNode["@id"].asText()),
            fødselsnummer = jsonNode["fødselsnummer"].asText(),
            json = json,
            speilSnapshotRestClient = speilSnapshotRestClient,
            vedtakDao = vedtakDao
        )
    }
}
