package no.nav.helse.mediator.kafka

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.mediator.kafka.meldinger.NyGodkjenningMessage
import no.nav.helse.mediator.kafka.meldinger.NyVedtaksperiodeEndretMessage
import no.nav.helse.mediator.kafka.meldinger.NyVedtaksperiodeForkastetMessage
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.IHendelsefabrikk
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import java.time.LocalDate
import java.util.*

internal class Hendelsefabrikk(
    private val vedtakDao: VedtakDao,
    private val commandContextDao: CommandContextDao,
    private val snapshotDao: SnapshotDao,
    private val speilSnapshotRestClient: SpeilSnapshotRestClient
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
        TODO()
    }

    override fun nyGodkjenning(json: String): NyGodkjenningMessage {
        TODO()
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
