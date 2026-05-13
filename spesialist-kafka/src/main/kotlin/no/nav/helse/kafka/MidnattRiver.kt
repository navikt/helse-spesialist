package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.db.SessionFactory
import no.nav.helse.mediator.asUUID
import no.nav.helse.spesialist.application.logg.loggInfo

class MidnattRiver(
    private val sessionFactory: SessionFactory,
) : SpesialistRiver {
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
        loggInfo("Mottok melding midnatt, hendelseId: $hendelseId")

        sessionFactory.transactionalSessionScope {
            val antallSlettet = it.dokumentDao.slettGamleDokumenter()
            loggInfo("Slettet $antallSlettet dokumenter")
        }
    }
}
