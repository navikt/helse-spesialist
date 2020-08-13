package no.nav.helse.mediator.kafka.meldinger

data class OverstyringMessage(
    val organisasjonsnummer: String,
    val fødselsnummer: String,
    val aktørId: String,
    val begrunnelse: String,
    val dager: String,
    val unntaFraInnsyn: Boolean
)
