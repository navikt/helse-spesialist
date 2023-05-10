package no.nav.helse.modell.overstyring

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate
import no.nav.helse.mediator.api.subsumsjonelementer
import no.nav.helse.modell.overstyring.Refusjonselement.Companion.refusjonselementer
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.spesialist.api.overstyring.SubsumsjonDto

internal class OverstyrtArbeidsgiver(
    val organisasjonsnummer: String,
    val månedligInntekt: Double,
    val fraMånedligInntekt: Double,
    val refusjonsopplysninger: List<Refusjonselement>?,
    val fraRefusjonsopplysninger: List<Refusjonselement>?,
    val begrunnelse: String,
    val forklaring: String,
    val subsumsjon: SubsumsjonDto?,
) {
    companion object {
        internal fun JsonNode.arbeidsgiverelementer(): List<OverstyrtArbeidsgiver> {
            return this.map { jsonNode ->
                OverstyrtArbeidsgiver(
                    organisasjonsnummer = jsonNode["organisasjonsnummer"].asText(),
                    månedligInntekt = jsonNode["månedligInntekt"].asDouble(),
                    fraMånedligInntekt = jsonNode["fraMånedligInntekt"].asDouble(),
                    refusjonsopplysninger = jsonNode["refusjonsopplysninger"].refusjonselementer(),
                    fraRefusjonsopplysninger = jsonNode["fraRefusjonsopplysninger"].refusjonselementer(),
                    begrunnelse = jsonNode["begrunnelse"].asText(),
                    forklaring = jsonNode["forklaring"].asText(),
                    subsumsjon = jsonNode["subsumsjon"].subsumsjonelementer()
                )
            }
        }
    }
}

internal class Refusjonselement(
    val fom: LocalDate,
    val tom: LocalDate? = null,
    val beløp: Double
) {
    internal companion object {
        internal fun JsonNode.refusjonselementer(): List<Refusjonselement>? {
            if (this.isNull) return null
            return this.map { jsonNode ->
                Refusjonselement(
                    fom = jsonNode["fom"].asLocalDate(),
                    tom = if (jsonNode["tom"].isNull) null else jsonNode["tom"].asLocalDate(),
                    beløp = jsonNode["beløp"].asDouble()
                )
            }
        }
    }
}