package no.nav.helse.db

import java.time.LocalDateTime

data class VedtakBegrunnelseMedSaksbehandlerIdentFraDatabase(
    val type: VedtakBegrunnelseTypeFraDatabase,
    val begrunnelse: String,
    val opprettet: LocalDateTime,
    val saksbehandlerIdent: String,
    val invalidert: Boolean,
)
