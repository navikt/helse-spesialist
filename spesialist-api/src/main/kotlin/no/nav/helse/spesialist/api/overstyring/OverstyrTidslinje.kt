package no.nav.helse.spesialist.api.overstyring

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate
import java.util.UUID
import no.nav.helse.spesialist.api.saksbehandler.Saksbehandler
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerHendelse

@JsonIgnoreProperties
class OverstyrTidslinje(
    private val aktørId: String,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private val begrunnelse: String,
    private val dager: List<Overstyringdag>,
    private val saksbehandlerOid: UUID
): SaksbehandlerHendelse {
    @JsonIgnoreProperties
    class Overstyringdag(
        val dato: LocalDate,
        val type: String,
        val fraType: String,
        val grad: Int?,
        val fraGrad: Int?
    ) {
        fun toJson(): Map<String, Any> {
            return mutableMapOf<String, Any>(
                "dato" to dato,
                "type" to type,
                "fraType" to fraType
            ).apply {
                compute("grad") { _, _ -> grad }
                compute("fraGrad") { _, _ -> fraGrad }
            }
        }
    }

    override fun tellernavn(): String = "overstyr_tidslinje"

    override fun saksbehandlerOid(): UUID = saksbehandlerOid

    override fun håndter(saksbehandler: Saksbehandler) {
        saksbehandler.overstyrTidslinje(aktørId, fødselsnummer, organisasjonsnummer, dager, begrunnelse)
    }
}