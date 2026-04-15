package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.db.SessionFactory
import no.nav.helse.db.overstyring.venting.MeldingId
import no.nav.helse.db.overstyring.venting.VenterPåKvitteringForOverstyring
import no.nav.helse.mediator.asUUID
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.application.logg.teamLogs
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.VedtaksperiodeId

class OvervåkOverstyringerRiver(
    private val sessionFactory: SessionFactory,
) : SpesialistRiver {
    override fun preconditions(): River.PacketValidation =
        River.PacketValidation {
            it.requireValue("@event_name", "overstyr_tidslinje")
        }

    override fun validations() =
        River.PacketValidation {
            it.requireKey("@id", "fødselsnummer", "vedtaksperiodeId")
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
            val vedtaksperiodeId = VedtaksperiodeId(packet["vedtaksperiodeId"].asUUID())
            val behandling =
                sessionContext.behandlingRepository.finnNyesteForVedtaksperiode(vedtaksperiodeId)
                    ?: return@transactionalSessionScope

            if (behandling.tilstand == Behandling.Tilstand.VidereBehandlingAvklares) {
                sessionContext.venterPåKvitteringForOverstyringRepository.lagre(
                    VenterPåKvitteringForOverstyring.ny(
                        meldingId = MeldingId(packet["@id"].asUUID()),
                        identitetsnummer = Identitetsnummer.fraString(packet["fødselsnummer"].asText()),
                    ),
                )
                logg.info("Avventer kvittering for overstyring av $vedtaksperiodeId")
            }
        }
    }
}
