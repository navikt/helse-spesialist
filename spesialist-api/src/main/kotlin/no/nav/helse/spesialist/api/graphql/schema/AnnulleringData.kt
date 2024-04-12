package no.nav.helse.spesialist.api.graphql.schema

data class AnnulleringData(
    val aktorId: String,
    val fodselsnummer: String,
    val organisasjonsnummer: String?,
    val vedtaksperiodeId: String?,
    val utbetalingId: String?,
    val begrunnelser: List<String>,
    val kommentar: String?,
)
