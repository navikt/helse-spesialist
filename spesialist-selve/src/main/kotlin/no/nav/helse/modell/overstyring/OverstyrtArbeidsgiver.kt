package no.nav.helse.modell.overstyring

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate
import java.util.UUID
import no.nav.helse.modell.overstyring.Refusjonselement.Companion.refusjonselementer
import no.nav.helse.modell.overstyring.Subsumsjon.Companion.subsumsjonelementer
import no.nav.helse.rapids_rivers.asLocalDate

internal class OverstyrtArbeidsgiver(
    val organisasjonsnummer: String,
    val månedligInntekt: Double,
    val fraMånedligInntekt: Double,
    val refusjonsopplysninger: List<Refusjonselement>?,
    val fraRefusjonsopplysninger: List<Refusjonselement>?,
    val begrunnelse: String,
    val forklaring: String,
    val subsumsjon: Subsumsjon?,
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

internal class SkjønnsfastsattArbeidsgiver(
    val organisasjonsnummer: String,
    val årlig: Double,
    val fraÅrlig: Double?,
    val årsak: String,
    val begrunnelseMal: String?,
    val begrunnelseFritekst: String?,
    val begrunnelseKonklusjon: String?,
    val subsumsjon: Subsumsjon?,
    val initierendeVedtaksperiodeId: UUID?
) {
    companion object {
        internal fun JsonNode.arbeidsgiverelementer(): List<SkjønnsfastsattArbeidsgiver> {
            return this.map { jsonNode ->
                val initierendeVedtaksperiodeIdString = jsonNode["initierendeVedtaksperiodeId"]
                val initierendeVedtaksperiodeId = initierendeVedtaksperiodeIdString.takeUnless { it.isNull || it.isEmpty }?.let { UUID.fromString(it.asText()) }
                    SkjønnsfastsattArbeidsgiver(
                    organisasjonsnummer = jsonNode["organisasjonsnummer"].asText(),
                    årlig = jsonNode["årlig"].asDouble(),
                    fraÅrlig = jsonNode["fraÅrlig"].asDouble(),
                    årsak = jsonNode["årsak"].asText(),
                    begrunnelseMal = jsonNode["begrunnelseMal"].asText(),
                    begrunnelseFritekst = jsonNode["begrunnelseFritekst"].asText(),
                    begrunnelseKonklusjon = jsonNode["begrunnelseKonklusjon"].asText(),
                    subsumsjon = jsonNode["subsumsjon"].subsumsjonelementer(),
                    initierendeVedtaksperiodeId = initierendeVedtaksperiodeId
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

internal class Subsumsjon(
    val paragraf: String,
    val ledd: String? = null,
    val bokstav: String? = null,
) {
    internal companion object {
        internal fun JsonNode.subsumsjonelementer(): Subsumsjon? {
            if (this.isNull) return null
            return Subsumsjon(
                paragraf = this["paragraf"].asText(),
                ledd = if (this["ledd"].isNull) null else this["ledd"].asText(),
                bokstav = if (this["bokstav"].isNull) null else this["bokstav"].asText(),
            )
        }
    }
}
