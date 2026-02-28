package no.nav.helse.db

data class VedtakBegrunnelseFraDatabase(
    val type: VedtakBegrunnelseTypeFraDatabase,
    val tekst: String,
)
