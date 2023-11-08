package no.nav.helse.modell.varsel

import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spesialist.api.varsel.Varsel
import org.slf4j.LoggerFactory

internal class Varselmelder(private val rapidsConnection: RapidsConnection) {

    private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

    fun meldVarselEndret(
        fødselsnummer: String,
        behandlingId: UUID?,
        vedtaksperiodeId: UUID,
        varselId: UUID,
        varseltittel: String,
        varselkode: String,
        forrigeStatus: Varsel.Varselstatus,
        gjeldendeStatus: Varsel.Varselstatus,
    ) {
        val message = JsonMessage.newMessage(
            "varsel_endret", mutableMapOf(
                "fødselsnummer" to fødselsnummer,
                "vedtaksperiode_id" to vedtaksperiodeId,
                "varsel_id" to varselId,
                "varseltittel" to varseltittel,
                "varselkode" to varselkode,
                "forrige_status" to forrigeStatus.name,
                "gjeldende_status" to gjeldendeStatus.name
            ).apply {
                behandlingId?.let { put("behandling_id" , behandlingId) }
            }
        )
        sikkerlogg.info(
            "Publiserer varsel_endret for varsel med {}, {}, {}",
            kv("varselId", varselId),
            kv("varselkode", varselkode),
            kv("status", gjeldendeStatus)
        )
        rapidsConnection.publish(fødselsnummer, message.toJson())
    }
}
