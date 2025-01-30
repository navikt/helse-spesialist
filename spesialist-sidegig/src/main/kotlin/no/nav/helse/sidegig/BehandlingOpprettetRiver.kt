package no.nav.helse.sidegig

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import java.util.UUID

class BehandlingOpprettetRiver(
    rapidsConnection: RapidsConnection,
    private val behandlingDao: BehandlingDao,
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            precondition {
                it.requireValue("@event_name", "behandling_opprettet")
                it.forbidValues("organisasjonsnummer", listOf("ARBEIDSLEDIG", "SELVSTENDIG", "FRILANS"))
            }
            validate {
                it.requireKey("vedtaksperiodeId", "behandlingId")
                it.requireKey("fom", "tom")
                it.requireKey("@opprettet")
            }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val behandling =
            Behandling(
                vedtaksperiodeId = packet["vedtaksperiodeId"].asUuid(),
                behandlingId = packet["behandlingId"].asUuid(),
                fom = packet["fom"].asLocalDate(),
                tom = packet["tom"].asLocalDate(),
                skjæringstidspunkt = packet["fom"].asLocalDate(),
                opprettet = packet["@opprettet"].asLocalDateTime(),
            )
        behandlingDao.lagreBehandling(behandling)
    }

    private fun JsonNode.asUuid(): UUID {
        return UUID.fromString(this.asText())
    }
}
