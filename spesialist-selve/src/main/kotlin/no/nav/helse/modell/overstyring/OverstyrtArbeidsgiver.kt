package no.nav.helse.modell.overstyring

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.mediator.api.refusjonselementer
import no.nav.helse.mediator.api.subsumsjonelementer
import no.nav.helse.spesialist.api.overstyring.RefusjonselementDto
import no.nav.helse.spesialist.api.overstyring.SubsumsjonDto

internal class OverstyrtArbeidsgiver(
    val organisasjonsnummer: String,
    val månedligInntekt: Double,
    val fraMånedligInntekt: Double,
    val refusjonsopplysninger: List<RefusjonselementDto>?,
    val fraRefusjonsopplysninger: List<RefusjonselementDto>?,
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