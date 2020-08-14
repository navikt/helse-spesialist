package no.nav.helse.mediator.kafka.meldinger

import java.util.*

data class OverstyringMessage(
    val saksbehandlerOid: UUID,
    val saksbehandlerEpost: String,
    val organisasjonsnummer: String,
    val fødselsnummer: String,
    val aktørId: String,
    val begrunnelse: String,
    val dager: String,
    val unntaFraInnsyn: Boolean
)
