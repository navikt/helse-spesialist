package no.nav.helse.mediator.kafka.meldinger

import java.time.LocalDate
import java.util.*

class OverstyringMessage(
    val saksbehandlerOid: UUID,
    val saksbehandlerEpost: String,
    val organisasjonsnummer: String,
    val fødselsnummer: String,
    val aktørId: String,
    val begrunnelse: String,
    val dager: List<OverstyringMessageDag>,
    val unntaFraInnsyn: Boolean
) {
    class OverstyringMessageDag(
        val dato: LocalDate,
        val type: String,
        val grad: Int?
    )
}
