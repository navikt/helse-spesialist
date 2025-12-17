package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import no.nav.helse.db.MeldingDao
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.asUUID
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.domain.SpleisBehandlingId

class AvsluttetUtenVedtakRiver : TransaksjonellRiver() {
    private val eventName: String = "avsluttet_uten_vedtak"

    override fun preconditions(): River.PacketValidation =
        River.PacketValidation {
            it.requireValue("@event_name", eventName)
        }

    override fun validations() =
        River.PacketValidation {
            it.requireKey("@id", "fødselsnummer", "vedtaksperiodeId", "organisasjonsnummer")
            it.requireKey("skjæringstidspunkt", "behandlingId")
        }

    override fun transaksjonellOnPacket(
        packet: JsonMessage,
        outbox: Outbox,
        transaksjon: SessionContext,
        eventMetadata: EventMetadata,
    ) {
        val spleisBehandlingId = SpleisBehandlingId(packet["behandlingId"].asUUID())
        transaksjon.meldingDao.lagre(
            id = packet["@id"].asUUID(),
            json = packet.toJson(),
            meldingtype = MeldingDao.Meldingtype.AVSLUTTET_UTEN_VEDTAK,
            vedtaksperiodeId = packet["vedtaksperiodeId"].asUUID(),
        )
        val behandling = transaksjon.behandlingRepository.finn(spleisBehandlingId) ?: error("Fant ikke behandling")
        val vedtaksperiode = transaksjon.vedtaksperiodeRepository.finn(behandling.vedtaksperiodeId) ?: error("Fant ikke vedtaksperiode")
        if (vedtaksperiode.forkastet) return

        val varsler = transaksjon.varselRepository.finnVarslerFor(behandling.id)
        if (varsler.isEmpty()) {
            behandling.avsluttetUtenVedtak()
        } else {
            behandling.avsluttetUtenVedtakMedVarsler()
        }
        transaksjon.behandlingRepository.lagre(behandling)
    }
}
