package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import no.nav.helse.db.MeldingDao
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.asUUID
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.VedtaksperiodeId

class BehandlingLukketRiver : TransaksjonellRiver() {
    override fun preconditions(): River.PacketValidation =
        River.PacketValidation {
            it.requireValue("@event_name", "behandling_lukket")
        }

    override fun validations() =
        River.PacketValidation {
            it.requireKey(
                "@id",
                "vedtaksperiodeId",
                "behandlingId",
            )
        }

    override fun transaksjonellOnPacket(
        packet: JsonMessage,
        outbox: Outbox,
        transaksjon: SessionContext,
        eventMetadata: EventMetadata,
    ) {
        val vedtaksperiodeId = VedtaksperiodeId(packet["vedtaksperiodeId"].asUUID())
        val spleisBehandlingId = SpleisBehandlingId(packet["behandlingId"].asUUID())

        transaksjon.meldingDao.lagre(
            id = packet["@id"].asUUID(),
            json = packet.toJson(),
            meldingtype = MeldingDao.Meldingtype.BEHANDLING_LUKKET,
            vedtaksperiodeId = vedtaksperiodeId.value,
        )

        val vedtak =
            transaksjon.vedtakRepository.finn(spleisBehandlingId) ?: run {
                logg.info("Fant ikke vedtak for $spleisBehandlingId - satser på at det betyr at det ikke er noe å gjøre her.")
                return
            }
        logg.info("Markerer fatting av vedtak for $spleisBehandlingId som behandlet av spleis")
        vedtak.markerSomBehandletAvSpleis()
        transaksjon.vedtakRepository.lagre(vedtak)
    }
}
