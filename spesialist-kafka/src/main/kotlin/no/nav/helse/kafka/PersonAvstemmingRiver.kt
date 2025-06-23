package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.db.BehandlingRepository
import no.nav.helse.mediator.asUUID
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.application.logg.sikkerlogg
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import java.time.LocalDateTime
import java.util.UUID

class PersonAvstemmingRiver(
    private val behandlingRepository: BehandlingRepository,
) : SpesialistRiver {
    override fun preconditions(): River.PacketValidation {
        return River.PacketValidation {
            it.requireValue("@event_name", "person_avstemt")
        }
    }

    override fun validations() =
        River.PacketValidation {
            it.requireKey("@id", "fødselsnummer", "arbeidsgivere")
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val hendelseId = packet["@id"].asUUID()
        logg.info("Mottok person_avstemt, {}", kv("hendelseId", hendelseId))

        packet["arbeidsgivere"].flatMap { arbeidsgiverNode ->
            val arbeidsgiver = arbeidsgiverNode["organisasjonsnummer"].asText()
            arbeidsgiverNode["vedtaksperioder"].flatMap { vedtaksperiode ->
                vedtaksperiode["behandlinger"].map { behandlingNode ->
                    AvstemtBehandling(
                        arbeidsgiver = arbeidsgiver,
                        id = behandlingNode["behandlingId"].asUUID(),
                        opprettet = behandlingNode["behandlingOpprettet"].asLocalDateTime(),
                    )
                }
            }
        }.forEach { behandling ->
            val vårBehandling = behandlingRepository.finn(SpleisBehandlingId(behandling.id))
            if (vårBehandling == null) {
                logg.warn("Fant ikke behandling med id: ${behandling.id} ved avstemming, se sikkerlogg for detaljer")
                sikkerlogg.warn(
                    "Fant ikke behandling med id: ${behandling.id} ved avstemming av person med fødselsnummer: ${packet["fødselsnummer"].asText()}",
                )
            }
        }
    }

    private data class AvstemtBehandling(
        val arbeidsgiver: String,
        val id: UUID,
        val opprettet: LocalDateTime,
    )
}
