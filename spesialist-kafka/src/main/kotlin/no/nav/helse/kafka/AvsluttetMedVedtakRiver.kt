package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import no.nav.helse.VedtakFattetMeldingBuilder
import no.nav.helse.VedtakFattetMeldingBuilder.Companion.YRKESAKTIVITETSTYPE_SELVSTENDIG_NÆRINGSDRIVENDE
import no.nav.helse.bootstrap.EnvironmentToggles
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.asUUID
import no.nav.helse.spesialist.application.ForsikringsvurderingHenter
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.SpleisBehandlingId

class AvsluttetMedVedtakRiver(
    private val forsikringsvurderingHenter: ForsikringsvurderingHenter,
    private val environmentToggles: EnvironmentToggles,
) : TransaksjonellRiver() {
    private val eventName = "avsluttet_med_vedtak"

    override fun preconditions(): River.PacketValidation =
        River.PacketValidation {
            it.requireValue("@event_name", eventName)
        }

    override fun validations() =
        River.PacketValidation {
            it.requireKey(
                "@id",
                "fødselsnummer",
                "yrkesaktivitetstype",
                "vedtaksperiodeId",
                "behandlingId",
                "vedtakFattetTidspunkt",
                "hendelser",
                "sykepengegrunnlagsfakta",
            )
            it.requireArray("hendelser")
        }

    override fun transaksjonellOnPacket(
        packet: JsonMessage,
        outbox: Outbox,
        transaksjon: SessionContext,
        eventMetadata: EventMetadata,
    ) {
        val spleisBehandlingId = SpleisBehandlingId(packet["behandlingId"].asUUID())
        val identitetsnummer = Identitetsnummer.fraString(packet["fødselsnummer"].asString())
        val vedtakFattetMeldingBuilder =
            VedtakFattetMeldingBuilder(
                identitetsnummer = identitetsnummer,
                sessionContext = transaksjon,
                behandlingId = spleisBehandlingId,
                packet = packet,
                forsikringsvurderingHenter = forsikringsvurderingHenter,
                environmentToggles = environmentToggles,
            )
        val erSelvstendig = packet["yrkesaktivitetstype"].asString() == YRKESAKTIVITETSTYPE_SELVSTENDIG_NÆRINGSDRIVENDE

        val begrunnelseForSkjønnsfastsattSykepengegrunnlag = transaksjon.begrunnelseForSkjønnsfastsettelseRepository.finnBegrunnelseForSkjønnsfastsattSykepengegrunnlag(identitetsnummer)
        val vedtakFattetMelding =
            if (erSelvstendig) {
                vedtakFattetMeldingBuilder.byggVedtakFattetMeldingForSelvstendig()
            } else {
                vedtakFattetMeldingBuilder.byggVedtakFattetMeldingForArbeidstaker(begrunnelseForSkjønnsfastsattSykepengegrunnlag)
            }
        val behandling = transaksjon.behandlingRepository.finn(spleisBehandlingId) ?: error("Finner ikke behandling for spleisBehandlingId $spleisBehandlingId")
        behandling.vedtakFattet()
        transaksjon.behandlingRepository.lagre(behandling)

        outbox.leggTil(
            identitetsnummer = identitetsnummer,
            hendelse = vedtakFattetMelding,
            årsak = eventName,
        )
    }
}
