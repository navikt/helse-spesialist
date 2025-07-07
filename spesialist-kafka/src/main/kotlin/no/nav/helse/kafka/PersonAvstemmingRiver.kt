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
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

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
        val spesialistBehandlinger = mediator.finnBehandlingerFor(fødselsnummer)

        val spleisBehandlinger =
            pakkUtBehandlinger(packet).filter {
                it.behandlingOpprettet.isAfter(LocalDate.of(2024, 5, 1).atStartOfDay())
            }.map { it.behandlingId }
        val felles = spleisBehandlinger.intersect(spesialistBehandlinger.mapNotNull { it.spleisBehandlingId })

        val antallBehandlingerISpesialist = felles.size
        val antallBehandlingerISpleis = spleisBehandlinger.size

        if (antallBehandlingerISpesialist == antallBehandlingerISpleis) {
            val melding = "Antall behandlinger i Spleis ($antallBehandlingerISpleis) samsvarer med antall behandlinger i Spesialist ($antallBehandlingerISpesialist)"
            logg.info(melding)
            sikkerlogg.info("$melding for person med fødselsnummer $fødselsnummer")
        } else {
            val melding = "Antall behandlinger i Spleis ($antallBehandlingerISpleis) samsvarer ikke med antall behandlinger i Spesialist ($antallBehandlingerISpesialist)"
            logg.warn(melding)
            sikkerlogg.warn("$melding for person med fødselsnummer $fødselsnummer")
        }
    }

    private fun pakkUtBehandlinger(packet: JsonMessage): List<SpleisBehandling> =
        packet["arbeidsgivere"].flatMap { arbeidsgiverNode ->
            arbeidsgiverNode["vedtaksperioder"].flatMap { vedtaksperiode ->
                vedtaksperiode["behandlinger"].map { behandlingNode ->
                    SpleisBehandling(behandlingNode["behandlingId"].asUUID(), behandlingNode["behandlingOpprettet"].asLocalDateTime())
                }
            }
        }

    data class SpleisBehandling(val behandlingId: UUID, val behandlingOpprettet: LocalDateTime)
}
