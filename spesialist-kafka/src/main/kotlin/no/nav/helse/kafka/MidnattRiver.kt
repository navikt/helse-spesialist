package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.db.SessionFactory
import no.nav.helse.mediator.asUUID
import no.nav.helse.spesialist.application.logg.logg
import java.time.Duration

class MidnattRiver(
    private val sessionFactory: SessionFactory,
) : SpesialistRiver {
    private companion object {
        private val EN_UKE = Duration.ofDays(7)
    }

    override fun preconditions(): River.PacketValidation =
        River.PacketValidation {
            it.requireAny("@event_name", listOf("midnatt", "slett_gamle_dokumenter_spesialist"))
        }

    override fun validations() =
        River.PacketValidation {
            it.requireKey("@id")
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val hendelseId = packet["@id"].asUUID()
        logg.info("Mottok melding midnatt, {}", kv("hendelseId", hendelseId))

        sessionFactory.transactionalSessionScope {
            val antallSlettet = it.dokumentDao.slettGamleDokumenter()
            logg.info("Slettet $antallSlettet dokumenter")
        }

        sessionFactory.transactionalSessionScope {
            val antallSlettet = it.personPseudoIdDao.slettPseudoIderEldreEnn(EN_UKE)
            logg.info("Slettet $antallSlettet pseudo-ider")
        }
    }
}
