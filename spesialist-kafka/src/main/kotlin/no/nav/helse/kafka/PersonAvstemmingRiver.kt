package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.application.logg.sikkerlogg
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import java.time.LocalDateTime
import java.util.UUID

class PersonAvstemmingRiver(
    private val mediator: MeldingMediator,
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

        val fødselsnummer = packet["fødselsnummer"].asText()
        val behandlinger = mediator.finnBehandlingerFor(fødselsnummer)

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
            if (behandlinger.none { it.id == SpleisBehandlingId(behandling.id) }) {
                logg.warn("Fant ikke behandling med id: ${behandling.id} ved avstemming, se sikkerlogg for detaljer")
                sikkerlogg.warn(
                    "Fant ikke behandling med id: ${behandling.id} ved avstemming av person med fødselsnummer: $fødselsnummer",
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
