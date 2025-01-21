package no.nav.helse.modell.melding

import java.util.UUID

data class VarselEndret(
    val vedtaksperiodeId: UUID,
    val behandlingId: UUID,
    val varselId: UUID,
    val varseltittel: String,
    val varselkode: String,
    val forrigeStatus: String,
    val gjeldendeStatus: String,
) : UtgåendeHendelse
