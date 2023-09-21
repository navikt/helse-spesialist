package no.nav.helse.db

import java.util.UUID

data class OppgaveFraDatabase(
    val id: Long,
    val egenskap: String,
    val egenskaper: List<String>,
    val status: String,
    val vedtaksperiodeId: UUID,
    val utbetalingId: UUID,
    val hendelseId: UUID,
    val ferdigstiltAvIdent: String? = null,
    val ferdigstiltAvOid: UUID? = null,
    val tildelt: SaksbehandlerFraDatabase? = null,
    val påVent: Boolean = false
)