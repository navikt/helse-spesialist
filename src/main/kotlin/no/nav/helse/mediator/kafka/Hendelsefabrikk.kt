package no.nav.helse.mediator.kafka

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.api.OppgaveMediator
import no.nav.helse.mediator.kafka.meldinger.NyGodkjenningMessage
import no.nav.helse.mediator.kafka.meldinger.NyVedtaksperiodeEndretMessage
import no.nav.helse.mediator.kafka.meldinger.NyVedtaksperiodeForkastetMessage
import no.nav.helse.mediator.kafka.meldinger.OverstyringMessage
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.IHendelsefabrikk
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.overstyring.OverstyringDagDto
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.saksbehandler.SaksbehandlerDao
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.helse.tildeling.ReservasjonDao
import java.time.LocalDate
import java.util.*

internal class Hendelsefabrikk(
    private val personDao: PersonDao,
    private val arbeidsgiverDao: ArbeidsgiverDao,
    private val vedtakDao: VedtakDao,
    private val commandContextDao: CommandContextDao,
    private val snapshotDao: SnapshotDao,
    private val reservasjonsDao: ReservasjonDao,
    private val saksbehandlerDao: SaksbehandlerDao,
    private val overstyringDao: OverstyringDao,
    private val risikovurderingDao: RisikovurderingDao,
    private val speilSnapshotRestClient: SpeilSnapshotRestClient,
    private val oppgaveMediator: OppgaveMediator
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
            speilSnapshotRestClient = speilSnapshotRestClient,
            oppgaveMediator = oppgaveMediator
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
        reservasjonDao = reservasjonsDao,
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
