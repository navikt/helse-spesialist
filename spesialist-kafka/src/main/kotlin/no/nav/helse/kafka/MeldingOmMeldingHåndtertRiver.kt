package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.db.SessionFactory
import no.nav.helse.db.overstyring.venting.MeldingId
import no.nav.helse.mediator.asUUID
import no.nav.helse.spesialist.application.logg.teamLogs
import no.nav.helse.spesialist.domain.Opptegnelse

class MeldingOmMeldingHåndtertRiver(
    private val sessionFactory: SessionFactory,
) : SpesialistRiver {
    override fun preconditions(): River.PacketValidation =
        River.PacketValidation {
            it.requireValue("@event_name", "melding_om_melding_håndtert")
            it.requireValue("originalt_event_name", "overstyr_tidslinje")
        }

    override fun validations() =
        River.PacketValidation {
            it.requireKey("original_id")
        }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
        metadata: MessageMetadata,
    ) {
        teamLogs.warn(problems.toString())
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        sessionFactory.transactionalSessionScope { sessionContext ->
            val overstyringMeldingId = packet["original_id"].asUUID().let(::MeldingId)
            val venterPåKvitteringForOverstyring =
                sessionContext.venterPåKvitteringForOverstyringRepository.finn(overstyringMeldingId)
                    ?: return@transactionalSessionScope

            sessionContext.opptegnelseRepository.lagre(
                Opptegnelse.ny(
                    identitetsnummer = venterPåKvitteringForOverstyring.identitetsnummer,
                    type = Opptegnelse.Type.PERSONDATA_OPPDATERT,
                ),
            )
            sessionContext.venterPåKvitteringForOverstyringRepository.slett(venterPåKvitteringForOverstyring.id)
        }
    }
}
