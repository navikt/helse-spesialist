package no.nav.helse.kafka

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import no.nav.helse.db.MeldingDao
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.asUUID
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.VarselId
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import java.time.LocalDateTime
import java.util.UUID

class NyeVarslerRiver : TransaksjonellRiver() {
    override fun preconditions(): River.PacketValidation =
        River.PacketValidation {
            it.requireAny("@event_name", listOf("aktivitetslogg_ny_aktivitet", "nye_varsler"))
            it.require("aktiviteter", inneholderVarslerParser)
        }

    override fun validations() =
        River.PacketValidation {
            it.requireKey("@id", "fødselsnummer")
            it.require("@opprettet") { message -> message.asLocalDateTime() }
            it.requireArray("aktiviteter") {
                requireKey("melding", "nivå", "id")
                require("tidsstempel", JsonNode::asLocalDateTime)
                requireArray("kontekster") {
                    requireKey("konteksttype", "kontekstmap")
                }
            }
        }

    private val inneholderVarslerParser: (JsonNode) -> Any = { node ->
        check((node as ArrayNode).any { element -> element.path("varselkode").isTextual }) { "Ingen av elementene har varselkode." }
    }

    override fun transaksjonellOnPacket(
        packet: JsonMessage,
        outbox: Outbox,
        transaksjon: SessionContext,
        eventMetadata: EventMetadata,
    ) {
        transaksjon.meldingDao.lagre(
            id = eventMetadata.`@id`,
            meldingtype = MeldingDao.Meldingtype.NYE_VARSLER,
            json = packet.toJson(),
            vedtaksperiodeId = null,
        )
        val nyeVarsler = packet["aktiviteter"].nyeVarsler()
        nyeVarsler
            .groupBy { it.vedtaksperiodeId }
            .mapNotNull { (vedtaksperiodeId, varsler) ->
                transaksjon.vedtaksperiodeRepository.finn(vedtaksperiodeId)?.let { vedtaksperiode -> vedtaksperiode to varsler }
            }.filterNot { (vedtaksperiode) -> vedtaksperiode.forkastet }
            .forEach { (vedtaksperiode, nyeVarsler) ->
                val nyesteBehandling = transaksjon.behandlingRepository.finnNyesteForVedtaksperiode(vedtaksperiode.id) ?: error("Fant ikke behandling")
                val eksisterendeVarsler = transaksjon.varselRepository.finnVarslerFor(nyesteBehandling.id).associateBy { it.kode }

                val varsler =
                    nyeVarsler
                        .map {
                            Varsel.nytt(
                                id = VarselId(it.id),
                                behandlingUnikId = nyesteBehandling.id,
                                spleisBehandlingId = nyesteBehandling.spleisBehandlingId,
                                kode = it.kode,
                                opprettetTidspunkt = it.opprettet,
                            )
                        }.distinctBy { it.kode }

                val (finnesFraFør, finnesIkkeFraFør) = varsler.partition { it.kode in eksisterendeVarsler.keys }
                transaksjon.varselRepository.lagre(finnesIkkeFraFør)
                finnesFraFør
                    .forEach {
                        val eksisterendeVarsel = eksisterendeVarsler.getValue(it.kode)
                        if (it.erVarselOmAvvik()) {
                            transaksjon.varselRepository.slett(eksisterendeVarsel.id)
                            transaksjon.varselRepository.lagre(it)
                        }
                        if (eksisterendeVarsel.erInaktivt()) {
                            eksisterendeVarsel.reaktiver()
                            transaksjon.varselRepository.lagre(eksisterendeVarsel)
                        }
                    }
            }
    }

    private data class NyttVarsel(
        val id: UUID,
        val kode: String,
        val opprettet: LocalDateTime,
        val vedtaksperiodeId: VedtaksperiodeId,
    )

    private companion object {
        private fun JsonNode.nyeVarsler(): List<NyttVarsel> =
            filter { it["nivå"].asText() == "VARSEL" && it["varselkode"]?.asText() != null }
                .filter { it["kontekster"].any { kontekst -> kontekst["konteksttype"].asText() == "Vedtaksperiode" } }
                .map { jsonNode ->
                    val vedtaksperiodeId =
                        jsonNode["kontekster"]
                            .find { it["konteksttype"].asText() == "Vedtaksperiode" }!!["kontekstmap"]["vedtaksperiodeId"]
                            .asUUID()
                    NyttVarsel(
                        id = jsonNode["id"].asUUID(),
                        kode = jsonNode["varselkode"].asText(),
                        opprettet = jsonNode["tidsstempel"].asLocalDateTime(),
                        vedtaksperiodeId = VedtaksperiodeId(vedtaksperiodeId),
                    )
                }
    }
}
