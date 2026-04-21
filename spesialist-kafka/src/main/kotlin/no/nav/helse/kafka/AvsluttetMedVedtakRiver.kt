package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import no.nav.helse.VedtakFattetMeldingBuilder
import no.nav.helse.VedtakFattetMeldingBuilder.Companion.YRKESAKTIVITETSTYPE_SELVSTENDIG_NÆRINGSDRIVENDE
import no.nav.helse.bootstrap.EnvironmentToggles
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.melding.VedtakFattetMelding
import no.nav.helse.modell.person.vedtaksperiode.LegacyVedtaksperiode.Companion.finnBehandling
import no.nav.helse.spesialist.application.ForsikringHenter
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.SpleisBehandlingId

class AvsluttetMedVedtakRiver(
    private val forsikringHenter: ForsikringHenter,
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
        val identitetsnummer = Identitetsnummer.fraString(packet["fødselsnummer"].asText())
        val vedtakFattetMeldingBuilder =
            VedtakFattetMeldingBuilder(
                identitetsnummer = identitetsnummer,
                sessionContext = transaksjon,
                behandlingId = spleisBehandlingId,
                packet = packet,
                forsikringHenter = forsikringHenter,
                environmentToggles = environmentToggles,
            )
        val erSelvstendig = packet["yrkesaktivitetstype"].asText() == YRKESAKTIVITETSTYPE_SELVSTENDIG_NÆRINGSDRIVENDE

        lateinit var vedtakFattetMelding: VedtakFattetMelding
        transaksjon.legacyPersonRepository.brukPersonHvisFinnes(identitetsnummer.value) {
            vedtakFattetMelding =
                if (erSelvstendig) {
                    vedtakFattetMeldingBuilder.byggVedtakFattetMeldingForSelvstendig()
                } else {
                    vedtakFattetMeldingBuilder.byggVedtakFattetMeldingForArbeidstaker(this.skjønnsfastsatteSykepengegrunnlag)
                }
            val vedtaksperiode =
                vedtaksperioder().finnBehandling(spleisBehandlingId.value)
                    ?: error("Behandling med spleisBehandlingId=$spleisBehandlingId finnes ikke")
            val behandling = vedtaksperiode.finnBehandling(spleisBehandlingId.value)
            behandling.håndterVedtakFattet()
        }
        outbox.leggTil(
            identitetsnummer = identitetsnummer,
            hendelse = vedtakFattetMelding,
            årsak = eventName,
        )
    }
}
