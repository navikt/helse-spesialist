package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.application.logg.sikkerlogg

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
            packet["arbeidsgivere"].flatMap { arbeidsgiverNode ->
                arbeidsgiverNode["vedtaksperioder"].flatMap { vedtaksperiode ->
                    vedtaksperiode["behandlinger"].map { behandlingNode ->
                        behandlingNode["behandlingId"].asUUID()
                    }
                }
            }

        val spesialistBehandlingerMedSpleisBehandlingId =
            spesialistBehandlinger.filter { it.spleisBehandlingId != null }

        val antallBehandlingerISpleis = spleisBehandlinger.size
        val antallBehandlingerISpesialist = spesialistBehandlinger.size
        val antallSpesialstBehandlingerMedSpleisBehandlingId = spesialistBehandlingerMedSpleisBehandlingId.size
        val antallSpleisBehandlingerSomOgsåFinnesISpesialist =
            spesialistBehandlingerMedSpleisBehandlingId.filter { spleisBehandlinger.contains(it.spleisBehandlingId) }.size

        if (antallBehandlingerISpesialist != antallBehandlingerISpleis) {
            val melding =
                "Antall behandlinger i Spleis ($antallBehandlingerISpleis) samsvarer ikke med antall behandlinger i Spesialist ($antallBehandlingerISpesialist). $antallSpesialstBehandlingerMedSpleisBehandlingId av $antallBehandlingerISpesialist behandlinger har spleis behandling id, av disse finnes $antallSpleisBehandlingerSomOgsåFinnesISpesialist i Spleis"
            logg.warn(
                melding,
            )
            sikkerlogg.warn(
                "$melding, for person med fødselsnummer $fødselsnummer",
            )
        }
    }
}
