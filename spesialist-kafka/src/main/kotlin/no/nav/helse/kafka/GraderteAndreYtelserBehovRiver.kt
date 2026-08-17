package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import no.nav.helse.db.SessionContext
import no.nav.helse.mediator.asLocalDate
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Periode
import no.nav.helse.spesialist.kafka.objectMapper
import tools.jackson.databind.node.ObjectNode

class GraderteAndreYtelserBehovRiver : TransaksjonellRiver() {
    private val eventName = "behov"
    private val behovNavn = "GraderteAndreYtelserForBeregning"

    override fun preconditions(): River.PacketValidation =
        River.PacketValidation {
            it.requireValue("@event_name", eventName)
            it.requireAll("@behov", listOf(behovNavn))
            it.forbid("@løsning")
        }

    override fun validations() =
        River.PacketValidation {
            it.requireKey(
                "@id",
                "fødselsnummer",
                "$behovNavn.fom",
                "$behovNavn.tom",
            )
        }

    override fun transaksjonellOnPacket(
        packet: JsonMessage,
        outbox: Outbox,
        transaksjon: SessionContext,
        eventMetadata: EventMetadata,
    ) {
        val fom = packet["$behovNavn.fom"].asLocalDate()
        val tom = packet["$behovNavn.tom"].asLocalDate()
        val identitetsnummer = Identitetsnummer.fraString(packet["fødselsnummer"].asString())
        val forespurtPeriode = Periode(fom, tom)

        val løsning =
            transaksjon.graderteAndreYtelserRepository
                .finnAlleForIdentitetsnummer(identitetsnummer)
                .asSequence()
                .filterNot { it.fjernet }
                .flatMap { graderteAndreYtelser ->
                    graderteAndreYtelser.perioder
                        .asSequence()
                        .filter { it.periode overlapper forespurtPeriode }
                        .map { periode ->
                            mapOf(
                                "graderteAndreYtelserType" to graderteAndreYtelser.graderteAndreYtelserType.name,
                                "fom" to maxOf(periode.periode.fom, forespurtPeriode.fom),
                                "tom" to minOf(periode.periode.tom, forespurtPeriode.tom),
                                "grad" to periode.grad,
                            )
                        }
                }.sortedWith(
                    compareBy<Map<String, Any?>>(
                        { it.getValue("graderteAndreYtelserType") as String },
                        { it.getValue("fom").toString() },
                        { it.getValue("tom").toString() },
                        { it.getValue("grad") as Int },
                    ),
                ).toList()

        val løstPacket =
            (objectMapper.readTree(packet.toJson()) as ObjectNode).also { original ->
                original.putObject("@løsning").set(behovNavn, objectMapper.valueToTree(løsning))
            }

        outbox.leggTil(
            identitetsnummer = identitetsnummer,
            packet = løstPacket.toString(),
            årsak = behovNavn,
        )
    }
}
