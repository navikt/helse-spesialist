package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.db.MeldingDao
import no.nav.helse.db.SessionContext
import no.nav.helse.db.SessionFactory
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.vedtaksperiode.BehandlingOpprettet
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.domain.SpleisBehandlingId

class BehandlingOpprettetRiver(
    private val mediator: MeldingMediator,
    private val sessionFactory: SessionFactory,
) : SpesialistRiver {
    override fun preconditions(): River.PacketValidation =
        River.PacketValidation {
            it.requireValue("@event_name", "behandling_opprettet")
        }

    override fun validations() =
        River.PacketValidation {
            it.requireKey(
                "@id",
                "vedtaksperiodeId",
                "behandlingId",
                "fødselsnummer",
                "organisasjonsnummer",
                "yrkesaktivitetstype",
            )
            it.requireKey("fom", "tom")
        }

    fun behandlerIkke(organisasjonsnummer: String) {
        logg.info("Tar ikke imot behandling opprettet for: $organisasjonsnummer")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val organisasjonsnummer = packet["organisasjonsnummer"].asText()
        if (organisasjonsnummer in listOf("ARBEIDSLEDIG", "FRILANS")) {
            behandlerIkke(organisasjonsnummer)
            return
        }
        val vedtaksperiodeId = packet["vedtaksperiodeId"].asUUID()
        val spleisBehandlingId = SpleisBehandlingId(packet["behandlingId"].asUUID())

        val fom = packet["fom"].asLocalDate()
        val tom = packet["tom"].asLocalDate()
        val yrkesaktivitetstype = Yrkesaktivitetstype.valueOf(packet["yrkesaktivitetstype"].asText())

        sessionFactory.wrapOgDeleger(
            messageContext = context,
            meldingnavn = "behandling_opprettet",
            meldingtype = MeldingDao.Meldingtype.BEHANDLING_OPPRETTET,
            vedtaksperiodeId = vedtaksperiodeId,
            packet = packet,
        ) {
            val gjeldendeBehandling =
                this.behandlingRepository.finnNyeste(vedtaksperiodeId) ?: return@wrapOgDeleger nyVedtaksperiode(packet)

            val nyGjeldendeBehandling =
                gjeldendeBehandling.lagNyGjeldendeBehandlingFra(
                    spleisBehandlingId = spleisBehandlingId,
                    fom = fom,
                    tom = tom,
                    yrkesaktivitetstype = yrkesaktivitetstype,
                )

            this.behandlingRepository.lagre(nyGjeldendeBehandling)
        }

        mediator.mottaMelding(
            BehandlingOpprettet(
                id = packet["@id"].asUUID(),
                fødselsnummer = packet["fødselsnummer"].asText(),
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                spleisBehandlingId = spleisBehandlingId.value,
                fom = packet["fom"].asLocalDate(),
                tom = packet["tom"].asLocalDate(),
                yrkesaktivitetstype = yrkesaktivitetstype,
                json = packet.toJson(),
            ),
            MessageContextMeldingPubliserer(context),
        )
    }

    private fun SessionContext.nyVedtaksperiode(packet: JsonMessage) {
        this.vedtaksperiodeRepository.lagreVedtaksperioder()
    }
}
