package no.nav.helse.modell.melding

import java.util.UUID

data class VarselEndret(
    val vedtaksperiodeId: UUID,
    // Denne iden brukes av Spaghet for å vite hvilke varsler som ble vurdert i forbindelse med godkjenning av denne behandlingen
    val behandlingIdForBehandlingSomBleGodkjent: UUID,
    val varselId: UUID,
    val varseltittel: String,
    val varselkode: String,
    val forrigeStatus: String,
    val gjeldendeStatus: String,
) : UtgåendeHendelse
