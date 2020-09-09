package no.nav.helse.mediator.kafka

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.api.OppgaveMediator
import no.nav.helse.mediator.kafka.meldinger.NyGodkjenningMessage
import no.nav.helse.mediator.kafka.meldinger.NyVedtaksperiodeEndretMessage
import no.nav.helse.mediator.kafka.meldinger.NyVedtaksperiodeForkastetMessage
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.IHendelsefabrikk
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.rapids_rivers.isMissingOrNull
import java.time.LocalDate
import java.util.*

internal class Hendelsefabrikk(
    private val personDao: PersonDao,
    private val arbeidsgiverDao: ArbeidsgiverDao,
    private val vedtakDao: VedtakDao,
    private val commandContextDao: CommandContextDao,
    private val snapshotDao: SnapshotDao,
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
            id, fødselsnummer, aktørId, organisasjonsnummer, vedtaksperiodeId, periodeFom, periodeTom, warnings, periodetype, json,
            personDao, arbeidsgiverDao, vedtakDao, snapshotDao, speilSnapshotRestClient, oppgaveMediator
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
            periodetype = jsonNode.path("periodetype").takeUnless(JsonNode::isMissingOrNull)?.let { Saksbehandleroppgavetype.valueOf(it.asText()) },
            json = json
        )
    }

    override fun nyNyVedtaksperiodeEndret(id: UUID, vedtaksperiodeId: UUID, fødselsnummer: String, json: String): NyVedtaksperiodeEndretMessage {
        return NyVedtaksperiodeEndretMessage(
            id,
            vedtaksperiodeId,
            fødselsnummer,
            json,
            vedtakDao,
            snapshotDao,
            speilSnapshotRestClient
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

    override fun nyNyVedtaksperiodeForkastet(id: UUID, vedtaksperiodeId: UUID, fødselsnummer: String, json: String): NyVedtaksperiodeForkastetMessage {
        return NyVedtaksperiodeForkastetMessage(
            id,
            vedtaksperiodeId,
            fødselsnummer,
            json,
            commandContextDao,
            vedtakDao,
            snapshotDao,
            speilSnapshotRestClient
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
