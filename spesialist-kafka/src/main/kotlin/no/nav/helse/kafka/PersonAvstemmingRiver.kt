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
import no.nav.helse.spesialist.domain.Behandling
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

        val vedtaksperioderFraSpleis =
            packet["arbeidsgivere"].flatMap { arbeidsgiverNode ->
                val arbeidsgiver = arbeidsgiverNode["organisasjonsnummer"].asText()
                arbeidsgiverNode["vedtaksperioder"].map { vedtaksperiode ->
                    AvstemtVedtaksperiode(
                        arbeidsgiver = arbeidsgiver,
                        id = vedtaksperiode["id"].asUUID(),
                        opprettet = vedtaksperiode["opprettet"].asLocalDateTime(),
                    )
                }
            }

        logg.info(
            "I spleis har personen ${vedtaksperioderFraSpleis.pretty()}, i spesialist sin database ligger det ${behandlinger.pretty()}",
        )
    }

    private fun List<AvstemtVedtaksperiode>.pretty() =
        when (val antall = size) {
            1 -> "1 vedtaksperiode"
            else -> "$antall vedtaksperioder"
        }

    private fun List<Behandling>.pretty() =
        when (val antall = size) {
            1 -> "1 behandling"
            else -> "$antall behandlinger"
        }

    private data class AvstemtVedtaksperiode(
        val arbeidsgiver: String,
        val id: UUID,
        val opprettet: LocalDateTime,
    )
}
