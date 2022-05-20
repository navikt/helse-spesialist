package no.nav.helse

import java.util.UUID

class SaksbehandlerTilganger(
    private val gruppetilganger: List<UUID>,
    private val kode7Saksbehandlergruppe: UUID,
    private val riskSaksbehandlergruppe: UUID,
    private val beslutterSaksbehandlergruppe: UUID
) {

    fun harTilgangTilKode7Oppgaver() = kode7Saksbehandlergruppe in gruppetilganger

    fun harTilgangTilRiskOppgaver() = riskSaksbehandlergruppe in gruppetilganger

    fun harTilgangTilBeslutterOppgaver() = beslutterSaksbehandlergruppe in gruppetilganger

}
