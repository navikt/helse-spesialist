package no.nav.helse.spesialist.api

import java.util.UUID

class SaksbehandlerTilganger(
    private val gruppetilganger: List<UUID>,
    private val saksbehandlerIdent: String,
    private val kode7Saksbehandlergruppe: UUID,
    private val riskSaksbehandlergruppe: UUID,
    private val beslutterSaksbehandlergruppe: UUID,
    private val skjermedePersonerSaksbehandlergruppe: UUID,
    private val saksbehandlereMedTilgangTilStikkprøve: List<String>,
    private val saksbehandlereMedTilgangTilSpesialsaker: List<String> = emptyList(),
) {

    fun harTilgangTilKode7() = kode7Saksbehandlergruppe in gruppetilganger

    fun harTilgangTilRiskOppgaver() = riskSaksbehandlergruppe in gruppetilganger

    fun harTilgangTilBeslutterOppgaver() = beslutterSaksbehandlergruppe in gruppetilganger

    fun harTilgangTilSkjermedePersoner() = skjermedePersonerSaksbehandlergruppe in gruppetilganger
    fun hartilgangTilStikkprøve() = saksbehandlerIdent in saksbehandlereMedTilgangTilStikkprøve || erDev()
    fun hartilgangTilSpesialsaker() = saksbehandlerIdent in saksbehandlereMedTilgangTilSpesialsaker || erDev()
}
