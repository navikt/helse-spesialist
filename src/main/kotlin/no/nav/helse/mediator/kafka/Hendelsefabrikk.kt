package no.nav.helse.mediator.kafka

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.api.OppgaveMediator
import no.nav.helse.mediator.kafka.meldinger.*
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.IHendelsefabrikk
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.command.HendelseDao
import no.nav.helse.modell.command.OppgaveDao
import no.nav.helse.modell.dkif.DigitalKontaktinformasjonDao
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.overstyring.OverstyringDagDto
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.saksbehandler.SaksbehandlerDao
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.helse.tildeling.ReservasjonDao
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class Hendelsefabrikk(
    private val hendelseDao: HendelseDao,
    private val personDao: PersonDao,
    private val arbeidsgiverDao: ArbeidsgiverDao,
    private val vedtakDao: VedtakDao,
    private val oppgaveDao: OppgaveDao,
    private val commandContextDao: CommandContextDao,
    private val snapshotDao: SnapshotDao,
    private val reservasjonDao: ReservasjonDao,
    private val saksbehandlerDao: SaksbehandlerDao,
    private val overstyringDao: OverstyringDao,
    private val risikovurderingDao: RisikovurderingDao,
    private val digitalKontaktinformasjonDao: DigitalKontaktinformasjonDao,
    private val åpneGosysOppgaverDao: ÅpneGosysOppgaverDao,
    private val speilSnapshotRestClient: SpeilSnapshotRestClient,
    private val oppgaveMediator: OppgaveMediator,
    private val miljøstyrtFeatureToggle: MiljøstyrtFeatureToggle,
    private val automatisering: Automatisering
) : IHendelsefabrikk {
    private companion object {
        private val mapper = jacksonObjectMapper()
    }

    override fun nyGodkjenning(
        id: UUID,
        fødselsnummer: String,
        aktørId: String,
        organisasjonsnummer: String,
        periodeFom: LocalDate,
        periodeTom: LocalDate,
        vedtaksperiodeId: UUID,
        warnings: List<String>,
        periodetype: Saksbehandleroppgavetype?,
        json: String
    ): NyGodkjenningMessage {
        return NyGodkjenningMessage(
            id = id,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            periodeFom = periodeFom,
            periodeTom = periodeTom,
            warnings = warnings,
            periodetype = periodetype,
            json = json,
            personDao = personDao,
            arbeidsgiverDao = arbeidsgiverDao,
            vedtakDao = vedtakDao,
            snapshotDao = snapshotDao,
            risikovurderingDao = risikovurderingDao,
            digitalKontaktinformasjonDao = digitalKontaktinformasjonDao,
            åpneGosysOppgaverDao = åpneGosysOppgaverDao,
            reservasjonDao = reservasjonDao,
            speilSnapshotRestClient = speilSnapshotRestClient,
            oppgaveMediator = oppgaveMediator,
            miljøstyrtFeatureToggle = miljøstyrtFeatureToggle,
            automatisering = automatisering
        )
    }

    override fun nyGodkjenning(json: String): NyGodkjenningMessage {
        val jsonNode = mapper.readTree(json)
        return nyGodkjenning(
            id = UUID.fromString(jsonNode.path("@id").asText()),
            fødselsnummer = jsonNode.path("fødselsnummer").asText(),
            aktørId = jsonNode.path("aktørId").asText(),
            organisasjonsnummer = jsonNode.path("organisasjonsnummer").asText(),
            periodeFom = LocalDate.parse(jsonNode.path("periodeFom").asText()),
            periodeTom = LocalDate.parse(jsonNode.path("periodeTom").asText()),
            vedtaksperiodeId = UUID.fromString(jsonNode.path("vedtaksperiodeId").asText()),
            warnings = jsonNode.path("warnings").path("aktiviteter").map(JsonNode::asText),
            periodetype = jsonNode.path("periodetype").takeUnless(JsonNode::isMissingOrNull)
                ?.let { Saksbehandleroppgavetype.valueOf(it.asText()) },
            json = json
        )
    }

    override fun saksbehandlerløsning(
        id: UUID,
        godkjenningsbehovhendelseId: UUID,
        contextId: UUID,
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
    ) = SaksbehandlerløsningMessage(
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
        oppgaveDao = oppgaveDao
    )

    override fun saksbehandlerløsning(json: String): SaksbehandlerløsningMessage {
        val jsonNode = mapper.readTree(json)
        return saksbehandlerløsning(
            id = UUID.fromString(jsonNode["@id"].asText()),
            godkjenningsbehovhendelseId = UUID.fromString(jsonNode["hendelseId"].asText()),
            contextId = UUID.fromString(jsonNode["contextId"].asText()),
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
    ) = OverstyringMessage(
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

    override fun overstyring(json: String): OverstyringMessage {
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
    ): NyTilbakerullingMessage {
        return NyTilbakerullingMessage(
            id = id,
            fødselsnummer = fødselsnummer,
            json = json,
            vedtaksperiodeIder = vedtaksperiodeIder,
            commandContextDao = commandContextDao,
            oppgaveDao = oppgaveDao,
            vedtakDao = vedtakDao
        )
    }

    override fun tilbakerulling(json: String): NyTilbakerullingMessage {
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

    override fun nyNyVedtaksperiodeEndret(
        id: UUID,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        json: String
    ): NyVedtaksperiodeEndretMessage {
        return NyVedtaksperiodeEndretMessage(
            id = id,
            vedtaksperiodeId = vedtaksperiodeId,
            fødselsnummer = fødselsnummer,
            json = json,
            vedtakDao = vedtakDao,
            snapshotDao = snapshotDao,
            speilSnapshotRestClient = speilSnapshotRestClient
        )
    }

    override fun nyNyVedtaksperiodeEndret(json: String): NyVedtaksperiodeEndretMessage {
        val jsonNode = mapper.readTree(json)
        return nyNyVedtaksperiodeEndret(
            id = UUID.fromString(jsonNode.path("@id").asText()),
            vedtaksperiodeId = UUID.fromString(jsonNode.path("vedtaksperiodeId").asText()),
            fødselsnummer = jsonNode.path("fødselsnummer").asText(),
            json = json
        )
    }

    override fun nyNyVedtaksperiodeForkastet(
        id: UUID,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        json: String
    ): NyVedtaksperiodeForkastetMessage {
        return NyVedtaksperiodeForkastetMessage(
            id = id,
            vedtaksperiodeId = vedtaksperiodeId,
            fødselsnummer = fødselsnummer,
            json = json,
            commandContextDao = commandContextDao,
            vedtakDao = vedtakDao,
            oppgaveDao = oppgaveDao,
            snapshotDao = snapshotDao,
            speilSnapshotRestClient = speilSnapshotRestClient
        )
    }

    override fun nyNyVedtaksperiodeForkastet(json: String): NyVedtaksperiodeForkastetMessage {
        val jsonNode = mapper.readTree(json)
        return nyNyVedtaksperiodeForkastet(
            id = UUID.fromString(jsonNode.path("@id").asText()),
            vedtaksperiodeId = UUID.fromString(jsonNode.path("vedtaksperiodeId").asText()),
            fødselsnummer = jsonNode.path("fødselsnummer").asText(),
            json = json
        )
    }
}
