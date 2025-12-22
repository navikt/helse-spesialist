package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import no.nav.helse.db.MeldingDao
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.asEnum
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.Vedtaksperiode
import no.nav.helse.spesialist.domain.VedtaksperiodeId

class BehandlingOpprettetRiver : TransaksjonellRiver() {
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
        loggInfo(
            "Tar ikke imot behandling_opprettet på grunn av manglende støtte for yrkesaktivitetstype",
            "organisasjonsnummer: $organisasjonsnummer",
        )
    }

    override fun transaksjonellOnPacket(
        packet: JsonMessage,
        outbox: Outbox,
        transaksjon: SessionContext,
        eventMetadata: EventMetadata,
    ) {
        val organisasjonsnummer = packet["organisasjonsnummer"].asText()
        if (organisasjonsnummer in listOf("ARBEIDSLEDIG", "FRILANS", "JORDBRUKER")) {
            behandlerIkke(organisasjonsnummer)
            return
        }
        val identitetsnummer = Identitetsnummer.fraString(packet["fødselsnummer"].asText())
        val vedtaksperiodeId = VedtaksperiodeId(packet["vedtaksperiodeId"].asUUID())
        val spleisBehandlingId = SpleisBehandlingId(packet["behandlingId"].asUUID())

        transaksjon.meldingDao.lagre(
            id = packet["@id"].asUUID(),
            json = packet.toJson(),
            meldingtype = MeldingDao.Meldingtype.BEHANDLING_OPPRETTET,
            vedtaksperiodeId = vedtaksperiodeId.value,
        )

        if (transaksjon.behandlingRepository.finn(spleisBehandlingId) != null) return

        val eksisterendeVedtaksperiode = transaksjon.vedtaksperiodeRepository.finn(vedtaksperiodeId)

        val behandling: Behandling
        if (eksisterendeVedtaksperiode == null) {
            val vedtaksperiode =
                Vedtaksperiode.ny(
                    id = vedtaksperiodeId,
                    identitetsnummer = identitetsnummer,
                    organisasjonsnummer = organisasjonsnummer,
                )
            transaksjon.vedtaksperiodeRepository.lagre(vedtaksperiode)

            behandling =
                Behandling.ny(
                    spleisBehandlingId,
                    vedtaksperiodeId,
                    fom = packet["fom"].asLocalDate(),
                    tom = packet["tom"].asLocalDate(),
                    yrkesaktivitetstype = packet["yrkesaktivitetstype"].asEnum<Yrkesaktivitetstype>(),
                )
        } else {
            if (eksisterendeVedtaksperiode.forkastet) return
            val tidligereBehandling =
                transaksjon.behandlingRepository
                    .finnNyesteForVedtaksperiode(vedtaksperiodeId)
                    ?: error("Fant ikke tidligere behandling for vedtaksperiode")
            behandling =
                Behandling.nyBasertPåTidligereBehandling(
                    spleisBehandlingId,
                    fom = packet["fom"].asLocalDate(),
                    tom = packet["tom"].asLocalDate(),
                    yrkesaktivitetstype = packet["yrkesaktivitetstype"].asEnum<Yrkesaktivitetstype>(),
                    tidligereBehandling = tidligereBehandling,
                )
            val aktiveVarslerForTidligereBehandling =
                transaksjon
                    .varselRepository
                    .finnVarslerFor(tidligereBehandling.id)
                    .filter { it.kanVurderes() }

            if (aktiveVarslerForTidligereBehandling.isNotEmpty()) {
                aktiveVarslerForTidligereBehandling
                    .onEach {
                        it.flyttTil(behandling.id, behandling.spleisBehandlingId)
                    }.also {
                        transaksjon.varselRepository.lagre(it)
                    }
            }
        }
        transaksjon.behandlingRepository.lagre(behandling)
    }
}
